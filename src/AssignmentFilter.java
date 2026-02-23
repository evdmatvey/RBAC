@FunctionalInterface
public interface AssignmentFilter {
    boolean test(RoleAssignment assignment);
}