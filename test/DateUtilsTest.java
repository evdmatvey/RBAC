import utils.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Nested
    @DisplayName("Current date/time methods tests")
    class CurrentDateTimeTests {

        @Test
        void getCurrentDateReturnsFormattedDate() {
            String expected = LocalDate.now().format(FORMATTER);
            assertEquals(expected, DateUtils.getCurrentDate());
        }

        @Test
        void getCurrentDateTimeReturnsNonEmptyString() {
            assertNotNull(DateUtils.getCurrentDateTime());
            assertFalse(DateUtils.getCurrentDateTime().isEmpty());
        }
    }

    @Nested
    @DisplayName("Date comparison methods tests")
    class DateComparisonTests {

        @ParameterizedTest
        @CsvSource({
                "2023-01-01, 2023-01-02, true",
                "2023-01-02, 2023-01-01, false",
                "2023-01-01, 2023-01-01, false"
        })
        void isBeforeReturnsCorrectResult(String date1, String date2, boolean expected) {
            assertEquals(expected, DateUtils.isBefore(date1, date2));
        }

        @ParameterizedTest
        @CsvSource({
                "2023-01-02, 2023-01-01, true",
                "2023-01-01, 2023-01-02, false",
                "2023-01-01, 2023-01-01, false"
        })
        void isAfterReturnsCorrectResult(String date1, String date2, boolean expected) {
            assertEquals(expected, DateUtils.isAfter(date1, date2));
        }

        @ParameterizedTest
        @CsvSource({
                "2023-01-01, 2023-01-01, true",
                "2023-01-01, 2023-01-02, false"
        })
        void isEqualReturnsCorrectResult(String date1, String date2, boolean expected) {
            assertEquals(expected, DateUtils.isEqual(date1, date2));
        }

        @Test
        void comparisonMethodsWithNullReturnFalse() {
            assertFalse(DateUtils.isBefore(null, "2023-01-01"));
            assertFalse(DateUtils.isBefore("2023-01-01", null));
            assertFalse(DateUtils.isAfter(null, "2023-01-01"));
            assertFalse(DateUtils.isAfter("2023-01-01", null));
            assertFalse(DateUtils.isEqual(null, "2023-01-01"));
            assertFalse(DateUtils.isEqual("2023-01-01", null));
        }
    }

    @Nested
    @DisplayName("addDays method tests")
    class AddDaysTests {

        @ParameterizedTest
        @CsvSource({
                "2023-01-01, 5, 2023-01-06",
                "2023-12-30, 2, 2024-01-01",
                "2023-01-01, -1, 2022-12-31"
        })
        void addDaysReturnsCorrectDate(String date, int days, String expected) {
            assertEquals(expected, DateUtils.addDays(date, days));
        }

        @Test
        void addDaysWithNullReturnsNull() {
            assertNull(DateUtils.addDays(null, 5));
        }

        @Test
        void addDaysWithInvalidDateReturnsOriginalDate() {
            String invalidDate = "invalid-date";
            assertEquals(invalidDate, DateUtils.addDays(invalidDate, 5));
        }
    }

    @Nested
    @DisplayName("formatRelativeTime method tests")
    class FormatRelativeTimeTests {

        @Test
        void formatRelativeTimeWithTodayDateReturnsToday() {
            String today = LocalDate.now().format(FORMATTER);
            assertEquals("today", DateUtils.formatRelativeTime(today));
        }

        @Test
        void formatRelativeTimeWithTomorrowReturnsIn1Day() {
            String tomorrow = LocalDate.now().plusDays(1).format(FORMATTER);
            assertEquals("in 1 day", DateUtils.formatRelativeTime(tomorrow));
        }

        @Test
        void formatRelativeTimeWithFutureDateReturnsInXDays() {
            String futureDate = LocalDate.now().plusDays(5).format(FORMATTER);
            assertEquals("in 5 days", DateUtils.formatRelativeTime(futureDate));
        }

        @Test
        void formatRelativeTimeWithYesterdayReturns1DayAgo() {
            String yesterday = LocalDate.now().minusDays(1).format(FORMATTER);
            assertEquals("1 day ago", DateUtils.formatRelativeTime(yesterday));
        }

        @Test
        void formatRelativeTimeWithPastDateReturnsXDaysAgo() {
            String pastDate = LocalDate.now().minusDays(10).format(FORMATTER);
            assertEquals("10 days ago", DateUtils.formatRelativeTime(pastDate));
        }

        @Test
        void formatRelativeTimeWithNullReturnsEmpty() {
            assertEquals("", DateUtils.formatRelativeTime(null));
        }

        @Test
        void formatRelativeTimeWithInvalidDateReturnsOriginalDate() {
            String invalidDate = "invalid-date";
            assertEquals(invalidDate, DateUtils.formatRelativeTime(invalidDate));
        }
    }

    @Nested
    @DisplayName("isValidDate method tests")
    class IsValidDateTests {

        @ParameterizedTest
        @ValueSource(strings = {"2023-01-01", "2023-12-31", "2024-02-29"})
        void isValidDateWithValidDatesReturnsTrue(String date) {
            assertTrue(DateUtils.isValidDate(date));
        }

        @ParameterizedTest
        @ValueSource(strings = {"2023-13-01", "2023-01-32", "invalid-date", "01-01-2023"})
        void isValidDateWithInvalidDatesReturnsFalse(String date) {
            assertFalse(DateUtils.isValidDate(date));
        }

        @Test
        void isValidDateWithNullReturnsFalse() {
            assertFalse(DateUtils.isValidDate(null));
        }
    }

    @Nested
    @DisplayName("Date calculation methods tests")
    class DateCalculationTests {

        @Test
        void getDateAfterDaysReturnsCorrectDate() {
            int days = 5;
            String expected = LocalDate.now().plusDays(days).format(FORMATTER);
            assertEquals(expected, DateUtils.getDateAfterDays(days));
        }

        @Test
        void getDateBeforeDaysReturnsCorrectDate() {
            int days = 5;
            String expected = LocalDate.now().minusDays(days).format(FORMATTER);
            assertEquals(expected, DateUtils.getDateBeforeDays(days));
        }

        @ParameterizedTest
        @CsvSource({
                "2023-01-01, 2023-01-10, 9",
                "2023-01-10, 2023-01-01, -9",
                "2023-01-01, 2023-01-01, 0"
        })
        void daysBetweenReturnsCorrectDifference(String date1, String date2, long expected) {
            assertEquals(expected, DateUtils.daysBetween(date1, date2));
        }

        @Test
        void daysBetweenWithNullReturnsZero() {
            assertEquals(0, DateUtils.daysBetween(null, "2023-01-01"));
            assertEquals(0, DateUtils.daysBetween("2023-01-01", null));
        }

        @Test
        void daysBetweenWithInvalidDatesReturnsZero() {
            assertEquals(0, DateUtils.daysBetween("invalid", "2023-01-01"));
        }
    }

    @Nested
    @DisplayName("getDayOfWeek method tests")
    class GetDayOfWeekTests {

        @ParameterizedTest
        @CsvSource({
                "2024-01-01, MONDAY",
                "2024-01-02, TUESDAY",
                "2024-01-03, WEDNESDAY",
                "2024-01-04, THURSDAY",
                "2024-01-05, FRIDAY",
                "2024-01-06, SATURDAY",
                "2024-01-07, SUNDAY"
        })
        void getDayOfWeekReturnsCorrectDay(String date, String expected) {
            assertEquals(expected, DateUtils.getDayOfWeek(date));
        }

        @Test
        void getDayOfWeekWithNullReturnsEmpty() {
            assertEquals("", DateUtils.getDayOfWeek(null));
        }

        @Test
        void getDayOfWeekWithInvalidDateReturnsEmpty() {
            assertEquals("", DateUtils.getDayOfWeek("invalid-date"));
        }
    }
}