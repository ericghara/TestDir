package org.ericghara.testdir;

import java.math.BigDecimal;

import static java.math.MathContext.UNLIMITED;
import static java.math.RoundingMode.CEILING;

public enum SizeUnit {

    B(0),
    KB(1),
    MB (2),
    GB(3);

    public final BigDecimal bytesPerUnit;
    public final BigDecimal BYTES_PER_KB = BigDecimal.valueOf(1024L);

    SizeUnit(int orderOfMag) {
        bytesPerUnit = BYTES_PER_KB.pow(orderOfMag);
    }

    public long toBytes(BigDecimal units) throws ArithmeticException {
        return bytesPerUnit.multiply(units)
                    .setScale(0, CEILING)
                    .longValueExact();
    }

    public BigDecimal fromBytes(long bytes) throws ArithmeticException {
        var numBytes = new BigDecimal(bytes);
        return numBytes.divide(bytesPerUnit, UNLIMITED);
    }
}
