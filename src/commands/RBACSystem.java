package commands;

import entities.*;
import repositories.*;
import utils.FormatUtils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.NoSuchElementException;

public class RBACSystem {
    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;
    private String currentUser;

    public RBACSystem() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);
    }

    public void setCurrentUser(String username) {
        currentUser = username;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public void initialize() {
        ArrayList<Permission> permissions = generatePermissions();

        roleManager.add(createAdminRole(permissions));
        roleManager.add(createManagerRole(permissions));
        roleManager.add(createViewerRole(permissions));

        createAdmin();
        assignRoleToAdmin();
    }

    private ArrayList<Permission> generatePermissions() {
        String[] names = {"read", "write", "delete", "update"};
        String[] resources = {"users", "documents", "tags", "posts"};

        ArrayList<Permission> permissions = new ArrayList<>();

        for (String name : names) {
            for (String resource : resources) {
                permissions.add(new Permission(name, resource, String.format("Can %s %s", name, resource)));
            }
        }

        return permissions;
    }

    private Role createAdminRole(ArrayList<Permission> permissions) {
        Role admin = new Role("admin", "Full system access");

        for (Permission permission : permissions) {
            admin.addPermission(permission);
        }

        return admin;
    }

    private Role createManagerRole(ArrayList<Permission> permissions) {
        Role manager = new Role("manager", "Manage system access");

        for (Permission permission : permissions) {
            String normalizedRoleName = permission.name().toLowerCase(Locale.ROOT);

            if(normalizedRoleName.equals("read") || normalizedRoleName.equals("write")) {
                manager.addPermission(permission);
            }
        }

        return manager;
    }

    private Role createViewerRole(ArrayList<Permission> permissions) {
        Role viewer = new Role("viewer", "View system access");

        for (Permission permission : permissions) {
            String normalizedRoleName = permission.name().toLowerCase(Locale.ROOT);

            if(normalizedRoleName.equals("read")) {
                viewer.addPermission(permission);
            }
        }

        return viewer;
    }

    private void createAdmin() {
        User admin = User.create("admin", "Elvis Presley", "admin@coolsystem.com");
        userManager.add(admin);
    }

    private void assignRoleToAdmin() {
        User admin = userManager.findByUsername("admin")
                .orElseThrow(() -> new NoSuchElementException("Admin user not found in user manager!"));
        Role adminRole = roleManager.findByName("admin")
                .orElseThrow(() -> new NoSuchElementException("Admin role not found in role manager!"));

        AssignmentMetadata metadata = AssignmentMetadata.now("system", "initialization");
        PermanentAssignment adminAssignment = new PermanentAssignment(admin, adminRole, metadata);

        assignmentManager.add(adminAssignment);
    }

    public String generateStatistics() {
        String[] headers = {"Name", "Count"};
        ArrayList<String[]> rows = new ArrayList<>();

        rows.add(new String[]{"Users", String.valueOf(userManager.count())});
        rows.add(new String[]{"Roles", String.valueOf(roleManager.count())});
        rows.add(new String[]{"Assignments", String.valueOf(assignmentManager.count())});

        return FormatUtils.formatTable(headers, rows);
    }
}
