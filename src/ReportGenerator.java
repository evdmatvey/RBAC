import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ReportGenerator {

    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        if (userManager == null || assignmentManager == null) {
            throw new IllegalArgumentException("Managers cannot be null");
        }

        StringBuilder report = new StringBuilder();
        List<User> users = userManager.findAll();

        report.append("USER REPORT\n");
        report.append(String.format("Total users: %d\n\n", users.size()));

        for (User user : users) {
            report.append("------------------------------------------------------------\n");
            report.append(user.format()).append("\n");

            List<RoleAssignment> assignments = assignmentManager.findByUser(user);
            if (assignments.isEmpty()) {
                report.append("  Roles: not assigned\n");
            } else {
                report.append("  Roles:\n");
                for (RoleAssignment assignment : assignments) {
                    String status = assignment.isActive() ? "ACTIVE" : "INACTIVE";
                    String roleInfo = String.format("    - %s (%s)",
                            assignment.role().getName(), status);
                    report.append(roleInfo).append("\n");

                    if (assignment instanceof TemporaryAssignment) {
                        report.append("      Temporary assignment\n");
                    }
                }
            }
            report.append("\n");
        }

        return report.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        if (roleManager == null || assignmentManager == null) {
            throw new IllegalArgumentException("Managers cannot be null");
        }

        StringBuilder report = new StringBuilder();
        List<Role> roles = roleManager.findAll();

        report.append("ROLE REPORT\n");
        report.append(String.format("Total roles: %d\n\n", roles.size()));

        for (Role role : roles) {
            report.append("------------------------------------------------------------\n");
            report.append(String.format("Role: %s [ID: %s]\n", role.getName(), role.getId()));
            report.append(String.format("Description: %s\n", role.getDescription()));

            List<RoleAssignment> assignments = assignmentManager.findByRole(role);
            long activeAssignments = assignments.stream()
                    .filter(RoleAssignment::isActive)
                    .count();

            report.append(String.format("Assignments: %d (active: %d)\n",
                    assignments.size(), activeAssignments));

            Set<Permission> permissions = role.getPermissions();
            report.append(String.format("Permissions: %d\n", permissions.size()));

            if (!permissions.isEmpty()) {
                report.append("  Permissions:\n");
                for (Permission permission : permissions) {
                    report.append("    - ").append(permission.format()).append("\n");
                }
            }

            report.append("\n");
        }

        return report.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        if (userManager == null || assignmentManager == null) {
            throw new IllegalArgumentException("Managers cannot be null");
        }

        StringBuilder matrix = new StringBuilder();
        List<User> users = userManager.findAll();

        matrix.append("PERMISSION MATRIX\n\n");

        Set<String> allResources = new TreeSet<>();
        Map<String, Set<String>> userPermissions = new HashMap<>();

        for (User user : users) {
            Set<Permission> permissions = assignmentManager.getUserPermissions(user);
            Set<String> userResourcePerms = new TreeSet<>();

            for (Permission permission : permissions) {
                String resource = permission.resource();
                allResources.add(resource);
                userResourcePerms.add(resource + ":" + permission.name());
            }
            userPermissions.put(user.username(), userResourcePerms);
        }

        List<String> resourceList = new ArrayList<>(allResources);

        matrix.append(String.format("%-20s", "User"));
        for (String resource : resourceList) {
            matrix.append(String.format(" | %-15s", resource));
        }
        matrix.append("\n");
        matrix.append("-".repeat(20 + resourceList.size() * 18)).append("\n");

        for (User user : users) {
            matrix.append(String.format("%-20s", user.username()));

            for (String resource : resourceList) {
                Set<String> perms = userPermissions.get(user.username());
                boolean hasAccess = false;
                if (perms != null) {
                    hasAccess = perms.stream().anyMatch(p -> p.startsWith(resource + ":"));
                }
                matrix.append(String.format(" | %-15s", hasAccess ? "✓" : "✗"));
            }
            matrix.append("\n");
        }

        matrix.append("\n").append("Detailed information:\n");
        matrix.append("-".repeat(60)).append("\n");

        for (User user : users) {
            Set<Permission> permissions = assignmentManager.getUserPermissions(user);
            if (!permissions.isEmpty()) {
                matrix.append(user.username()).append(":\n");
                for (Permission permission : permissions) {
                    matrix.append("  - ").append(permission.format()).append("\n");
                }
            }
        }

        return matrix.toString();
    }

    public void exportToFile(String report, String filename) {
        ValidationUtils.requireNonEmpty(report, "report");
        ValidationUtils.requireNonEmpty(filename, "filename");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(report);
            System.out.println("Report saved to file: " + filename);
        } catch (IOException e) {
            System.err.println("Error saving report: " + e.getMessage());
        }
    }
}