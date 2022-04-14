package org.ericghara.csv;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.MappingStrategy;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.ericghara.csv.beanfilter.EmptyLineFilter;
import org.ericghara.csv.dto.TestDirCSVLine;
import org.ericghara.csv.rowprocessor.TestDirRowProcessor;
import org.ericghara.csv.rowvalidator.CorrectNumColumnsValidator;
import org.ericghara.exception.DirCreationException;
import org.ericghara.exception.FileCreationException;
import org.ericghara.exception.ReaderCloseException;
import org.ericghara.testdir.SizeUnit;
import org.ericghara.testdir.TestDir;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static lombok.AccessLevel.PACKAGE;
import static org.ericghara.csv.LineType.DIRECTORY;
import static org.ericghara.csv.LineType.FILE;

/**
 * Facilitates writing a CSV to a new or existing {@link TestDir}.  The CSV may be read in from any {@link Reader}.  The
 * {@link ReaderUtils} class has functions to read common file sources.
 * <p>
 * The CSV format is as follows:
 * <ul>
 *     <li>
 *         A file entry must contain 4 columns.
 *         <ol>
 *             <li>F - this key specifies a file entry type (non case sensitive)</li>
 *             <li>Path - the file path.  Should be a relative file path, e.g. {@code aDir/aFile}, not {@code /aDir/aFile}</li>
 *             <li>size - the size of the file to write in the specified {@code unit}</li>
 *             <li>unit - the unit of the size.  Valid units should correspond to {@link SizeUnit} values
 *             (not case sensitive).</li>
 *         </ol>
 *     </li>
 *     <li>
 *         A directory entry must contain 2 columns.  The columns are read in a fixed order.
 *         <ol>
 *             <li>D - this key specifies a directory entry type (non case sensitive)</li>
 *             <li>Path - the directory path. If the path has spaces, the path must be enclosed in quotes.  If quotes
 *                occur in the path, they must be escaped with: <code>\\</code>.</li></li>
 *         </ol>
 *     </li>
 *     <li>
 *         No header is required.  If a header is desired include it as a comment.
 *     </li>
 *     <li>
 *         The CSV delimiter is a comma (@code{,})
 *     </li>
 *     <li>
 *         Comments are indicated by a {@code #}.  All following text on the line will be ignored
 *     </li>
 *     <li>
 *         Empty lines are supported and simply ignored.  Leading and trailing whitespace in each column
 *         is removed (irrespective of if it is within quotes).  Whitespace within filenames is not removed.
 *     </li>
 *     <li>
 *         Paths that include a comma the path should be enclosed in quotes: {@code "aDir/File,with,commas"}.
 *     </li>
 *     <li>
 *         If a path includes a quote character {@code (")} it must be escaped with a double backslash ({@code //}),<br>
 *         E.g {@code aDir/File\\"with\\"quotes} represents the path {@code aDir/file"with"quotes}
 *     </li>
 *     <br><br>
 *     Example:
 *     <pre>
 *         # This is an example csv file
 *         # FileType  Path        Size  Units(Optional)
 *           D,        aDir
 *           F,        aDir/b File  16,    B  # read in as aDir/b File
 *           F,        bDir/aFile, 1.34   MB
 *     </pre>
 * </ul>
 */
public class WriteFromCSV {

    final private CSVParser parser;

    public WriteFromCSV() {
        parser = buildParser();
    }

    CSVParser buildParser() {
        var builder = new CSVParserBuilder();
        return builder.withEscapeChar('\\')
                .withSeparator(',')
                .withQuoteChar('"')
                .withIgnoreLeadingWhiteSpace(true)
                .build();
    }

    /**
     * Writes a csv, as a {@code reader} character stream to a new {@code TestDir} created at the specified {@code
     * dirPath}.
     *
     * @param dirPath   directory to create the {@code TestDir} in. It must exist and be writeable.
     * @param csvStream {@code reader} reading a properly formatted CSV {@link WriteFromCSV}
     * @return the new {@code TestDir} instance
     * @throws FileCreationException if there is an error creating any file
     * @throws DirCreationException  if there is an error creating any directory
     */
    public TestDir write(Path dirPath, Reader csvStream) throws
            FileCreationException, DirCreationException {
        var testDir = new TestDir(dirPath);
        write(testDir, csvStream);
        return testDir;
    }

    public void write(TestDir testDir, Reader csvStream) throws
            FileCreationException, DirCreationException {
        var writeJob = new CSVWriteJob(testDir, csvStream);
        writeJob.write();
    }

    public class CSVWriteJob {

        private final Map<LineType, Consumer<TestDirCSVLine>> funcMap;

