package org.ericghara.write;

import org.ericghara.exception.ByteUnderflowException;

@FunctionalInterface
public interface ByteSupplier {

    byte getAsByte() throws ByteUnderflowException;
}
