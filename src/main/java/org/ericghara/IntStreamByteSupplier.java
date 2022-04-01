package org.ericghara;

import org.ericghara.exception.ByteUnderflowException;

import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class IntStreamByteSupplier implements ByteSupplier {

    private final Spliterator.OfInt spliterator;
    private final IntConsumer consumer;
    private byte b;

    public IntStreamByteSupplier(IntStream stream) {
        spliterator = stream.spliterator();
        consumer = getIntConsumer();
    }

    IntConsumer getIntConsumer() {
        return (int i) -> b = (byte) i;
    }

    @Override
    public byte getAsByte() throws ByteUnderflowException {
        if (!spliterator.tryAdvance(consumer) ) {
            throw new ByteUnderflowException("Stream is empty");
        }
        return b;
    }
}
