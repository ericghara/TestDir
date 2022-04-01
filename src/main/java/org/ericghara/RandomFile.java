package org.ericghara;

import java.math.BigDecimal;
import java.nio.file.Path;

import static java.lang.String.format;

/**
 * A uniformly random file.
 */
public class RandomFile extends DummyFile {

    public RandomFile(Path filePath, long sizeB) {
        super(filePath);
        randomFill(sizeB);
    }

    public RandomFile(Path filePath, BigDecimal size, SizeUnit unit) {
        this(filePath, unit.toBytes(size) );
    }

    // for testing
    RandomFile(Path filePath) {
        super(filePath);
    }

    @Override
    public String toString() {
        return format("%s %s", this.getClass(), filePath);
    }

    @Override
    public boolean equals(Object obj) {
        boolean eq;
        switch (obj) {
            case null -> eq = false;
            case RandomFile other &&
                    this.filePath.equals(other.filePath) -> eq = true;
            default -> eq = false;
        }
        return eq;
    }

    void randomFill(long sizeB) {
        var writer = new FileWriter(filePath);
        writer.write(sizeB,
                new RandomByteSupplier() );
    }

}
