import repositories.UserManager;
import entities.User;
import filters.UserFilter;
import filters.UserFilters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserManager Tests")
class UserManagerTest {

    private UserManager userManager;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
    }

    @Test
    @DisplayName("Should add user successfully")
    void testAddUser() {
        User user = User.create("elvis_presley", "elvis presley", "elvis@example.com");

        userManager.add(user);

        assertEquals(1, userManager.count());
        assertTrue(userManager.findByUsername("elvis_presley").isPresent());
        assertEquals(user, userManager.findByUsername("elvis_presley").get());
    }

    @Test
    @DisplayName("Should throw exception when adding duplicate username")
    void testAddDuplicateUsername() {
        User user1 = User.create("elvis_presley", "elvis presley", "elvis@example.com");
        User user2 = User.create("elvis_presley", "elvis Smith", "elvis2@example.com");

        userManager.add(user1);

        assertThrows(IllegalArgumentException.class, () -> userManager.add(user2));
        assertEquals(1, userManager.count());
    }

    @Test
    @DisplayName("Should throw exception when adding duplicate email")
    void testAddDuplicateEmail() {
        User user1 = User.create("elvis_presley", "elvis presley", "elvis@example.com");
        User user2 = User.create("jane_presley", "Jane presley", "elvis@example.com");

        userManager.add(user1);

        assertThrows(IllegalArgumentException.class, () -> userManager.add(user2));
        assertEquals(1, userManager.count());
    }

    @Test
    @DisplayName("Should remove user successfully")
    void testRemoveUser() {
        User user = User.create("elvis_presley", "elvis presley", "elvis@example.com");
        userManager.add(user);

        boolean removed = userManager.remove(user);

        assertTrue(removed);
        assertEquals(0, userManager.count());
        assertFalse(userManager.findByUsername("elvis_presley").isPresent());
    }

    @Test
    @DisplayName("Should return false when removing non-existent user")
    void testRemoveNonExistentUser() {
        User user = User.create("elvis_presley", "elvis presley", "elvis@example.com");

        boolean removed = userManager.remove(user);

        assertFalse(removed);
        assertEquals(0, userManager.count());
    }

    @Test
    @DisplayName("Should find user by username")
    void testFindByUsername() {
        User user = User.create("elvis_presley", "elvis presley", "elvis@example.com");
        userManager.add(user);

        Optional<User> found = userManager.findByUsername("elvis_presley");

        assertTrue(found.isPresent());
        assertEquals(user, found.get());
    }

    @Test
    @DisplayName("Should find user by email")
    void testFindByEmail() {
        User user = User.create("elvis_presley", "elvis presley", "elvis@example.com");
        userManager.add(user);

        Optional<User> found = userManager.findByEmail("elvis@example.com");

        assertTrue(found.isPresent());
        assertEquals(user, found.get());
    }

    @Test
    @DisplayName("Should update user")
    void testUpdateUser() {
        User user = User.create("elvis_presley", "elvis presley", "elvis@example.com");
        userManager.add(user);

        userManager.update("elvis_presley", "elvis Updated", "elvis.updated@example.com");

        Optional<User> updated = userManager.findByUsername("elvis_presley");
        assertTrue(updated.isPresent());
        assertEquals("elvis Updated", updated.get().fullName());
        assertEquals("elvis.updated@example.com", updated.get().email());
    }

    @Test
    @DisplayName("Should find users by filter")
    void testFindByFilter() {
        userManager.add(User.create("elvis_presley", "elvis presley", "elvis@example.com"));
        userManager.add(User.create("jane_presley", "Jane presley", "jane@example.com"));
        userManager.add(User.create("bob_smith", "Bob Smith", "bob@example.com"));

        List<User> users = userManager.findByFilter(UserFilters.byUsernameContains("presley"));

        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.username().equals("elvis_presley")));
        assertTrue(users.stream().anyMatch(u -> u.username().equals("jane_presley")));
    }

    @Test
    @DisplayName("Should find all users using parallel stream")
    void testFindAllParallel() {
        int userCount = 100;
        for (int i = 0; i < userCount; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            userManager.add(user);
        }

        List<User> users = userManager.findAllParallel();

        assertEquals(userCount, users.size());
        assertTrue(users.stream().allMatch(u -> u.username().startsWith("user_")));
    }

    @Test
    @DisplayName("Should find empty list when no users exist in findAllParallel")
    void testFindAllParallelEmpty() {
        List<User> users = userManager.findAllParallel();
        assertNotNull(users);
        assertTrue(users.isEmpty());
        assertEquals(0, users.size());
    }

    @Test
    @DisplayName("Should find users by filter using parallel stream")
    void testFindByFilterParallel() {
        for (int i = 0; i < 50; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            userManager.add(user);
        }
        userManager.add(User.create("admin_user", "Admin User", "admin@example.com"));
        userManager.add(User.create("admin_test", "Admin Test", "admin_test@example.com"));

        UserFilter filter = UserFilters.byUsernameContains("admin");
        List<User> users = userManager.findByFilterParallel(filter);

        assertEquals(2, users.size());
        assertTrue(users.stream().allMatch(u -> u.username().contains("admin")));
    }

    @Test
    @DisplayName("Should return all users when filter is null in findByFilterParallel")
    void testFindByFilterParallelWithNullFilter() {
        int userCount = 50;
        for (int i = 0; i < userCount; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            userManager.add(user);
        }

        List<User> users = userManager.findByFilterParallel((UserFilter) null);

        assertEquals(userCount, users.size());
    }

    @Test
    @DisplayName("Should find users by email domain using parallel stream")
    void testFindByFilterParallelWithEmailDomain() {
        for (int i = 0; i < 30; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            userManager.add(user);
        }
        for (int i = 0; i < 20; i++) {
            User user = User.create("gmail_user_" + i, "Gmail User " + i, "user" + i + "@gmail.com");
            userManager.add(user);
        }

        UserFilter filter = UserFilters.byEmailDomain("@gmail.com");
        List<User> users = userManager.findByFilterParallel(filter);

        assertEquals(20, users.size());
        assertTrue(users.stream().allMatch(u -> u.email().endsWith("@gmail.com")));
    }

    @Test
    @DisplayName("Should find users by full name contains using parallel stream")
    void testFindByFilterParallelWithFullName() {
        userManager.add(User.create("john_doe", "John Doe", "john@example.com"));
        userManager.add(User.create("jane_doe", "Jane Doe", "jane@example.com"));
        userManager.add(User.create("bob_smith", "Bob Smith", "bob@example.com"));

        UserFilter filter = UserFilters.byFullNameContains("Doe");
        List<User> users = userManager.findByFilterParallel(filter);

        assertEquals(2, users.size());
        assertTrue(users.stream().allMatch(u -> u.fullName().contains("Doe")));
    }

    @Test
    @DisplayName("Should handle concurrent parallel operations")
    void testConcurrentParallelOperations() throws InterruptedException {
        int userCount = 200;
        for (int i = 0; i < userCount; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            userManager.add(user);
        }

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    List<User> allUsers = userManager.findAllParallel();
                    List<User> filteredUsers = userManager.findByFilterParallel(
                            UserFilters.byUsernameContains("1")
                    );

                    assertNotNull(allUsers);
                    assertNotNull(filteredUsers);
                    assertTrue(allUsers.size() > 0);

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
        int userCount = 100;
        for (int i = 0; i < userCount; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            userManager.add(user);
        }

        List<User> sequential = userManager.findAll();
        List<User> parallel = userManager.findAllParallel();

        assertEquals(sequential.size(), parallel.size());
        assertTrue(sequential.containsAll(parallel) && parallel.containsAll(sequential));
    }

    @Test
    @DisplayName("Should maintain consistency between sequential and parallel findByFilter")
    void testSequentialVsParallelFindByFilterConsistency() {
        for (int i = 0; i < 50; i++) {
            User user = User.create("test_user_" + i, "Test User " + i, "test" + i + "@example.com");
            userManager.add(user);
        }
        for (int i = 0; i < 50; i++) {
            User user = User.create("admin_user_" + i, "Admin User " + i, "admin" + i + "@example.com");
            userManager.add(user);
        }

        UserFilter filter = UserFilters.byUsernameContains("admin");

        List<User> sequential = userManager.findByFilter(filter);
        List<User> parallel = userManager.findByFilterParallel(filter);

        assertEquals(sequential.size(), parallel.size());
        assertTrue(sequential.containsAll(parallel) && parallel.containsAll(sequential));
    }

    @Test
    @DisplayName("Should handle multiple parallel findByFilter calls simultaneously")
    void testMultipleParallelFilters() throws InterruptedException {
        int userCount = 500;
        for (int i = 0; i < userCount; i++) {
            User user = User.create("user_" + i, "User " + i, "user" + i + "@example.com");
            userManager.add(user);
        }

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger completedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int filterId = i;
            executor.submit(() -> {
                try {
                    UserFilter filter;
                    if (filterId % 3 == 0) {
                        filter = UserFilters.byUsernameContains("10");
                    } else if (filterId % 3 == 1) {
                        filter = UserFilters.byEmailDomain("@example.com");
                    } else {
                        filter = UserFilters.byFullNameContains("User");
                    }

                    List<User> result = userManager.findByFilterParallel(filter);
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
                            String username = "temp_user_" + threadId + "_" + j;
                            User user = User.create(username, "Temp User", username + "@example.com");
                            userManager.add(user);
                            writeSuccessCount.incrementAndGet();
                        } catch (IllegalArgumentException e) {
                        }
                    } else {
                        try {
                            List<User> users = userManager.findAllParallel();
                            List<User> filtered = userManager.findByFilterParallel(
                                    UserFilters.byUsernameContains("user")
                            );
                            assertNotNull(users);
                            assertNotNull(filtered);
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
    @DisplayName("Should handle concurrent additions safely")
    void testConcurrentAdditions() throws InterruptedException {
        int threadCount = 10;
        int usersPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < usersPerThread; j++) {
                    String username = "user_" + threadId + "_" + j;
                    String email = username + "@example.com";
                    try {
                        User user = User.create(username, "User " + username, email);
                        userManager.add(user);
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

        assertEquals(threadCount * usersPerThread, successCount.get() + failureCount.get());
        assertEquals(threadCount * usersPerThread, userManager.count());
    }

    @Test
    @DisplayName("Should handle concurrent reads and writes safely")
    void testConcurrentReadsAndWrites() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            userManager.add(User.create("user_" + i, "User " + i, "user" + i + "@example.com"));
        }

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger readCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    if (threadId % 2 == 0) {
                        try {
                            String username = "temp_user_" + threadId + "_" + j;
                            User user = User.create(username, "Temp User", username + "@example.com");
                            userManager.add(user);
                            userManager.remove(user);
                        } catch (IllegalArgumentException e) {
                        }
                    } else {
                        List<User> users = userManager.findAll();
                        readCount.addAndGet(users.size());
                    }
                }
                latch.countDown();
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(readCount.get() > 0);
        assertEquals(100, userManager.count());
    }

    @Test
    @DisplayName("Should maintain data consistency under concurrent updates")
    void testConcurrentUpdates() throws InterruptedException {
        User user = User.create("elvis_presley", "elvis presley", "elvis@example.com");
        userManager.add(user);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int updateId = i;
            executor.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    String newName = "elvis Updated_" + updateId + "_" + j;
                    String newEmail = "elvis.updated" + updateId + "_" + j + "@example.com";
                    try {
                        userManager.update("elvis_presley", newName, newEmail);
                    } catch (Exception e) {
                    }
                }
                latch.countDown();
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Optional<User> finalUser = userManager.findByUsername("elvis_presley");
        assertTrue(finalUser.isPresent());
        assertNotNull(finalUser.get().fullName());
        assertNotNull(finalUser.get().email());
    }
}