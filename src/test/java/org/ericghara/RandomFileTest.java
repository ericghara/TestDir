package org.ericghara;

import org.ericghara.write.RandomByteFrequenciesTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.ericghara.SizeUnit.MB;
import static org.junit.jupiter.api.Assertions.*;

class RandomFileTest {

    @TempDir
    Path tempDir;

    @ParameterizedTest(name="[{index}] {0}")
    @CsvSource(useHeadersInDisplayName = true, delimiter = '|', textBlock = """
            Label             | relativePath  | sizeMB
            "simpleFile"      | aFile         |  3.14159
            "zero mb file"    | aFile         |  0
            """)
    void createFile(String _label, String relativePath, BigDecimal sizeMB) throws IOException {
        var sizeB = MB.toBytes(sizeMB);

        var path = tempDir.resolve(Paths.get(relativePath) );
        var file = new RandomFile(path, sizeMB, MB);

        assertTrue(Files.exists(path) );
        assertEquals(sizeB,  Files.size(path) );
    }

    @Test
    void RandomFileThrowsWhenInvalidPath() {
        var path = tempDir.resolve(Paths.get("a/aFile") );
        assertThrows(RuntimeException.class, () -> new RandomFile(path, 0) );
    }

    @Test
    void RandomFileIsUnique() throws IOException {
        var pathA = tempDir.resolve(Paths.get("A") );
        var pathB = tempDir.resolve(Paths.get("B") );
        new RandomFile(pathA, 2_718_281L);
        new RandomFile(pathB, 2_718_281L);
        assertTrue(-1L < Files.mismatch(pathA, pathB) );
    }

    // Note: since this a probabilistic test, it may occasionally fail
    @ParameterizedTest(name="Random byte distribution test (may occasionally fail) - alpha: {0}")
    @ValueSource(doubles = {.01, .05 })
    void RandomFileHasRandomlyDistributedBytes(double alpha) throws IOException {
        var pathA = tempDir.resolve(Paths.get("A") );
        new RandomFile(pathA, 2_718_281L);
        var test = new RandomByteFrequenciesTest(pathA);
        assertFalse(test.probablyNonRandom(alpha) );
    }


    @Test
    void equalsReflexiveTrue() {
        Path relPath = Paths.get("aFile");
        Path filePath = tempDir.resolve(relPath);
        var randFile = new RandomFile(filePath, 0);
        assertEquals(randFile, randFile);
    }

    @Test
    void equalsNullFalse() {
        Path relPath = Paths.get("aFile");
        Path filePath = tempDir.resolve(relPath);
        var randFile = new RandomFile(filePath, 0);
        assertNotEquals(null, randFile);
    }

    @Test
    void equalsSamePathTrue() throws IOException {
        Path relPath = Paths.get("aFile");
        Path filePath = tempDir.resolve(relPath);
        var randFileA = new RandomFile(filePath, 0);
        Files.delete(filePath);
        var randFileB = new RandomFile(filePath, 0);
        assertEquals(randFileA, randFileB);
    }

    @Test
    void equalsDifferentPathFalse() {
        Path relPathA = Paths.get("aFile");
        Path relPathB = Paths.get("bFile");

        Path filePathA = tempDir.resolve(relPathA);
        Path filePathB = tempDir.resolve(relPathB);

        var randFileA = new RandomFile(filePathA, 0);
        var randFileB = new RandomFile(filePathB, 0);

        assertNotEquals(randFileA, randFileB);
    }
}