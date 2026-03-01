import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]+$");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.%+-]+@[\\w.-]+\\.[a-z]{2,}$");

    private static final Pattern DATE_PATTERN =
            Pattern.compile("^(0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[0-2])\\.(19|20)\\d{2}$");

    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        if (username.isEmpty()) return false;
        if (username.length() < 3 || username.length() > 20) return false;
        return USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        if (email.isEmpty()) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidDate(String date) {
        if (date == null) return false;
        if (date.isEmpty()) return false;

        if (!DATE_PATTERN.matcher(date).matches()) {
            return false;
        }

        String[] parts = date.split("\\.");
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[2]);

        return isValidDateComponents(day, month, year);
    }

    public static String normalizeString(String input) {
        if (input == null) return "";
        if (input.trim().isEmpty()) return "";

        String trimmed = input.trim();
        String singleSpaced = trimmed.replaceAll("\\s+", " ");

        if (singleSpaced.isEmpty()) return "";

        return singleSpaced.substring(0, 1).toUpperCase() +
                singleSpaced.substring(1).toLowerCase();
    }

    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(
                    String.format("Поле '%s' не должно быть null!", fieldName)
            );
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Поле '%s' не должно быть пустым!", fieldName)
            );
        }
    }

    private static boolean isValidDateComponents(int day, int month, int year) {
        if (month < 1 || month > 12) return false;
        if (day < 1) return false;
        if (year < 1900 || year > 2099) return false;

        int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        if (month == 2) {
            boolean isLeapYear = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
            if (isLeapYear) {
                return day <= 29;
            }
            return day <= 28;
        }

        return day <= daysInMonth[month - 1];
    }
}