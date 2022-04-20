package org.ericghara.write.bytesupplier;

import org.ericghara.exception.ByteUnderflowException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class InputStreamByteSupplierTest {

    byte[] expectedBytes = "Test".getBytes();
    ByteArrayInputStream stream = new ByteArrayInputStream(expectedBytes);
    InputStreamByteSupplier supplier = new InputStreamByteSupplier(stream);

    @Test
    @DisplayName("getAsByte reads the expected number of bytes")
    void getAsByteReadsExpectedNumBytes() {
        for (var _b : expectedBytes) {
            assertDoesNotThrow(() -> supplier.getAsByte());
        }
        assertThrows(ByteUnderflowException.class, () -> supplier.getAsByte());
    }

    @Test
    @DisplayName("getAsByte throws IllegalStateException when no bytes to supply and error closing")
    void getAsByteThrowsWhenErrorClosing() throws IOException {
        var streamSpy = spy(stream);
        var supplier = new InputStreamByteSupplier(streamSpy);
        doThrow(new IOException()).when(streamSpy).close();

        for (var _b : expectedBytes) {
            assertDoesNotThrow(supplier::getAsByte);
        }
        assertThrows(IllegalStateException.class, supplier::getAsByte);
    }
}