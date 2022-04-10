package org.ericghara.write;

import lombok.NonNull;
import org.ericghara.Constant;
import org.ericghara.TestFile;
import org.ericghara.exception.ByteUnderflowException;
import org.ericghara.exception.WriteFailureException;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;
import static java.nio.file.StandardOpenOption.WRITE;

public class FileWriter {

    private static final int BLOCK_SIZE = Constant.BLOCK_SIZE.value;

    @NonNull
    private final Path filePath;

    public FileWriter(Path filePath) {
        this.filePath = assertValid(filePath);
    }

    public FileWriter(TestFile file) {
        this(file.getPath() );
    }

    public long fileSize() throws RuntimeException {
        try {
            return Files.size(filePath);
        }
        catch (IOException e) {
            throw new RuntimeException(
                    "Error reading file " + filePath, e);
        }
    }

    /**
     * Writes bytes from {@code ByteSupplier} to a {@code filePath}.  The first
     * byte is written to {@code startPos} and the number of bytes written is
     * {@code numBytes}.
     * <br><br>
     * <em>Note: </em> the supplier must be able to provide the required number
     * of bytes.
     * <br><br>
     * @param startPos position to write the first byte
     * @param numBytes number of bytes to write
     * @param supplier supplier of the bytes to be written
     * @throws WriteFailureException if any error occurs while writing
     * @throws IllegalArgumentException if start block is greater than file size
     * @see ByteSupplier
     */
    public void write(long startPos,
                      long numBytes,
                      @NonNull ByteSupplier supplier)
            throws WriteFailureException, IllegalArgumentException {
        new WriteJob(startPos, numBytes, supplier).write();
    }

    /**
     * Writes bytes from {@code ByteSupplier} to a {@code filePath}.  The first
     * byte is written to the beginning of file and the number of bytes written is
     * {@code numBytes}.
     * <br><br>
     * equivalent to: {@code write(0, numBytes, supplier}
     * @param numBytes number of bytes to write
     * @param supplier supplier of the bytes to be written
     * @throws WriteFailureException if any error occurs while writing
     * @see FileWriter#write(long, long, ByteSupplier)
     */
    public void write(long numBytes, ByteSupplier supplier)
            throws WriteFailureException {
        write(0, numBytes, supplier);
    }

    // Path points to a regular file and is writeable
    Path assertValid(Path path) throws WriteFailureException {
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
                switch (e) {
                    case IOException _e -> throw new WriteFailureException("Error writing to file", e);
                    case ByteUnderflowException _e -> throw new WriteFailureException("Empty buffer", e);
                    case default -> throw new WriteFailureException("Unknown exception, see stacktrace", e);
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
