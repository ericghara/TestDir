package org.ericghara.core;

import java.math.BigDecimal;

import static java.math.MathContext.UNLIMITED;
import static java.math.RoundingMode.CEILING;

/**
 * Represents different file size units
 */
public enum SizeUnit {

    B(0),
    KB(1),
    MB (2),
    GB(3);
    /**
     * Number of bytes represented by this unit.
     */
    public final BigDecimal bytesPerUnit;
    /**
     * Number of bytes per kilobyte
     */
    public final BigDecimal BYTES_PER_KB = BigDecimal.valueOf(1024L);


    SizeUnit(int orderOfMag) {
        bytesPerUnit = BYTES_PER_KB.pow(orderOfMag);
    }

    /**
     * Converts the number of units to bytes
     * @param numUnits number of this {@link SizeUnit}
     * @return number of bytes
     * @throws ArithmeticException if any conversion error occurs
     */
    public long toBytes(BigDecimal numUnits) throws ArithmeticException {
        return bytesPerUnit.multiply(numUnits)
                    .setScale(0, CEILING)
                    .longValueExact();
    }

    /**
     * Converts from bytes to this {@link SizeUnit}
     *
     * @param numBytes number of bytes
     * @return number of {@link SizeUnit} as a {@link BigDecimal}
     * @throws ArithmeticException if any conversion error occurs
     */
    public BigDecimal fromBytes(long numBytes) throws ArithmeticException {
        var n = new BigDecimal(numBytes);
        return n.divide(bytesPerUnit, UNLIMITED);
    }
}
