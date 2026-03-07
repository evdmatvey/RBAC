package commands;

import entities.*;
import filters.*;
import repositories.AssignmentManager;
import repositories.UserManager;
import utils.ConsoleUtils;
import utils.FormatUtils;

import java.util.*;

public class CommandRegistry {
    private static AuditLog auditLog = new AuditLog();

    public static void setupCommands(CommandParser commandParser) {
        setupUserManageCommands(commandParser);
        setupRoleManageCommands(commandParser);
        setupServiceCommands(commandParser);
    }

    private static void setupUserManageCommands(CommandParser commandParser) {
        commandParser.registerCommand("user-list", "Show all users",
                (scanner, rbacSystem) -> {
            List<User> users = rbacSystem.getUserManager().findAll();

            if (users.isEmpty()) {
                System.out.println("No users!");
                return;
            }

            String[] headers = {"Username", "Full name", "email"};
            ArrayList<String[]> rows = new ArrayList<>();

            for (User user : users) {
                rows.add(new String[]{user.username(), user.fullName(), user.email()});
            }

            System.out.println(FormatUtils.formatTable(headers, rows));
        });

        commandParser.registerCommand("user-create", "Create new user",
                (scanner, rbacSystem) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);
            String fullName = ConsoleUtils.promptString(scanner, "Enter full name: ", true);
            String email = ConsoleUtils.promptString(scanner, "Enter email: ", true);

            User created = User.create(username, fullName, email);

            rbacSystem.getUserManager().add(created);
            System.out.println("User created: " + created.format());

            auditLog.log("CREATE_USER", rbacSystem.getCurrentUser(), "users", created.format());
        });

        commandParser.registerCommand("user-view", "Get user information by username",
                (scanner, rbacSystem) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);

            User user = rbacSystem.getUserManager().findByUsername(username)
                    .orElseThrow(() -> new NoSuchElementException(String.format("User with username \"%s\" not found!", username)));

            Set<Permission> permissions = rbacSystem.getAssignmentManager().getUserPermissions(user);
            Set<Role> roles = rbacSystem.getAssignmentManager().getUserRoles(user);

            List<String> userPermissions = new ArrayList<>();
            List<String> userRoles = new ArrayList<>();

            for (Permission permission : permissions) {
                userPermissions.add(permission.format());
            }

            for (Role role : roles) {
                userRoles.add(role.format());
            }

            System.out.println("User: " + user.format());
            System.out.println(FormatUtils.formatHeader("Permissions"));
            System.out.println(FormatUtils.formatList(userPermissions, "- ", ""));
            System.out.println(FormatUtils.formatHeader("Roles"));
            System.out.println(FormatUtils.formatList(userRoles, "- ", ""));
        });

        commandParser.registerCommand("user-update", "Update user by username",
                (scanner, rbacSystem) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);

            String fullName = ConsoleUtils.promptString(scanner, "Enter new full name: ", true);
            String email = ConsoleUtils.promptString(scanner, "Enter new email: ", true);

            rbacSystem.getUserManager().update(username, fullName, email);

            User updated = rbacSystem.getUserManager().findByUsername(username)
                    .orElseThrow(() -> new NoSuchElementException(String.format("User with username \"%s\" not found!", username)));

            System.out.println("Updated user: " + updated.format());
        });

        commandParser.registerCommand("user-delete", "Delete user by username",
                (scanner, rbacSystem) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);
            boolean confirm = ConsoleUtils.promptYesNo(scanner, "Are you sure?");

            if (!confirm) return;

            User user = rbacSystem.getUserManager().findByUsername(username)
                    .orElseThrow(() -> new NoSuchElementException(String.format("User with username \"%s\" not found!", username)));

            boolean result = rbacSystem.getUserManager().remove(user);

            if (result) {
                System.out.println("User: " + user.format() + " [deleted]");
                auditLog.log("DELETE_USER", rbacSystem.getCurrentUser(), "users", user.format());
            } else {
                System.out.println("Some errors detected while deleting, try again later [0_o]");
            }
        });

        commandParser.registerCommand("user-search", "Search user with selected method",
                (scanner, rbacSystem) -> {
            List<String> options = Arrays.asList("By username", "By email", "By email domain [o_0]", "By full name");
            String choice = ConsoleUtils.promptChoice(scanner, "Select user search method: ", options);

            Optional<User> user = Optional.empty();
            List<User> filteredUsers = new ArrayList<>();
            UserManager userManager = rbacSystem.getUserManager();

            if(options.get(0).equals(choice)) {
                String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);
                user = userManager.findByUsername(username);
            }

            if(options.get(1).equals(choice)) {
                String email = ConsoleUtils.promptString(scanner, "Enter email: ", true);
                user = userManager.findByEmail(email);
            }

            if(options.get(2).equals(choice)) {
                String domain = ConsoleUtils.promptString(scanner, "Enter email domain: ", true);
                UserFilter domainFilter = UserFilters.byEmailDomain(domain);

                filteredUsers = userManager.findByFilter(domainFilter);
            }

            if(options.get(3).equals(choice)) {
                String fullName = ConsoleUtils.promptString(scanner, "Enter full name: ", true);
                UserFilter fullNameFilter = UserFilters.byFullNameContains(fullName);

                filteredUsers = userManager.findByFilter(fullNameFilter);
            }

            if(user.isEmpty()) {
                List<String> normalizedUsers = filteredUsers.stream()
                                .map(User::format)
                                .toList();

                System.out.println(FormatUtils.formatHeader("Users found"));
                System.out.println(FormatUtils.formatList(normalizedUsers, "- ", ""));
            } else {
                User u = user.orElseThrow(() -> new NoSuchElementException("User not found!"));
                System.out.println("User: " + u.format());
            }
        });
    }

    private static void setupRoleManageCommands(CommandParser commandParser) {
        commandParser.registerCommand("role-list", "Show all roles",
                (scanner, rbacSystem) -> {
            List<Role> roles = rbacSystem.getRoleManager().findAll();

            if(roles.isEmpty()) {
                System.out.println("No roles!");
                return;
            }

            String[] headers = {"Name", "Permissions count", "Id"};
            List<String[]> rows = new ArrayList<>();

            for(Role role : roles) {
                rows.add(new String[]{role.getName(), String.valueOf(role.getPermissions().size()), role.getId()});
            }

            System.out.println(FormatUtils.formatTable(headers, rows));
        });

        commandParser.registerCommand("role-create", "Create role",
                (scanner, rbacSystem) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter name: ", true);
            String description = ConsoleUtils.promptString(scanner, "Enter description: ", true);

            Role created = new Role(name, description);
            rbacSystem.getRoleManager().add(created);

            System.out.println("Role created: " + created.compactFormat());

            auditLog.log("CREATE_ROLE", rbacSystem.getCurrentUser(), "roles", created.compactFormat());
        });

        commandParser.registerCommand("role-view", "Get role information by name",
                (scanner, rbacSystem) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter name: ", true);

            Role role = rbacSystem.getRoleManager().findByName(name)
                    .orElseThrow(() -> new NoSuchElementException(String.format("Role by name \"%s\" not found!", name)));

            System.out.println(role.format());
        });

        commandParser.registerCommand("role-delete", "Delete role by name",
                (scanner, rbacSystem) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter name: ", true);

            Role role = rbacSystem.getRoleManager().findByName(name)
                    .orElseThrow(() -> new NoSuchElementException(String.format("Role by name \"%s\" not found!", name)));

            AssignmentFilter roleFilter = AssignmentFilters.byRole(role);

            List<User> users = rbacSystem.getAssignmentManager().findByFilter(roleFilter).stream()
                    .map(RoleAssignment::user)
                    .toList();

            if (!users.isEmpty()) {
                String[] headers = {"Name", "Full name", "Email"};
                List<String[]> rows = users.stream()
                        .map(u -> new String[]{u.username(), u.fullName(), u.email()})
                        .toList();

                System.out.println(FormatUtils.formatHeader("Users with role"));
                System.out.println(FormatUtils.formatTable(headers, rows));
            }

            boolean confirm = ConsoleUtils.promptYesNo(scanner, "Are you sure? (yes/no): ");

            if(confirm) {
                boolean isDeleted = rbacSystem.getRoleManager().remove(role);
                if(isDeleted){
                    System.out.println(String.format("Role deleted: %s" + role.compactFormat()));
                    auditLog.log("DELETE_ROLE", rbacSystem.getCurrentUser(), "roles", "delete success");
                } else {
                    System.out.println("Some issues found while deleting role. Try again later [0-0]");
                }
            }
        });

        commandParser.registerCommand("role-add-permission", "Add new permission to role",
                (scanner, rbacSystem) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter name: ", true);

            Role role = rbacSystem.getRoleManager().findByName(name)
                    .orElseThrow(() -> new NoSuchElementException(String.format("Role by name \"%s\" not found!", name)));

            String permissionName = ConsoleUtils.promptString(scanner, "Enter name: ", true);
            String permissionResource = ConsoleUtils.promptString(scanner, "Enter resource: ", true);
            String permissionDescription = ConsoleUtils.promptString(scanner, "Enter description: ", true);

            Permission permission = new Permission(permissionName, permissionResource, permissionDescription);

            rbacSystem.getRoleManager().addPermissionToRole(role.getName(), permission);

            System.out.println(FormatUtils.formatHeader("Role updated!"));
            System.out.println("Add new permission: " + permission.format());
        });

        commandParser.registerCommand("role-remove-permission", "Remove permission from role by name",
                (scanner, rbacSystem) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter name: ", true);

            Role role = rbacSystem.getRoleManager().findByName(name)
                    .orElseThrow(() -> new NoSuchElementException(String.format("Role by name \"%s\" not found!", name)));

            Set<Permission> permissions = role.getPermissions();
            List<String> permissionNames = permissions.stream()
                    .map(Permission::format)
                    .toList();

            String choice = ConsoleUtils.promptChoice(scanner, "Select permission to remove: ", permissionNames);
            String[] parts = choice.split(" ");
            String pName = parts[0];
            String pResource = parts[2].replace(":", "");

            Permission permission = permissions.stream()
                    .filter(p ->  p.name().equals(pName) && p.resource().equals(pResource))
                    .toList()
                    .getFirst();

            boolean confirm = ConsoleUtils.promptYesNo(scanner, "Are you sure?");

            if (confirm) {
                rbacSystem.getRoleManager().removePermissionFromRole(role.getName(), permission);

                System.out.println("Permission deleted: " + permission.format());
            }
        });

        commandParser.registerCommand("role-search", "Search role by selected method",
                (scanner, rbacSystem) -> {
            List<String> options = Arrays.asList("By name", "By permission", "By min permissions count");

            String choice = ConsoleUtils.promptChoice(scanner, "Select search method: ", options);

            List<Role> roles = new ArrayList<>();

            if(options.get(0).equals(choice)) {
                String name = ConsoleUtils.promptString(scanner, "Enter name: ", true);

                Role role = rbacSystem.getRoleManager().findByName(name)
                    .orElseThrow(() -> new NoSuchElementException(String.format("Role by name \"%s\" not found!", name)));

                System.out.println(role.format());
                return;
            }

            if(options.get(1).equals(choice)) {
                String permissionName = ConsoleUtils.promptString(scanner, "Enter permission name: ", true);
                String permissionResource = ConsoleUtils.promptString(scanner, "Enter permission resource: ", true);

                roles = rbacSystem.getRoleManager().findRolesWithPermission(permissionName, permissionResource);
            }

            if(options.get(2).equals(choice)) {
                int minimalPermissionsCount = ConsoleUtils.promptInt(scanner, "Enter minimal permission count: ", 0, 100);
                RoleFilter minimalFilter = RoleFilters.hasAtLeastNPermissions(minimalPermissionsCount);

                roles = rbacSystem.getRoleManager().findByFilter(minimalFilter);
            }

            String[] headers = {"Id", "Name", "Permissions Count", "Description"};
            List<String[]> rows = roles.stream()
                    .map(r -> new String[]{r.getId(), r.getName(), String.valueOf(r.getPermissions().size()), r.getDescription()})
                    .toList();

            if(rows.isEmpty()) {
                System.out.println("Roles not found!");
            } else {
                System.out.println(FormatUtils.formatTable(headers, rows));
            }
        });
    }

    private static void setupServiceCommands(CommandParser commandParser) {
        commandParser.registerCommand("help", "Get information about available commands",
                (scanner, rbacSystem) -> {
            commandParser.printHelp();
        });

        commandParser.registerCommand("stats", "Get RBAC statistics",
                (scanner, rbacSystem) -> {
            System.out.println(rbacSystem.generateStatistics());

            AssignmentManager assignmentManager = rbacSystem.getAssignmentManager();
            String totalAssignments = String.valueOf(assignmentManager.count());
            String activeAssignments = String.valueOf(assignmentManager.getActiveAssignments().size());
            String inactiveAssignments = String.valueOf(assignmentManager.getExpiredAssignments().size());

            String[] headers = {"Total", "Active", "Inactive"};
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{totalAssignments, activeAssignments, inactiveAssignments});

            System.out.println(FormatUtils.formatHeader("Assignments distribution"));
            System.out.println(FormatUtils.formatTable(headers, rows));

            int usersCount = rbacSystem.getUserManager().count();
            int rolesCount = rbacSystem.getRoleManager().count();
            float averageRolesUsers = (float)usersCount / (float)rolesCount;

            System.out.println(FormatUtils.formatHeader("Average roles by user: " +
                    FormatUtils.formatFloatNumber(averageRolesUsers)));

            Map<Role, Integer> roleRating = new HashMap<>();

            for (RoleAssignment assignment : assignmentManager.getActiveAssignments()) {
                Role role = assignment.role();

                if (roleRating.containsKey(role)) {
                    roleRating.put(role, roleRating.get(role) + 1);
                } else {
                    roleRating.put(role, 1);
                }
            }

            List<Map.Entry<Role, Integer>> rolesTop = roleRating.entrySet().stream()
                    .sorted((r1, r2) -> r1.getValue().compareTo(r2.getValue()))
                    .limit(3)
                    .toList();

            String[] rolesTopHeaders = {"Id", "Name", "Description", "Usage", "Permissions count"};
            List<String[]> rolesTopRows = rolesTop.stream()
                    .map(rt -> {
                        Role role = rt.getKey();
                        String usage = String.valueOf(rt.getValue());
                        String permissionsCount = String.valueOf(role.getPermissions().size());

                        return new String[]{role.getId(), role.getName(), role.getDescription(), usage, permissionsCount};
                    })
                    .toList();

            System.out.println(FormatUtils.formatHeader("Top 3 roles"));
            System.out.println(FormatUtils.formatTable(rolesTopHeaders, rolesTopRows));
        });

        commandParser.registerCommand("clear", "Clear console",
                (scanner, rbacSystem) -> {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        });

        commandParser.registerCommand("exit", "Shutdown RBAC",
                (scanner, rbacSystem) -> {
            boolean confirm = ConsoleUtils.promptYesNo(scanner, "Are you sure?");

            if(confirm) {
                System.exit(0);
            }
        });

        commandParser.registerCommand("audit-log", "Show audit log",
                (scanner, rbacSystem) -> {
            auditLog.printLog();
        });
    }
}
