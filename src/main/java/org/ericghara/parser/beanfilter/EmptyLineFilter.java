package org.ericghara.parser.beanfilter;

import com.opencsv.bean.CsvToBeanFilter;
import lombok.NoArgsConstructor;

/**
 * Filters lines that are empty after processing.
 */
@NoArgsConstructor
public class EmptyLineFilter implements CsvToBeanFilter {

    @Override
    public boolean allowLine(String[] line) {
        // based on CorrectNumColumnsValidator rules
        // if line[0] is empty, all must be empty;
        return line[0].isEmpty();  // this must be a line with all empties
    }
}
