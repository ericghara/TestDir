package org.ericghara.csv.dto;

import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.ericghara.csv.LineType;
import org.ericghara.testdir.SizeUnit;

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
