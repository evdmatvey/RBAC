import repositories.UserManager;
import entities.User;
import filters.UserFilters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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