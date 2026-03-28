import utils.*;
import repositories.*;
import entities.*;
import filters.UserFilter;
import filters.UserFilters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    @DisplayName("Should generate user report using parallel stream")
    void generateUserReportParallel_Success_ReturnsFormattedReport() {
        AssignmentMetadata am = AssignmentMetadata.now("Me", "Test");
        RoleAssignment assignment = new PermanentAssignment(testUser, testRole, am);
        assignmentManager.add(assignment);

        String report = reportGenerator.generateUserReportParallel(userManager, assignmentManager);

        assertNotNull(report);
        assertTrue(report.contains("Users report (Parallel)"));
        assertTrue(report.contains("john_doe"));
        assertTrue(report.contains("Admin"));
        assertTrue(report.contains("ACTIVE"));
    }

    @Test
    @DisplayName("Should generate user report parallel with null managers throws exception")
    void generateUserReportParallel_WithNullManagers_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.generateUserReportParallel(null, assignmentManager));
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.generateUserReportParallel(userManager, null));
    }

    @Test
    @DisplayName("Should generate user report parallel with multiple users")
    void generateUserReportParallel_WithMultipleUsers_ReturnsCompleteReport() {
        for (int i = 0; i < 50; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            userManager.add(user);

            Role role = new Role("ROLE_" + i, "Role " + i);
            roleManager.add(role);

            AssignmentMetadata am = AssignmentMetadata.now("admin", "Test");
            RoleAssignment assignment = new PermanentAssignment(user, role, am);
            assignmentManager.add(assignment);
        }

        String report = reportGenerator.generateUserReportParallel(userManager, assignmentManager);

        assertNotNull(report);
        assertTrue(report.contains("Users report (Parallel)"));
        assertTrue(report.contains("Total users: 51"));
    }

    @Test
    @DisplayName("Should generate permission matrix using parallel stream")
    void generatePermissionMatrixParallel_Success_ReturnsFormattedMatrix() {
        AssignmentMetadata am = AssignmentMetadata.now("Me", "Test");
        RoleAssignment assignment = new PermanentAssignment(testUser, testRole, am);
        assignmentManager.add(assignment);

        String report = reportGenerator.generatePermissionMatrixParallel(userManager, assignmentManager);

        assertNotNull(report);
        assertTrue(report.contains("Permission Matrix (Parallel)"));
        assertTrue(report.contains("john_doe"));
        assertTrue(report.contains("file"));
    }

    @Test
    @DisplayName("Should generate permission matrix parallel with null managers throws exception")
    void generatePermissionMatrixParallel_WithNullManagers_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.generatePermissionMatrixParallel(null, assignmentManager));
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.generatePermissionMatrixParallel(userManager, null));
    }

    @Test
    @DisplayName("Should generate permission matrix parallel with multiple users")
    void generatePermissionMatrixParallel_WithMultipleUsers_ReturnsCompleteMatrix() {
        for (int i = 0; i < 30; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            userManager.add(user);

            Permission permission = new Permission("perm_" + i, "resource_" + i, "Test permission");
            Role role = new Role("ROLE_" + i, "Role " + i);
            role.addPermission(permission);
            roleManager.add(role);

            AssignmentMetadata am = AssignmentMetadata.now("admin", "Test");
            RoleAssignment assignment = new PermanentAssignment(user, role, am);
            assignmentManager.add(assignment);
        }

        String report = reportGenerator.generatePermissionMatrixParallel(userManager, assignmentManager);

        assertNotNull(report);
        assertTrue(report.contains("Permission Matrix (Parallel)"));
        assertTrue(report.contains("DETAILED PERMISSIONS"));
    }

    @Test
    @DisplayName("Should find users by filter using parallel stream")
    void findByFilterParallel_WithPredicate_ReturnsFilteredUsers() {
        for (int i = 0; i < 100; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            userManager.add(user);
        }

        List<User> filteredUsers = reportGenerator.findByFilterParallel(
                userManager,
                user -> user.username().contains("50")
        );

        assertNotNull(filteredUsers);
        assertTrue(filteredUsers.size() >= 1);
        assertTrue(filteredUsers.stream().allMatch(u -> u.username().contains("50")));
    }

    @Test
    @DisplayName("Should find users by user filter using parallel stream")
    void findByUserFilterParallel_WithUserFilter_ReturnsFilteredUsers() {
        for (int i = 0; i < 100; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            userManager.add(user);
        }

        UserFilter filter = UserFilters.byUsernameContains("75");
        List<User> filteredUsers = reportGenerator.findByUserFilterParallel(userManager, filter);

        assertNotNull(filteredUsers);
        assertTrue(filteredUsers.size() >= 1);
        assertTrue(filteredUsers.stream().allMatch(u -> u.username().contains("75")));
    }

    @Test
    @DisplayName("Should find users by user filter parallel with null filter returns all users")
    void findByUserFilterParallel_WithNullUserFilter_ReturnsAllUsers() {
        for (int i = 0; i < 50; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            userManager.add(user);
        }

        List<User> users = reportGenerator.findByUserFilterParallel(userManager, null);

        assertNotNull(users);
        assertEquals(51, users.size());
    }

    @Test
    @DisplayName("Should handle concurrent parallel report generation")
    void testConcurrentParallelReportGeneration() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            userManager.add(user);

            Role role = new Role("ROLE_" + i, "Role " + i);
            roleManager.add(role);

            AssignmentMetadata am = AssignmentMetadata.now("admin", "Test");
            RoleAssignment assignment = new PermanentAssignment(user, role, am);
            assignmentManager.add(assignment);
        }

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    String userReport = reportGenerator.generateUserReportParallel(userManager, assignmentManager);
                    String matrixReport = reportGenerator.generatePermissionMatrixParallel(userManager, assignmentManager);

                    assertNotNull(userReport);
                    assertNotNull(matrixReport);
                    assertTrue(userReport.contains("Users report (Parallel)"));
                    assertTrue(matrixReport.contains("Permission Matrix (Parallel)"));

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Exception in parallel report generation: " + e.getMessage());
                }
                latch.countDown();
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(threadCount, successCount.get());
    }

    @Test
    @DisplayName("Should handle empty user list in parallel reports")
    void testParallelReportsWithEmptyUserList() {
        userManager.clear();

        String userReport = reportGenerator.generateUserReportParallel(userManager, assignmentManager);
        String matrixReport = reportGenerator.generatePermissionMatrixParallel(userManager, assignmentManager);

        assertNotNull(userReport);
        assertNotNull(matrixReport);
        assertTrue(userReport.contains("Total users: 0"));
        assertTrue(matrixReport.contains("Permission Matrix (Parallel)"));
    }

    @Test
    @DisplayName("Should handle findByFilterParallel with null UserManager")
    void testFindByFilterParallelWithNullUserManager() {
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.findByFilterParallel(null, user -> true));
    }

    @Test
    @DisplayName("Should handle findByUserFilterParallel with null UserManager")
    void testFindByUserFilterParallelWithNullUserManager() {
        assertThrows(IllegalArgumentException.class,
                () -> reportGenerator.findByUserFilterParallel(null, UserFilters.byUsernameContains("test")));
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