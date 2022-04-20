package org.ericghara.core;

import org.ericghara.exception.DirCreationException;
import org.ericghara.exception.FileCreationException;
import org.ericghara.exception.WriteFailureException;
import org.ericghara.write.FileWriter;
import org.ericghara.write.bytesupplier.ByteSupplier;
import org.ericghara.write.bytesupplier.RandomByteSupplier;

import java.math.BigDecimal;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This is intended for testing methods that implement filesystem I/O operations.  All files
 * are written with random bytes.
 */
public class TestDir {

    private final Path dirPath; // parent which all relative paths in csv are resolved against
    private final FileSystem fileSystem;
    private final Set<Path> files = new HashSet<>(); // all files successfully written
    private final Set<Path> dirs = new HashSet<>(); // all dirs successfully written

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
        this.dirPath = dirPath;
        this.fileSystem = dirPath.getFileSystem();
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

    /**
     * Returns a builder useful for configuring a new {@code TestDir}
     *
     * @return a {@link TestDirBuilder}
     */
    public static TestDirBuilder builder() {
        return new TestDirBuilder();
    }

    /**
     * Path of this {@code TestDir}.  All files and folders
     * created by this {@code TestDir} instance are children of
     * the returned path.
     * @return {@link Path} of this {@code TestDir} instance
     */
    public Path getPath() {
        return dirPath;
    }

    /**
     * The {@link FileSystem} used by this {TestDir}.<br><br>
     *
     * {@code TestDir}s are designed to use non-default fileSystems.
     * This method provides access to the FileSystem used by this TestDir.<br><br>
     *
     * Example:<br>
     * <pre>
     *     Path onNonDefaultFs = ...;
     *     TestDir testDir = new TestDir(onNonDefaultFs);
     *     FileSystem fs = testDir.getFileSystem();
     *     Path query = fs.getPath("aDir");
     *     Path aDir = testDir.getDir(query);
     * </pre>
     *
     * @return {@link FileSystem} of this {@code TestDir}
     */
    public FileSystem getFileSystem() {
        return fileSystem;
    }

    /**
     * @param byteSupplier the {@link ByteSupplier} to use.
     * @throws IllegalArgumentException if the {@code byteSupplier} is null
     */
    public void setByteSupplier(ByteSupplier byteSupplier) throws IllegalArgumentException {
        if (Objects.isNull(byteSupplier) ) {
            throw new IllegalArgumentException("Received a null ByteSupplier");
        }
        this.byteSupplier = byteSupplier;
    }

    /**
     * @return the current {@link ByteSupplier}
     */
    public ByteSupplier getByteSupplier() {
        return byteSupplier;
    }

    /**
     * Absolute paths of all files created by this {@link TestDir} instance
     * @return {@link Set} of file {@link Path}s
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
        Path absPath = dirPath.resolve(query);
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
        Path path = fileSystem.getPath(query);
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
        Path absPath = dirPath.resolve(path);
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
        Path path = fileSystem.getPath(pathStr);
        return getDir(path);
    }

    /**
     * Absolute paths of all the directories created
     * by this {@link TestDir} instance
     * @return {@code Set<Path>} of all dirs
     */
    public Set<Path> getDirs() {
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
     * @see TestDir#createFile(Path, BigDecimal, SizeUnit)
     */
    public Path createFile(String pathString, BigDecimal size, SizeUnit unit) throws FileCreationException {
        Path path = fileSystem.getPath(pathString); //
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
     * @see TestDir#createFile(String, BigDecimal, SizeUnit)
     */
    public Path createFile(Path path, BigDecimal size, SizeUnit unit) throws FileCreationException {
        validatePath(path);
        Path absPath = dirPath.resolve(path);
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
     * Writes to an already existing file using the current {@link ByteSupplier}.  The
     * file size will remain constant if the end position is {@literal <}= the file size; otherwise
     * the file size will increase.
     * @param path the file to modify (must be within this {@code TestDir}
     * @param startPos the position of the first byte to modify, in the units specified by {@code unit}
     * @param endPos the end byte (exclusive), in the units specified by {@code unit}
     * @param unit the units of {@code startPos} and {@code endPos}
     * @return absolute {@link Path} of the file modified
     * @throws IllegalArgumentException if the {@code path} is not a file in this TestDir
     * @throws IllegalArgumentException if {@code endPos} is {@literal <} start pos or {@code startPos} is negative
     * @throws WriteFailureException if any I/O error occurs
     */
    public Path modifyFile(Path path, BigDecimal startPos, BigDecimal endPos, SizeUnit unit ) throws
            IllegalArgumentException, WriteFailureException {
        Path absPath = getFile(path);
        if (Objects.isNull(absPath) ) {
            throw new IllegalArgumentException("The supplied path is not a file in this TestDir. " + path);
        }
        long startByte = unit.toBytes(startPos);
        long numBytes = unit.toBytes(endPos) - startByte;
        new FileWriter(absPath).modify(startByte, numBytes, byteSupplier);
        return absPath;
    }

    /**
     * Writes to an already existing file using the current {@link ByteSupplier}.  The
     * file size will remain constant if the end position is {@literal <}= the file size; otherwise
     * the file size will increase.
     * @param pathString the file to modify (must be within this {@code TestDir}
     * @param startPos the position of the first byte to modify, in the units specified by {@code unit}
     * @param endPos the end byte (exclusive), in the units specified by {@code unit}
     * @param unit the units of {@code startPos} and {@code endPos}
     * @return absolute {@link Path} of the file modified
     * @throws IllegalArgumentException if the {@code path} is not a file in this TestDir or if {@code startPos} is negative
     * @throws IllegalArgumentException if {@code endPos} is {@literal <} start pos or {@code startPos} is negative
     * @throws WriteFailureException if any I/O error occurs
     */
    public Path modifyFile(String pathString, BigDecimal startPos, BigDecimal endPos, SizeUnit unit ) throws
            IllegalArgumentException, WriteFailureException {
        Path absPath = getFile(pathString);
        if (Objects.isNull(absPath) ) {
            throw new IllegalArgumentException("The supplied path is not a file in this TestDir. " + pathString);
        }
        return modifyFile(absPath, startPos, endPos, unit);
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
        Path path = fileSystem.getPath(pathString);
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
        Path absPath = dirPath.resolve(path);
        try {
            Files.createDirectories(absPath);
        } catch (Exception e) {
            throw new DirCreationException(absPath.toString(), e);
        }
        recordDirs(absPath);
        return absPath;
    }

    private void recordDirs(Path absPath) {
        while (!absPath.equals(dirPath) &&
                dirs.add(absPath) ) {
            absPath = absPath.getParent();
        }
    }

    private void validatePath(Path path) throws IllegalArgumentException {
        if (path.getFileSystem() != fileSystem) {
            throw new IllegalArgumentException("The provided path is using a different filesystem than this testdir.\n" +
                    "Consider passing the path as a string and allowing TestDir to convert it to a path, " +
                    "or manually converting with TestDir#getFileSystem().getPath(...path)" );
        }
        if (path.isAbsolute() && !path.startsWith(dirPath) ) {
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