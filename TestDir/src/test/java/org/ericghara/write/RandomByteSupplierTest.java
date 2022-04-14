package org.ericghara.write;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.SplittableRandom;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RandomByteSupplierTest {

    @Mock
    SplittableRandom random;


    @Test
    void getAsByteReturnsExpectedValues() {
        long l = 0x0706050403020100L;
        when(random.nextLong())
                .thenReturn(l);
        var supplier = new RandomByteSupplier(random);
        for (byte b = 0; b < 17; b++) {
            assertEquals(b % 8, supplier.getAsByte()  );
        }
    }

    @Test
    void getAsByteCallEfficiencyNextLong() {
        long l = -1L;
        when(random.nextLong())
                .thenReturn(l);
        var supplier = new RandomByteSupplier(random);
        IntStream.range(0, 17)
                 .forEach( i -> supplier.getAsByte() );
        verify(random, times(3) )
                .nextLong();
    }
}