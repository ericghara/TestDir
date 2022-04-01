package org.ericghara;

import org.ericghara.exception.ByteUnderflowException;

@FunctionalInterface
public interface ByteSupplier {

    byte getAsByte() throws ByteUnderflowException;
}
