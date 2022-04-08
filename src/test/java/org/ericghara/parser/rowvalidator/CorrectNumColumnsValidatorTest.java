package org.ericghara.parser.rowvalidator;

import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CorrectNumColumnsValidatorTest {

    CorrectNumColumnsValidator validator = new CorrectNumColumnsValidator();

    @Test
    @DisplayName("lastBlank(String[]) throws on null row")
    void lastBlankThrowsOnNullRow() {
        assertThrowsExactly(NullPointerException.class,
                () -> validator.lastFull(null),
                "Received a null row");
    }

    @Test
    @DisplayName("lastBlank(String[]) throws on null column")
    void lastBlankThrowsOnNullColumn() {
        String[] row = {"a", "b", null};
        assertThrowsExactly(NullPointerException.class,
                () -> validator.lastFull(row),
                "Received a null column.  Col: 1");
    }

    @ParameterizedTest(name="[{index}] {0}")
    @CsvSource(useHeadersInDisplayName = true, delimiter = '|', quoteCharacter = '\"',
            textBlock = """
            Label                                 | row            | expected
            "all blank returns -1"                |       ",,"     | -1
            "{"text ", "", "", "text"} returns 3" | "text ,,,text" | 3
            "{"a","b","c",""} returns 2"          | "a, b, c,"     | 2
            """)
    void lastBlankTest(String _label, String arrayStr, int expected) {
        var row = arrayStr.split(",");
        assertEquals(expected, validator.lastFull(row) );
    }

    @Test
    @DisplayName("Returns -1 for zero length row")
    void lastBlankZeroLengthRow() {
        var row = new String[0];
        assertEquals(-1, validator.lastFull(row) );
    }

    @Test
    @DisplayName("Validate throws CsvValidation exception when isValid() = false")
    void validateThrows() {
        String[] row = {"col0", "col1", "col2"};
        CorrectNumColumnsValidator spy = spy(validator);
        doReturn(false).when(spy).isValid(any(String[].class) );
        assertThrows(CsvValidationException.class, () -> spy.validate(row) );
    }

    @Test
    @DisplayName("Validate does not throw when isValid() = true")
    void validateDoesNotThrow() {
        String[] row = {"col0", "col1", "col2"};
        CorrectNumColumnsValidator spy = spy(validator);
        doReturn(true).when(spy).isValid(any(String[].class) );
        assertDoesNotThrow( () -> spy.validate(row) );
    }

    @Test
    @DisplayName("isValid returns false when lastBlank throws a NullPointerException")
    void isValidFalseWhenLastBlankThrows() {
        String[] row = {"col0", "col1", "col2"};
        CorrectNumColumnsValidator spy = spy(validator);
        doThrow(NullPointerException.class).when(spy).lastFull(any() );
        assertFalse(spy.isValid(row) );
    }

    @ParameterizedTest(name="[{index}] {0}")
    @CsvSource(useHeadersInDisplayName = true, delimiter = '|', quoteCharacter = '\"',
            textBlock = """
                        Label                           |     row          | expected
            "all blank returns true"                    |       ",,"       |  true
            "{"DIRECTORY", "a", "", ""} returns true"   | "DIRECTORY,a,,"  |  true
            "{"directory", "a", "", ""} returns false"  | "directory,a,,"  |  false
            "{"FILE", "a", "", ""} returns false        | "FILE,a,,"       |  false
            "{"FILE", "a", "1", ""} returns false       | "FILE,a,1,"      |  false
            "{"DIRECTORY", "a", "1", ""} returns false  | "DIRECTORY,a,1," |  false
            "{"FILE", "a", "1", "B"} returns true       | "FILE,a,1,B"     |  true
            "{"FILE", "a", "1", "B", "C"} returns false | "FILE,a,1,B,C"   |  false
            """)
    void isValidSwitchLogicTests(String _label, String arrayStr, boolean expected) {
        var row = arrayStr.split(",");
        assertEquals(expected, validator.isValid(row) );
    }


}