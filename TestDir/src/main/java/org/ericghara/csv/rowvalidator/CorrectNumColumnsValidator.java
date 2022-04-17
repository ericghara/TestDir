package org.ericghara.csv.rowvalidator;

import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.validators.RowValidator;
import org.ericghara.core.SizeUnit;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.ericghara.csv.LineType.DIRECTORY;
import static org.ericghara.csv.LineType.FILE;

public class CorrectNumColumnsValidator implements RowValidator {

    private final Set<String> unitSet;

    public CorrectNumColumnsValidator() {
        unitSet = initUnitSet();
    }

    Set<String> initUnitSet() {
        return Stream.of(SizeUnit.values())
                     .map(e-> e.name().toUpperCase() )
                     .collect(Collectors.toUnmodifiableSet());

    }

    @Override
    public boolean isValid(String[] row) {
        int last;
        try {
            last = lastFull(row);
        } catch (NullPointerException e) {
            return false;
        }
        switch (last) {
            case -1 -> {
                return true;
            }
            case 1 -> {
                return row[0].equals(DIRECTORY.name() );
            }
            case 3 -> {
                return row[0].equals(FILE.name() )
                        && unitSet.contains(row[3]);
            }
            default -> {
                return false;
            }
        }
    }

    int lastFull(String[] row) throws NullPointerException {
        Objects.requireNonNull(row, "Received a null row");
        int last = -1;
        for (int i = 0; i < row.length; i++) {
            String col = row[i];
            Objects.requireNonNull(row, "Received a null column.  Col: " + i);
            if (!col.isBlank() ) {
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
