package org.ericghara;

import org.ericghara.exception.FailedDirCreationException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.ericghara.SizeUnit.MB;

/**
 * This creates a dummy Movie dir from a csv file -- see Example.csv for composition details.
 * This is intended for testing methods that implement filesystem I/O operations.  All files
 * are dummies that are just written with 0's to the specified file size.
 */
public class TestDir {

    // match all text after # and all leading whitespace
    private final String hashCommentsRegEx = "(\\b|^)\\s*#.*$";
    private final Matcher hashComments = compileMatcher(hashCommentsRegEx);
    // match whitespace with 0 or even number of " ahead
    private final String whitespaceNotInQuotesRegEx = "\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
    private final Matcher whitespaceNotInQuotes = compileMatcher(whitespaceNotInQuotesRegEx);
    // match text between quotes which appear at the beginning and end of the line
    private final String textInQuotesRegEx = "(?<=^\").+(?=\"$)";
    private final Matcher textInQuotes = compileMatcher(textInQuotesRegEx);
            
    private final Path testDir; // parent which all relative paths in csv are resolved against
    private final LinkedList<TestFile> files = new LinkedList<>(); // all files successfully written
    private final LinkedList<Path> dirs = new LinkedList<>(); // all dirs successfully written

    /**
     * Initializes a TestDir from a csv template. The location of the movie dir is defined by the {@code dirPath} argument.
     *
     * @param csvFile the csv file
     * @param dirPath absolute path to the desired directory
     * @see TestDir#getResourceFile
     */
    public TestDir(File csvFile, Path dirPath) {
        this(dirPath);
        Scanner csvScanner = getFileScanner(csvFile);
        parse(csvScanner);
    }

    public TestDir(String csvStr, Path testDir) {
        this(testDir);
        Scanner csvScanner = getStringScanner(csvStr);
        parse(csvScanner);
    }

    public TestDir(Path testDir) {
        mustBeDir(testDir);
        this.testDir = testDir;
    }

    public Path getTestDir() {
        return testDir;
    }

    public Set<TestFile> getFiles() {
        return Set.copyOf(files);
    }

    public Set<Path> getDirs() {
        return Set.copyOf(dirs);
    }

    /**
     * Convenience function to get a resource file. Intended to be used
     * with the {@code TestDir(File, Path)} constructor.<br><br>
     *
     * Usage:
     *
     * <pre>
     *     TestDir.getResourceFile(this, "aFile.csv");
     * </pre>
     *
     * @param aThis class instance to get resources from
     * @param csvName name of resource
     * @return resource file
     * @throws IllegalArgumentException If resource cannot be retrieved
     *
     */
    public static File getResourceFile(Object aThis, String csvName) throws IllegalArgumentException {
        File csv;
        try {
            URI csvPath = aThis.getClass().getResource(csvName).toURI();
            csv = new File(csvPath);
        } catch (Exception e) {
            throw new IllegalArgumentException("Couldn't open the csv " + csvName, e);
        }
        return csv;
    }

    /**
     * Creates file of specified size at the given path.  The path if absolute must be within {@code testDir}
     * Any new directories required to complete the file path are created.
     * @param pathString file path
     * @param size size of the file to create in {@code unit}
     * @param unit the units which size was provided in
     * @return the absolute path to the file
     * @see org.ericghara.TestDir#createFile(Path, BigDecimal, SizeUnit)
     */
    public TestFile createFile(String pathString, BigDecimal size, SizeUnit unit) {
        Path path = Paths.get(pathString);
        return createFile(path, size, unit);
    }

    /**
     * Creates file of specified size at the given path.  The path if absolute must be within {@code testDir}
     * Any new directories required to complete the file path are created.
     * @param path file path
     * @param size size of the file to create in {@code unit}
     * @param unit the units which size was provided in
     * @return the absolute path to the file
     * @see org.ericghara.TestDir#createFile(String, BigDecimal, SizeUnit)
     */
    public TestFile createFile(Path path, BigDecimal size, SizeUnit unit) throws IllegalArgumentException {
        validatePath(path);
        Path filePath = testDir.resolve(path);
        Path parentPath = filePath.getParent();
        if (Files.notExists(parentPath, LinkOption.NOFOLLOW_LINKS)) {
            createDirs(parentPath);
        }
        try {
            var file = new RandomFile(filePath, size, unit);
            files.addLast(file);
            return file;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create the file:" + filePath + ".", e);
        }
    }

