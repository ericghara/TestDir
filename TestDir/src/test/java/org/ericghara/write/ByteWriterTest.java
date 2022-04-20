package org.ericghara.write;

import org.ericghara.exception.FileReadException;
import org.ericghara.exception.WriteFailureException;
import org.ericghara.write.bytesupplier.ByteSupplier;
import org.ericghara.write.bytesupplier.IntStreamByteSupplier;
import org.ericghara.write.bytesupplier.RandomByteSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.ericghara.core.SizeUnit.B;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ByteWriterTest {

    @TempDir
    Path tempDir;
    Path filePath;
    ByteWriter writer;

    @BeforeEach
    void beforeEach() {
        Path relPath = tempDir.getFileSystem().getPath("testFile");
        filePath = tempDir.resolve(relPath);
        writer = new ByteWriter(filePath);
    }

    @Test
    void constructorThrowsWhenNullArg() {
        assertThrows(NullPointerException.class,
                () -> new ByteWriter((Path) null));
    }

    @Test
    void fileSizeReturnsCorrectSize() throws IOException {
        writer.create(1234, new RandomByteSupplier());
        assertEquals(Files.size(filePath), writer.fileSize());
    }

    @Test
    @DisplayName("truncate throws when invalid path")
    void truncateThrowsWhenInvalidPath() throws IOException {
        try (MockedStatic<Files> filesMock = mockStatic(Files.class) ) {
            filesMock.when( () -> Files.isRegularFile(any(Path.class) ) ).thenReturn(false);
            filesMock.when( () -> Files.isWritable(any(Path.class) ) ).thenReturn(true);
            assertThrows(WriteFailureException.class, () -> writer.truncate(BigDecimal.ONE, B) );
        }
    }


    @Test
    @DisplayName("truncate throws when Files.size throws")
    void truncateThrowsWhenCannotReadFileSize() {
        try (MockedStatic<Files> filesMock = mockStatic(Files.class) ) {
            filesMock.when( () -> Files.size(any(Path.class) ) ).thenThrow(new IOException() );
            filesMock.when( () -> Files.isRegularFile(any(Path.class) ) ).thenReturn(true);
            filesMock.when( () -> Files.isWritable(any(Path.class) ) ).thenReturn(true);
            assertThrows(FileReadException.class, () -> writer.truncate(BigDecimal.ONE, B) );
        }
    }

    @Test
    @DisplayName("truncate throws when newSize greater than current size")
    void truncateThrowsWhenNewSizeGreaterThanCurrent() {
        try (MockedStatic<Files> filesMock = mockStatic(Files.class) ) {
            filesMock.when( () -> Files.size(any(Path.class) ) ).thenReturn(0L);
            filesMock.when( () -> Files.isRegularFile(any(Path.class) ) ).thenReturn(true);
            filesMock.when( () -> Files.isWritable(any(Path.class) ) ).thenReturn(true);
            assertThrows(WriteFailureException.class, () -> writer.truncate(BigDecimal.ONE, B) );
        }
    }

    @Test
    @DisplayName("truncate throws when seekableByteChannel throws")
    void truncateThrowsWhenByteChannelThrows() throws IOException {
        SeekableByteChannel channelMock = mock(SeekableByteChannel.class);
        when(channelMock.truncate(anyLong() ) ).thenThrow(new IOException() );

        try (MockedStatic<Files> filesMock = mockStatic(Files.class) ) {
            filesMock.when( () -> Files.size(any(Path.class) ) ).thenReturn(1L);
            filesMock.when( () -> Files.isRegularFile(any(Path.class) ) ).thenReturn(true);
            filesMock.when( () -> Files.isWritable(any(Path.class) ) ).thenReturn(true);
            filesMock.when( () -> Files.newByteChannel(any(Path.class), any(OpenOption.class) ) ).thenReturn(channelMock);
            assertThrows(WriteFailureException.class, () -> writer.truncate(BigDecimal.ZERO, B) );
        }
    }

    @Test
    @DisplayName("truncate reduces file size")
    void truncateCallsByteChannelTruncate() throws IOException {
        writer.create(2, new IntStreamByteSupplier( IntStream.range(0,2) ) );
        writer.truncate(BigDecimal.ONE, B);
        assertEquals(1,  Files.size(filePath) );
    }



    @Nested
    class WriteJobTests {

        ByteSupplier supplier;

        @Test
        void writeThrowsWhenNegativeNumBytes() {
            supplier = new RandomByteSupplier();
            assertThrows(IllegalArgumentException.class,
                    () -> writer.create(-1, supplier));
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1, 1234, 4096, 5678})
        void writeWritesExpectedNumBytes(long expectedBytes) {
            supplier = new RandomByteSupplier();
            writer.create(expectedBytes, supplier);
            assertEquals(expectedBytes, writer.fileSize());
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1, 1234, 4096, 5678})
        void writeCallsGetAsByteMinNumTimes(long expectedBytes) {
            supplier = mock(RandomByteSupplier.class);
            lenient().when(supplier.getAsByte())
                    .thenReturn((byte) 255);
            writer.create(expectedBytes, supplier);
            verify(supplier, times((int) expectedBytes)).getAsByte();
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1})
        void validStartPosTooLargeThrows(long pos) {
            supplier = new RandomByteSupplier();
            writer.create(pos, supplier);
            assertThrows(IllegalArgumentException.class,
                    () -> writer.modify(pos + 1, 0, supplier));
        }

        @Test
        void startPosNegativeThrows() {
            MockedStatic<Files> filesMock = mockStatic(Files.class);
            filesMock.when(() -> Files.size(any(Path.class)))
                    .thenReturn(0L);
            filesMock.when(() -> Files.isWritable(any(Path.class)))
                    .thenReturn(true);
            filesMock.when(() -> Files.isRegularFile(any(Path.class)))
                    .thenReturn(true);
            supplier = new RandomByteSupplier();
            assertThrows(IllegalArgumentException.class,
                    () -> writer.modify(-1, 0, supplier));
            filesMock.closeOnDemand();
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1, 1234, 4096, 5678})
        void writeCanBeginAtNonZeroStartPos(long bytes) {

            supplier = new RandomByteSupplier();
            writer.create(bytes, supplier);
            writer.modify(bytes, bytes, supplier);

            assertEquals(2 * bytes, writer.fileSize());
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

            var reader = ByteBuffer.allocateDirect(2 * bytes);
            var channel = Files.newByteChannel(filePath);

            channel.read(reader);
            channel.close();
            reader.rewind();

            LongStream.range(0, bytes)
                    .forEach(l ->
                            assertEquals(firstBlock, reader.get()));
            LongStream.range(0, bytes)
                    .forEach(l ->
                            assertEquals(secondBlock, reader.get()));
        }
    }
}