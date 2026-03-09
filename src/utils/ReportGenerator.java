package utils;

import entities.*;
import repositories.*;

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

        report.append(FormatUtils.formatHeader("Users report"));
        report.append(String.format("Total users: %d\n\n", users.size()));

        for (User user : users) {
            report.append(FormatUtils.formatBox(user.format()));

            List<RoleAssignment> assignments = assignmentManager.findByUser(user);

            if (assignments.isEmpty()) {
                report.append("  Roles: not assigned\n\n");
            } else {
                report.append("  Roles:\n");

                String[] roleHeaders = {"Role", "Status", "Type"};
                List<String[]> roleRows = new ArrayList<>();

                for (RoleAssignment assignment : assignments) {
                    String status = assignment.isActive() ? "ACTIVE" : "INACTIVE";
                    String type = (assignment instanceof TemporaryAssignment)
                            ? "Temporary" : "Permanent";

                    roleRows.add(new String[]{
                            assignment.role().getName(),
                            status,
                            type
                    });
                }

                report.append(FormatUtils.formatTable(roleHeaders, roleRows));
                report.append("\n");
            }
        }

        return report.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        if (roleManager == null || assignmentManager == null) {
            throw new IllegalArgumentException("Managers cannot be null");
        }

        StringBuilder report = new StringBuilder();
        List<Role> roles = roleManager.findAll();

        report.append(FormatUtils.formatHeader("Role report"));
        report.append(String.format("Total roles: %d\n\n", roles.size()));

        for (Role role : roles) {
            List<RoleAssignment> assignments = assignmentManager.findByRole(role);
            long activeAssignments = assignments.stream()
                    .filter(RoleAssignment::isActive)
                    .count();

            Set<Permission> permissions = role.getPermissions();

            String[] headers = {"Property", "Value"};
            List<String[]> rows = new ArrayList<>();

            rows.add(new String[]{"Role ID", role.getId()});
            rows.add(new String[]{"Role Name", role.getName()});
            rows.add(new String[]{"Description", role.getDescription()});
            rows.add(new String[]{"Total Assignments", String.valueOf(assignments.size())});
            rows.add(new String[]{"Active Assignments", String.valueOf(activeAssignments)});
            rows.add(new String[]{"Total Permissions", String.valueOf(permissions.size())});

            report.append(FormatUtils.formatTable(headers, rows));

            if (!permissions.isEmpty()) {
                report.append(FormatUtils.padLeft("Permissions:", 4));
                report.append("\n");

                String[] permHeaders = {"Permission", "Resource", "Description"};
                List<String[]> permRows = new ArrayList<>();

                for (Permission permission : permissions) {
                    permRows.add(new String[]{
                            permission.name(),
                            permission.resource(),
                            FormatUtils.truncate(permission.description(), 40)
                    });
                }

                report.append(FormatUtils.formatTable(permHeaders, permRows));
            } else {
                report.append(FormatUtils.padLeft("No permissions assigned", 4));
                report.append("\n");
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

        matrix.append(FormatUtils.formatHeader("Permission Matrix"));

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

        String[] headers = new String[resourceList.size() + 1];
        headers[0] = "User";
        for (int i = 0; i < resourceList.size(); i++) {
            headers[i + 1] = resourceList.get(i);
        }

        List<String[]> rows = new ArrayList<>();

        for (User user : users) {
            String[] row = new String[resourceList.size() + 1];
            row[0] = user.username();

            for (int i = 0; i < resourceList.size(); i++) {
                String resource = resourceList.get(i);
                Set<String> perms = userPermissions.get(user.username());
                boolean hasAccess = false;
                if (perms != null) {
                    hasAccess = perms.stream().anyMatch(p -> p.startsWith(resource + ":"));
                }
                row[i + 1] = hasAccess ? "yes" : "no";
            }
            rows.add(row);
        }

        matrix.append(FormatUtils.formatTable(headers, rows));
        matrix.append("\n");

        matrix.append(FormatUtils.formatHeader("DETAILED PERMISSIONS"));

        for (User user : users) {
            Set<Permission> permissions = assignmentManager.getUserPermissions(user);
            if (!permissions.isEmpty()) {
                matrix.append(FormatUtils.formatBox(user.username()));

                String[] permHeaders = {"Permission", "Resource", "Description"};
                List<String[]> permRows = new ArrayList<>();

                for (Permission permission : permissions) {
                    permRows.add(new String[]{
                            permission.name(),
                            permission.resource(),
                            FormatUtils.truncate(permission.description(), 50)
                    });
                }

                matrix.append(FormatUtils.formatTable(permHeaders, permRows));
                matrix.append("\n");
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