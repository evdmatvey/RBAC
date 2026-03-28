import utils.*;
import commands.RBACSystem;
import entities.*;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ScheduledExpiredAssignmentsCleanerTest {

    private RBACSystem rbacSystem;
    private ScheduledExpiredAssignmentsCleaner cleaner;

    @BeforeEach
    void setUp() {
        rbacSystem = new RBACSystem();
        rbacSystem.initialize();
        rbacSystem.setCurrentUser("tester");

        cleaner = new ScheduledExpiredAssignmentsCleaner(
                rbacSystem,
                rbacSystem.getBackgroundExecutor(),
                1
        );
    }

    @AfterEach
    void tearDown() {
        rbacSystem.shutdown();
    }

    private TemporaryAssignment createExpiredAssignment(String username) {
        User user = User.create(username, "Test User", username + "@test.com");
        rbacSystem.getUserManager().add(user);

        Role role = new Role("test_role", "Test Role");
        rbacSystem.getRoleManager().add(role);

        AssignmentMetadata metadata = AssignmentMetadata.now("tester", "test");
        TemporaryAssignment assignment = new TemporaryAssignment(user, role, metadata);
        String expiredDate = LocalDate.now().minusDays(1).toString();
        assignment.extend(expiredDate);

        return assignment;
    }

    @Test
    void testCleanerIgnoresActiveAssignments() throws Exception {
        User user = User.create("active_user", "Active User", "active@test.com");
        rbacSystem.getUserManager().add(user);

        Role role = new Role("active_role", "Active Role");
        rbacSystem.getRoleManager().add(role);

        AssignmentMetadata metadata = AssignmentMetadata.now("tester", "test");
        TemporaryAssignment activeAssignment = new TemporaryAssignment(user, role, metadata);
        String futureDate = LocalDate.now().plusDays(7).toString();
        activeAssignment.extend(futureDate);
        rbacSystem.getAssignmentManager().add(activeAssignment);

        int activeBefore = rbacSystem.getAssignmentManager().getActiveAssignments().size();

        cleaner.start();

        Thread.sleep(1500);

        assertEquals(activeBefore, rbacSystem.getAssignmentManager().getActiveAssignments().size());
        assertEquals(0, cleaner.getTotalExpiredFound());
        assertEquals(0, cleaner.getTotalExpiredMarked());
    }

    @Test
    void testCleanerSkipsPermanentAssignments() throws Exception {
        User user = User.create("perm_user", "Perm User", "perm@test.com");
        rbacSystem.getUserManager().add(user);

        Role role = new Role("perm_role", "Perm Role");
        rbacSystem.getRoleManager().add(role);

        PermanentAssignment permanent = new PermanentAssignment(
                user,
                role,
                AssignmentMetadata.now("tester", "permanent")
        );
        rbacSystem.getAssignmentManager().add(permanent);

        assertEquals(0, rbacSystem.getAssignmentManager().getExpiredAssignments().size());

        cleaner.start();

        Thread.sleep(1500);

        assertEquals(0, cleaner.getTotalExpiredFound());
        assertEquals(0, cleaner.getTotalExpiredMarked());
        assertTrue(rbacSystem.getAssignmentManager().userHasRole(user, role));
    }

    @Test
    void testCleanerDoesNotCrashWhenNoAssignments() throws Exception {
        cleaner.start();

        Thread.sleep(1500);

        assertEquals(0, cleaner.getTotalExpiredFound());
        assertEquals(0, cleaner.getTotalExpiredMarked());
        assertTrue(cleaner.getTaskRuns() >= 1);
    }
}