package org.ericghara.parser;

import com.opencsv.processor.RowProcessor;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommentProcessor implements RowProcessor {

    private final String hashCommentsRegEx = "(\\b|^)\\s*#.*$";
    private final Matcher hashComments = Pattern.compile(hashCommentsRegEx).matcher("");

    record ProcessedColumn(boolean hasComment, String text){}

    @Override
    public String processColumnItem(String column) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Method not implemented");
    }

    ProcessedColumn processColumn(String column) {
        hashComments.reset(column);
        boolean hasComment = hashComments.find();
        if (hasComment) {
            String strippedColumn = column.substring(0, hashComments.start() );
            return new ProcessedColumn(hasComment, strippedColumn);
        }
        return new ProcessedColumn(hasComment, column);
    }

    ProcessedColumn returnsEmptyText(String column) {
        return new ProcessedColumn(true, "");
    }

    @Override
    public void processRow(String[] row) {
        Function<String, ProcessedColumn> processor = this::processColumn;
        for (int i = 0; i < row.length; i++) {
            ProcessedColumn col = processor.apply(row[i]);
            if (col.hasComment() ) {
                row[i] = col.text();
                processor = this::returnsEmptyText;
            }
        }
    }
}
