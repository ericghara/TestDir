package org.ericghara.parser;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.NoArgsConstructor;
import org.ericghara.SizeUnit;
import org.ericghara.TestDir;
import org.ericghara.exception.DirCreationException;
import org.ericghara.exception.FileCreationException;
import org.ericghara.exception.ReaderCloseException;
import org.ericghara.parser.beanfilter.EmptyLineFilter;
import org.ericghara.parser.entity.TestDirCSVLine;
import org.ericghara.parser.rowprocessor.CommentAndLineTypeProcessor;
import org.ericghara.parser.rowvalidator.CorrectNumColumnsValidator;

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
import static org.ericghara.parser.LineType.DIRECTORY;
import static org.ericghara.parser.LineType.FILE;

/**
 * Facilitates writing a CSV to a new or existing {@link TestDir}.  The CSV may be read in from any {@link Reader}.  The
 * {@link ReaderUtils} class has functions to read common file sources.
 * <p>
 * The CSV format is as follows:
 * <ul>
 *     <li>
 *         A file entry must contain 3-4 columns.  The columns are read in a fixed order.
 *         <ol>
 *             <li>F - this key specifies a file entry type (non case sensitive)</li>
 *             <li>Path - the file path, if the path has spaces, the path must be enclosed in quotes.  If quotes
 *             occur in the path, they must be escaped with: <code>\\</code>.</li>
 *             <li>size - the size of the file to write in the specified {@code unit}</li>
 *             <li>unit - (optional) the unit of the size.  Valid units should correspond to {@link SizeUnit} values (not case sensitive).  The default is MB.</li>
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
 *         The CSV delimiter is a space
 *     </li>
 *     <li>
 *         A delimiter in quotes is ignored (e.g. {@code "ignored delimiter"})
 *     </li>
 *     <li>
 *         Comments are indicated by a {@code #}.  All following text on the line will be ignored
 *     </li>
 *     <li>
 *         Empty lines are supported and simply ignored
 *     </li>
 *     <li>
 *         No header is required.  If a header is desired include it as a comment.
 *     </li>
 *     <br><br>
 *     Example:
 *     <pre>
 *         # This is an example csv file
 *         # FileType   Path            Size  Units(Optional)
 *           D          aDir
 *           F          "aDir/b File"   16    B
 *           F          bDir/aFile      1.34         # defaults to MB, bDir is implicitly created
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

        private final TestDir testDir;
        private final Reader csvStream;

        CSVWriteJob(TestDir testDir, Reader csvStream) {
            this.testDir = testDir;
            this.csvStream = csvStream;
            funcMap = buildFuncMap();
        }

        void write() throws FileCreationException, DirCreationException {
            var result = new CSVResult();
            Map<LineType, List<TestDirCSVLine>> results = result.getResultsByLineType(csvStream);
            results.forEach(this::writeLineType);
        }

        void writeLineType(LineType lineType, List<TestDirCSVLine> lines) throws UnsupportedOperationException {
            var writer = funcMap.get(lineType);
            if (Objects.isNull(writer)) {
                throw new UnsupportedOperationException(
                        format("Improper configuration.  No writer for %s defined.", lineType));
            }
            lines.forEach(writer);
        }

        Map<LineType, Consumer<TestDirCSVLine>> buildFuncMap() {
            return Map.of(FILE, this::writeFile,
                    DIRECTORY, this::writeDir);
        }

        void writeFile(TestDirCSVLine line) throws FileCreationException {
            String pathString = line.getPathStr();
            BigDecimal size = line.getSize();
            SizeUnit unit = line.getUnit();
            testDir.createFile(pathString, size, unit);
        }

        void writeDir(TestDirCSVLine line) throws DirCreationException {
            String pathString = line.getPathStr();
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
                    .collect(Collectors.groupingBy(TestDirCSVLine::getLineType));
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
                return toBeans.stream().toList();
            } catch (IOException e) {
                throw new ReaderCloseException("Failure closing the csvStream.", e);
            }
        }

        CSVReader buildCSVReader(Reader in) {
            var builder = new CSVReaderBuilder(in);
            var rowProcessor = new CommentAndLineTypeProcessor();
            var rowValidator = new CorrectNumColumnsValidator();
            return builder.withCSVParser(parser)
                    .withRowProcessor(rowProcessor)
                    .withRowValidator(rowValidator)
                    .build();
        }

        CsvToBean<TestDirCSVLine> buildCsvToBean(CSVReader csvReader) {
            var filter = new EmptyLineFilter();
            var builder = new CsvToBeanBuilder<TestDirCSVLine>(csvReader);
            return builder.withType(TestDirCSVLine.class)
                    .withFilter(filter)
                    .build();
        }

    }
}
