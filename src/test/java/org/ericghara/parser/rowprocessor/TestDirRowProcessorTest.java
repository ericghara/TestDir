package org.ericghara.parser.rowprocessor;

import org.ericghara.parser.LineType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.VoidAnswer1;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.answerVoid;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestDirRowProcessorTest {

    TestDirRowProcessor processor = new TestDirRowProcessor();

    @Test
    void initEnumMapProperMapping() {
        var map = processor.initEnumMap();
        Stream.of(LineType.values() )
              .forEach( e -> {
            assertEquals(e, map.get(e.getKey() ) );
        });
    }

    @Test
    void assignLineTypeSubstitutesAtKey() {
        String[] found = {"f", "f", "f"};
        processor.assignLineType(found); // mutates found in place
        String[] expected = {"FILE", "f", "f"};
        assertArrayEquals(expected, found);
    }

    @Test
    void assignLineTypeNoSubstituteWithNoKey() {
        String[] found = {"notAkey", "f", "f"};
        String[] expected = found.clone();
        processor.assignLineType(found);
        assertArrayEquals(expected, found);
    }

    @Test
    @DisplayName("stripWhitespace removes leading and trailing whitespace")
    void stripWhitespaceStripsWhitespace() {
        String[] expected = {"a", "b", "c"};

        String[] found = {"a", " b ", "c"}; // modified in place
        processor.stripWhitespace(1, found);

        assertArrayEquals(expected, found);
    }

    @ParameterizedTest(name="[{index}] {0}")
    @CsvSource(useHeadersInDisplayName = true, delimiter = '|', quoteCharacter = '\"',
            textBlock = """
            Label                 | column        |  expectedText | hasComment
            "space then comment"  | "x# commment" |   "x"         |  true
            "comment begins line" | "# comment"   |    ""         |  true
            "no comment"          | "comment"     |   "comment"   |  false
            """)
    void processCommentTest(String _label, String column, String expectedText, boolean hasComment) {
        // don't use spaces in any column tests.  Auto stripped despite quoteChar
        var processed = processor.processComment(column);
        assertEquals(hasComment,processed.hasComment() );
        assertEquals(expectedText, processed.text() );
    }

   @Test
    void rowStripperTestBeginningOfFirstColumnComment() {
        String[] found = {"#col0", "col1", "col2"}; // modified in place
        processor.rowStripper(found);
        String[] expected = {"", "", ""};
        assertArrayEquals(expected, found);
    }

    @Test
    void rowStripperTestMiddleOfThirdColumnComment() {
        String[] found = {"col0", "col1", "col#2"}; // modified in place
        processor.rowStripper(found);
        String[] expected = {"col0", "col1", "col"};
        assertArrayEquals(expected, found);
    }

    @Test
    @DisplayName("rowStripper calls processComment 2x, returnsEmptyText 1x and stripWhitespace 3x")
    void rowStripperCallsExpectedMethods() {
        TestDirRowProcessor mock = mock(TestDirRowProcessor.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS) );
        String[] input = {"col0", "#col1", "col2"};
        mock.rowStripper(input);
        verify(mock, times(2) ).processComment(any(String.class) );
        verify(mock, times(1) ).returnsEmptyText(any(String.class) );
        verify(mock, times(3) ).stripWhitespace(anyInt(), any(String[].class) );
    }

    @Nested
    class ProcessRowTests {

        private TestDirRowProcessor mock;

        @BeforeEach
        void setupMock() {
            mock = mock(TestDirRowProcessor.class,
                    withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS) );
        }

        @Test
        void expectedCalls() {
            doNothing().when(mock).rowStripper(any() );
            doNothing().when(mock).assignLineType(any() );
            String[] input = {"col0", "#col1", "col2"};

            mock.processRow(input);

            verify(mock, times(1) ).processRow(any() );
            verify(mock, times(1) ).rowStripper(any() );
            verify(mock, times(1) ).assignLineType(any() );
            verifyNoMoreInteractions(mock);
        }

        @Test
        void expectedRowVals() {
            final String[] found = {"col0", "#col1", "col2"};  // modified in place
            final String[] expectedStripperArgs = found.clone();
            final String[] expectedAssignArgs = {"col0X", "#col1X", "col2X"};
            final String[] expectedResult = {"col0XX", "#col1XX", "col2XX"};

            VoidAnswer1<String[]> ansStripper = a -> {
                assertArrayEquals(expectedStripperArgs, a );  // assertion
                for (int i = 0; i < a.length; i++) {
                    a[i] += "X";
                }
            };

            VoidAnswer1<String[]> ansAssign = a -> {
                assertArrayEquals(expectedAssignArgs, a ); // assertion
                for (int i = 0; i < a.length; i++) {
                    a[i] += "X";
                }
            };

            doAnswer(answerVoid(ansStripper) ).when(mock).rowStripper(any() );
            doAnswer(answerVoid(ansAssign) ).when(mock).assignLineType(any() );
            mock.processRow(found);

            assertArrayEquals(expectedResult, found); // assertion
        }
    }
}