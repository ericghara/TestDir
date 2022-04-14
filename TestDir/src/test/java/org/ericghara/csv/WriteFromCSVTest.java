package org.ericghara.csv;

import com.opencsv.exceptions.CsvException;
import org.ericghara.csv.dto.TestDirCSVLine;
import org.ericghara.testdir.SizeUnit;
import org.ericghara.testdir.TestDir;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.ericghara.csv.LineType.DIRECTORY;
import static org.ericghara.csv.LineType.FILE;
import static org.ericghara.testdir.SizeUnit.MB;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WriteFromCSVTest {

    WriteFromCSV writer = new WriteFromCSV();

    @Mock
    TestDir testDir;

    @Test
    @DisplayName("write() integration test of nested classes")
    void writeTest() {
        var csv = """
                            F, aFile, 1, MB
                            D, aDir
                        """;
        var csvStream = ReaderUtils.getStringReader(csv);

        writer.write(testDir, csvStream);

        verify(testDir, times(1) ).createDirs(any(String.class) );
        verify(testDir, times(1) )
                .createFile(any(String.class), any(BigDecimal.class), any(SizeUnit.class) );
    }


    @Nested
    @DisplayName("CSVResult tests")
    class CSVResultTests {

        WriteFromCSV.CSVResult results = writer.new CSVResult();

        @Test
        @DisplayName("getResultsBy lineType properly groups results")
        void getResultsByLineTypeGrouping() {
            var input = """
                    F, aFile, 1, MB
                    D, aDir
                    """;
            var reader = ReaderUtils.getStringReader(input);
            Map<LineType, List<TestDirCSVLine>> foundMap = results.getResultsByLineType(reader);

            Map<LineType, List<TestDirCSVLine>> expectedMap = new HashMap<>();
            expectedMap.put(FILE,
                    List.of(new TestDirCSVLine(FILE, "aFile", BigDecimal.ONE, MB ) ) );
            expectedMap.put(DIRECTORY,
                    List.of(new TestDirCSVLine(DIRECTORY, "aDir", null, null ) ) );

            assertEquals(foundMap, expectedMap);
        }


        @ParameterizedTest(name="[{index}] {0}")
        @CsvSource(useHeadersInDisplayName = true, delimiter = '|', quoteCharacter = '\"',
                textBlock = """
            Label                                  | line                  | type | path      | size | unit
            "Parse simple File"                    | F, aFile, 1, MB       | FILE | aFile     |   1  |  MB
            "Parse file with escaped comma"        | F, a\\,File, 1, MB    | FILE | a,File    |   1  |  MB
            "Parse file in a subdir"               | F, aDir/aFile, 1, MB  | FILE | aDir/aFile|   1  |  MB
            "Parse fields with trailing whitespace"| "F , aFile , 1 , MB " | FILE | aFile     |   1  |  MB
            """)
        void getResultsIntegrationTests(String _label, String line, String type, String pathStr, BigDecimal size, String units) {
            var resultsList = results.getResults(ReaderUtils.getStringReader(line));
            assertEquals(1, resultsList.size() );
            var dto = resultsList.get(0);

            assertEquals(type, dto.getType().name() );
            assertEquals(pathStr, dto.getPath() );
            assertEquals(size, dto.getSize() );
            assertEquals(units, dto.getUnit().name() );
        }

        @Test
        @DisplayName("A complex multiline parse with comments, leading whitespace and trailing whitespace")
        void getResultsComplexMultiLineParse() {
            var lines = """
                    # comment
                       # whitespace then comment
                     F, aFile, 1, MB
                    F ,        aFile ,1 ,MB , #comment
                    # XXX
                    D,aDir
                    # comment
                    """ ;
            var expectedFile = new TestDirCSVLine(FILE, "aFile", BigDecimal.ONE, MB);
            var expectedDir = new TestDirCSVLine(DIRECTORY, "aDir", null, null);
            var resultsList = results.getResults(ReaderUtils.getStringReader(lines) );
            assertEquals(3, resultsList.size() );
            assertEquals(expectedFile, resultsList.get(0) );
            assertEquals(expectedFile, resultsList.get(1) );
            assertEquals(expectedDir, resultsList.get(2) );
        }

        @ParameterizedTest(name="[{index}] {0}")
        @CsvSource(useHeadersInDisplayName = true, delimiter = '|', quoteCharacter = '\"',
                textBlock = """
            Label                                  | line                  | expected
            "Parse simple File"                    | F, aFile, 1, MB       | FILE ,aFile, 1, MB
            "Parse file in a subdir"               | F, aDir/aFile, 1, MB  | FILE, aDir/aFile, 1, MB
            "Parse fields with trailing whitespace"| "F , aFile , 1 , MB " | FILE, aFile, 1,  MB
            "Parse a dir"                          | D, aDir               | DIRECTORY, aDir
            """)
        void csvParserTests(String _label, String line, String expected) throws IOException, CsvException {
            var csvReader = results.buildCSVReader(ReaderUtils.getStringReader(line) );
            List<String[]> lines = csvReader.readAll();

            assertEquals(1, lines.size() );

            final String[] foundSplit = lines.get(0);
            var expectedSplit = expected.split("\\s*,\\s*");
            assertArrayEquals(expectedSplit, foundSplit);
        }
    }

    @Nested
    @DisplayName("CSVWriteJob Tests")
    class CSVWriteJobTests {

        @Mock
        Reader csvStream;

        WriteFromCSV.CSVWriteJob job;

        @BeforeEach
        void beforeEach() {
            job = writer.new CSVWriteJob(testDir, csvStream);
        }

        @Test
        @DisplayName("writeFile calls TestDir#createFile with correct args")
        void writeFileTest() {
            var path = "aFile";
            var size = BigDecimal.ONE;

            TestDirCSVLine line = new TestDirCSVLine(FILE, path, size, MB);
            var stringCaptor = ArgumentCaptor.forClass(String.class);
            var bigDecCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            var unitCaptor = ArgumentCaptor.forClass(SizeUnit.class);

            job.writeFile(line);

            verify(testDir, times(1)).createFile(stringCaptor.capture(), bigDecCaptor.capture(), unitCaptor.capture() );

            assertEquals(path, stringCaptor.getValue() );
            assertEquals(size, bigDecCaptor.getValue() );
            assertEquals(MB, unitCaptor.getValue() );
        }

        @Test
        @DisplayName("writeFile throws when DIRECTORY LineType passed")
        void WriteFileThrowsFileLineType() {
            assertThrows(IllegalArgumentException.class, () ->
                    job.writeFile(
                            new TestDirCSVLine(DIRECTORY, "aFile", BigDecimal.ONE, MB) ) );
        }

        @Test
        @DisplayName("writeDir calls TestDir#createDirs with correct args")
        void writeDirTest() {
            var path = "aDir";

            TestDirCSVLine line = new TestDirCSVLine(DIRECTORY, path, null, null);
            var stringCaptor = ArgumentCaptor.forClass(String.class);

            job.writeDirs(line);

            verify(testDir, times(1)).createDirs(stringCaptor.capture() );
            assertEquals(path, stringCaptor.getValue() );
        }

        @Test
        @DisplayName("writeDirs throws when TestDirCSVLine has extraneous arguments")
        void WriteDirsThrowsWhenTooManyArguments() {
            assertThrows(IllegalArgumentException.class, () ->
                    job.writeDirs(
                            new TestDirCSVLine(DIRECTORY, "aDir", BigDecimal.ONE, null) ) );
            assertThrows(IllegalArgumentException.class, () ->
                    job.writeDirs(
                            new TestDirCSVLine(DIRECTORY, "aDir", null, MB) ) );

        }

        @Test
        @DisplayName("writeDirs throws when FILE LineType passed")
        void WriteDirsThrowsFileLineType() {
            assertThrows(IllegalArgumentException.class, () ->
                    job.writeDirs(
                            new TestDirCSVLine(FILE, "aDir", null, null) ) );
        }

        @Test
        @DisplayName("writeLineType throws when LineType is unrecognized")
        void writeLineTypeThrowsWhenLineTypeUnrecognized() {
            Map<LineType, Consumer<TestDirCSVLine>> funcMap = Map.of();
            var job = writer.new CSVWriteJob(testDir, csvStream, funcMap);
            assertThrows(UnsupportedOperationException.class,
                    () -> job.writeLineType(FILE, List.of() ) );
        }

        @Test
        @DisplayName("writeLineType FILE calls TestDir#createFile")
        void writeLineTypeFILECalls() {
            var lineList = List.of(new TestDirCSVLine(FILE, "aFile", BigDecimal.ONE, MB) );
            job.writeLineType(FILE, lineList);
            verify(testDir, times(1)).createFile(any(String.class), any(BigDecimal.class), any(SizeUnit.class) );
        }

        @Test
        @DisplayName("writeLineType DIRECTORY calls TestDir#createFile")
        void writeLineTypeDIRECTORYCalls() {
            var lineList = List.of(new TestDirCSVLine(DIRECTORY, "aFile", null, null) );
            job.writeLineType(DIRECTORY, lineList);
            verify(testDir, times(1)).createDirs(any(String.class) );
        }

        @Test
        @DisplayName("write calls writeLineType expected number of times")
        void writeCallsWriteLineType() {
            var csv = """
                            F, aFile, 1, MB
                            D, aDir
                        """;
            csvStream = ReaderUtils.getStringReader(csv);
            beforeEach();  // reconstruct with an actual reader
            job.write();
            verify(testDir, times(1) ).createDirs(any(String.class) );
            verify(testDir, times(1) ).createFile(any(String.class), any(BigDecimal.class), any(SizeUnit.class) );
        }
    }
}
