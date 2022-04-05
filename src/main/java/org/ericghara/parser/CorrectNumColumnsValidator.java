package org.ericghara.parser;

import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.validators.RowValidator;
import org.ericghara.SizeUnit;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CorrectNumColumnsValidator implements RowValidator {

    private final Set<String> unitSet;

    public CorrectNumColumnsValidator() {
        unitSet = getUnitSet();
    }

    Set<String> getUnitSet() {
        return Stream.of(SizeUnit.values())
                     .map(e-> e.name().toUpperCase() )
                     .collect(Collectors.toUnmodifiableSet());

    }

    @Override
    public boolean isValid(String[] row) {
        int last;
        try {
            last = lastBlank(row);
        } catch (IllegalStateException e) {
            return false;
        }
        if (last == 0 ) { // blank line.  Bean filter will filter
            return true;
        }
        if (last == 2 && row[0].equals("D") ) {
            return true;
        }
        if (last == 3
                && row[0].equals("F")
                && unitSet.contains(row[2]) ) {
            return true;
        }
        return false;
    }

    int lastBlank(String[] row) throws IllegalStateException {
        if (row.length == 0) {
            return 0;
        }
        int last = row.length;
        for (int i = 0; i < row.length; i++) {
            String col = row[i];
            if (Objects.isNull(col) ) {
                throw new IllegalStateException("Received a null column");
            }
            if (col.isBlank() ) {
                last = i;
            }
        }
        return last;
    }



    @Override
    public void validate(String[] row) throws CsvValidationException {
        if (!isValid(row) ) {
            throw new CsvValidationException("Caught an improperly formatted row");
        }
    }
}
