package org.ericghara;

import org.ericghara.exception.DirCreationException;
import org.ericghara.exception.FileCreationException;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Set;

/**
 * This is intended for testing methods that implement filesystem I/O operations.  All files
 * are written with random bytes.
 */
public class TestDir {

    private final Path testDir; // parent which all relative paths in csv are resolved against
    private final LinkedList<TestFile> files = new LinkedList<>(); // all files successfully written
    private final LinkedList<Path> dirs = new LinkedList<>(); // all dirs successfully written

    /**
     *
     * @param dirPath absolute path to this {@code TestDir}
     */
    public TestDir(Path dirPath) {
        mustBeDir(dirPath);
        this.testDir = dirPath;
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
     * Creates file of specified size at the given path.  The path if absolute must be within {@code testDir}
     * Any new directories required to complete the file path are created.
     * @param pathString file path
     * @param size size of the file to create in {@code unit}
     * @param unit the units which size was provided in
     * @return the absolute path to the file
     * @throws FileCreationException if there are any errors creating or writing to the file
     * @see org.ericghara.TestDir#createFile(Path, BigDecimal, SizeUnit)
     */
    public TestFile createFile(String pathString, BigDecimal size, SizeUnit unit) throws FileCreationException {
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
     * @throws FileCreationException if there are any errors creating or writing to the file
     * @see org.ericghara.TestDir#createFile(String, BigDecimal, SizeUnit)
     */
    public TestFile createFile(Path path, BigDecimal size, SizeUnit unit) throws FileCreationException {
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
     * @throws DirCreationException if there is any error creating the dirs
     */
    public Path createDirs(String pathString) throws IllegalArgumentException, DirCreationException {
        Path path = Paths.get(pathString);
        return createDirs(path);
    }

    /**
     * Creates a directory at the given path.  If the path is absolute, it must be within the {@code TestDir}.
     * If the parent path to the directory does not yet exist, directories are created in order to complete the path.<br><br>
     * @param path path to the directory to create (may be relative or absolute)
     * @return absolute path to the created directory
     * @throws IllegalArgumentException if {@code path} is not within the {@code TestDir}
     * @throws DirCreationException if there is any error creating the dirs
     */
    public Path createDirs(Path path) throws IllegalArgumentException, DirCreationException {
        validatePath(path);
        Path absPath = testDir.resolve(path);
        try {
            Files.createDirectories(absPath);
        } catch (Exception e) {
            throw new DirCreationException(absPath.toString(), e);
        }
        dirs.addLast(absPath);
        return absPath;
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
}