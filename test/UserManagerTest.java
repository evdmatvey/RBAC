import entities.User;
import filters.UserFilter;
import filters.UserFilters;
import org.junit.jupiter.api.*;
import repositories.UserManager;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class UserManagerTest {
    private UserManager userManager;
    private User user;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        user = User.create("john", "John Doe", "john@email.com");
    }

    @Test
    void addUser() {
        userManager.add(user);
        assertEquals(1, userManager.count());
        assertTrue(userManager.findByUsername("john").isPresent());
    }

    @Test
    void addDuplicateUsername() {
        userManager.add(user);
        User duplicate = User.create("john", "Johnny", "johnny@email.com");
        assertThrows(IllegalArgumentException.class, () -> userManager.add(duplicate));
    }

    @Test
    void addDuplicateEmail() {
        userManager.add(user);
        User duplicateEmail = User.create("johnny", "Johnny", "john@email.com");
        assertThrows(IllegalArgumentException.class, () -> userManager.add(duplicateEmail));
    }

    @Test
    void removeUser() {
        userManager.add(user);
        assertTrue(userManager.remove(user));
        assertEquals(0, userManager.count());
    }

    @Test
    void findById() {
        userManager.add(user);
        Optional<User> found = userManager.findById("john");
        assertTrue(found.isPresent());
        assertEquals("john", found.get().username());
    }

    @Test
    void findByUsername() {
        userManager.add(user);
        Optional<User> found = userManager.findByUsername("john");
        assertTrue(found.isPresent());
        assertEquals("john", found.get().username());
    }

    @Test
    void findByEmail() {
        userManager.add(user);
        Optional<User> found = userManager.findByEmail("john@email.com");
        assertTrue(found.isPresent());
        assertEquals("john", found.get().username());
    }

    @Test
    void findByFilter() {
        userManager.add(user);
        UserFilter filter = UserFilters.byUsername("john");
        List<User> result = userManager.findByFilter(filter);
        assertEquals(1, result.size());
        assertEquals("john", result.get(0).username());
    }

    @Test
    void findAll() {
        userManager.add(user);
        userManager.add(User.create("jane", "Jane Smith", "jane@email.com"));
        List<User> all = userManager.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void exists() {
        userManager.add(user);
        assertTrue(userManager.exists("john"));
        assertFalse(userManager.exists("jane"));
    }

    @Test
    void updateUser() {
        userManager.add(user);
        userManager.update("john", "John Updated", "john.new@email.com");

        Optional<User> updated = userManager.findByUsername("john");
        assertTrue(updated.isPresent());
        assertEquals("John Updated", updated.get().fullName());
        assertEquals("john.new@email.com", updated.get().email());
    }

    @Test
    void updateUserNotFound() {
        assertThrows(NoSuchElementException.class,
                () -> userManager.update("john", "New Name", "new@email.com"));
    }

    @Test
    void clear() {
        userManager.add(user);
        userManager.clear();
        assertEquals(0, userManager.count());
    }

    @Test
    void count() {
        assertEquals(0, userManager.count());
        userManager.add(user);
        assertEquals(1, userManager.count());
    }
}