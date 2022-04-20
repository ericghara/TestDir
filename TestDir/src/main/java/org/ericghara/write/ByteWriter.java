package org.ericghara.write;

import lombok.NonNull;
import org.ericghara.core.SizeUnit;
import org.ericghara.exception.ByteUnderflowException;
import org.ericghara.exception.FileCreationException;
import org.ericghara.exception.FileReadException;
import org.ericghara.exception.WriteFailureException;
import org.ericghara.write.bytesupplier.ByteSupplier;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;
import static java.nio.file.StandardOpenOption.WRITE;

public class ByteWriter {

    private static final int BLOCK_SIZE = 4096;

    @NonNull
    private final Path filePath;

    public ByteWriter(Path filePath) {
        this.filePath = assertValidCreate(filePath);
    }

    public long fileSize() throws FileReadException {
        try {
            return Files.size(filePath);
        }
        catch (IOException e) {
            throw new FileReadException(
                    "Error reading file " + filePath, e);
        }
    }

    /**
     * Modifies an existing file, Writing bytes from {@code ByteSupplier}.
     * The first byte is written to {@code startPos} and the number of bytes written is
     * {@code numBytes}.  The data written is provided by {@code byteSupplier}.
     *
     * <br><br>
     * <em>Note: </em> the byteSupplier must be able to provide the required number
     * of bytes.
     * <br><br>
     * @param startPos position to write the first byte
     * @param numBytes number of bytes to write
     * @param byteSupplier byteSupplier of the bytes to be written
     * @throws WriteFailureException if the file does not exist or if any error occurs while writing
     * @throws IllegalArgumentException if start block is greater than file size
     * @see ByteSupplier
     * @see ByteWriter#create
     */
    public void modify(long startPos,
                       long numBytes,
                       @NonNull ByteSupplier byteSupplier)
            throws WriteFailureException, IllegalArgumentException {
        assertValidModify(filePath);
        new WriteJob(startPos, numBytes, byteSupplier).write();
    }

    /**
     * Modifies an existing file, Writing bytes from {@code ByteSupplier}.
     * The first byte is written to {@code startPos} and the amount of data written
     * is {@code numUnits}.  The units of {@code startPos} and {@code numUnits} are
     * defined by {@code unit}.  The data written is provided by {@code byteSupplier}.
     *
     * <br><br>
     * <em>Note: </em> the byteSupplier must be able to provide the required number
     * of bytes.
     * <br><br>
     * @param startPos position to write the first byte
     * @param numUnits number of {@code unit}s to write
     * @param unit size unit of {@code startPos} and {@code endPos}
     * @param byteSupplier byteSupplier of the bytes to be written
     * @throws WriteFailureException if the file does not exist or if any error occurs while writing
     * @throws IllegalArgumentException if start block is greater than file size
     * @see ByteSupplier
     * @see ByteWriter#create
     */
    public void modify(@NonNull BigDecimal startPos,
                       @NonNull BigDecimal numUnits,
                       @NonNull SizeUnit unit,
                       ByteSupplier byteSupplier) throws WriteFailureException, IllegalArgumentException {
        modify(unit.toBytes(startPos), unit.toBytes(numUnits), byteSupplier);
    }

    /**
     * Reduces a file size by truncating all data beyond {@code newSize}.
     *
     * @param newSize the size of the new file
     * @param unit the units of {@code newSize}
     * @throws WriteFailureException if an invalid path is provided
     * @throws WriteFailureException if the current file size is {@literal <} the new size
     * @throws WriteFailureException if an IO error occurs while writing to the filesystem
     * @throws FileReadException if the file size cannot be read
     */
    public void truncate(@NonNull BigDecimal newSize,
                         @NonNull SizeUnit unit) throws WriteFailureException, FileReadException {
        truncate(unit.toBytes(newSize) );
    }

    /**
     * Reduces a file size by truncating all data beyond {@code newSize}.
     *
     * @param newSize the size of the new file in bytes.  This is equivalent to the smallest byte index
     *                to be removed from the file.
     * @throws WriteFailureException if an invalid path is provided
     * @throws WriteFailureException if the current file size is {@literal <} the new size
     * @throws WriteFailureException if an IO error occurs while writing to the filesystem
     * @throws FileReadException if the file size cannot be read
     */
    public void truncate(long newSize) throws WriteFailureException, FileReadException {
        assertValidModify(filePath);
        long curSize;
        try {
            curSize = Files.size(filePath);
        } catch (IOException e) {
            throw new FileReadException("Could not read the file size. " + filePath);
        }
        if (newSize >= curSize) {
            throw new WriteFailureException("The current file size is no less than newSize");
        }
        try {
            var channel = Files.newByteChannel(filePath, WRITE);
            channel.truncate(newSize);
        } catch (IOException e) {
            throw new WriteFailureException("Unable to truncate the file " + filePath, e);
        }
    }

