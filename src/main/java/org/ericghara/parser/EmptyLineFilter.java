package org.ericghara.parser;

import com.opencsv.bean.CsvToBeanFilter;

/**
 * Filters lines that are empty after processing.
 */
public class EmptyLineFilter implements CsvToBeanFilter {

    @Override
    public boolean allowLine(String[] line) {
        return line[0].isEmpty();  // this must be a line with all blanks
    }
}
