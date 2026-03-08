package commands;

import entities.RoleAssignment;
import utils.FormatUtils;

import java.util.List;

public class CommandRegistryHelper {
    public static String getUserNotFound(String username) {
        return String.format("User by username \"%s\" not found!", username);
    }

    public static String getRoleNotFound(String roleName) {
        return String.format("Role by name \"%s\" not found", roleName);
    }

    public static String getAssignmentsTable(List<RoleAssignment> assignments) {
        String[] headers = {"Username", "Role", "Type", "Status", "Assigned at"};
        List<String[]> rows = assignments.stream()
                .map(a -> {
                    String username = a.user().username();
                    String role = a.role().getName();
                    String type = a.assignmentType();
                    String status = a.isActive() ? "ACTIVE" : "INACTIVE";
                    String assignedAt = a.metadata().assignedAt();

                    return new String[]{username, role, type, status, assignedAt};
                })
                .toList();

        if(rows.isEmpty()) {
            return "No assignments!";

        }

        return FormatUtils.formatTable(headers, rows);
    }
}
