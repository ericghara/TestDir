package org.ericghara.parser.beanfilter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmptyLineFilterTest {

    EmptyLineFilter filter = new EmptyLineFilter();

    @Test
    void allowLineReturnsTrueForBlank() {
        String[] firstBlank = {"", "notBlank"};
        assertTrue(filter.allowLine(firstBlank) );
    }

    @Test
    void allowLineReturnsFalseForFull() {
        String[] firstSpace = {" ", "notBlank"};
        assertFalse(filter.allowLine(firstSpace) );
    }
}