package org.ericghara.write.bytesupplier;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.ericghara.exception.ByteUnderflowException;

import java.io.IOException;
import java.io.InputStream;

@AllArgsConstructor
public class InputStreamByteSupplier implements ByteSupplier, AutoCloseable {

    @NonNull
    private final InputStream stream;

    private RuntimeException autoClose(ByteUnderflowException e) {
        try (this) {
            return e;
        } catch (IOException ex) {
            ex.addSuppressed(e);
            return new IllegalStateException("The input stream is empty and could not be closed.", ex);
        }
    }

    @Override
    public byte getAsByte() throws ByteUnderflowException, IllegalStateException {
        int byteInt;
        try {
            byteInt = stream.read();
        } catch (IOException e) {
            var ex = new ByteUnderflowException("Could not read next byte from the inputStream.", e);
            throw autoClose(ex);
        }
        if (byteInt == -1) {
            var ex = new ByteUnderflowException("The input stream is empty.");
            throw autoClose(ex);
        }
        return (byte) byteInt;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
