package org.ericghara.write;

import org.apache.commons.math3.stat.inference.ChiSquareTest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static java.lang.String.format;


/**
 * Chi-square "goodness of fit" test for the hypothesis: frequencies of byte values in
 * the file represent a random distribution.
 */
public class RandomByteFrequenciesTest {

    private static final int RADIX = 256;
    private static final int DEGREES_FREEDOM = RADIX - 1;

    private final Path filePath;
    private final int size; // bytes

    private final double[] expectedFreqDistribution;
    private final long[] observedFreqs;

    public RandomByteFrequenciesTest(Path filePath) throws IOException, ArithmeticException {
        this.filePath = filePath;
        size = validateSize(filePath);
        observedFreqs = observedFrequencies();
        expectedFreqDistribution = calcExpectedFreqDistribution();
    }

    /**
     * Tests if the file does not conform to a random frequency distribution with confidence {@code 1 - alpha}.
     * @param alpha significance level of test 0 < alpha < 0.5
     * @return true iff null hypothesis can be rejected with confidence 1 - alpha
     */
    public boolean probablyNonRandom(double alpha) {
        var test = new ChiSquareTest();
        return test.chiSquareTest(expectedFreqDistribution, observedFreqs, alpha);
    }

    public Path getPath() {
        return filePath;
    }

    public long[] observedFreqs() {
        return observedFreqs;
    }

    public double[] expectedFreqDistribution() {
        return expectedFreqDistribution;
    }

    long[] observedFrequencies() throws IOException {
        long[] freqs = new long[RADIX];
        var bytes = readFile();

        while (bytes.hasRemaining()) {
            var b = bytes.get();
            var i = Byte.toUnsignedInt(b);
            freqs[i]++;
        }
        return freqs;
    }

    static double[] calcExpectedFreqDistribution() {
        var freqDist = new double[RADIX];
        double p = 1D / RADIX;
        Arrays.fill(freqDist, p);
        return freqDist;
    }

    ByteBuffer readFile() throws IOException {
        var bytes = ByteBuffer.allocateDirect(size);
        Files.newByteChannel(filePath).read(bytes);
        bytes.rewind();
        return bytes;
    }

    // Returns size or throws
    static int validateSize(Path filePath) throws IOException {
        long sizeB = Files.size(filePath);

        if (sizeB > Integer.MAX_VALUE) {
            throw new ArithmeticException("File is too big");
        }
        if (sizeB < RADIX * 10) {
            throw new IllegalStateException(format("File size must be >%d bytes for accurate results" +
                    " with %d degrees of freedom", 10 * RADIX, RADIX));
        }
        return (int) sizeB;
    }
}




