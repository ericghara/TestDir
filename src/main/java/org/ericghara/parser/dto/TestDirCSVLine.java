package org.ericghara.parser.dto;

import com.opencsv.bean.CsvBindByPosition;
import lombok.*;
import org.ericghara.SizeUnit;
import org.ericghara.parser.LineType;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class TestDirCSVLine {

    @CsvBindByPosition(position = 0)
    private LineType type;

    @CsvBindByPosition(position = 1)
    private String path;

    @CsvBindByPosition(position = 2)
    private BigDecimal size;

    @CsvBindByPosition(position = 3)
    private SizeUnit unit;
}