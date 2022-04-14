package org.ericghara.write;

import org.ericghara.exception.ByteUnderflowException;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntStreamByteSupplierTest {

    @Test
    void getAsByteReturnsExpectedBytes() {
        var stream = IntStream.range(0, 5);

        var supplier = new IntStreamByteSupplier(stream);
        IntStream.range(0,5)
                 .forEach( i ->
                    assertEquals( (byte) i, supplier.getAsByte() ));
    }

    @Test
    void getAsByteThrowsWhenEmpty() {
        var stream = IntStream.range(0, 5);
        var supplier = new IntStreamByteSupplier(stream);

        assertThrows(ByteUnderflowException.class,
                () -> IntStream.range(0,6)
                               .forEach(i -> supplier.getAsByte() ) );
    }

}