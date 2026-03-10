import utils.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleUtilsTest {

    private Scanner scanner;
    private ByteArrayInputStream testInput;

    @BeforeEach
    void setUp() {
        scanner = new Scanner(System.in);
    }

    private void provideInput(String data) {
        testInput = new ByteArrayInputStream(data.getBytes());
        scanner = new Scanner(testInput);
    }

    @Nested
    @DisplayName("promptString method tests")
    class PromptStringTests {

        @Test
        void promptStringWithValidInputReturnsTrimmedValue() {
            provideInput("  test value  \n");
            String result = ConsoleUtils.promptString(scanner, "Enter value: ", true);
            assertEquals("test value", result);
        }

        @Test
        void promptStringWithRequiredAndEmptyInputShowsErrorAndRetries() {
            provideInput("\n\nvalid\n");
            String result = ConsoleUtils.promptString(scanner, "Enter value: ", true);
            assertEquals("valid", result);
        }

        @Test
        void promptStringNotRequiredReturnsEmptyString() {
            provideInput("\n");
            String result = ConsoleUtils.promptString(scanner, "Enter value: ", false);
            assertEquals("", result);
        }

        @Test
        void promptStringWithNullMessageDoesNotThrow() {
            provideInput("test\n");
            assertDoesNotThrow(() -> ConsoleUtils.promptString(scanner, null, true));
        }

        @Test
        void promptStringWithNullScannerThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> ConsoleUtils.promptString(null, "message", true));
        }
    }

    @Nested
    @DisplayName("promptInt method tests")
    class PromptIntTests {

        @Test
        void promptIntWithValidInputReturnsValue() {
            provideInput("5\n");
            int result = ConsoleUtils.promptInt(scanner, "Enter number: ", 1, 10);
            assertEquals(5, result);
        }

        @Test
        void promptIntWithInputBelowMinShowsErrorAndRetries() {
            provideInput("0\n5\n");
            int result = ConsoleUtils.promptInt(scanner, "Enter number: ", 1, 10);
            assertEquals(5, result);
        }

        @Test
        void promptIntWithInputAboveMaxShowsErrorAndRetries() {
            provideInput("11\n8\n");
            int result = ConsoleUtils.promptInt(scanner, "Enter number: ", 1, 10);
            assertEquals(8, result);
        }

        @Test
        void promptIntWithEmptyInputShowsErrorAndRetries() {
            provideInput("\n7\n");
            int result = ConsoleUtils.promptInt(scanner, "Enter number: ", 1, 10);
            assertEquals(7, result);
        }

        @Test
        void promptIntWithNonNumericInputShowsErrorAndRetries() {
            provideInput("abc\n3\n");
            int result = ConsoleUtils.promptInt(scanner, "Enter number: ", 1, 10);
            assertEquals(3, result);
        }

        @Test
        void promptIntWithMinEqualsMaxReturnsThatValue() {
            provideInput("5\n");
            int result = ConsoleUtils.promptInt(scanner, "Enter number: ", 5, 5);
            assertEquals(5, result);
        }

        @Test
        void promptIntWithMinGreaterThanMaxThrowsException() {
            provideInput("5\n");
            assertThrows(IllegalArgumentException.class,
                    () -> ConsoleUtils.promptInt(scanner, "message", 10, 1));
        }

        @Test
        void promptIntWithNullMessageDoesNotThrow() {
            provideInput("5\n");
            assertDoesNotThrow(() -> ConsoleUtils.promptInt(scanner, null, 1, 10));
        }

        @Test
        void promptIntWithNullScannerThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> ConsoleUtils.promptInt(null, "message", 1, 10));
        }
    }

    @Nested
    @DisplayName("promptYesNo method tests")
    class PromptYesNoTests {

        @Test
        void promptYesNoWithYesReturnsTrue() {
            provideInput("yes\n");
            boolean result = ConsoleUtils.promptYesNo(scanner, "Continue?");
            assertTrue(result);
        }

        @Test
        void promptYesNoWithYReturnsTrue() {
            provideInput("y\n");
            boolean result = ConsoleUtils.promptYesNo(scanner, "Continue?");
            assertTrue(result);
        }

        @Test
        void promptYesNoWithNoReturnsFalse() {
            provideInput("no\n");
            boolean result = ConsoleUtils.promptYesNo(scanner, "Continue?");
            assertFalse(result);
        }

        @Test
        void promptYesNoWithNReturnsFalse() {
            provideInput("n\n");
            boolean result = ConsoleUtils.promptYesNo(scanner, "Continue?");
            assertFalse(result);
        }

        @Test
        void promptYesNoWithUppercaseReturnsCorrectValue() {
            provideInput("YES\n");
            boolean result = ConsoleUtils.promptYesNo(scanner, "Continue?");
            assertTrue(result);
        }

        @Test
        void promptYesNoWithMixedCaseReturnsCorrectValue() {
            provideInput("YeS\n");
            boolean result = ConsoleUtils.promptYesNo(scanner, "Continue?");
            assertTrue(result);
        }

        @Test
        void promptYesNoWithInvalidInputShowsErrorAndRetries() {
            provideInput("maybe\nyes\n");
            boolean result = ConsoleUtils.promptYesNo(scanner, "Continue?");
            assertTrue(result);
        }

        @Test
        void promptYesNoWithEmptyInputShowsErrorAndRetries() {
            provideInput("\nno\n");
            boolean result = ConsoleUtils.promptYesNo(scanner, "Continue?");
            assertFalse(result);
        }

        @Test
        void promptYesNoWithNullMessageDoesNotThrow() {
            provideInput("yes\n");
            assertDoesNotThrow(() -> ConsoleUtils.promptYesNo(scanner, null));
        }

        @Test
        void promptYesNoWithNullScannerThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> ConsoleUtils.promptYesNo(null, "message"));
        }
    }

    @Nested
    @DisplayName("promptChoice method tests")
    class PromptChoiceTests {

        private List<String> options;

        @BeforeEach
        void setUp() {
            options = Arrays.asList("Option 1", "Option 2", "Option 3");
        }

        @Test
        void promptChoiceWithValidNumberReturnsSelectedOption() {
            provideInput("2\n");
            String result = ConsoleUtils.promptChoice(scanner, "Choose:", options);
            assertEquals("Option 2", result);
        }

        @Test
        void promptChoiceWithNumberBelowRangeShowsErrorAndRetries() {
            provideInput("0\n2\n");
            String result = ConsoleUtils.promptChoice(scanner, "Choose:", options);
            assertEquals("Option 2", result);
        }

        @Test
        void promptChoiceWithNumberAboveRangeShowsErrorAndRetries() {
            provideInput("4\n3\n");
            String result = ConsoleUtils.promptChoice(scanner, "Choose:", options);
            assertEquals("Option 3", result);
        }

        @Test
        void promptChoiceWithEmptyInputShowsErrorAndRetries() {
            provideInput("\n1\n");
            String result = ConsoleUtils.promptChoice(scanner, "Choose:", options);
            assertEquals("Option 1", result);
        }

        @Test
        void promptChoiceWithNonNumericInputShowsErrorAndRetries() {
            provideInput("abc\n2\n");
            String result = ConsoleUtils.promptChoice(scanner, "Choose:", options);
            assertEquals("Option 2", result);
        }

        @Test
        void promptChoiceWithSingleOptionReturnsThatOption() {
            List<String> singleOption = Collections.singletonList("Only Option");
            provideInput("1\n");
            String result = ConsoleUtils.promptChoice(scanner, "Choose:", singleOption);
            assertEquals("Only Option", result);
        }

        @Test
        void promptChoiceWithNullMessageDoesNotThrow() {
            provideInput("1\n");
            assertDoesNotThrow(() -> ConsoleUtils.promptChoice(scanner, null, options));
        }

        @Test
        void promptChoiceWithEmptyOptionsListThrowsException() {
            provideInput("1\n");
            assertThrows(IllegalArgumentException.class,
                    () -> ConsoleUtils.promptChoice(scanner, "message", Collections.emptyList()));
        }

        @Test
        void promptChoiceWithNullOptionsListThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> ConsoleUtils.promptChoice(scanner, "message", null));
        }

        @Test
        void promptChoiceWithNullScannerThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> ConsoleUtils.promptChoice(null, "message", options));
        }
    }
}