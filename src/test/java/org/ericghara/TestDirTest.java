package org.ericghara;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.ericghara.SizeUnit.MB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDirTest {

    @TempDir
    Path tempDir;

    TestDir testDir;

    @BeforeEach
    void beforeEach() {
        testDir = new TestDir(tempDir);
    }


    @ParameterizedTest(name="[{index}] {0}")
    @CsvSource(useHeadersInDisplayName = true, delimiter = '|', textBlock = """
            Label             | relativePath  | sizeMB
            "simpleFile"      | aFile         |  1
            "zero mb file"    | aFile         |  0
            "subdir and file" | aFolder/aFile |  1.1234
            """)
    void createFile(String _label, String relativePath, BigDecimal sizeMB) throws IOException {
        TestFile testFile = testDir.createFile(relativePath, sizeMB, MB);
        Path relP =  Paths.get(relativePath);
        Path expectedP = tempDir.resolve(relP);

        assertEquals(expectedP, testFile.getPath());  // reports correct path
        assertTrue(Files.exists(expectedP) ); // creates file
        assertEquals(MB.toBytes(sizeMB),  Files.size(expectedP) ); // file size correct
        assertTrue(testDir.getFilePaths()
                          .stream()
                          .anyMatch(p ->  p.equals(expectedP) ) ); // records file creation
        if (relP.getNameCount() > 1) {
            var dir = expectedP.getParent();
            assertTrue(testDir.getDirPaths().contains(dir) ); // records subdir creation (if any)
        }
    }

    @Test
    void createFolder() {
        final var pathStr = "a/b/c/d";
        var expectedP = tempDir.resolve(Paths.get(pathStr) );
        var foundP = testDir.createDirs(pathStr);
        assertEquals(expectedP, foundP); // reports correct path
        assertTrue(Files.isDirectory(expectedP) ); // creates directory at correct path
        assertTrue(testDir.getDirPaths().contains(expectedP) ); // records dir creation
    }

}