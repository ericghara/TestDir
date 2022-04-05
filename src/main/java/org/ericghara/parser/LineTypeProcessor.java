package org.ericghara.parser;

import com.opencsv.processor.RowProcessor;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LineTypeProcessor implements RowProcessor {

    private final Map<String,LineType> enumMap;

    public LineTypeProcessor() {
        enumMap = initEnumMap();
    }

    Map<String,LineType> initEnumMap() {
        return Stream.of(LineType.values() )
                     .collect(Collectors.toUnmodifiableMap(
                             e -> e.getKey().toUpperCase(),
                             e -> e  ) );
    }

    @Override
    public String processColumnItem(String column) {
        LineType val = enumMap.get(column.toUpperCase() );
        if (Objects.nonNull(val) ) {
            return val.name();
        }
        return column;
    }

    @Override
    public void processRow(String[] row) {
        if (row.length >= 1) {
            row[0] = processColumnItem(row[0]);
        }
    }
}
