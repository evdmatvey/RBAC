import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    public static boolean isBefore(String date1, String date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.compareTo(date2) < 0;
    }

    public static boolean isAfter(String date1, String date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.compareTo(date2) > 0;
    }

    public static boolean isEqual(String date1, String date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.equals(date2);
    }

    public static String addDays(String date, int days) {
        if (date == null) {
            return null;
        }

        try {
            LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
            LocalDate newDate = localDate.plusDays(days);
            return newDate.format(DATE_FORMATTER);
        } catch (Exception e) {
            return date;
        }
    }

    public static String formatRelativeTime(String date) {
        if (date == null) {
            return "";
        }

        try {
            LocalDate targetDate = LocalDate.parse(date, DATE_FORMATTER);
            LocalDate now = LocalDate.now();

            long daysDiff = ChronoUnit.DAYS.between(now, targetDate);

            if (daysDiff == 0) {
                return "today";
            } else if (daysDiff > 0) {
                if (daysDiff == 1) {
                    return "in 1 day";
                } else {
                    return "in " + daysDiff + " days";
                }
            } else {
                long absDays = Math.abs(daysDiff);
                if (absDays == 1) {
                    return "1 day ago";
                } else {
                    return absDays + " days ago";
                }
            }
        } catch (Exception e) {
            return date;
        }
    }

    public static boolean isValidDate(String date) {
        if (date == null) {
            return false;
        }

        try {
            LocalDate.parse(date, DATE_FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getDateAfterDays(int days) {
        return LocalDate.now().plusDays(days).format(DATE_FORMATTER);
    }

    public static String getDateBeforeDays(int days) {
        return LocalDate.now().minusDays(days).format(DATE_FORMATTER);
    }

    public static long daysBetween(String date1, String date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }

        try {
            LocalDate d1 = LocalDate.parse(date1, DATE_FORMATTER);
            LocalDate d2 = LocalDate.parse(date2, DATE_FORMATTER);
            return ChronoUnit.DAYS.between(d1, d2);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getDayOfWeek(String date) {
        if (date == null) {
            return "";
        }

        try {
            LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
            return localDate.getDayOfWeek().toString();
        } catch (Exception e) {
            return "";
        }
    }
}