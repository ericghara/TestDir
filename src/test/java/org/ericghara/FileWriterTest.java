package org.ericghara;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.IntFunction;
import java.util.stream.LongStream;

import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileWriterTest {

    @TempDir
    Path tempDir;
    Path filePath;
    FileWriter writer;

    @BeforeEach
    void beforeEach() throws IOException {
        Path relPath = Paths.get("testFile");
        filePath = Files.createFile(tempDir.resolve(relPath) );
        writer = new FileWriter(filePath);
    }

    @Test
    void constructorThrowsWhenNullArg() {
        assertThrows(NullPointerException.class,
                () -> new FileWriter( (Path) null) );
    }

    @Test
    void fileSizeReturnsCorrectSize() throws IOException {
        writer.write(1234, new RandomByteSupplier() );
        assertEquals(Files.size(filePath), writer.fileSize() );
    }


    @Nested
    class WriteJobTests {

        ByteSupplier supplier;

        @BeforeEach
        void BeforeEach() throws IOException {
            var channel = Files.newByteChannel(filePath, WRITE);
            channel.truncate(0);// Resets to 0 byte file.
            channel.close();
        }

        @Test
        void writeThrowsWhenNegativeNumBytes() {
            supplier = new RandomByteSupplier();
            assertThrows(IllegalArgumentException.class,
                    () -> writer.write(-1, supplier) );
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1, 1234, 4096, 5678})
        void writeWritesExpectedNumBytes(long expectedBytes) {
            supplier = new RandomByteSupplier();
            writer.write(expectedBytes, supplier);
            assertEquals(expectedBytes, writer.fileSize() );
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1, 1234, 4096, 5678})
        void writeCallsGetAsByteMinNumTimes(long expectedBytes) {
            supplier = mock(RandomByteSupplier.class);
            lenient().when(supplier.getAsByte())
                    .thenReturn((byte) 255);
            writer.write(expectedBytes, supplier);
            verify(supplier, times( (int) expectedBytes) ).getAsByte();
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1})
        void validStartPosTooLargeThrows(long pos) {
            supplier = new RandomByteSupplier();
            writer.write(pos, supplier);
            assertThrows(IllegalArgumentException.class,
                    () -> writer.write(pos+1, 0, supplier) );
        }

        @Test
        void startPosNegativeThrows() {
            supplier = new RandomByteSupplier();
            assertThrows(IllegalArgumentException.class,
                    () -> writer.write(-1, 0, supplier) );
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1, 1234, 4096, 5678})
        void writeCanBeginAtNonZeroStartPos(long bytes) {

            supplier = new RandomByteSupplier();
            writer.write(bytes, supplier);
            writer.write(bytes, bytes, supplier);

            assertEquals(2*bytes, writer.fileSize() );
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 1234, 4096, 5678})
        void writeWritesToExpectedStartPosition(int bytes) throws IOException {

            int firstBlock = -1;
            int secondBlock = 0;

            IntFunction<ByteSupplier> getSupplier = (i) -> {
                byte b = (byte) i;
                return () -> b;
            };

            supplier = getSupplier.apply(firstBlock);
            writer.write(bytes, supplier);
            supplier = getSupplier.apply(secondBlock);
            writer.write(bytes, bytes, supplier);

            var reader = ByteBuffer.allocateDirect(2*bytes);
            var channel = Files.newByteChannel(filePath);

            channel.read(reader);
            channel.close();
            reader.rewind();

            LongStream.range(0, bytes)
                      .forEach(l ->
                              assertEquals(firstBlock, reader.get() ) );
            LongStream.range(0, bytes)
                      .forEach(l ->
                              assertEquals(secondBlock, reader.get() ) );
        }
    }
}