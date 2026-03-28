import repositories.RoleManager;
import entities.Permission;
import entities.Role;
import filters.RoleFilter;
import filters.RoleFilters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    @DisplayName("Should find all roles using parallel stream")
    void testFindAllParallel() {
        int roleCount = 100;
        for (int i = 0; i < roleCount; i++) {
            Role role = new Role("ROLE_" + i, "Role description " + i);
            roleManager.add(role);
        }

        List<Role> roles = roleManager.findAllParallel();

        assertEquals(roleCount, roles.size());
        assertTrue(roles.stream().allMatch(r -> r.getName().startsWith("ROLE_")));
    }

    @Test
    @DisplayName("Should find empty list when no roles exist in findAllParallel")
    void testFindAllParallelEmpty() {
        List<Role> roles = roleManager.findAllParallel();
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
        assertEquals(0, roles.size());
    }

    @Test
    @DisplayName("Should find roles by filter using parallel stream")
    void testFindByFilterParallel() {
        for (int i = 0; i < 50; i++) {
            Role role = new Role("ROLE_" + i, "Role description " + i);
            roleManager.add(role);
        }
        roleManager.add(new Role("ADMIN_ROLE", "Admin role"));
        roleManager.add(new Role("ADMIN_TEST", "Admin test role"));

        RoleFilter filter = RoleFilters.byNameContains("ADMIN");
        List<Role> roles = roleManager.findByFilterParallel(filter);

        assertEquals(2, roles.size());
        assertTrue(roles.stream().allMatch(r -> r.getName().contains("ADMIN")));
    }

    @Test
    @DisplayName("Should return all roles when filter is null in findByFilterParallel")
    void testFindByFilterParallelWithNullFilter() {
        int roleCount = 50;
        for (int i = 0; i < roleCount; i++) {
            Role role = new Role("ROLE_" + i, "Role description " + i);
            roleManager.add(role);
        }

        List<Role> roles = roleManager.findByFilterParallel((RoleFilter) null);

        assertEquals(roleCount, roles.size());
    }

    @Test
    @DisplayName("Should find roles by name contains using parallel stream")
    void testFindByFilterParallelWithNameContains() {
        roleManager.add(new Role("ADMIN", "Administrator role"));
        roleManager.add(new Role("SUPER_ADMIN", "Super administrator role"));
        roleManager.add(new Role("EDITOR", "Editor role"));
        roleManager.add(new Role("VIEWER", "Viewer role"));

        RoleFilter filter = RoleFilters.byNameContains("ADMIN");
        List<Role> roles = roleManager.findByFilterParallel(filter);

        assertEquals(2, roles.size());
        assertTrue(roles.stream().anyMatch(r -> r.getName().equals("ADMIN")));
        assertTrue(roles.stream().anyMatch(r -> r.getName().equals("SUPER_ADMIN")));
    }

    @Test
    @DisplayName("Should find roles with at least N permissions using parallel stream")
    void testFindByFilterParallelWithMinPermissions() {
        Permission readUsers = new Permission("READ", "users", "Read users");
        Permission writeUsers = new Permission("WRITE", "users", "Write users");
        Permission deleteUsers = new Permission("DELETE", "users", "Delete users");

        Role admin = new Role("ADMIN", "Admin role");
        Role editor = new Role("EDITOR", "Editor role");
        Role viewer = new Role("VIEWER", "Viewer role");

        roleManager.add(admin);
        roleManager.add(editor);
        roleManager.add(viewer);

        roleManager.addPermissionToRole("ADMIN", readUsers);
        roleManager.addPermissionToRole("ADMIN", writeUsers);
        roleManager.addPermissionToRole("ADMIN", deleteUsers);
        roleManager.addPermissionToRole("EDITOR", readUsers);
        roleManager.addPermissionToRole("EDITOR", writeUsers);

        RoleFilter filter = RoleFilters.hasAtLeastNPermissions(2);
        List<Role> roles = roleManager.findByFilterParallel(filter);

        assertEquals(2, roles.size());
        assertTrue(roles.stream().anyMatch(r -> r.getName().equals("ADMIN")));
        assertTrue(roles.stream().anyMatch(r -> r.getName().equals("EDITOR")));
    }

    @Test
    @DisplayName("Should handle concurrent parallel operations")
    void testConcurrentParallelOperations() throws InterruptedException {
        int roleCount = 200;
        for (int i = 0; i < roleCount; i++) {
            Role role = new Role("ROLE_" + i, "Role description " + i);
            roleManager.add(role);
        }

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    List<Role> allRoles = roleManager.findAllParallel();
                    List<Role> filteredRoles = roleManager.findByFilterParallel(
                            RoleFilters.byNameContains("1")
                    );

                    assertNotNull(allRoles);
                    assertNotNull(filteredRoles);
                    assertTrue(allRoles.size() > 0);

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Exception in parallel operation: " + e.getMessage());
                }
                latch.countDown();
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(threadCount, successCount.get());
    }

    @Test
    @DisplayName("Should maintain consistency between sequential and parallel findAll")
    void testSequentialVsParallelFindAllConsistency() {
        int roleCount = 100;
        for (int i = 0; i < roleCount; i++) {
            Role role = new Role("ROLE_" + i, "Role description " + i);
            roleManager.add(role);
        }

        List<Role> sequential = roleManager.findAll();
        List<Role> parallel = roleManager.findAllParallel();

        assertEquals(sequential.size(), parallel.size());
        assertTrue(sequential.containsAll(parallel) && parallel.containsAll(sequential));
    }

    @Test
    @DisplayName("Should maintain consistency between sequential and parallel findByFilter")
    void testSequentialVsParallelFindByFilterConsistency() {
        for (int i = 0; i < 50; i++) {
            Role role = new Role("TEST_ROLE_" + i, "Test role " + i);
            roleManager.add(role);
        }
        for (int i = 0; i < 50; i++) {
            Role role = new Role("ADMIN_ROLE_" + i, "Admin role " + i);
            roleManager.add(role);
        }

        RoleFilter filter = RoleFilters.byNameContains("ADMIN");

        List<Role> sequential = roleManager.findByFilter(filter);
        List<Role> parallel = roleManager.findByFilterParallel(filter);

        assertEquals(sequential.size(), parallel.size());
        assertTrue(sequential.containsAll(parallel) && parallel.containsAll(sequential));
    }

    @Test
    @DisplayName("Should handle multiple parallel findByFilter calls simultaneously")
    void testMultipleParallelFilters() throws InterruptedException {
        int roleCount = 500;
        for (int i = 0; i < roleCount; i++) {
            Role role = new Role("ROLE_" + i, "Role description " + i);
            roleManager.add(role);
        }

        Permission readPerm = new Permission("READ", "users", "Read users");
        Permission writePerm = new Permission("WRITE", "users", "Write users");

        roleManager.addPermissionToRole("ROLE_100", readPerm);
        roleManager.addPermissionToRole("ROLE_200", readPerm);
        roleManager.addPermissionToRole("ROLE_300", writePerm);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger completedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int filterId = i;
            executor.submit(() -> {
                try {
                    RoleFilter filter;
                    if (filterId % 3 == 0) {
                        filter = RoleFilters.byNameContains("100");
                    } else if (filterId % 3 == 1) {
                        filter = RoleFilters.hasPermission("READ", "users");
                    } else {
                        filter = RoleFilters.hasAtLeastNPermissions(1);
                    }

                    List<Role> result = roleManager.findByFilterParallel(filter);
                    assertNotNull(result);
                    completedCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Exception in parallel filter: " + e.getMessage());
                }
                latch.countDown();
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(threadCount, completedCount.get());
    }

    @Test
    @DisplayName("Should handle concurrent additions and parallel reads")
    void testConcurrentAdditionsAndParallelReads() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger readSuccessCount = new AtomicInteger(0);
        AtomicInteger writeSuccessCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < 50; j++) {
                    if (threadId % 2 == 0) {
                        try {
                            String roleName = "TEMP_ROLE_" + threadId + "_" + j;
                            Role role = new Role(roleName, "Temporary role");
                            roleManager.add(role);
                            writeSuccessCount.incrementAndGet();
                        } catch (IllegalArgumentException e) {
                        }
                    } else {
                        try {
                            List<Role> allRoles = roleManager.findAllParallel();
                            List<Role> filteredRoles = roleManager.findByFilterParallel(
                                    RoleFilters.byNameContains("ROLE")
                            );
                            assertNotNull(allRoles);
                            assertNotNull(filteredRoles);
                            readSuccessCount.incrementAndGet();
                        } catch (Exception e) {
                            fail("Exception in parallel read: " + e.getMessage());
                        }
                    }
                }
                latch.countDown();
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        assertTrue(readSuccessCount.get() > 0);
        assertTrue(writeSuccessCount.get() > 0);
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