import java.time.LocalDateTime;

public class AssignmentFilters {
    public static AssignmentFilter byUser(User user) {
        return assignment -> assignment.user().equals(user);
    }

    public static AssignmentFilter byUsername(String username) {
        return assignment -> assignment.user().username()
                .equals(username);
    }

    public static AssignmentFilter byRole(Role role) {
        return assignment -> assignment.role().equals(role);
    }

    public static AssignmentFilter byRoleName(String roleName) {
        return assignment -> assignment.role().getName()
                .equals(roleName);
    }

    public static AssignmentFilter activeOnly() {
        return assignment -> assignment.isActive();
    }

    public static AssignmentFilter inactiveOnly() {
        return assignment -> !assignment.isActive();
    }

    public static AssignmentFilter byType(String type) {
        return assignment -> assignment.assignmentType()
                .equals(type);
    }

    public static AssignmentFilter assignedBy(String username) {
        return assignment -> assignment.metadata()
                .assignedBy().equals(username);
    }

    public static AssignmentFilter assignedAfter(String date) {
        return assignment -> {
            LocalDateTime filterDate = LocalDateTime.parse(date);
            LocalDateTime assignedDate = LocalDateTime.parse(assignment.metadata().assignedAt());

            return assignedDate.isAfter(filterDate);
        };
    }

    public static AssignmentFilter expiringBefore(String date) {
        return assignment -> {
            if (!(assignment instanceof TemporaryAssignment temp)) {
                return false;
            }

            LocalDateTime filterDate = LocalDateTime.parse(date);
            LocalDateTime expiresAt = LocalDateTime.parse(temp.getExpiresAt());

            return expiresAt.isBefore(filterDate);
        };
    }
}
