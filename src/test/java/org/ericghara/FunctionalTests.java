package org.ericghara;

import org.ericghara.csv.ReaderUtils;
import org.ericghara.csv.WriteFromCSV;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class both serves as a group of functional tests but also a demo for how and when
 * the {@link TestDir} library can be used.
 */
public class FunctionalTests {

    @TempDir
    Path tempDir;

    @Nested
    class ParameterizedTest {

        String paramsCSV = """
                              # File or Dir         Path          size  unit
                              F,                  aDir/aFile,      1,   KB
                              F,                  aDir/bFile,      3,   KB
                              D,                  aDir
                            """;
        WriteFromCSV writer;
        Reader csvStream;

        @BeforeEach
        void before() {
            writer = new WriteFromCSV();
            csvStream = ReaderUtils.getStringReader(paramsCSV);
        }

        @Test
        @DisplayName("Functional test of example file deletion method")
        void aTest() throws Exception {
            /* This is an example method that we might want
            to test.  It deletes all files in a directory
            with a size (in bytes) greater than a threshold value
             */
            BiConsumerThrows<Path, Long> deleteLargerThan = (path, threshold) -> {
                List<Path> files = Files.walk(path, 1)
                                        .toList();
                for (Path p: files) {
                    if (Files.isRegularFile(p) &&
                            Files.size(p) > threshold) {
                        Files.delete(p);
                    }
                }
            };
            // Sets up a TestDir in the tempDir created by JUnit
            // The csv is read in from a Reader (byte stream)
            TestDir testDir = writer.write(tempDir, csvStream);

            // Retrieve the actual location of the directory
            // we created.  JUnit creates a TempDir in a random folder
            Path parentDir = testDir.getDir("aDir");

            // Get TestFile instances of the files we created
            Path fileA = testDir.getFile("aDir/aFile");
            Path fileB = testDir.getFile("aDir/bFile");

            // run the unit we want to test
            deleteLargerThan.accept(parentDir, 2048L);
            // verify the results
            assertTrue(Files.exists(fileA) );
            assertFalse(Files.exists(fileB) );
        }
    }


    // This is a BiConsumer that allows exceptions to be thrown.
    // It was useful writing our example method inline without
    // having to also having to clutter our example with try/catch
    // blocks
    @FunctionalInterface
    interface BiConsumerThrows<T,U> {

        void accept(T t, U u) throws Exception;
    }
}
