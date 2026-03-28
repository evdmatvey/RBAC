import repositories.RoleManager;
import entities.Permission;
import entities.Role;
import filters.RoleFilters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleManager Tests")
class RoleManagerTest {

    private RoleManager roleManager;

    @BeforeEach
    void setUp() {
        roleManager = new RoleManager();
    }

    @Test
    @DisplayName("Should add role successfully")
    void testAddRole() {
        Role role = new Role("ADMIN", "Administrator role");

        roleManager.add(role);

        assertEquals(1, roleManager.count());
        assertTrue(roleManager.findByName("ADMIN").isPresent());
        assertEquals(role, roleManager.findByName("ADMIN").get());
    }

    @Test
    @DisplayName("Should throw exception when adding duplicate role name")
    void testAddDuplicateRole() {
        Role role1 = new Role("ADMIN", "Administrator role");
        Role role2 = new Role("ADMIN", "Another admin role");

        roleManager.add(role1);

        assertThrows(IllegalArgumentException.class, () -> roleManager.add(role2));
        assertEquals(1, roleManager.count());
    }

    @Test
    @DisplayName("Should remove role successfully")
    void testRemoveRole() {
        Role role = new Role("ADMIN", "Administrator role");
        roleManager.add(role);

        boolean removed = roleManager.remove(role);

        assertTrue(removed);
        assertEquals(0, roleManager.count());
        assertFalse(roleManager.findByName("ADMIN").isPresent());
    }

    @Test
    @DisplayName("Should add permission to role")
    void testAddPermissionToRole() {
        Role role = new Role("ADMIN", "Administrator role");
        roleManager.add(role);

        Permission permission = new Permission("READ", "users", "Can read users");
        roleManager.addPermissionToRole("ADMIN", permission);

        Optional<Role> found = roleManager.findByName("ADMIN");
        assertTrue(found.isPresent());
        assertTrue(found.get().hasPermission(permission));
    }

    @Test
    @DisplayName("Should remove permission from role")
    void testRemovePermissionFromRole() {
        Role role = new Role("ADMIN", "Administrator role");
        roleManager.add(role);

        Permission permission = new Permission("READ", "users", "Can read users");
        roleManager.addPermissionToRole("ADMIN", permission);
        roleManager.removePermissionFromRole("ADMIN", permission);

        Optional<Role> found = roleManager.findByName("ADMIN");
        assertTrue(found.isPresent());
        assertFalse(found.get().hasPermission(permission));
    }

    @Test
    @DisplayName("Should find roles with specific permission")
    void testFindRolesWithPermission() {
        Permission readUsers = new Permission("READ", "users", "Read users");
        Permission writeUsers = new Permission("WRITE", "users", "Write users");

        Role admin = new Role("ADMIN", "Admin role");
        Role editor = new Role("EDITOR", "Editor role");

        roleManager.add(admin);
        roleManager.add(editor);

        roleManager.addPermissionToRole("ADMIN", readUsers);
        roleManager.addPermissionToRole("ADMIN", writeUsers);
        roleManager.addPermissionToRole("EDITOR", readUsers);

        List<Role> roles = roleManager.findRolesWithPermission("READ", "users");

        assertEquals(2, roles.size());
        assertTrue(roles.stream().anyMatch(r -> r.getName().equals("ADMIN")));
        assertTrue(roles.stream().anyMatch(r -> r.getName().equals("EDITOR")));
    }

    @Test
    @DisplayName("Should find roles by filter")
    void testFindByFilter() {
        roleManager.add(new Role("ADMIN", "Admin role"));
        roleManager.add(new Role("EDITOR", "Editor role"));
        roleManager.add(new Role("VIEWER", "Viewer role"));

        List<Role> roles = roleManager.findByFilter(RoleFilters.byNameContains("AD"));

        assertEquals(1, roles.size());
        assertEquals("ADMIN", roles.get(0).getName());
    }

    @Test
    @DisplayName("Should handle concurrent role additions")
    void testConcurrentRoleAdditions() throws InterruptedException {
        int threadCount = 10;
        int rolesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < rolesPerThread; j++) {
                    String roleName = "ROLE_" + threadId + "_" + j;
                    try {
                        Role role = new Role(roleName, "Test role " + roleName);
                        roleManager.add(role);
                        successCount.incrementAndGet();
                    } catch (IllegalArgumentException e) {
                        failureCount.incrementAndGet();
                    }
                }
                latch.countDown();
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threadCount * rolesPerThread, successCount.get() + failureCount.get());
        assertEquals(threadCount * rolesPerThread, roleManager.count());
    }

    @Test
    @DisplayName("Should handle concurrent permission additions")
    void testConcurrentPermissionAdditions() throws InterruptedException {
        Role role = new Role("ADMIN", "Admin role");
        roleManager.add(role);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < 50; j++) {
                    Permission permission = new Permission(
                            "PERM_" + threadId + "_" + j,
                            "resource",
                            "Test permission"
                    );
                    roleManager.addPermissionToRole("ADMIN", permission);
                    successCount.incrementAndGet();
                }
                latch.countDown();
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Optional<Role> updatedRole = roleManager.findByName("ADMIN");
        assertTrue(updatedRole.isPresent());
        assertEquals(threadCount * 50, updatedRole.get().getPermissions().size());
    }

    @Test
    @DisplayName("Should maintain consistency between id and name maps")
    void testMapConsistency() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    String roleName = "ROLE_" + threadId + "_" + j;
                    Role role = new Role(roleName, "Test role");
                    roleManager.add(role);

                    Optional<Role> byName = roleManager.findByName(roleName);
                    Optional<Role> byId = roleManager.findById(role.getId());

                    assertTrue(byName.isPresent());
                    assertTrue(byId.isPresent());
                    assertEquals(byName.get(), byId.get());
                }
                latch.countDown();
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threadCount * 100, roleManager.count());
    }
}