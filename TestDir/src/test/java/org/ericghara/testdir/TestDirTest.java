package org.ericghara.testdir;

import com.google.common.jimfs.Jimfs;
import org.ericghara.write.RandomByteFrequenciesTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import static org.ericghara.testdir.SizeUnit.B;
import static org.ericghara.testdir.SizeUnit.MB;
import static org.junit.jupiter.api.Assertions.*;

class TestDirTest {

    @TempDir
    Path tempDir;

    TestDir testDir;

    @BeforeEach
    void beforeEach() {
        testDir = new TestDir(tempDir);
    }

    @Test
    @DisplayName("validatePath throws when different filesystem provided than testdir")
    void validatePathThrowsWithDifferentFS() {
        FileSystem homeFs = Jimfs.newFileSystem();
        FileSystem otherFs = Jimfs.newFileSystem();

        Path home = homeFs.getPath("").toAbsolutePath();
        TestDir testDir = new TestDir(home);

        Path aDirOnOther = otherFs.getPath("aDir");
        assertThrows(IllegalArgumentException.class,
                () -> testDir.createDirs(aDirOnOther) );

    }



    @ParameterizedTest(name="[{index}] {0}")
    @CsvSource(useHeadersInDisplayName = true, delimiter = '|', textBlock = """
            Label             | relativePath  | sizeMB
            "simpleFile"      | aFile         |  1
            "zero mb file"    | aFile         |  0
            "subdir and file" | aFolder/aFile |  1.1234
            """)
    void createFile(String _label, String relativePath, BigDecimal sizeMB) throws IOException {
        Path foundPath = testDir.createFile(relativePath, sizeMB, MB);
        Path relP =  testDir.getFileSystem().getPath(relativePath);
        Path expectedP = tempDir.resolve(relP);

        assertEquals(expectedP, foundPath);  // reports correct path
        assertTrue(Files.exists(expectedP) ); // creates file
        assertEquals(MB.toBytes(sizeMB),  Files.size(expectedP) ); // file size correct
        assertTrue(testDir.getFiles()
                          .stream()
                          .anyMatch(p ->  p.equals(expectedP) ) ); // records file creation
        if (relP.getNameCount() > 1) {
            var dir = expectedP.getParent();
            assertTrue(testDir.getDirs().contains(dir) ); // records subdir creation (if any)
        }
    }

    @Test
    void RandomFileIsUnique() throws IOException {
        Path aFile = testDir.createFile("aFile", BigDecimal.valueOf(2_718_281L), B);
        Path bFile = testDir.createFile("bFile", BigDecimal.valueOf(2_718_281L), B);

        assertTrue(-1L < Files.mismatch(aFile, bFile) );
    }

    // Note: since this a probabilistic test, it may occasionally fail
    @ParameterizedTest(name="Random byte distribution test (may occasionally fail) - alpha: {0}")
    @ValueSource(doubles = {.05 })
    void RandomFileHasRandomlyDistributedBytes(double alpha) {
        BooleanSupplier isNonRandom = () -> {
            var pathA = testDir.createFile("aFile", BigDecimal.valueOf(2_718_281L), B);
            try {
                var test = new RandomByteFrequenciesTest(pathA);
                return test.probablyNonRandom(alpha);
            } catch (Exception e) {
                fail("IO exception while reading file", e);
            }
            return true;
        };
        if (isNonRandom.getAsBoolean() && isNonRandom.getAsBoolean() ) {
            fail("Null hypothesis that file has randomly distributed bits was rejected in 2 subsequent tests at alpha: " + alpha);
        }
    }

    @Test
    void createFolder() {
        final var pathStr = "a/b/c/d";
        var expectedP = tempDir.resolve(testDir.getFileSystem().getPath(pathStr) );
        var foundP = testDir.createDirs(pathStr);
        assertEquals(expectedP, foundP); // reports correct path
        assertTrue(Files.isDirectory(expectedP) ); // creates directory at correct path
    }

    @Test
    @DisplayName("recordDirs adds all subdirs created")
    void recordDirs() {
        final var pathStr = "a/b/c/d";
        testDir.createDirs(pathStr);
        Path testPath = testDir.getFileSystem().getPath("");
        for (char dir = 'a'; dir < 'e'; dir++) {
            testPath = testPath.resolve(Character.toString(dir));
            assertTrue(Objects.nonNull(testDir.getDir(testPath) ) );
        }
    }
}