public class PermanentAssignment extends AbstractRoleAssignment {
    private boolean revoked = false;

    public PermanentAssignment(User user, Role role, AssignmentMetadata assignmentMetadata) {
        super(user, role, assignmentMetadata);
    }

    @Override
    public String assignmentType() {
        return "PERMANENT";
    }

    @Override
    public boolean isActive() {
        if (this.revoked)
            return false;

        return true;
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isRevoked() {
        return this.revoked;
    }
}
