import utils.*;

import repositories.*;
import entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {

    private ReportGenerator reportGenerator;
    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;
    private User testUser;
    private Role testRole;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);
        reportGenerator = new ReportGenerator();

        testPermission = new Permission("read", "file", "Read permission");
        Set<Permission> permissions = new HashSet<>();
        permissions.add(testPermission);

        testRole = new Role("Admin", "Administrator role");
        permissions.forEach(p -> testRole.addPermission(p));

        testUser = User.create("john_doe", "John Doe", "john@example.com");

        userManager.add(testUser);
        roleManager.add(testRole);
    }

    @Test
    void generateUserReport_Success_ReturnsFormattedReport() {
        String report = reportGenerator.generateUserReport(userManager, assignmentManager);
        assertNotNull(report);
        assertTrue(report.contains("Users report"));
        assertTrue(report.contains("john_doe"));
    }

    @Test
    void generateUserReport_WithNullManagers_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.generateUserReport(null, assignmentManager));
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.generateUserReport(userManager, null));
    }

    @Test
    void generateUserReport_WithUserAssignments_IncludesRolesInReport() {
        AssignmentMetadata am = AssignmentMetadata.now("Me", "Test");
        RoleAssignment assignment = new PermanentAssignment(testUser, testRole, am);
        assignmentManager.add(assignment);

        String report = reportGenerator.generateUserReport(userManager, assignmentManager);
        assertTrue(report.contains("Admin"));
        assertTrue(report.contains("ACTIVE"));
    }

    @Test
    void generateRoleReport_Success_ReturnsFormattedReport() {
        String report = reportGenerator.generateRoleReport(roleManager, assignmentManager);
        assertNotNull(report);
        assertTrue(report.contains("Role report"));
        assertTrue(report.contains("Admin"));
    }

    @Test
    void generateRoleReport_WithNullManagers_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.generateRoleReport(null, assignmentManager));
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.generateRoleReport(roleManager, null));
    }

    @Test
    void generatePermissionMatrix_Success_ReturnsFormattedMatrix() {
        AssignmentMetadata am = AssignmentMetadata.now("Me", "Test");
        RoleAssignment assignment = new PermanentAssignment(testUser, testRole, am);
        assignmentManager.add(assignment);

        String report = reportGenerator.generatePermissionMatrix(userManager, assignmentManager);
        assertNotNull(report);
        assertTrue(report.contains("Permission Matrix"));
        assertTrue(report.contains("john_doe"));
    }

    @Test
    void generatePermissionMatrix_WithNullManagers_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.generatePermissionMatrix(null, assignmentManager));
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.generatePermissionMatrix(userManager, null));
    }

    @Test
    void generatePermissionMatrix_MultipleUsers_CreatesMatrixWithAllResources() {
        User user2 = User.create("jane_doe", "Jane Doe", "jane@example.com");
        userManager.add(user2);

        Permission perm1 = new Permission("read", "file", "Read permission");
        Permission perm2 = new Permission("write", "file", "Write permission");

        Set<Permission> permissions1 = new HashSet<>();
        permissions1.add(perm1);

        Set<Permission> permissions2 = new HashSet<>();
        permissions2.add(perm2);

        Role role1 = new Role("Reader", "Read role");
        permissions1.forEach(p -> role1.addPermission(p));

        Role role2 = new Role("Writer", "Write role");
        permissions2.forEach(p -> role2.addPermission(p));

        roleManager.add(role1);
        roleManager.add(role2);

        AssignmentMetadata am1 = AssignmentMetadata.now("Me", "Test");
        AssignmentMetadata am2 = AssignmentMetadata.now("Me", "Test");

        RoleAssignment assignment1 = new PermanentAssignment(testUser, role1, am1);
        RoleAssignment assignment2 = new PermanentAssignment(user2, role2, am2);

        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);

        String report = reportGenerator.generatePermissionMatrix(userManager, assignmentManager);
        assertTrue(report.contains("file"));
        assertTrue(report.contains("yes"));
        assertFalse(report.contains("no"));
    }

    @Test
    void exportToFile_WithEmptyReport_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.exportToFile("", "test.txt"));
    }

    @Test
    void exportToFile_WithEmptyFilename_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.exportToFile("report", ""));
    }

    @Test
    void exportToFile_WithNullReport_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.exportToFile(null, "test.txt"));
    }

    @Test
    void exportToFile_WithIOException_HandlesError() {
        String report = "Test report";
        String invalidPath = "/invalid/path/that/does/not/exist/report.txt";
        assertDoesNotThrow(() -> reportGenerator.exportToFile(report, invalidPath));
    }
}