    /**
     * Creates a directory at the given path.  If the path is absolute, it must be within the {@code TestDir}.
     * If the parent path to the directory does not yet exist, directories are created in order to complete the path.<br><br>
     * @param pathString path to the directory to create (may be relative or absolute)
     * @return absolute path to the created directory
     * @throws IllegalArgumentException if {@code pathString} is not within the {@code TestDir}
     * @throws FailedDirCreationException if there is any error creating the dirs
     */
    public Path createDirs(String pathString) throws IllegalArgumentException, FailedDirCreationException {
        Path path = Paths.get(pathString);
        return createDirs(path);
    }

    /**
     * Creates a directory at the given path.  If the path is absolute, it must be within the {@code TestDir}.
     * If the parent path to the directory does not yet exist, directories are created in order to complete the path.<br><br>
     * @param path path to the directory to create (may be relative or absolute)
     * @return absolute path to the created directory
     * @throws IllegalArgumentException if {@code path} is not within the {@code TestDir}
     * @throws FailedDirCreationException if there is any error creating the dirs
     */
    public Path createDirs(Path path) throws IllegalArgumentException, FailedDirCreationException {
        validatePath(path);
        Path absPath = testDir.resolve(path);
        try {
            Files.createDirectories(absPath);
        } catch (Exception e) {
            throw new FailedDirCreationException(absPath.toString(), e);
        }
        dirs.addLast(absPath);
        return absPath;
    }

    // Parse the csv, writing the dir/file structure to disk
    void parse(Scanner csvScanner) {
        for (int i = 0; csvScanner.hasNextLine(); i++) {
            String line = hashComments.reset(csvScanner.nextLine())
                                      .replaceAll(""); // strip comments
            String[] splitLine = whitespaceNotInQuotes.pattern()
                                                      .split(line); // split columns
            if (splitLine.length >= 2 &&
                    textInQuotes.reset(splitLine[1])
                                .find() ) {
                splitLine[1] = textInQuotes.group(); // remove quotes
            }
            if (splitLine.length == 1 && splitLine[0].isEmpty()) { // ignore empty line (or a stripped comment line)
                continue;
            }
            if (splitLine[0].equalsIgnoreCase("D") && splitLine.length == 2 ) { // directory
                createDirs(splitLine[1]);
            }
            else if (splitLine[0].equalsIgnoreCase("F") && splitLine.length == 3) { // file
                String pathString = splitLine[1];
                BigDecimal sizeMB = new BigDecimal(splitLine[2]);
                createFile(pathString, sizeMB, MB);
            }
            else {
                throw new IllegalArgumentException("Couldn't parse line: " + i + ".");
            }
        }
        csvScanner.close();
    }

    Scanner getFileScanner(File file) {
        Scanner scanner;
        try {
            scanner = new Scanner(file);
        } catch (Exception e) {
            throw new IllegalArgumentException("Couldn't open the csv " + file, e);
        }
        return scanner;
    }

    Scanner getStringScanner(String csv) {
        return new Scanner(csv);
    }

    static Matcher compileMatcher(String regEx) {
        return Pattern.compile(regEx).matcher("");
    }

    void validatePath(Path path) throws IllegalArgumentException {
        if (path.isAbsolute() && !path.startsWith(testDir) ) {
            throw new IllegalArgumentException("The specified path is not within the TestDir. " + path);
        }
    }

    void mustBeAbsolute(Path path) {
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("Received a path that was not absolute " + path + ".");
        }
    }

    void mustBeDir(Path path) {
        mustBeAbsolute(path);
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) ) {
            throw new IllegalArgumentException("Expected an existing directory but it does not exist: "
                    + path + ".");
        }
    }

    /*Simple test, will write the file tree specified in the given csv to the
     /tmp/XXX dir (the exact path is printed to std out)*/
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Improper usage, provide 1 argument " +
                    "- the name of the csv file resource");
        }
        // Note this dir will persist after exit
        Path tempDir = Files.createTempDirectory("TestMovieDir");
        TestDir tmd = new TestDir(args[0], tempDir);
        System.out.printf("A test movie dir was successfully created at: %s%n", tmd.getTestDir() );
    }
}