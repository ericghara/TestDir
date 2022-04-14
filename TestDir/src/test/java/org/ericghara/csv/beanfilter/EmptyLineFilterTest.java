package org.ericghara.csv.beanfilter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmptyLineFilterTest {

    EmptyLineFilter filter = new EmptyLineFilter();

    @Test
    void allowLineReturnsTrueForBlank() {
        String[] firstBlank = {"", "notBlank"};
        assertFalse(filter.allowLine(firstBlank) );
    }

    @Test
    void allowLineReturnsFalseForFull() {
        String[] firstSpace = {" ", "notBlank"};
        assertTrue(filter.allowLine(firstSpace) );
    }
}