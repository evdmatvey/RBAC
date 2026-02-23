import java.time.LocalDateTime;
import java.util.Comparator;

public class AssignmentSorters {

    public static Comparator<RoleAssignment> byUsername() {
        return (a1, a2) -> a1.user().username().compareTo(a2.user().username());
    }

    public static Comparator<RoleAssignment> byRoleName() {
        return (a1, a2) -> a1.role().getName().compareTo(a2.role().getName());
    }

    public static Comparator<RoleAssignment> byAssignmentDate() {
        return (a1, a2) -> {
            LocalDateTime date1 = LocalDateTime.parse(a1.metadata().assignedAt());
            LocalDateTime date2 = LocalDateTime.parse(a2.metadata().assignedAt());
            return date1.compareTo(date2);
        };
    }
}