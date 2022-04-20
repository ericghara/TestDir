package org.ericghara.write;

import org.ericghara.write.bytesupplier.ByteSupplier;
import org.ericghara.write.bytesupplier.RandomByteSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntFunction;
import java.util.stream.LongStream;

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
    void beforeEach() {
        Path relPath = tempDir.getFileSystem().getPath("testFile");
        filePath = tempDir.resolve(relPath);
        writer = new FileWriter(filePath);
    }

    @Test
    void constructorThrowsWhenNullArg() {
        assertThrows(NullPointerException.class,
                () -> new FileWriter( (Path) null) );
    }

    @Test
    void fileSizeReturnsCorrectSize() throws IOException {
        writer.create(1234, new RandomByteSupplier() );
        assertEquals(Files.size(filePath), writer.fileSize() );
    }


    @Nested
    class WriteJobTests {

        ByteSupplier supplier;

        @Test
        void writeThrowsWhenNegativeNumBytes() {
            supplier = new RandomByteSupplier();
            assertThrows(IllegalArgumentException.class,
                    () -> writer.create(-1, supplier) );
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1, 1234, 4096, 5678})
        void writeWritesExpectedNumBytes(long expectedBytes) {
            supplier = new RandomByteSupplier();
            writer.create(expectedBytes, supplier);
            assertEquals(expectedBytes, writer.fileSize() );
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1, 1234, 4096, 5678})
        void writeCallsGetAsByteMinNumTimes(long expectedBytes) {
            supplier = mock(RandomByteSupplier.class);
            lenient().when(supplier.getAsByte())
                    .thenReturn((byte) 255);
            writer.create(expectedBytes, supplier);
            verify(supplier, times( (int) expectedBytes) ).getAsByte();
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1})
        void validStartPosTooLargeThrows(long pos) {
            supplier = new RandomByteSupplier();
            writer.create(pos, supplier);
            assertThrows(IllegalArgumentException.class,
                    () -> writer.modify(pos+1, 0, supplier) );
        }

        @Test
        void startPosNegativeThrows() {
            MockedStatic<Files> filesMock = mockStatic(Files.class);
            filesMock.when(() -> Files.size(any(Path.class) ) )
                     .thenReturn(0L);
            filesMock.when(() -> Files.isWritable(any(Path.class) ) )
                    .thenReturn(true);
            filesMock.when(() -> Files.isRegularFile(any(Path.class) ) )
                    .thenReturn(true);
            supplier = new RandomByteSupplier();
            assertThrows(IllegalArgumentException.class,
                    () -> writer.modify(-1, 0, supplier) );
            filesMock.closeOnDemand();
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1, 1234, 4096, 5678})
        void writeCanBeginAtNonZeroStartPos(long bytes) {

            supplier = new RandomByteSupplier();
            writer.create(bytes, supplier);
            writer.modify(bytes, bytes, supplier);

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
            writer.create(bytes, supplier);
            supplier = getSupplier.apply(secondBlock);
            writer.modify(bytes, bytes, supplier);

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