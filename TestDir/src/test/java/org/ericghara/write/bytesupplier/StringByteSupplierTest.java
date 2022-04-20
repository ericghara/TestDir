package org.ericghara.write.bytesupplier;

import org.ericghara.exception.ByteUnderflowException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.ericghara.core.SizeUnit.KB;
import static org.junit.jupiter.api.Assertions.*;

class StringByteSupplierTest {

    String input = "Test";
    StringByteSupplier supplier = new StringByteSupplier(input);
    byte[] expectedBytes = input.getBytes(StandardCharsets.UTF_8);

    @Test
    @DisplayName("Remaining returns the expected number of bytes")
    void remaining() {
        assertEquals(supplier.remaining(), expectedBytes.length);
    }

    @Test
    @DisplayName("Remaining returns the expected number of KB")
    void testRemaining() {
        assertEquals(KB.fromBytes(expectedBytes.length), supplier.remaining(KB) );
    }

    @Test
    @DisplayName("getAsByte supplies the expected number of bytes")
    void getAsByteSuppliesExpectedNumBytes() {
        for (var _b : expectedBytes) {
            assertDoesNotThrow( () -> supplier.getAsByte() );
        }
        assertThrows(ByteUnderflowException.class, () -> supplier.getAsByte() );
    }

    @Test
    @DisplayName("getAsByte supplies the expected bytes")
    void getAsByteSuppliesExpectedBytes() {
        for (var b: expectedBytes) {
            assertEquals(b, supplier.getAsByte() );
        }
    }
}