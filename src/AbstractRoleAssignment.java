import java.util.UUID;

public abstract class AbstractRoleAssignment implements RoleAssignment {
    private String assignmentId;
    private User user;
    private Role role;
    private AssignmentMetadata assignmentMetadata;

    public AbstractRoleAssignment(User user, Role role, AssignmentMetadata assignmentMetadata) {
        this.assignmentId = "assignment_" + UUID.randomUUID().toString();
        this.user = user;
        this.role = role;
        this.assignmentMetadata = assignmentMetadata;
    }

    public abstract boolean isActive();

    public abstract String assignmentType();

    @Override
    public String assignmentId() {
        return this.assignmentId;
    }

    @Override
    public User user() {
        return this.user;
    }

    @Override
    public Role role() {
        return this.role;
    }

    @Override
    public AssignmentMetadata metadata() {
        return this.assignmentMetadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractRoleAssignment that = (AbstractRoleAssignment) o;
        return assignmentId.equals(that.assignmentId);
    }

    @Override
    public int hashCode() {
        return assignmentId.hashCode();
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(this.assignmentType()).append("] ")
                .append(this.role.getName()).append(" assigned to ")
                .append(this.user.username()).append(" by ")
                .append(this.assignmentMetadata.assignedBy())
                .append(" at ").append(this.assignmentMetadata.assignedAt())
                .append("\n");

        sb.append("Reason ").append(this.assignmentMetadata.reason())
                .append("\n");

        sb.append("Status: ").append(this.isActive() ? "ACTIVE" : "INACTIVE");

        return sb.toString();
    }
}
