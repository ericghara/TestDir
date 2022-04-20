package org.ericghara.write.bytesupplier;

import org.ericghara.exception.ByteUnderflowException;

/**
 * A functional interface to supply bytes
 */
@FunctionalInterface
public interface ByteSupplier {

    /**
     * Gets a byte
     * @return a byte
     * @throws ByteUnderflowException if a byte cannot be supplied
     */
    byte getAsByte() throws ByteUnderflowException;
}
