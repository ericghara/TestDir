package org.ericghara;

public enum Constant {

    BLOCK_SIZE(4096),
    BYTES_PER_LONG(8);

    public final int value;

    Constant(int i) {
        value = i;
    }

}
