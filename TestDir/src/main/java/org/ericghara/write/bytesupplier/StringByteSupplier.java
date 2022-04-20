package org.ericghara.write.bytesupplier;

import lombok.NonNull;
import org.ericghara.core.SizeUnit;
import org.ericghara.exception.ByteUnderflowException;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class StringByteSupplier implements ByteSupplier {

    private final ByteBuffer bytes;

    /**
     * Creates a {@link ByteSupplier} from a string.
     *
     * @param string to convert to bytes using default {@link Charset} for encoding
     * @see Charset#defaultCharset()
     */
    public StringByteSupplier(String string) {
        this(string, Charset.defaultCharset() );
    }

    /**
     * Creates a {@link ByteSupplier} from a string.
     *
     * @param string to convert to bytes
     * @param charSet the charset to use for encoding
     */
    public StringByteSupplier(@NonNull String string, @NonNull Charset charSet) {
        bytes = ByteBuffer.wrap(string.getBytes(charSet) );
    }

    /**
     * Number of remaining bytes that may be supplied.
     *
     * @return number of bytes remaining
     */
    public int remaining() {
        return bytes.remaining();
    }

    /**
     * Number of {@code unit} remaining to be supplied.  A convenience
     * method that converts {@link StringByteSupplier#remaining()} to the
     * provided unit.
     *
     * @param unit of size
     * @return amount of data remaining to be supplied
     */
    public BigDecimal remaining(SizeUnit unit) {
        return unit.fromBytes(remaining() );
    }

    /**
     * Get the next byte.
     *
     * @return a byte
     * @throws ByteUnderflowException if there are no bytes left to supply
     */
    @Override
    public byte getAsByte() throws ByteUnderflowException {
        try {
            return bytes.get();
        }
        catch (java.nio.BufferUnderflowException e) {
            throw new ByteUnderflowException("There are no remaining bytes to supply", e);
        }
    }
}
