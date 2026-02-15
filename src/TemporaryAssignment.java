import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class TemporaryAssignment extends AbstractRoleAssignment{
    private String expiresAt = "";
    private boolean autoRenew = false;

    public TemporaryAssignment(User user, Role role, AssignmentMetadata assignmentMetadata) {
        super(user, role, assignmentMetadata);
    }

    public void extend(String newExpirationDate) {
        if (newExpirationDate == null || newExpirationDate.isEmpty())
            throw new IllegalArgumentException("Укажите корректную новую дату!");

        this.expiresAt = newExpirationDate;
    }

    public boolean isExpired() {
        LocalDateTime expiresAt = LocalDateTime.parse(this.expiresAt);

        return expiresAt.isBefore(LocalDateTime.now());
    }

    public String getTimeRemaining() {
        if (expiresAt == null || expiresAt.isEmpty()) {
            return "Never expires";
        }

        try {
            LocalDateTime expirationDate = LocalDateTime.parse(expiresAt);
            LocalDateTime now = LocalDateTime.now();

            if (expirationDate.isBefore(now)) {
                return "Expired";
            }

            Duration duration = Duration.between(now, expirationDate);

            long days = duration.toDays();
            long hours = duration.toHoursPart();
            long minutes = duration.toMinutesPart();
            long seconds = duration.toSecondsPart();

            if (days > 0) {
                return String.format("%d day%s %d hour%s %d minute%s",
                        days, days > 1 ? "s" : "",
                        hours, hours > 1 ? "s" : "",
                        minutes, minutes > 1 ? "s" : "");
            } else if (hours > 0) {
                return String.format("%d hour%s %d minute%s",
                        hours, hours > 1 ? "s" : "",
                        minutes, minutes > 1 ? "s" : "");
            } else if (minutes > 0) {
                return String.format("%d minute%s %d second%s",
                        minutes, minutes > 1 ? "s" : "",
                        seconds, seconds > 1 ? "s" : "");
            } else {
                return String.format("%d second%s",
                        seconds, seconds > 1 ? "s" : "");
            }

        } catch (DateTimeParseException e) {
            return "Invalid date format";
        }
    }

    @Override
    public boolean isActive() {
        return !this.isExpired();
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    @Override
    public String summary() {
        String summary = super.summary();

        return summary + String.format("\nExpires at: %s", this.expiresAt);
    }
}