    /**
     * Creates a new file.  The data written is provided by {@code byteSupplier}.
     * The size of the created file is {@code numBytes}.
     * <br><br>
     * @param numBytes number of bytes to write
     * @param byteSupplier byteSupplier of the bytes to be written
     * @throws WriteFailureException if any error occurs while writing
     * @throws FileCreationException if the file cannot be created for any reason (e.g. it already exists)
     */
    public void create(long numBytes, ByteSupplier byteSupplier)
            throws FileCreationException, WriteFailureException {
        try {
            Files.createFile(filePath);
        } catch (Exception e) {
            throw new FileCreationException("Unable to create the file " + filePath, e);
        }
        modify(0, numBytes, byteSupplier);
    }

    /**
     * Creates a new file.  The data written is provided by {@code byteSupplier}.
     * The size of the created file is specified by {@code size} and {@code unit}.
     * <br><br>
     * @param size size of the file to create
     * @param unit unit of size (i.e. {@link SizeUnit#MB}
     * @param byteSupplier byteSupplier of the bytes to be written
     * @throws WriteFailureException if any error occurs while writing
     * @throws FileCreationException if the file cannot be created for any reason (e.g. it already exists)
     */
    public void create(BigDecimal size, SizeUnit unit, ByteSupplier byteSupplier) throws
            FileCreationException, WriteFailureException{
        long sizeB = unit.toBytes(size);
        create(sizeB, byteSupplier);
    }

    // absolute, parent exists, parent writeable
    Path assertValidCreate(Path path) throws WriteFailureException {
        if (!path.isAbsolute() ) {
            throw new WriteFailureException("The provided path is a relative file path.  " +
                    "Provide an absolute path when creating the File/Path. " + path);
        }
        Path parent = path.getParent();
        if (!Files.isDirectory(parent) ) {
            throw new WriteFailureException(format("Invalid path, the parent directory does not exist: %s.", path) );
        }
        if (!Files.isWritable(parent) ) {
            throw new WriteFailureException("Insufficient to permissions to write to the parent folder: " + parent);
        }
        return path;
    }

    // Path points to a regular file and is writeable
    // Path known to be absolute (no check)
    Path assertValidModify(Path path) throws WriteFailureException {
        if (Files.isRegularFile(path) &&
                Files.isWritable(path) ) {
            return path;
        }
        throw new WriteFailureException("Invalid path " + path);
    }

    class WriteJob implements Closeable {

        private final long numBytes;
        private final ByteSupplier supplier;
        private final SeekableByteChannel channel;
        private ByteBuffer buffer;

        WriteJob(long startPos, long numBytes, ByteSupplier supplier)
            throws WriteFailureException, IllegalArgumentException {
            this.numBytes = validNumBytes(numBytes);
            this.supplier = supplier;
            channel = openChannel(startPos);
            buffer = initBuffer();
        }

        public void close() throws WriteFailureException {
            try {
                channel.close();
            } catch (IOException e) {
                throw new WriteFailureException("Error closing ByteChannel.", e);
            }
            buffer = null;
        }

        void write() throws WriteFailureException {
            try (this) {
                long remain = numBytes;
                while (remain >= BLOCK_SIZE) {
                    completeFill();
                    int written = channel.write(buffer);
                    remain -= written;
                }
                partialFill( (int) remain);
                channel.write(buffer);
            } catch (Exception e) {
                if (e instanceof IOException){
                    throw new WriteFailureException("Error writing to file", e);
                }
                if (e instanceof ByteUnderflowException) {
                    throw new WriteFailureException("Empty buffer", e);
                }
                else {
                    throw new WriteFailureException("Unknown exception, see stacktrace", e);
                }
            }
        }

        // Returns ByteBuffer with position set to BLOCK_SIZE (ie hasRemaining = false);
        ByteBuffer initBuffer() {
            var buffer = ByteBuffer.allocateDirect(BLOCK_SIZE);
            buffer.position(BLOCK_SIZE);
            return buffer;
        }

        // fills buffer from current position to end
        void fillBuffer() {
            while (buffer.hasRemaining() ) {
                var b = supplier.getAsByte();
                buffer.put(b);
            }
        }

        void completeFill() {
            buffer.rewind();
            fillBuffer();
            buffer.rewind();
        }

        void partialFill(int remain) {
            buffer.position( BLOCK_SIZE - remain);
            buffer.mark();
            fillBuffer();
            buffer.reset();
        }

        SeekableByteChannel openChannel(long startPos)
                throws WriteFailureException, IllegalArgumentException {
            validStartPos(startPos);
            try {
                var channel = Files.newByteChannel(filePath, WRITE);
                channel.position(startPos);
                return channel;
            } catch (IOException e) {
                throw new WriteFailureException("Unable to open ByteChannel", e);
            }
        }

        long validNumBytes(long bytes) {
            if (bytes < 0) {
                throw new IllegalArgumentException("Received a negative numBytes.");
            }
            return bytes;
        }

        void validStartPos(long startPos) throws IllegalArgumentException {
            if (startPos > fileSize() ) {
                throw new IllegalArgumentException(format(
                        "Start position: %d is greater than the file size: %d",
                        startPos, fileSize() ) );
            }
            if (startPos < 0) {
                throw new IllegalArgumentException("Received a negative start position");
            }
        }
    }
}
