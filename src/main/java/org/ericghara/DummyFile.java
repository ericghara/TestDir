package org.ericghara;

import org.ericghara.exception.FileReadException;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;

public abstract class DummyFile implements TestFile {

    final Path filePath;

    public DummyFile(Path filePath) {
        assertAbsolute(filePath);
        this.filePath = filePath;
        create();
    }

    public Path getPath() {
        return filePath;
    }

    /**
     * Reads and returns the current file size.
     * <br><br>
     * @return {@code long} file size in bytes
     * @throws FileReadException if there is an error retrieving the file size
     */
    public long getSize() throws FileReadException {
        try {
            return Files.size(filePath);
        } catch (Exception e) {
            throw new FileReadException (
                    "Error while retrieving file size: " + filePath);
        }
    }

    /**
     * Reads and returns the current file size.
     * <br><br>
     * @param unit {@code SizeUnit} size units to return i.e. kB, mB etc.
     * @return {@code BigDecimal} file size in the specified units
     * @throws FileReadException there is an error retrieving the file size
     */
    public BigDecimal getSize(SizeUnit unit) throws FileReadException {
        return unit.fromBytes(getSize() );
    }

    public boolean exists() {
        return Files.exists(filePath);
    }

    @Override
    public String toString() {
        return format("%s %s", this.getClass(), filePath);
    }

    @Override
    public boolean equals(Object obj) {
        boolean eq;
        switch (obj) {
            case null -> eq = false;
            case DummyFile other &&
                    this.filePath.equals(other.filePath) -> eq = true;
            default -> eq = false;
        }
        return eq;
    }

    @Override
    public int hashCode() {
        return filePath.hashCode();
    }

    void assertAbsolute(Path path) throws IllegalArgumentException {
        if (!path.isAbsolute() ) {
            throw new IllegalArgumentException("Must provide an absolute path.");
        }
    }

     // Creates a file in an existing subfolder
    void create() {
        try {
            Files.createFile(filePath);
        } catch (Exception e) {
            throw new RuntimeException("Could not create the file:" + filePath + ".", e);
        }
    }

}
