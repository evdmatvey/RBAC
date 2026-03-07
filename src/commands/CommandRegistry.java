package commands;

import entities.Permission;
import entities.Role;
import entities.User;
import filters.UserFilter;
import filters.UserFilters;
import repositories.AssignmentManager;
import repositories.UserManager;
import utils.ConsoleUtils;
import utils.FormatUtils;

import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {
    public static void setupCommands(CommandParser commandParser) {
        setupUserManageCommands(commandParser);
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
            boolean confirm = ConsoleUtils.promptYesNo(scanner, "Are you sure? : ");

            if (!confirm) return;

            User user = rbacSystem.getUserManager().findByUsername(username)
                    .orElseThrow(() -> new NoSuchElementException(String.format("User with username \"%s\" not found!", username)));

            boolean result = rbacSystem.getUserManager().remove(user);

            if (result) {
                System.out.println("User: " + user.format() + " [deleted]");
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
}
