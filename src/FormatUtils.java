import java.util.List;

public class FormatUtils {

    private static final String HORIZONTAL_LINE = "-";
    private static final String VERTICAL_LINE = "|";
    private static final String CROSS = "+";

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return "";
        }

        int[] columnWidths = new int[headers.length];

        for (int i = 0; i < headers.length; i++) {
            columnWidths[i] = headers[i].length();
        }

        if (rows != null) {
            for (String[] row : rows) {
                if (row != null) {
                    for (int i = 0; i < Math.min(row.length, headers.length); i++) {
                        if (row[i] != null) {
                            columnWidths[i] = Math.max(columnWidths[i], row[i].length());
                        }
                    }
                }
            }
        }

        StringBuilder table = new StringBuilder();

        table.append(borderLine(columnWidths));
        table.append(dataRow(headers, columnWidths));
        table.append(borderLine(columnWidths));

        if (rows != null) {
            for (String[] row : rows) {
                if (row != null) {
                    table.append(dataRow(row, columnWidths));
                }
            }
        }

        table.append(borderLine(columnWidths));

        return table.toString();
    }

    private static String borderLine(int[] widths) {
        StringBuilder line = new StringBuilder(CROSS);
        for (int width : widths) {
            line.append(HORIZONTAL_LINE.repeat(width + 2));
            line.append(CROSS);
        }
        line.append("\n");
        return line.toString();
    }

    private static String dataRow(String[] data, int[] widths) {
        StringBuilder row = new StringBuilder(VERTICAL_LINE);
        for (int i = 0; i < widths.length; i++) {
            String cell = (i < data.length && data[i] != null) ? data[i] : "";
            row.append(" ").append(padRight(cell, widths[i])).append(" ").append(VERTICAL_LINE);
        }
        row.append("\n");
        return row.toString();
    }

    public static String formatBox(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String[] lines = text.split("\n");
        int maxLength = 0;
        for (String line : lines) {
            maxLength = Math.max(maxLength, line.length());
        }

        StringBuilder box = new StringBuilder();

        box.append(CROSS).append(HORIZONTAL_LINE.repeat(maxLength + 2)).append(CROSS).append("\n");

        for (String line : lines) {
            box.append(VERTICAL_LINE).append(" ").append(padRight(line, maxLength)).append(" ").append(VERTICAL_LINE).append("\n");
        }

        box.append(CROSS).append(HORIZONTAL_LINE.repeat(maxLength + 2)).append(CROSS).append("\n");

        return box.toString();
    }

    public static String formatHeader(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String line = HORIZONTAL_LINE.repeat(text.length() + 4);

        return String.format("%s\n| %s |\n%s\n", line, text, line);
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (maxLength < 3) {
            return ".".repeat(maxLength);
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        if (text == null) {
            text = "";
        }
        if (text.length() >= length) {
            return text;
        }
        return text + " ".repeat(length - text.length());
    }

    public static String padLeft(String text, int length) {
        if (text == null) {
            text = "";
        }
        if (text.length() >= length) {
            return text;
        }
        return " ".repeat(length - text.length()) + text;
    }

    public static String formatCurrency(double amount) {
        return String.format("$%,.2f", amount);
    }

    public static String formatPercentage(double value) {
        return String.format("%.1f%%", value * 100);
    }

    public static String formatList(List<?> items, String prefix, String suffix) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                result.append("\n");
            }
            result.append(prefix).append(items.get(i).toString()).append(suffix);
        }
        return result.toString();
    }
}