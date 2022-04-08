package org.ericghara.parser.rowprocessor;

import com.opencsv.processor.RowProcessor;
import org.ericghara.parser.LineType;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestDirRowProcessor implements RowProcessor {

    private final String hashCommentsRegEx = "(\\b|^)\\s*#.*$";
    private final Matcher hashComments = Pattern.compile(hashCommentsRegEx).matcher("");

    private final Map<String, LineType> enumMap;

    record ProcessedColumn(boolean hasComment, String text){}

    public TestDirRowProcessor() {
        enumMap = initEnumMap();
    }

    Map<String,LineType> initEnumMap() {
        return Stream.of(LineType.values() )
                .collect(Collectors.toUnmodifiableMap(
                        e -> e.getKey()
                                .toUpperCase(),
                        e -> e  ) );
    }

    @Override
    public void processRow(String[] row) {
        rowStripper(row);
        assignLineType(row);
    }

    @Override
    public String processColumnItem(String column) throws UnsupportedOperationException {
        // this method does not fit with the requirement to do multiple different types
        // of processing.  Method never called externally according to docs.
        throw new UnsupportedOperationException("Method not implemented");
    }

    void rowStripper(String[] row) {
        Function<String, ProcessedColumn> whiteSpaceStripper = this::processComment;
        for (int i = 0; i < row.length; i++) {
            stripWhitespace(i, row);
            ProcessedColumn col = whiteSpaceStripper.apply(row[i]);
            if (col.hasComment() ) {
                row[i] = col.text();
                whiteSpaceStripper = this::returnsEmptyText;
            }
        }
    }

    void stripWhitespace(int i, String[] row) {
        row[i] = row[i].stripLeading().stripTrailing();
    }

    ProcessedColumn returnsEmptyText(String column) {
        return new ProcessedColumn(true, "");
    }


    ProcessedColumn processComment(String column) {
        hashComments.reset(column);
        boolean hasComment = hashComments.find();
        if (hasComment) {
            String strippedColumn = column.substring(0, hashComments.start() );
            return new ProcessedColumn(hasComment, strippedColumn);
        }
        return new ProcessedColumn(hasComment, column);
    }

    void assignLineType(String[] row) {
        final int LINE_TYPE_ROW = 0;
        if (row.length >= 1) {
            var lineTypeKey = row[LINE_TYPE_ROW].toUpperCase();
            LineType lineType = enumMap.get(lineTypeKey);
            if (Objects.nonNull(lineType) ) {
                row[LINE_TYPE_ROW] = lineType.name();
            }
        }
    }

}
