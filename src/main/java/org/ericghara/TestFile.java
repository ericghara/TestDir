package org.ericghara;

import org.ericghara.exception.FileReadException;

import java.math.BigDecimal;
import java.nio.file.Path;

public interface TestFile {

    /**
     *
     * @return File size in Bytes
     */
    long getSize() throws FileReadException;

    /**
     *
     * @param units Size units to return
     * @return size of the testFile in {@code units}
     * @see org.ericghara.SizeUnit
     */
    BigDecimal getSize(SizeUnit units) throws FileReadException;

    /**
     * Gets the absolute file path
     *
     * @return the absolute file {@link java.nio.file.Path}
     */
    Path getPath();

    /**
     * Returns if the file exists on the filesystem
     *
     * @return {@code true} if it exists, {@code false} if it does not exist
     */
    boolean exists();

}