        @NonNull
        private final TestDir testDir;
        @NonNull
        private final Reader csvStream;

        CSVWriteJob(TestDir testDir, Reader csvStream) {
            this.testDir = testDir;
            this.csvStream = csvStream;
            funcMap = buildFuncMap();
        }
        
        /* For Testing */
        CSVWriteJob(TestDir testDir, Reader csvStream,
                    Map<LineType, Consumer<TestDirCSVLine>> funcMap) {
            this.testDir = testDir;
            this.csvStream = csvStream;
            this.funcMap = funcMap;
        }

        void write() throws
                FileCreationException, DirCreationException, IllegalArgumentException {
            var result = new CSVResult();
            Map<LineType, List<TestDirCSVLine>> results = result.getResultsByLineType(csvStream);
            results.forEach(this::writeLineType);
        }

        void writeLineType(LineType lineType, List<TestDirCSVLine> lines) throws UnsupportedOperationException {
            var writeFunc = funcMap.get(lineType);
            if (Objects.isNull(writeFunc)) {
                throw new UnsupportedOperationException(
                        format("Improper configuration.  No writer for %s defined.", lineType));
            }
            lines.forEach(writeFunc);
        }

        Map<LineType, Consumer<TestDirCSVLine>> buildFuncMap() {
            return Map.of(FILE, this::writeFile,
                    DIRECTORY, this::writeDirs);
        }

        void assertCorrectType(TestDirCSVLine line, LineType expectedType) throws IllegalArgumentException {
            var found = line.getType();
            if (expectedType != found) {
                throw new IllegalArgumentException(
                        format("Could not perform the write.  " +
                                "Expected a %s but received a %s", expectedType, found) );
            }
        }

        void writeFile(TestDirCSVLine line) throws FileCreationException, IllegalArgumentException {
            assertCorrectType(line, FILE);
            String pathString = line.getPath();
            BigDecimal size = line.getSize();
            SizeUnit unit = line.getUnit();
            testDir.createFile(pathString, size, unit);
        }

        void writeDirs(TestDirCSVLine line) throws DirCreationException, IllegalArgumentException {
            assertCorrectType(line, DIRECTORY);
            if (Objects.nonNull(line.getSize() ) || Objects.nonNull(line.getUnit() ) ) {
                throw new IllegalArgumentException("Received a TestDirCSV line with extraneous arguments");
            }
            String pathString = line.getPath();
            testDir.createDirs(pathString);
        }
    }

    @NoArgsConstructor(access = PACKAGE)
    public class CSVResult {

        /**
         * Returns a modifiable map with {@code TestDirCsvLine}s grouped by {@code LineType}.  A {@link
         * java.util.Map#get} for a {@code LineType} not found in the map will return {@code null};
         *
         * @param in csv as a {@code reader} character stream
         * @return a modifiable map
         */
        public Map<LineType, List<TestDirCSVLine>> getResultsByLineType(Reader in) {
            return getResults(in).stream()
                    .collect(Collectors.groupingBy(TestDirCSVLine::getType) );
        }

        /**
         * Returns an unmodifiable list of {@code TestDirCsvLine}s parsed from {@code csvStream}.
         *
         * @param csvStream the CSV data as a {@code reader} character stream
         * @return an unmodifiable list
         * @throws ReaderCloseException if the {@code csvStream} cannot be closed.
         */
        public List<TestDirCSVLine> getResults(Reader csvStream) throws ReaderCloseException {
            try (csvStream) {
                var reader = buildCSVReader(csvStream);
                var toBeans = buildCsvToBean(reader);
                return toBeans.parse();
            } catch (IOException e) {
                throw new ReaderCloseException("Failure closing the csvStream.", e);
            }
        }

        CSVReader buildCSVReader(Reader in) {
            var builder = new CSVReaderBuilder(in);
            var rowProcessor = new TestDirRowProcessor();
            var rowValidator = new CorrectNumColumnsValidator();
            return builder.withCSVParser(parser)
                    .withRowProcessor(rowProcessor)
                    .withRowValidator(rowValidator)
                    .build();
        }

        MappingStrategy<TestDirCSVLine> buildMappingStrategy() {
            ColumnPositionMappingStrategy<TestDirCSVLine> strategy = new ColumnPositionMappingStrategy<>();
            strategy.setType(TestDirCSVLine.class);
            return strategy;
        }

        CsvToBean<TestDirCSVLine> buildCsvToBean(CSVReader csvReader) {
            var filter = new EmptyLineFilter();
            var builder = new CsvToBeanBuilder<TestDirCSVLine>(csvReader);
            return builder
                    .withMappingStrategy(buildMappingStrategy() )
                    .withFilter(filter)
                    .build();
        }

    }
}
