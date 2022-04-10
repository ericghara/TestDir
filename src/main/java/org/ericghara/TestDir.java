package org.ericghara;

import org.ericghara.exception.DirCreationException;
import org.ericghara.exception.FileCreationException;
import org.ericghara.write.ByteSupplier;
import org.ericghara.write.FileWriter;
import org.ericghara.write.RandomByteSupplier;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is intended for testing methods that implement filesystem I/O operations.  All files
 * are written with random bytes.
 */
public class TestDir {

    private final Path testDir; // parent which all relative paths in csv are resolved against
    private final Set<Path> files = new HashSet<>(); // all files successfully written
    private final LinkedList<Path> dirs = new LinkedList<>(); // all dirs successfully written

    private ByteSupplier byteSupplier;

    /**
     * Creates a TestDir instance in the specified directory and
     * configured to use the provided ByteSupplier {@link ByteSupplier}.
     *
     * @param dirPath absolute path to this {@code TestDir}.  Directory <em>must</em> exist
     * @param byteSupplier the source of data for file writes
     * @see TestDir#setByteSupplier(ByteSupplier)
     * @see TestDir#TestDir(Path, ByteSupplier)
     * @throws IllegalArgumentException if either argument is null or {@code dirPath} is not a directory
     */
    public TestDir(Path dirPath, ByteSupplier byteSupplier) throws IllegalArgumentException {
        mustBeDir(dirPath);
        this.testDir = dirPath;
        this.byteSupplier = byteSupplier;
    }

    /**
     * Creates a TestDir instance in the specified directory.  Unless modified, {@link RandomByteSupplier}
     * will supply data for all files created.
     *
     * @param dirPath absolute path to this {@code TestDir}.  Directory <em>must</em> exist
     * @see TestDir#setByteSupplier(ByteSupplier)
     * @see TestDir#TestDir(Path, ByteSupplier)
     * @throws IllegalArgumentException if {@code dirPath} is null or is not a directory
     */
    public TestDir(Path dirPath) throws IllegalArgumentException {
        this(dirPath, new RandomByteSupplier() );
    }

    public Path getTestDir() {
        return testDir;
    }

    public void setByteSupplier(ByteSupplier byteSupplier) {
        if (Objects.isNull(byteSupplier) ) {
            throw new IllegalArgumentException("Received a null ByteSupplier");
        }
        this.byteSupplier = byteSupplier;
    }

    public ByteSupplier getByteSupplier() {
        return byteSupplier;
    }

    /**
     * Paths of all files created by this {@link TestDir} instance
     * @return {@link Set} of file {@link Path}s
     */
    public Set<Path> getFilePaths() {
        return Set.copyOf(files);
    }

    /**
     * All files created by this {@link TestDir} instance
     * @return {@link Set} of {@link Path}s
     */
    public Set<Path> getFiles() {
        return Set.copyOf(files);
    }

    /**
     * Searches for a {@link Path} matching the provided {@code query}
     * in this {@code TestDir}.  If a relative path is provided
     * it is resolved with the path to this {@link TestDir} to
     * create an absolute path.  If an absolute path is provided it is
     * used as is for the search.
     *
     * @param query a relative or absolute path to the file
     * @return the absolute {@link Path} corresponding to the {@code query} or {@code null}
     */
    public Path getFile(Path query) {
        try {
            validatePath(query);
        } catch(IllegalArgumentException e) {
            return null;
        }
        Path absPath = testDir.resolve(query);
        return files.contains(absPath) ? absPath :
                null;
    }

    /**
     * Searches for a {@link Path} matching the provided {@code query}
     * in this {@code TestDir}.  The query is converted to a {@link Path}.
     * If a relative path is provided it is resolved with the path to this {@link TestDir} to
     * create an absolute path.  If an absolute path is provided it is
     * used as is for the search.
     *
     * @param query {@link String} representation of a relative or absolute path to the file
     * @return the absolute {@link Path} corresponding to the {@code query} or {@code null}
     */
    public Path getFile(String query) {
        Path path = Paths.get(query);
        return getFile(path);
    }


    /**
     * If the given path represents a relative or
     * absolute path to a directory created by this {@link TestDir}
     * instance, the absolute path to the directory is returned.
     * @param path a relative or absolute path to the dir
     * @return the absolute {@link Path} of the dir or {@code null}
     */
    public Path getDir(Path path) {
        try {
            validatePath(path);
        } catch(IllegalArgumentException e) {
            return null;
        }
        Path absPath = testDir.resolve(path);
        return dirs.contains(absPath) ?
                absPath : null;
    }

    /**
     * If the given path represents a relative or
     * absolute path to a directory created by this {@link TestDir}
     * instance the absolute path to the directory is returned.
     * @param pathStr a relative or absolute path to the dir
     * @return the absolute {@link Path} of the dir or {@code null}
     */
    public Path getDir(String pathStr) {
        Path path = Paths.get(pathStr);
        return getDir(path);
    }

    public Set<Path> getDirPaths() {
        return Set.copyOf(dirs);
    }

    /**
     * Creates file of specified size at the given path.  The path if absolute must be within {@code testDir}
     * Any new directories required to complete the file path are created.
     * @param pathString file path
     * @param size size of the file to create in {@code unit}
     * @param unit the units which size was provided in
     * @return the {@link java.io.File} created
     * @throws FileCreationException if there are any errors creating or writing to the file
     * @see org.ericghara.TestDir#createFile(Path, BigDecimal, SizeUnit)
     */
    public Path createFile(String pathString, BigDecimal size, SizeUnit unit) throws FileCreationException {
        Path path = Paths.get(pathString);
        return createFile(path, size, unit);
    }

    /**
     * Creates file of specified size at the given path.  The path if absolute must be within {@code testDir}
     * Any new directories required to complete the file path are created.
     * @param path file path
     * @param size size of the file to create in {@code unit}
     * @param unit the units which size was provided in
     * @return the {@link Path} of the created file
     * @throws FileCreationException if there are any errors creating or writing to the file
     * @see org.ericghara.TestDir#createFile(String, BigDecimal, SizeUnit)
     */
    public Path createFile(Path path, BigDecimal size, SizeUnit unit) throws FileCreationException {
        validatePath(path);
        Path absPath = testDir.resolve(path);
        Path parentPath = absPath.getParent();
        if (Files.notExists(parentPath, LinkOption.NOFOLLOW_LINKS)) {
            createDirs(parentPath);
        }
        if (Files.exists(absPath) ) {
            throw new FileCreationException("The specified file already exists: " + path);
        }
        try {
            new FileWriter(absPath).create(size, unit, byteSupplier);
            files.add(absPath);
            return absPath;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create the file:" + absPath + ".", e);
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