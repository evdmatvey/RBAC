import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuditLog {

    private List<AuditEntry> entries;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuditLog() {
        this.entries = new ArrayList<>();
    }

    public void log(String action, String performer, String target, String details) {
        ValidationUtils.requireNonEmpty(action, "action");
        ValidationUtils.requireNonEmpty(performer, "performer");

        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String normalizedTarget = ValidationUtils.normalizeString(target != null ? target : "");
        String normalizedDetails = ValidationUtils.normalizeString(details != null ? details : "");

        AuditEntry entry = new AuditEntry(
                timestamp,
                action,
                performer,
                normalizedTarget,
                normalizedDetails
        );

        entries.add(entry);
    }

    public List<AuditEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        ValidationUtils.requireNonEmpty(performer, "performer");

        return entries.stream()
                .filter(entry -> entry.performer().equals(performer))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getByAction(String action) {
        ValidationUtils.requireNonEmpty(action, "action");

        return entries.stream()
                .filter(entry -> entry.action().equals(action))
                .collect(Collectors.toList());
    }

    public void printLog() {
        if (entries.isEmpty()) {
            System.out.println("Лог пуст");
            return;
        }

        System.out.println("=== Аудит лог ===");
        System.out.printf("%-20s | %-15s | %-15s | %-20s | %s%n",
                "Timestamp", "Action", "Performer", "Target", "Details");
        System.out.println("-".repeat(90));

        for (AuditEntry entry : entries) {
            System.out.printf("%-20s | %-15s | %-15s | %-20s | %s%n",
                    entry.timestamp(),
                    entry.action(),
                    entry.performer(),
                    entry.target(),
                    entry.details()
            );
        }
    }

    public void saveToFile(String filename) {
        ValidationUtils.requireNonEmpty(filename, "filename");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("Timestamp,Action,Performer,Target,Details");
            writer.newLine();

            for (AuditEntry entry : entries) {
                String line = String.format("%s,%s,%s,%s,%s",
                        entry.timestamp(),
                        entry.action(),
                        entry.performer(),
                        entry.target(),
                        entry.details()
                );
                writer.write(line);
                writer.newLine();
            }

            System.out.println("Лог сохранен в файл: " + filename);

        } catch (IOException e) {
            System.err.println("Ошибка при сохранении файла: " + e.getMessage());
        }
    }
}