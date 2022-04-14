package org.ericghara.testdir;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.ericghara.csv.WriteFromCSV;
import org.ericghara.write.ByteSupplier;
import org.ericghara.write.IntStreamByteSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import static org.ericghara.testdir.FsType.OSX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class TestDirBuilderTest {

    @Test
    @DisplayName("setCsvSource throws IllegalStateException if it has previously been set")
    void setCsvSourceThrowsIfAlreadySet() {
        var mockReader = mock(Reader.class);
        var builder = TestDir.builder();
        assertThrows(IllegalStateException.class, () -> builder.setCsvSource(mockReader).setCsvSource(mockReader) );
    }

    @Test
    @DisplayName("setDir throws IllegalStateException if it has previously been set")
    void setDirThrowsIfAlreadySet() {
        var mockPath = mock(Path.class);
        var builder = TestDir.builder();
        assertThrows(IllegalStateException.class, () -> builder.setDir(mockPath).setDir(mockPath) );
    }

    @Test
    @DisplayName("setByteSupplier throws IllegalStateException if it has previously been set")
    void setByteSupplierThrowsIfAlreadySet() {
        var mockByteSupplier = mock(ByteSupplier.class);
        var builder = TestDir.builder();
        assertThrows(IllegalStateException.class, () -> builder.setByteSupplier(mockByteSupplier).setByteSupplier(mockByteSupplier) );
    }

    @Test
    @DisplayName("setFsType throws IllegalStateException if it has previously been set")
    void setFsTypeThrowsIfAlreadySet() {
        var mockByteFsType = mock(FsType.class);
        var builder = TestDir.builder();
        assertThrows(IllegalStateException.class, () -> builder.setFsType(mockByteFsType).setFsType(mockByteFsType) );
    }

    @Nested
    @DisplayName("build() tests")
    class BuildTests {

        private MockedStatic<Jimfs> jimMock;

        @Captor
        private ArgumentCaptor<Jimfs> jimCaptor;

        @Mock
        private WriteFromCSV writerMock;
        @Captor
        private ArgumentCaptor<WriteFromCSV> writerCaptor;

        private TestDirBuilder builder = TestDir.builder();

        @BeforeEach
        void before() {
            FileSystem fs = Paths.get("").getFileSystem();
            jimMock = mockStatic(Jimfs.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
            jimMock.when(() -> Jimfs.newFileSystem(any(Configuration.class) ) ).thenReturn(fs);
        }

        @AfterEach
        void after() {
            jimMock.closeOnDemand();
        }


        @Test
        @DisplayName("Default configuration uses jimFS")
        void noArgs() {
            assertDoesNotThrow( () -> builder.build() );
            jimMock.verify( () -> Jimfs.newFileSystem(any(Configuration.class)), times(1));
        }

        @Test
        @DisplayName("FsType set uses jimFS")
        void fsTypeSet() {
            assertDoesNotThrow( () -> builder.setFsType(OSX).build() );
            var expected = OSX.configuration();
            jimMock.verify( () -> Jimfs.newFileSystem(expected), times(1));
        }

        @Test
        @DisplayName("targetDir set does not use jimFS and constructs TestDir with expected path")
        void targetDirSet(@TempDir Path path) {
            TestDir testDir = builder.setDir(path).build();

            jimMock.verify( () -> Jimfs.newFileSystem(any(Configuration.class)), times(0));
            assertEquals(path, testDir.getPath());
        }

        @Test
        @DisplayName("targetDir set and FsType set throws IllegalState exception")
        void targetDirSetAndFsTypeSetThrows(@TempDir Path path) {
            assertThrows( IllegalStateException.class,
                    () -> builder.setDir(path).setFsType(OSX).build() );
            jimMock.verify( () -> Jimfs.newFileSystem(any(Configuration.class)), times(0));
        }

        @Test
        @DisplayName("Set byteSupplier properly sets the supplier")
        void setByteSupplier() {
            var supplier = new IntStreamByteSupplier(IntStream.range(0,0) );
            ByteSupplier getter = supplier::getAsByte;
            TestDir testDir = builder.setByteSupplier(getter).build();
            assertEquals(getter, testDir.getByteSupplier() );
            jimMock.verify( () -> Jimfs.newFileSystem(any(Configuration.class)), times(1));
        }

        @Test
        @DisplayName("setCsv source sets csv and csv is written to testDir")
        void setCsv() throws IOException {
            String pathStr = """
                                F, aDir/aFile, 0, B
                             """;
            Reader csvStream = mock(StringReader.class, withSettings().useConstructor(pathStr).defaultAnswer(CALLS_REAL_METHODS) );
            TestDir testDir = builder.setCsvSource(csvStream).build();
            verify(csvStream, atLeastOnce() ).read(any(char[].class), anyInt(), anyInt() );
        }
    }
}