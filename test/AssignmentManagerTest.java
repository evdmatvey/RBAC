import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.*;

class AssignmentManagerTest {
    private AssignmentManager assignmentManager;
    private UserManager userManager;
    private RoleManager roleManager;
    private User user;
    private AssignmentMetadata metadata;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);

        user = User.create("john", "John Doe", "john@email.com");
        userManager.add(user);

        metadata = new AssignmentMetadata("admin", LocalDateTime.now().toString(), "Test assignment");
    }

    @Test
    void addAssignment() {
        Role role = new Role("admin", "Administrator");
        roleManager.add(role);
        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);

        assignmentManager.add(assignment);
        assertEquals(1, assignmentManager.count());
        assertTrue(assignmentManager.findById(assignment.assignmentId()).isPresent());
    }

    @Test
    void addDuplicateAssignment() {
        Role role = new Role("admin", "Administrator");
        roleManager.add(role);
        PermanentAssignment assignment1 = new PermanentAssignment(user, role, metadata);
        PermanentAssignment assignment2 = new PermanentAssignment(user, role, metadata);

        assignmentManager.add(assignment1);
        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(assignment2));
    }

    @Test
    void addAssignmentWithNonExistentUser() {
        Role role = new Role("admin", "Administrator");
        roleManager.add(role);
        User fakeUser = User.create("fake", "Fake User", "fake@email.com");
        PermanentAssignment assignment = new PermanentAssignment(fakeUser, role, metadata);

        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(assignment));
    }

    @Test
    void addAssignmentWithNonExistentRole() {
        Role fakeRole = new Role("fake", "Fake Role");
        PermanentAssignment assignment = new PermanentAssignment(user, fakeRole, metadata);

        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(assignment));
    }

    @Test
    void removeAssignment() {
        Role role = new Role("admin", "Administrator");
        roleManager.add(role);
        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);

        assignmentManager.add(assignment);
        assertTrue(assignmentManager.remove(assignment));
        assertEquals(0, assignmentManager.count());
    }

    @Test
    void findById() {
        Role role = new Role("admin", "Administrator");
        roleManager.add(role);
        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);

        assignmentManager.add(assignment);
        Optional<RoleAssignment> found = assignmentManager.findById(assignment.assignmentId());
        assertTrue(found.isPresent());
        assertEquals(assignment.assignmentId(), found.get().assignmentId());
    }

    @Test
    void findByUser() {
        Role role1 = new Role("admin", "Administrator");
        Role role2 = new Role("user", "User");
        roleManager.add(role1);
        roleManager.add(role2);

        PermanentAssignment assignment1 = new PermanentAssignment(user, role1, metadata);
        TemporaryAssignment assignment2 = new TemporaryAssignment(user, role2, metadata);

        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);

        List<RoleAssignment> result = assignmentManager.findByUser(user);
        assertEquals(2, result.size());
    }

    @Test
    void findByRole() {
        Role role = new Role("admin", "Administrator");
        roleManager.add(role);
        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);

        assignmentManager.add(assignment);
        List<RoleAssignment> result = assignmentManager.findByRole(role);
        assertEquals(1, result.size());
    }

    @Test
    void findAll() {
        Role role1 = new Role("admin", "Administrator");
        Role role2 = new Role("user", "User");
        roleManager.add(role1);
        roleManager.add(role2);

        PermanentAssignment assignment1 = new PermanentAssignment(user, role1, metadata);
        TemporaryAssignment assignment2 = new TemporaryAssignment(user, role2, metadata);

        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);

        assertEquals(2, assignmentManager.findAll().size());
    }

    @Test
    void count() {
        assertEquals(0, assignmentManager.count());

        Role role = new Role("admin", "Administrator");
        roleManager.add(role);
        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);

        assignmentManager.add(assignment);
        assertEquals(1, assignmentManager.count());
    }

    @Test
    void clear() {
        Role role1 = new Role("admin", "Administrator");
        Role role2 = new Role("user", "User");
        roleManager.add(role1);
        roleManager.add(role2);

        PermanentAssignment assignment1 = new PermanentAssignment(user, role1, metadata);
        TemporaryAssignment assignment2 = new TemporaryAssignment(user, role2, metadata);

        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);
        assignmentManager.clear();
        assertEquals(0, assignmentManager.count());
    }

    @Test
    void findByFilter() {
        Role role1 = new Role("admin", "Administrator");
        Role role2 = new Role("user", "User");
        roleManager.add(role1);
        roleManager.add(role2);

        PermanentAssignment assignment1 = new PermanentAssignment(user, role1, metadata);
        TemporaryAssignment assignment2 = new TemporaryAssignment(user, role2, metadata);

        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);

        AssignmentFilter filter = AssignmentFilters.byType("PERMANENT");
        List<RoleAssignment> result = assignmentManager.findByFilter(filter);

        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof PermanentAssignment);
    }

    @Test
    void getActiveAssignments() {
        Role role1 = new Role("admin", "Administrator");
        Role role2 = new Role("user", "User");
        roleManager.add(role1);
        roleManager.add(role2);

        PermanentAssignment assignment1 = new PermanentAssignment(user, role1, metadata);

        String futureDate = LocalDateTime.now().plusDays(30).toString();
        AssignmentMetadata metadataWithExpiry = new AssignmentMetadata(
                "admin",
                LocalDateTime.now().toString(),
                "Test assignment"
        );
        TemporaryAssignment assignment2 = new TemporaryAssignment(user, role2, metadataWithExpiry);
        assignment2.extend(futureDate);

        assignmentManager.add(assignment1);
        assignmentManager.add(assignment2);

        List<RoleAssignment> active = assignmentManager.getActiveAssignments();
        assertEquals(2, active.size());
    }

    @Test
    void userHasRole() {
        Role role = new Role("admin", "Administrator");
        roleManager.add(role);
        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);

        assignmentManager.add(assignment);
        assertTrue(assignmentManager.userHasRole(user, role));
    }

    @Test
    void userHasPermission() {
        Role role = new Role("admin", "Administrator");
        Permission permission = new Permission("read", "data", "can read");
        role.addPermission(permission);
        roleManager.add(role);

        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);
        assignmentManager.add(assignment);

        assertTrue(assignmentManager.userHasPermission(user, "read", "data"));
        assertFalse(assignmentManager.userHasPermission(user, "write", "data"));
    }

    @Test
    void getUserPermissions() {
        Role role = new Role("admin", "Administrator");
        Permission permission = new Permission("read", "data", "can read");
        role.addPermission(permission);
        roleManager.add(role);

        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);
        assignmentManager.add(assignment);

        Set<Permission> permissions = assignmentManager.getUserPermissions(user);
        assertEquals(1, permissions.size());
        assertTrue(permissions.contains(permission));
    }

    @Test
    void revokePermanentAssignment() {
        Role role = new Role("admin", "Administrator");
        roleManager.add(role);
        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);

        assignmentManager.add(assignment);
        assignmentManager.revokeAssignment(assignment.assignmentId());
        assertFalse(assignmentManager.userHasRole(user, role));
    }

    @Test
    void revokeTemporaryAssignment() {
        Role role = new Role("admin", "Administrator");
        roleManager.add(role);
        TemporaryAssignment assignment = new TemporaryAssignment(user, role, metadata);

        assignmentManager.add(assignment);
        assignmentManager.revokeAssignment(assignment.assignmentId());
        assertTrue(assignmentManager.findById(assignment.assignmentId()).isEmpty());
    }

    @Test
    void revokeNonExistentAssignment() {
        assertThrows(NoSuchElementException.class,
                () -> assignmentManager.revokeAssignment("nonexistent"));
    }

    @Test
    void extendTemporaryAssignment() {
        Role role = new Role("admin", "Administrator");
        roleManager.add(role);
        TemporaryAssignment assignment = new TemporaryAssignment(user, role, metadata);

        assignmentManager.add(assignment);
        String newDate = LocalDateTime.now().plusDays(30).toString();

        assertDoesNotThrow(() ->
                assignmentManager.extendTemporaryAssignment(assignment.assignmentId(), newDate));
    }

    @Test
    void extendPermanentAssignment() {
        Role role = new Role("admin", "Administrator");
        roleManager.add(role);
        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);

        assignmentManager.add(assignment);
        String newDate = LocalDateTime.now().plusDays(30).toString();

        assertThrows(IllegalArgumentException.class,
                () -> assignmentManager.extendTemporaryAssignment(assignment.assignmentId(), newDate));
    }
}