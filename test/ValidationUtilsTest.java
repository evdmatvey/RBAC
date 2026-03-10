package utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EmptySource;
import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Nested
    @DisplayName("isValidUsername method tests")
    class IsValidUsernameTests {

        @ParameterizedTest
        @ValueSource(strings = {"john_doe", "user123", "admin", "a1_b2", "valid_username"})
        void isValidUsernameWithValidInputsReturnsTrue(String username) {
            assertTrue(ValidationUtils.isValidUsername(username));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ab", "a", "very_long_username_exceeding_max_length", "user@name", "user-name", "user.name", "user name"})
        void isValidUsernameWithInvalidInputsReturnsFalse(String username) {
            assertFalse(ValidationUtils.isValidUsername(username));
        }

        @Test
        void isValidUsernameWithNullReturnsFalse() {
            assertFalse(ValidationUtils.isValidUsername(null));
        }

        @Test
        void isValidUsernameWithEmptyReturnsFalse() {
            assertFalse(ValidationUtils.isValidUsername(""));
        }
    }

    @Nested
    @DisplayName("isValidEmail method tests")
    class IsValidEmailTests {

        @ParameterizedTest
        @ValueSource(strings = {"user@example.com", "john.doe@company.co.uk", "user+filter@domain.org", "user_name@sub.domain.com", "123@domain.com"})
        void isValidEmailWithValidInputsReturnsTrue(String email) {
            assertTrue(ValidationUtils.isValidEmail(email));
        }

        @Test
        void isValidEmailWithNullReturnsFalse() {
            assertFalse(ValidationUtils.isValidEmail(null));
        }

        @Test
        void isValidEmailWithEmptyReturnsFalse() {
            assertFalse(ValidationUtils.isValidEmail(""));
        }
    }

    @Nested
    @DisplayName("isValidDate method tests")
    class IsValidDateTests {

        @ParameterizedTest
        @ValueSource(strings = {"2023-01-15", "2024-02-29", "2023-12-31", "2023-06-01"})
        void isValidDateWithValidInputsReturnsTrue(String date) {
            assertTrue(ValidationUtils.isValidDate(date));
        }
    }

    @Nested
    @DisplayName("isValidDateTime method tests")
    class IsValidDateTimeTests {

        @ParameterizedTest
        @ValueSource(strings = {"2023-01-15 14:30:00", "2023-12-31 23:59:59", "2024-02-29 00:00:00", "2023-06-01 09:05:30"})
        void isValidDateTimeWithValidInputsReturnsTrue(String dateTime) {
            assertTrue(ValidationUtils.isValidDateTime(dateTime));
        }

        @ParameterizedTest
        @ValueSource(strings = {"2023-01-15 24:00:00", "2023-01-15 14:60:00", "2023-01-15 14:30:60", "2023-13-01 14:30:00", "2023-01-15 14:30", "2023-01-15"})
        void isValidDateTimeWithInvalidInputsReturnsFalse(String dateTime) {
            assertFalse(ValidationUtils.isValidDateTime(dateTime));
        }
    }

    @Nested
    @DisplayName("normalizeString method tests")
    class NormalizeStringTests {

        @ParameterizedTest
        @CsvSource({
                "hello, Hello",
                "HELLO, Hello",
                "  hello  , Hello",
                "hello   world, Hello world",
                "john DOE, John doe",
                "'' , ''",
                "' ' , ''"
        })
        void normalizeStringReturnsCorrectResult(String input, String expected) {
            assertEquals(expected, ValidationUtils.normalizeString(input));
        }

        @Test
        void normalizeStringWithNullReturnsEmpty() {
            assertEquals("", ValidationUtils.normalizeString(null));
        }
    }

    @Nested
    @DisplayName("requireNonEmpty method tests")
    class RequireNonEmptyTests {

        @Test
        void requireNonEmptyWithValidInputDoesNotThrow() {
            assertDoesNotThrow(() -> ValidationUtils.requireNonEmpty("test", "field"));
        }

        @Test
        void requireNonEmptyWithNullThrowsException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> ValidationUtils.requireNonEmpty(null, "username"));
            assertTrue(exception.getMessage().contains("username"));
        }

        @Test
        void requireNonEmptyWithEmptyThrowsException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> ValidationUtils.requireNonEmpty("", "email"));
            assertTrue(exception.getMessage().contains("email"));
        }
    }
}