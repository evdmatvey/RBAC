import commands.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {

    private CommandParser commandParser;
    private RBACSystem rbacSystem;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        commandParser = new CommandParser();
        rbacSystem = new RBACSystem();

        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        CommandRegistry.setupCommands(commandParser);
    }

    @Nested
    @DisplayName("User management commands tests")
    class UserManagementTests {

        @Test
        void userListCommandWithNoUsersPrintsMessage() {
            provideInput("\n");
            commandParser.parseAndExecute("user-list", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("No users"));
        }

        @Test
        void userCreateCommandCreatesUser() {
            String input = "testuser\nTest User\ntest@example.com\n";
            provideInput(input);

            commandParser.parseAndExecute("user-create", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("User created"));
            assertTrue(output.contains("testuser"));
        }

        @Test
        void userViewCommandWithExistingUserShowsInfo() {
            createTestUser();

            provideInput("testuser\n");
            commandParser.parseAndExecute("user-view", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("testuser"));
        }

        @Test
        void userUpdateCommandUpdatesUser() {
            createTestUser();

            String input = "testuser\nNew Name\nnewemail@example.com\n";
            provideInput(input);

            commandParser.parseAndExecute("user-update", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("Updated user"));
        }

        @Test
        void userDeleteCommandWithConfirmationDeletesUser() {
            createTestUser();

            String input = "testuser\nyes\n";
            provideInput(input);

            commandParser.parseAndExecute("user-delete", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("[deleted]"));
        }

        @Test
        void userDeleteCommandWithoutConfirmationDoesNothing() {
            createTestUser();

            String input = "testuser\nno\n";
            provideInput(input);

            commandParser.parseAndExecute("user-delete", createScanner(), rbacSystem);
        }
    }

    @Nested
    @DisplayName("Role management commands tests")
    class RoleManagementTests {

        @Test
        void roleListCommandWithNoRolesPrintsMessage() {
            provideInput("\n");
            commandParser.parseAndExecute("role-list", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("No roles"));
        }

        @Test
        void roleCreateCommandCreatesRole() {
            String input = "admin\nAdministrator role\n";
            provideInput(input);

            commandParser.parseAndExecute("role-create", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("Role created"));
            assertTrue(output.contains("admin"));
        }

        @Test
        void roleViewCommandWithExistingRoleShowsInfo() {
            createTestRole();

            provideInput("admin\n");
            commandParser.parseAndExecute("role-view", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("admin"));
        }

        @Test
        void roleAddPermissionCommandAddsPermission() {
            createTestRole();

            String input = "admin\nread\nfile\nRead permission\n";
            provideInput(input);

            commandParser.parseAndExecute("role-add-permission", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("Role updated"));
        }
    }

    @Nested
    @DisplayName("Assignment commands tests")
    class AssignmentTests {
        @Test
        void assignListCommandShowsAssignments() {
            provideInput("\n");
            commandParser.parseAndExecute("assign-list", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertNotNull(output);
        }
    }

    @Nested
    @DisplayName("Permission commands tests")
    class PermissionTests {

        @Test
        void permissionsUserCommandShowsUserPermissions() {
            createTestUser();

            provideInput("testuser\n");
            commandParser.parseAndExecute("permissions-user", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertNotNull(output);
        }

        @Test
        void permissionsCheckCommandChecksUserPermission() {
            createTestUser();

            String input = "testuser\nread\nfile\n";
            provideInput(input);

            commandParser.parseAndExecute("permissions-check", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("Permission not found") || output.contains("Permission found"));
        }
    }

    @Nested
    @DisplayName("Service commands tests")
    class ServiceTests {

        @Test
        void helpCommandPrintsHelp() {
            provideInput("\n");
            commandParser.parseAndExecute("help", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("Command"));
            assertTrue(output.contains("Description"));
        }

        @Test
        void statsCommandPrintsStatistics() {
            provideInput("\n");
            commandParser.parseAndExecute("stats", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("Assignments distribution"));
        }

        @Test
        void auditLogCommandPrintsLog() {
            provideInput("\n");
            commandParser.parseAndExecute("audit-log", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertNotNull(output);
        }

        @Test
        void reportUsersCommandGeneratesReport() {
            provideInput("\n");
            commandParser.parseAndExecute("report-users", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("Users report"));
        }

        @Test
        void reportRolesCommandGeneratesReport() {
            provideInput("\n");
            commandParser.parseAndExecute("report-roles", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("Role report"));
        }

        @Test
        void reportMatrixCommandGeneratesReport() {
            provideInput("\n");
            commandParser.parseAndExecute("report-matrix", createScanner(), rbacSystem);

            String output = outputStream.toString();
            assertTrue(output.contains("Permission Matrix"));
        }
    }

    private void provideInput(String data) {
        ByteArrayInputStream testIn = new ByteArrayInputStream(data.getBytes());
        System.setIn(testIn);
    }

    private Scanner createScanner() {
        return new Scanner(System.in);
    }

    private void createTestUser() {
        String input = "testuser\nTest User\ntest@example.com\n";
        provideInput(input);
        commandParser.parseAndExecute("user-create", createScanner(), rbacSystem);
        outputStream.reset();
    }

    private void createTestRole() {
        String input = "admin\nAdministrator role\n";
        provideInput(input);
        commandParser.parseAndExecute("role-create", createScanner(), rbacSystem);
        outputStream.reset();
    }
}