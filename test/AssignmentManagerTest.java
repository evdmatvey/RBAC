import repositories.*;
import entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentManager Tests")
class AssignmentManagerTest {

    @Mock
    private UserManager userManager;

    @Mock
    private RoleManager roleManager;

    private AssignmentManager assignmentManager;
    private User testUser;
    private Role testRole;
    private AssignmentMetadata metadata;

    @BeforeEach
    void setUp() {
        assignmentManager = new AssignmentManager(userManager, roleManager);
        testUser = User.create("testuser", "Test User", "test@example.com");
        testRole = new Role("TEST_ROLE", "Test role");
        metadata = AssignmentMetadata.now("admin", "Testing assignment");

        lenient().when(userManager.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        lenient().when(roleManager.findByName("TEST_ROLE")).thenReturn(Optional.of(testRole));
    }

    @Test
    @DisplayName("Should add permanent assignment successfully")
    void testAddPermanentAssignment() {
        PermanentAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);

        assignmentManager.add(assignment);

        assertEquals(1, assignmentManager.count());
        assertTrue(assignmentManager.findById(assignment.assignmentId()).isPresent());
        assertTrue(assignmentManager.userHasRole(testUser, testRole));
    }

    @Test
    @DisplayName("Should add temporary assignment successfully")
    void testAddTemporaryAssignment() {
        TemporaryAssignment assignment = new TemporaryAssignment(testUser, testRole, metadata);
        assignmentManager.add(assignment);

        assertEquals(1, assignmentManager.count());
        assertTrue(assignmentManager.findById(assignment.assignmentId()).isPresent());
    }

