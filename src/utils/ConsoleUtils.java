package utils;

import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {

    private static final String YES_NO_PATTERN = "^(?i)(yes|no|y|n)$";

    public static String promptString(Scanner scanner, String message, boolean required) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner cannot be null");
        }
        if (message == null) {
            message = "";
        }

        while (true) {
            System.out.print(message);
            String input = scanner.nextLine();

            if (!required) {
                return input;
            }

            if (input == null || input.trim().isEmpty()) {
                System.out.println("Error: Value cannot be empty. Please try again.");
                continue;
            }

            return input.trim();
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner cannot be null");
        }
        if (message == null) {
            message = "";
        }
        if (min > max) {
            throw new IllegalArgumentException("Min value cannot be greater than max value");
        }

        while (true) {
            System.out.print(message);
            try {
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.println("Error: Value cannot be empty. Please try again.");
                    continue;
                }

                int value = Integer.parseInt(input);

                if (value < min || value > max) {
                    System.out.printf("Error: Value must be between %d and %d. Please try again.%n", min, max);
                    continue;
                }

                return value;

            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid integer.");
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner cannot be null");
        }
        if (message == null) {
            message = "";
        }

        while (true) {
            System.out.print(message + " (yes/no): ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.matches(YES_NO_PATTERN)) {
                return input.startsWith("y");
            }

            System.out.println("Error: Please enter 'yes' or 'no' (or 'y'/'n').");
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner cannot be null");
        }
        if (message == null) {
            message = "";
        }
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Options list cannot be null or empty");
        }

        while (true) {
            System.out.println(message);
            for (int i = 0; i < options.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, options.get(i).toString());
            }
            System.out.print("Enter your choice (1-" + options.size() + "): ");

            try {
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.println("Error: Choice cannot be empty. Please try again.");
                    continue;
                }

                int choice = Integer.parseInt(input);

                if (choice < 1 || choice > options.size()) {
                    System.out.printf("Error: Please enter a number between 1 and %d.%n", options.size());
                    continue;
                }

                return options.get(choice - 1);

            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid number.");
            }
        }
    }
}