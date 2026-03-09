import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class RoleManagerTest {
    private RoleManager roleManager;
    private Role adminRole;
    private Role userRole;
    private Permission readPermission;
    private Permission writePermission;

    @BeforeEach
    void setUp() {
        roleManager = new RoleManager();
        adminRole = new Role("admin", "Administrator");
        userRole = new Role("user", "Regular User");
        readPermission = new Permission("read", "data", "can read");
        writePermission = new Permission("write", "data", "can write");
    }

    @Test
    void addRole() {
        roleManager.add(adminRole);
        assertEquals(1, roleManager.count());
        assertTrue(roleManager.findByName("admin").isPresent());
    }

    @Test
    void addDuplicateRole() {
        roleManager.add(adminRole);
        Role duplicate = new Role("admin", "Another Admin");
        assertThrows(IllegalArgumentException.class, () -> roleManager.add(duplicate));
    }

    @Test
    void addNullRole() {
        assertThrows(IllegalArgumentException.class, () -> roleManager.add(null));
    }

    @Test
    void removeRole() {
        roleManager.add(adminRole);
        assertTrue(roleManager.remove(adminRole));
        assertEquals(0, roleManager.count());
        assertTrue(roleManager.findByName("admin").isEmpty());
    }

    @Test
    void removeNonExistentRole() {
        assertFalse(roleManager.remove(adminRole));
    }

    @Test
    void removeNullRole() {
        assertFalse(roleManager.remove(null));
    }

    @Test
    void findById() {
        roleManager.add(adminRole);
        Optional<Role> found = roleManager.findById(adminRole.getId());
        assertTrue(found.isPresent());
        assertEquals("admin", found.get().getName());
    }

    @Test
    void findByIdNull() {
        assertTrue(roleManager.findById(null).isEmpty());
    }

    @Test
    void findByName() {
        roleManager.add(adminRole);
        Optional<Role> found = roleManager.findByName("admin");
        assertTrue(found.isPresent());
        assertEquals(adminRole.getId(), found.get().getId());
    }

    @Test
    void findByNameNull() {
        assertTrue(roleManager.findByName(null).isEmpty());
    }

    @Test
    void findByNameNotFound() {
        assertTrue(roleManager.findByName("nonexistent").isEmpty());
    }

    @Test
    void findAll() {
        roleManager.add(adminRole);
        roleManager.add(userRole);
        List<Role> all = roleManager.findAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(adminRole));
        assertTrue(all.contains(userRole));
    }

    @Test
    void count() {
        assertEquals(0, roleManager.count());
        roleManager.add(adminRole);
        assertEquals(1, roleManager.count());
        roleManager.add(userRole);
        assertEquals(2, roleManager.count());
    }

    @Test
    void clear() {
        roleManager.add(adminRole);
        roleManager.add(userRole);
        roleManager.clear();
        assertEquals(0, roleManager.count());
        assertTrue(roleManager.findByName("admin").isEmpty());
    }

    @Test
    void exists() {
        roleManager.add(adminRole);
        assertTrue(roleManager.exists("admin"));
        assertFalse(roleManager.exists("user"));
    }

    @Test
    void existsNull() {
        assertFalse(roleManager.exists(null));
    }

    @Test
    void addPermissionToRole() {
        roleManager.add(adminRole);
        roleManager.addPermissionToRole("admin", readPermission);

        Optional<Role> found = roleManager.findByName("admin");
        assertTrue(found.isPresent());
        assertTrue(found.get().hasPermission(readPermission));
    }

    @Test
    void addPermissionToNonExistentRole() {
        assertThrows(NoSuchElementException.class,
                () -> roleManager.addPermissionToRole("nonexistent", readPermission));
    }

    @Test
    void addPermissionWithNullRoleName() {
        assertThrows(IllegalArgumentException.class,
                () -> roleManager.addPermissionToRole(null, readPermission));
    }

    @Test
    void addNullPermissionToRole() {
        roleManager.add(adminRole);
        assertThrows(IllegalArgumentException.class,
                () -> roleManager.addPermissionToRole("admin", null));
    }

    @Test
    void removePermissionFromRole() {
        roleManager.add(adminRole);
        roleManager.addPermissionToRole("admin", readPermission);
        roleManager.removePermissionFromRole("admin", readPermission);

        Optional<Role> found = roleManager.findByName("admin");
        assertTrue(found.isPresent());
        assertFalse(found.get().hasPermission(readPermission));
    }

    @Test
    void removePermissionFromNonExistentRole() {
        assertThrows(NoSuchElementException.class,
                () -> roleManager.removePermissionFromRole("nonexistent", readPermission));
    }

    @Test
    void findByFilter() {
        roleManager.add(adminRole);
        roleManager.add(userRole);

        adminRole.addPermission(readPermission);

        RoleFilter filter = RoleFilters.hasPermission(readPermission);
        List<Role> result = roleManager.findByFilter(filter);

        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getName());
    }

    @Test
    void findByFilterNull() {
        roleManager.add(adminRole);
        List<Role> result = roleManager.findByFilter(null);
        assertEquals(1, result.size());
    }

    @Test
    void findRolesWithPermission() {
        roleManager.add(adminRole);
        roleManager.add(userRole);

        adminRole.addPermission(readPermission);
        userRole.addPermission(writePermission);

        List<Role> result = roleManager.findRolesWithPermission("read", "data");

        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getName());
    }

    @Test
    void findRolesWithPermissionNullParams() {
        roleManager.add(adminRole);

        List<Role> result1 = roleManager.findRolesWithPermission(null, "data");
        List<Role> result2 = roleManager.findRolesWithPermission("read", null);

        assertTrue(result1.isEmpty());
        assertTrue(result2.isEmpty());
    }

    @Test
    void findAllWithFilterAndSorter() {
        roleManager.add(adminRole);
        roleManager.add(userRole);

        RoleFilter filter = RoleFilters.byNameContains("a");
        Comparator<Role> sorter = RoleSorters.byName();

        List<Role> result = roleManager.findAll(filter, sorter);

        assertEquals(1, result.size());
        assertTrue(result.contains(adminRole));
    }

    @Test
    void equalsAndHashCode() {
        roleManager.add(adminRole);

        RoleManager otherManager = new RoleManager();
        otherManager.add(adminRole);

        assertEquals(roleManager, otherManager);
        assertEquals(roleManager.hashCode(), otherManager.hashCode());
    }
}