    @Test
    @DisplayName("Should throw exception when user doesn't exist")
    void testAddAssignmentWithNonExistentUser() {
        when(userManager.findByUsername("testuser")).thenReturn(Optional.empty());

        PermanentAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);

        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(assignment));
        assertEquals(0, assignmentManager.count());
    }

    @Test
    @DisplayName("Should throw exception when role doesn't exist")
    void testAddAssignmentWithNonExistentRole() {
        when(roleManager.findByName("TEST_ROLE")).thenReturn(Optional.empty());

        PermanentAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);

        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(assignment));
        assertEquals(0, assignmentManager.count());
    }

    @Test
    @DisplayName("Should throw exception when adding duplicate active assignment")
    void testAddDuplicateActiveAssignment() {
        PermanentAssignment assignment1 = new PermanentAssignment(testUser, testRole, metadata);
        PermanentAssignment assignment2 = new PermanentAssignment(testUser, testRole, metadata);

        assignmentManager.add(assignment1);

        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(assignment2));
        assertEquals(1, assignmentManager.count());
    }

    @Test
    @DisplayName("Should remove assignment successfully")
    void testRemoveAssignment() {
        PermanentAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);
        assignmentManager.add(assignment);

        boolean removed = assignmentManager.remove(assignment);

        assertTrue(removed);
        assertEquals(0, assignmentManager.count());
        assertFalse(assignmentManager.userHasRole(testUser, testRole));
    }

    @Test
    @DisplayName("Should revoke permanent assignment")
    void testRevokePermanentAssignment() {
        PermanentAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);
        assignmentManager.add(assignment);

        assignmentManager.revokeAssignment(assignment.assignmentId());

        assertFalse(assignmentManager.userHasRole(testUser, testRole));
        assertTrue(assignmentManager.findById(assignment.assignmentId()).isPresent());
        assertFalse(assignmentManager.findById(assignment.assignmentId()).get().isActive());
    }

    @Test
    @DisplayName("Should find assignments by user")
    void testFindByUser() {
        Role role2 = new Role("ROLE_2", "Second role");
        when(roleManager.findByName("ROLE_2")).thenReturn(Optional.of(role2));

        PermanentAssignment assignment1 = new PermanentAssignment(testUser, testRole, metadata);
        PermanentAssignment assignment2 = new PermanentAssignment(testUser, role2, metadata);

        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);

        List<RoleAssignment> userAssignments = assignmentManager.findByUser(testUser);

        assertEquals(2, userAssignments.size());
    }

    @Test
    @DisplayName("Should get user permissions")
    void testGetUserPermissions() {
        Permission readPermission = new Permission("READ", "users", "Read users");
        Permission writePermission = new Permission("WRITE", "users", "Write users");

        testRole.addPermission(readPermission);
        testRole.addPermission(writePermission);

        PermanentAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);
        assignmentManager.add(assignment);

        Set<Permission> permissions = assignmentManager.getUserPermissions(testUser);

        assertEquals(2, permissions.size());
        assertTrue(permissions.contains(readPermission));
        assertTrue(permissions.contains(writePermission));
    }

    @Test
    @DisplayName("Should check user has permission")
    void testUserHasPermission() {
        Permission readPermission = new Permission("READ", "users", "Read users");
        testRole.addPermission(readPermission);

        PermanentAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);
        assignmentManager.add(assignment);

        assertTrue(assignmentManager.userHasPermission(testUser, "READ", "users"));
        assertFalse(assignmentManager.userHasPermission(testUser, "WRITE", "users"));
    }

    @Test
    @DisplayName("Should handle concurrent assignments")
    void testConcurrentAssignments() throws InterruptedException {
        int threadCount = 20;
        int usersCount = 10;
        int rolesCount = 10;

        List<User> users = new ArrayList<>();
        List<Role> roles = new ArrayList<>();

        for (int i = 0; i < usersCount; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            users.add(user);
            lenient().when(userManager.findByUsername("user_" + i)).thenReturn(Optional.of(user));
        }

        for (int i = 0; i < rolesCount; i++) {
            Role role = new Role("ROLE_" + i, "Role " + i);
            roles.add(role);
            lenient().when(roleManager.findByName("ROLE_" + i)).thenReturn(Optional.of(role));
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < 50; j++) {
                    User user = users.get(threadId % users.size());
                    Role role = roles.get(j % roles.size());

                    try {
                        PermanentAssignment assignment = new PermanentAssignment(
                                user, role, AssignmentMetadata.now("admin", "Concurrent test")
                        );
                        assignmentManager.add(assignment);
                        successCount.incrementAndGet();
                    } catch (IllegalArgumentException e) {
                        failureCount.incrementAndGet();
                    }
                }
                latch.countDown();
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test timed out");
        assertEquals(threadCount * 50, successCount.get() + failureCount.get());
        assertTrue(successCount.get() > 0, "No successful assignments");
    }

    @Test
    @DisplayName("Should handle concurrent permission checks")
    void testConcurrentPermissionChecks() throws InterruptedException {
        Permission readPermission = new Permission("READ", "users", "Read users");
        testRole.addPermission(readPermission);

        int userCount = 100;
        List<User> users = new ArrayList<>();

        for (int i = 0; i < userCount; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            users.add(user);
            lenient().when(userManager.findByUsername("user_" + i)).thenReturn(Optional.of(user));

            PermanentAssignment assignment = new PermanentAssignment(
                    user, testRole, AssignmentMetadata.now("admin", "Test")
            );
            assignmentManager.add(assignment);
        }

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger checkCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < 500; j++) {
                    User user = users.get(j % users.size());
                    boolean hasPermission = assignmentManager.userHasPermission(
                            user, "READ", "users"
                    );
                    if (hasPermission) {
                        checkCount.incrementAndGet();
                    }
                }
                latch.countDown();
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test timed out");
        assertEquals(threadCount * 500, checkCount.get(),
                "All checks should return true for users with active assignments");
    }

    @Test
    @DisplayName("Should handle concurrent revocation and checks")
    void testConcurrentRevocationAndChecks() throws InterruptedException {
        Permission readPermission = new Permission("READ", "users", "Read users");
        testRole.addPermission(readPermission);

        int assignmentCount = 50;
        List<RoleAssignment> assignments = new ArrayList<>();
        List<User> users = new ArrayList<>();

        for (int i = 0; i < assignmentCount; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            users.add(user);
            lenient().when(userManager.findByUsername("user_" + i)).thenReturn(Optional.of(user));

            PermanentAssignment assignment = new PermanentAssignment(
                    user, testRole, AssignmentMetadata.now("admin", "Test")
            );
            assignmentManager.add(assignment);
            assignments.add(assignment);
        }

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger revocationCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    if (threadId % 2 == 0) {
                        int index = (threadId * 100 + j) % assignments.size();
                        try {
                            assignmentManager.revokeAssignment(assignments.get(index).assignmentId());
                            revocationCount.incrementAndGet();
                        } catch (Exception e) {
                        }
                    } else {
                        for (int k = 0; k < 10; k++) {
                            User user = users.get(k % users.size());
                            assignmentManager.userHasPermission(user, "READ", "users");
                        }
                    }
                }
                latch.countDown();
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test timed out");
        assertTrue(revocationCount.get() > 0, "Some revocations should succeed");
    }

    @Test
    @DisplayName("Should verify userManager and roleManager are called correctly")
    void testVerifyManagerCalls() {
        PermanentAssignment assignment = new PermanentAssignment(testUser, testRole, metadata);
        assignmentManager.add(assignment);

        verify(userManager, times(1)).findByUsername("testuser");
        verify(roleManager, times(1)).findByName("TEST_ROLE");
    }
}