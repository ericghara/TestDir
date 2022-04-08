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
        var res = !line[0].isEmpty();
        return res;
    }
}
