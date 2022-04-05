package org.ericghara.parser;

import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ericghara.SizeUnit;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestDirCsvLine {

    @CsvBindByPosition(required = true, position = 0)
    private LineType lineType;

    @CsvBindByPosition(required = true,  position = 1)
    private String pathStr;

    @CsvBindByPosition(position = 2)
    private BigDecimal size;

    @CsvBindByPosition(position = 3)
    private SizeUnit unit;
}
