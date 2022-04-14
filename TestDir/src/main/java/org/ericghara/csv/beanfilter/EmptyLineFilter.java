package org.ericghara.csv.beanfilter;

import com.opencsv.bean.CsvToBeanFilter;
import lombok.NoArgsConstructor;

/**
 * Filters lines that are empty after processing.
 */
@NoArgsConstructor
public class EmptyLineFilter implements CsvToBeanFilter {

    @Override
    public boolean allowLine(String[] line) {
        return !line[0].isEmpty();
    }
}
