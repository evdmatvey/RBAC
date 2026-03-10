import utils.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FormatUtilsTest {

    @Nested
    @DisplayName("formatTable method tests")
    class FormatTableTests {

        @Test
        void formatTableWithEmptyHeadersReturnsEmpty() {
            String[] headers = {};
            assertEquals("", FormatUtils.formatTable(headers, null));
        }

        @Test
        void formatTableWithNullHeadersReturnsEmpty() {
            assertEquals("", FormatUtils.formatTable(null, null));
        }

        @Test
        void formatTableWithHeadersOnlyReturnsTableWithHeaderRow() {
            String[] headers = {"Name", "Age"};
            String result = FormatUtils.formatTable(headers, null);

            assertTrue(result.contains("Name"));
            assertTrue(result.contains("Age"));
            assertTrue(result.startsWith("+"));
        }

        @Test
        void formatTableWithDataReturnsFullTable() {
            String[] headers = {"Name", "Age"};
            List<String[]> rows = Arrays.asList(
                    new String[]{"John", "25"},
                    new String[]{"Alice", "30"}
            );

            String result = FormatUtils.formatTable(headers, rows);

            assertTrue(result.contains("John"));
            assertTrue(result.contains("Alice"));
            assertTrue(result.contains("25"));
            assertTrue(result.contains("30"));
        }

        @Test
        void formatTableHandlesNullCells() {
            String[] headers = {"Name", "Age"};
            List<String[]> rows = Collections.singletonList(new String[]{"John", null});

            String result = FormatUtils.formatTable(headers, rows);

            assertTrue(result.contains("John"));
        }
    }

    @Nested
    @DisplayName("formatBox method tests")
    class FormatBoxTests {

        @Test
        void formatBoxWithNullReturnsEmpty() {
            assertEquals("", FormatUtils.formatBox(null));
        }

        @Test
        void formatBoxWithEmptyReturnsEmpty() {
            assertEquals("", FormatUtils.formatBox(""));
        }

        @Test
        void formatBoxWithSingleLineReturnsBox() {
            String text = "Hello";
            String result = FormatUtils.formatBox(text);

            assertTrue(result.contains("Hello"));
            assertTrue(result.startsWith("+"));
            assertTrue(result.endsWith("+\n"));
        }

        @Test
        void formatBoxWithMultipleLinesReturnsBox() {
            String text = "Line 1\nLine 2";
            String result = FormatUtils.formatBox(text);

            assertTrue(result.contains("Line 1"));
            assertTrue(result.contains("Line 2"));
        }
    }

    @Nested
    @DisplayName("formatHeader method tests")
    class FormatHeaderTests {

        @Test
        void formatHeaderWithNullReturnsEmpty() {
            assertEquals("", FormatUtils.formatHeader(null));
        }

        @Test
        void formatHeaderWithTextReturnsFormattedHeader() {
            String text = "Title";
            String result = FormatUtils.formatHeader(text);

            assertTrue(result.contains("| Title |"));
            assertTrue(result.contains("--------"));
        }
    }

    @Nested
    @DisplayName("truncate method tests")
    class TruncateTests {

        @ParameterizedTest
        @CsvSource({
                "Hello World, 5, He...",
                "Hello, 10, Hello",
                "Test, 3, ..."
        })
        void truncateReturnsCorrectString(String input, int maxLength, String expected) {
            assertEquals(expected, FormatUtils.truncate(input, maxLength));
        }

        @Test
        void truncateWithNullReturnsEmpty() {
            assertEquals("", FormatUtils.truncate(null, 10));
        }
    }

    @Nested
    @DisplayName("padRight method tests")
    class PadRightTests {

        @ParameterizedTest
        @CsvSource({
                "Hello, 10, 'Hello     '",
                "Hi, 4, 'Hi  '",
                "Hello, 3, Hello"
        })
        void padRightReturnsCorrectString(String input, int length, String expected) {
            assertEquals(expected, FormatUtils.padRight(input, length));
        }

        @Test
        void padRightWithNullReturnsSpaces() {
            assertEquals("    ", FormatUtils.padRight(null, 4));
        }
    }

    @Nested
    @DisplayName("padLeft method tests")
    class PadLeftTests {

        @ParameterizedTest
        @CsvSource({
                "Hello, 10, '     Hello'",
                "Hi, 4, '  Hi'",
                "Hello, 3, Hello"
        })
        void padLeftReturnsCorrectString(String input, int length, String expected) {
            assertEquals(expected, FormatUtils.padLeft(input, length));
        }

        @Test
        void padLeftWithNullReturnsSpaces() {
            assertEquals("    ", FormatUtils.padLeft(null, 4));
        }
    }

    @Nested
    @DisplayName("formatList method tests")
    class FormatListTests {

        @Test
        void formatListWithNullItemsReturnsEmpty() {
            assertEquals("", FormatUtils.formatList(null, "- ", ""));
        }

        @Test
        void formatListWithItemsReturnsFormattedList() {
            List<String> items = Arrays.asList("Item1", "Item2", "Item3");
            String result = FormatUtils.formatList(items, "* ", "");

            assertEquals("* Item1\n* Item2\n* Item3", result);
        }

        @Test
        void formatListWithPrefixAndSuffix() {
            List<String> items = Arrays.asList("A", "B");
            String result = FormatUtils.formatList(items, "[", "]");

            assertEquals("[A]\n[B]", result);
        }
    }
}