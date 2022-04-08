package org.ericghara;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DummyFileTest {

    public static class EmptyFile extends DummyFile {

        public EmptyFile(Path filePath) {
            super(filePath);
        }
    }

    @TempDir
    Path tempDir;

    DummyFile mockConstructor(Path path) {
        return mock(DummyFile.class,
                withSettings().useConstructor(path)
                              .defaultAnswer(CALLS_REAL_METHODS) );
    }

    @Test
    void createMakesFile() {
        Path relPath = Paths.get("aFile");
        Path filePath = tempDir.resolve(relPath);
        var dummy = mockConstructor(filePath);
        assertTrue(Files.isRegularFile(filePath) );
    }

    @Test
    void getSizeCorrectSize() {
        var expected = 1234L;
        Path relPath = Paths.get("aFile");
        Path filePath = tempDir.resolve(relPath);
        var dummy = mockConstructor(filePath);
        new FileWriter(filePath).write(expected,
                new RandomByteSupplier() );
        assertEquals(expected, dummy.getSize() );
    }

    @Test
    void equalsReflexiveTrue() {
        Path relPath = Paths.get("aFile");
        Path filePath = tempDir.resolve(relPath);
        var dummy = new EmptyFile(filePath);
        assertEquals(dummy, dummy);
    }

    @Test
    void equalsNullFalse() {
        Path relPath = Paths.get("aFile");
        Path filePath = tempDir.resolve(relPath);
        var dummy = new EmptyFile(filePath);
        assertNotEquals(null, dummy);
    }

    @Test
    void equalsSamePathTrue() throws IOException {
        Path relPath = Paths.get("aFile");
        Path filePath = tempDir.resolve(relPath);
        var dummyA = new EmptyFile(filePath);
        Files.delete(filePath);
        var dummyB = new EmptyFile(filePath);
        assertEquals(dummyA, dummyB);
    }

    @Test
    void equalsDifferentPathFalse()  {
        Path relPathA = Paths.get("aFile");
        Path relPathB = Paths.get("bFile");

        try(MockedStatic<Files> mockFiles = mockStatic(Files.class) ) {
            mockFiles.when(() -> Files.createFile(any(Path.class)))
                    .thenReturn(Paths.get("fake/path"));

            Path filePathA = tempDir.resolve(relPathA);
            Path filePathB = tempDir.resolve(relPathB);

            var dummyA = new EmptyFile(filePathA);
            var dummyB = new EmptyFile(filePathB);

            assertNotEquals(dummyA, dummyB);
        }
    }

}