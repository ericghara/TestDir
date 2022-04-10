package org.ericghara.csv;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ReaderUtilsTest {

    Path FILENAME = Paths.get("basicTest.csv");
    Path RESOURCE_DIR = Paths.get("src/test/resources/org/ericghara/csv");

    String csv = """
            # This is an example csv file
            # FileType   Path            Size  Units(Optional)
              D          aDir
              F          "aDir/b File"   16    B
              F          bDir/aFile      1.34         # defaults to MB, bDir is implicitly created""";

    @Test
    void getFileReaderReadsFile() throws IOException {
        var absPath = RESOURCE_DIR.resolve(FILENAME);
        File file = new File(absPath.toUri() );
        var foundReader = ReaderUtils.getFileReader(file);
        char[] found = new char[csv.length()];
        foundReader.read(found);
        foundReader.close();

        char[] expected = csv.toCharArray();
        assertArrayEquals(expected, found);
    }

    @Test
    void getStringReaderReadsString() throws IOException {
        char[] found = new char[csv.length()];
        var reader = ReaderUtils.getStringReader(csv);
        reader.read(found);
        reader.close();

        char[] expected = csv.toCharArray();
        assertArrayEquals(expected, found);
    }

    @Test
    void getResourceFileReaderReadsResource() throws IOException {
        var reader = ReaderUtils.getResourceFileReader(this, "/org/ericghara/csv/basicTest.csv");
        char[] found = new char[csv.length()];
        reader.read(found);
        reader.close();

        char[] expected = csv.toCharArray();
        assertArrayEquals(expected, found);
    }
}