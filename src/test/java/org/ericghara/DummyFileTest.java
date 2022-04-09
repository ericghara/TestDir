package org.ericghara;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class DummyFileTest {

    public static class EmptyFile extends DummyFile {

        public EmptyFile(Path filePath) {
            super(filePath);
        }
    }

    @TempDir
    Path tempDir;

    MockedStatic<Files> filesMock;

    @BeforeEach
    void before() {
        filesMock = mockStatic(Files.class);
    }

    @AfterEach
    void after() {
        filesMock.closeOnDemand(); // tolerant of receiving an already closed mock;
    }

    EmptyFile noWriteConstructor(Path path) {
        Path absPath = tempDir.resolve(path);
        // prevents file writes by mocking java.nio.file.Files class
        filesMock.when( () -> Files.createFile(any(Path.class)))
                 .thenReturn(absPath);
        return new EmptyFile(absPath);
    }

    @Test
    // this test actually writes to disk
    void createMakesFile() {
        filesMock.closeOnDemand(); // don't want to mock during this test;
        Path relPath = Paths.get("aFile");
        Path filePath = tempDir.resolve(relPath);
        var dummy = new EmptyFile(filePath);
        assertTrue(Files.isRegularFile(filePath) );
    }

    @Test
    void getSizeCorrectSize() {
        var expected = 1234L;
        Path relPath = Paths.get("aFile");
        var dummy = noWriteConstructor(relPath);
        filesMock.when( () -> Files.size(any(Path.class)))
                 .thenReturn(expected);
            assertEquals(expected, dummy.getSize());
    }

    @Nested
    @DisplayName("Tests of the exists() method")
    class ExistsTests {

        @Test
        @DisplayName("Exists returns false when the file does not exist")
        void existsReturnsFalse() {
            filesMock.when( () -> Files.exists(any(Path.class) ) )
                     .thenReturn(false);
            var path = Paths.get("aFile");
            var testFile = noWriteConstructor(path);
            assertFalse(testFile.exists() );
        }

        @Test
        @DisplayName("Exists returns true when the file exists")
        void existsReturnsTrue() {
            filesMock.when( () -> Files.exists(any(Path.class) ) )
                    .thenReturn(true);
            var path = Paths.get("aFile");
            var testFile = noWriteConstructor(path);
            assertTrue(testFile.exists() );
        }
    }

    @Nested
    @DisplayName("Tests of equals()")
    class EqualsTests {

        @Test
        void equalsDifferentPathFalse()  {
            Path relPathA = Paths.get("aFile");
            Path relPathB = Paths.get("bFile");

            var dummyA = noWriteConstructor(relPathA);
            var dummyB = noWriteConstructor(relPathB);

            assertNotEquals(dummyA, dummyB);
        }

        @Test
        void equalsReflexiveTrue() {
            Path relPath = Paths.get("aFile");
            var dummy = noWriteConstructor(relPath);
            assertEquals(dummy, dummy);
        }

        @Test
        void equalsNullFalse() {
            Path relPath = Paths.get("aFile");
            var dummy = noWriteConstructor(relPath);
            assertNotEquals(null, dummy);
        }

        @Test
        void equalsSamePathTrue() {
            Path relPath = Paths.get("aFile");
            var dummyA = noWriteConstructor(relPath);
            var dummyB = noWriteConstructor(relPath);
            assertEquals(dummyA, dummyB);
        }

    }
}