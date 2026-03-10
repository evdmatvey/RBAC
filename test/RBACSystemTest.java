import commands.*;

import entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RBACSystemTest {

    private RBACSystem rbacSystem;

    @BeforeEach
    void setUp() {
        rbacSystem = new RBACSystem();
    }

    @Nested
    @DisplayName("Initialization tests")
    class InitializationTests {

        @Test
        void constructorCreatesEmptyManagers() {
            assertEquals(0, rbacSystem.getUserManager().count());
            assertEquals(0, rbacSystem.getRoleManager().count());
            assertEquals(0, rbacSystem.getAssignmentManager().count());
        }

        @Test
        void initializeCreatesRolesAndAdminUser() {
            rbacSystem.initialize();

            assertEquals(3, rbacSystem.getRoleManager().count());
            assertEquals(1, rbacSystem.getUserManager().count());
            assertEquals(1, rbacSystem.getAssignmentManager().count());
        }

        @Test
        void initializeCreatesAdminRoleWithAllPermissions() {
            rbacSystem.initialize();

            Role adminRole = rbacSystem.getRoleManager().findByName("admin").orElse(null);
            assertNotNull(adminRole);

            Set<Permission> permissions = adminRole.getPermissions();
            assertTrue(permissions.size() >= 16);
        }

        @Test
        void initializeAssignsAdminRoleToAdminUser() {
            rbacSystem.initialize();

            User admin = rbacSystem.getUserManager().findByUsername("admin").orElse(null);
            assertNotNull(admin);

            Set<Role> userRoles = rbacSystem.getAssignmentManager().getUserRoles(admin);
            assertTrue(userRoles.stream().anyMatch(r -> r.getName().equals("admin")));
        }
    }

    @Nested
    @DisplayName("Current user management tests")
    class CurrentUserTests {

        @Test
        void setCurrentUserUpdatesCurrentUser() {
            rbacSystem.setCurrentUser("testuser");
            assertEquals("testuser", rbacSystem.getCurrentUser());
        }

        @Test
        void getCurrentUserReturnsNullByDefault() {
            assertNull(rbacSystem.getCurrentUser());
        }

        @Test
        void setCurrentUserWithNullAllowed() {
            rbacSystem.setCurrentUser(null);
            assertNull(rbacSystem.getCurrentUser());
        }
    }

    @Nested
    @DisplayName("Statistics generation tests")
    class StatisticsTests {

        @Test
        void generateStatisticsWithEmptySystemReturnsTable() {
            String stats = rbacSystem.generateStatistics();

            assertTrue(stats.contains("Users"));
            assertTrue(stats.contains("Roles"));
            assertTrue(stats.contains("Assignments"));
            assertTrue(stats.contains("0"));
        }

        @Test
        void generateStatisticsAfterInitializationShowsCounts() {
            rbacSystem.initialize();

            String stats = rbacSystem.generateStatistics();

            assertTrue(stats.contains("Users"));
            assertTrue(stats.contains("1"));
            assertTrue(stats.contains("Roles"));
            assertTrue(stats.contains("3"));
            assertTrue(stats.contains("Assignments"));
            assertTrue(stats.contains("1"));
        }

        @Test
        void generateStatisticsAfterAddingDataUpdatesCounts() {
            rbacSystem.initialize();

            User newUser = User.create("testuser", "Test User", "test@example.com");
            rbacSystem.getUserManager().add(newUser);

            String stats = rbacSystem.generateStatistics();

            assertTrue(stats.contains("Users"));
            assertTrue(stats.contains("2"));
        }
    }

    @Nested
    @DisplayName("Integration tests")
    class IntegrationTests {

        @Test
        void systemCanCreateAndManageUsers() {
            User user = User.create("john", "John Doe", "john@example.com");
            rbacSystem.getUserManager().add(user);

            assertEquals(1, rbacSystem.getUserManager().count());
            assertTrue(rbacSystem.getUserManager().findByUsername("john").isPresent());
        }

        @Test
        void systemCanCreateAndManageRoles() {
            Role role = new Role("editor", "Can edit content");
            rbacSystem.getRoleManager().add(role);

            assertEquals(1, rbacSystem.getRoleManager().count());
            assertTrue(rbacSystem.getRoleManager().findByName("editor").isPresent());
        }

        @Test
        void systemCanCreateAssignments() {
            rbacSystem.initialize();

            User user = rbacSystem.getUserManager().findByUsername("admin").get();
            Role role = rbacSystem.getRoleManager().findByName("viewer").get();

            AssignmentMetadata metadata = AssignmentMetadata.now("system", "test");
            PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);

            rbacSystem.getAssignmentManager().add(assignment);

            assertEquals(2, rbacSystem.getAssignmentManager().count());
            assertTrue(rbacSystem.getAssignmentManager().userHasRole(user, role));
        }

        @Test
        void systemValidatesUserPermissions() {
            rbacSystem.initialize();

            User admin = rbacSystem.getUserManager().findByUsername("admin").get();

            boolean hasPermission = rbacSystem.getAssignmentManager()
                    .userHasPermission(admin, "read", "users");

            assertTrue(hasPermission);
        }

        @Test
        void systemReturnsEmptySetForNullUserPermissions() {
            Set<Permission> permissions = rbacSystem.getAssignmentManager().getUserPermissions(null);

            assertTrue(permissions.isEmpty());
        }
    }
}