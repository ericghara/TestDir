package org.ericghara.write.bytesupplier;

import java.util.SplittableRandom;

public class RandomByteSupplier implements ByteSupplier {

    private static final int BYTES_PER_LONG = Long.BYTES;
    private static final int BITS_PER_BYTE = 8;

    private final SplittableRandom random;
    private long l;
    private int i;

    public RandomByteSupplier() {
        this(new SplittableRandom() );
    }

    public RandomByteSupplier(SplittableRandom random) {
        this.random = random;
        i = BYTES_PER_LONG;
    }

    void nextLong() {
        l = random.nextLong();
        i = 0;
    }

    @Override
    public byte getAsByte() {
        /* Trying to minimize # of random #'s generated;
         nextLong() is origin of all randoms from
        SplittableRandom */
        if (i >= BYTES_PER_LONG) {
            nextLong();
        }
        byte b = (byte) (0xFF & l);
        l >>= BITS_PER_BYTE;
        i++;
        return b;
    }
}
