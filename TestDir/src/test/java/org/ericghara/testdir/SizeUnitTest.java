package org.ericghara.testdir;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.ericghara.testdir.SizeUnit.B;
import static org.ericghara.testdir.SizeUnit.KB;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SizeUnitTest {

    @Test
    void toBytesAlwaysRoundsUp() {
        var bytes = new BigDecimal("1.00000000001");
        assertEquals(2L, B.toBytes(bytes) );
    }

    @Test
    void toBytesPerformsCorrectConversion() {
        var kb = new BigDecimal("2.5");
        assertEquals(2560L, KB.toBytes(kb) );
    }

    @Test
    void fromBytesPerformsCorrectConversion() {
        var bytes = 2560L;
        var expected = new BigDecimal("2.5");
        assertEquals(expected, KB.fromBytes(bytes) );
    }
}
