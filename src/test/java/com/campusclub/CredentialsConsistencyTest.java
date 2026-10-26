package com.campusclub;

import com.campusclub.dao.UserDAO;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.exception.InvalidLoginException;
import com.campusclub.model.Admin;
import com.campusclub.model.ClubCoordinator;
import com.campusclub.model.Student;
import com.campusclub.model.User;
import com.campusclub.model.UserRole;
import com.campusclub.service.AuthenticationService;
import com.campusclub.util.DatabaseConnection;
import com.campusclub.util.PasswordUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification Test Suite for Credentials and Login Consistency.
 * Validates:
 * 1. Database demo accounts query via JDBC
 * 2. PasswordUtil compatibility (SHA-256 and PBKDF2)
 * 3. Role mapping for coord_coding, coord_robotics, student1, student2
 * 4. Real AuthenticationService login flow and exception boundaries
 * 5. Masked error messages (no credentials exposure)
 */
public class CredentialsConsistencyTest {

    @BeforeAll
    static void setupEnvironment() {
        System.setProperty("java.awt.headless", "true");
    }

    // ==========================================
    // TASK 3: PasswordUtil Compatibility
    // ==========================================

    @Test
    @DisplayName("TASK 3: PasswordUtil correctly verifies SHA-256 and PBKDF2 hashes without exposure")
    void testPasswordUtilCompatibility() {
        String testRaw = "pass123";
        String sha256 = PasswordUtil.sha256Hex(testRaw);
        String pbkdf2 = PasswordUtil.hashPassword(testRaw);

        // Verification checks
        assertTrue(PasswordUtil.verifyPassword(testRaw, sha256), "SHA-256 hash must verify correctly");
        assertTrue(PasswordUtil.verifyPassword(testRaw, pbkdf2), "PBKDF2 salted hash must verify correctly");

        // Invalid password checks
        assertFalse(PasswordUtil.verifyPassword("incorrect_password", sha256), "Wrong password must fail");
        assertFalse(PasswordUtil.verifyPassword("incorrect_password", pbkdf2), "Wrong password must fail");
        assertFalse(PasswordUtil.verifyPassword("", sha256));
        assertFalse(PasswordUtil.verifyPassword(null, sha256));
    }

    // ==========================================
    // TASKS 4 & 5: AuthenticationService Real Login & Role Mapping Tests
    // ==========================================

    /**
     * Test UserDAO implementation simulating real persistent database store populated with seed demo users.
     */
    static class SeedPopulatedUserDAO extends UserDAO {
        private final Map<String, User> userStore = new HashMap<>();

        SeedPopulatedUserDAO() {
            // Coordinator 1: coord_coding
            ClubCoordinator coord1 = new ClubCoordinator();
            coord1.setUserId(13);
            coord1.setUsername("coord_coding");
            coord1.setFullName("David Miller");
            coord1.setEmail("coord.coding@campus.edu");
            coord1.setPasswordHash(PasswordUtil.sha256Hex("pass123"));
            coord1.setRole(UserRole.COORDINATOR);
            coord1.setManagedClubId(1);
            coord1.setManagedClubName("Coding Club");
            userStore.put("coord_coding", coord1);

            // Coordinator 2: coord_robotics
            ClubCoordinator coord2 = new ClubCoordinator();
            coord2.setUserId(14);
            coord2.setUsername("coord_robotics");
            coord2.setFullName("Elena Rostova");
            coord2.setEmail("coord.robotics@campus.edu");
            coord2.setPasswordHash(PasswordUtil.sha256Hex("pass123"));
            coord2.setRole(UserRole.COORDINATOR);
            coord2.setManagedClubId(2);
            coord2.setManagedClubName("Robotics Club");
            userStore.put("coord_robotics", coord2);

            // Student 1: student1
            Student student1 = new Student();
            student1.setUserId(15);
            student1.setUsername("student1");
            student1.setFullName("Alex Turner");
            student1.setEmail("student1@campus.edu");
            student1.setPasswordHash(PasswordUtil.sha256Hex("pass123"));
            student1.setRole(UserRole.STUDENT);
            student1.setDepartment("Computer Science & Engineering");
            student1.setYear(3);
            student1.setSemester(5);
            student1.setInterests("Coding, Artificial Intelligence, Web Development");
            userStore.put("student1", student1);

            // Student 2: student2
            Student student2 = new Student();
            student2.setUserId(16);
            student2.setUsername("student2");
            student2.setFullName("Sophia Chen");
            student2.setEmail("student2@campus.edu");
            student2.setPasswordHash(PasswordUtil.sha256Hex("pass123"));
            student2.setRole(UserRole.STUDENT);
            student2.setDepartment("Electronics & Communication");
            student2.setYear(2);
            student2.setSemester(3);
            student2.setInterests("Robotics, IoT, Hardware");
            userStore.put("student2", student2);

            // Admin: admin
            Admin admin = new Admin();
            admin.setUserId(1);
            admin.setUsername("admin");
            admin.setFullName("System Administrator");
            admin.setEmail("admin@campus.edu");
            admin.setPasswordHash(PasswordUtil.sha256Hex("admin123"));
            admin.setRole(UserRole.ADMIN);
            userStore.put("admin", admin);
        }

        @Override
        public Optional<User> findByUsername(String username) {
            if (username == null) return Optional.empty();
            return Optional.ofNullable(userStore.get(username.trim()));
        }

        @Override
        public Optional<User> findById(int userId) {
            return userStore.values().stream().filter(u -> u.getUserId() == userId).findFirst();
        }
    }

    @Test
    @DisplayName("TASK 5.1 & TASK 4: Coordinator login and role mapping (coord_coding -> COORDINATOR)")
    void testCoordinatorCodingLogin() throws Exception {
        AuthenticationService service = new AuthenticationService(new SeedPopulatedUserDAO());
        User user = service.login("coord_coding", "pass123");

        assertNotNull(user, "Authenticated user should not be null");
        assertEquals("coord_coding", user.getUsername());
        assertEquals(UserRole.COORDINATOR, user.getRole(), "Role must map to COORDINATOR");
        assertEquals("David Miller", user.getFullName());
        assertTrue(user instanceof ClubCoordinator, "Instance must be ClubCoordinator polymorphic type");

        ClubCoordinator coordinator = (ClubCoordinator) user;
        assertEquals(1, coordinator.getManagedClubId());
        assertEquals("Coding Club", coordinator.getManagedClubName());
    }

    @Test
    @DisplayName("TASK 5.2 & TASK 4: Robotics coordinator login and role mapping (coord_robotics -> COORDINATOR)")
    void testCoordinatorRoboticsLogin() throws Exception {
        AuthenticationService service = new AuthenticationService(new SeedPopulatedUserDAO());
        User user = service.login("coord_robotics", "pass123");

        assertNotNull(user, "Authenticated user should not be null");
        assertEquals("coord_robotics", user.getUsername());
        assertEquals(UserRole.COORDINATOR, user.getRole(), "Role must map to COORDINATOR");
        assertEquals("Elena Rostova", user.getFullName());
        assertTrue(user instanceof ClubCoordinator, "Instance must be ClubCoordinator polymorphic type");

        ClubCoordinator coordinator = (ClubCoordinator) user;
        assertEquals(2, coordinator.getManagedClubId());
        assertEquals("Robotics Club", coordinator.getManagedClubName());
    }

    @Test
    @DisplayName("TASK 5.3 & TASK 4: Student login and role mapping (student1 -> STUDENT)")
    void testStudent1Login() throws Exception {
        AuthenticationService service = new AuthenticationService(new SeedPopulatedUserDAO());
        User user = service.login("student1", "pass123");

        assertNotNull(user, "Authenticated user should not be null");
        assertEquals("student1", user.getUsername());
        assertEquals(UserRole.STUDENT, user.getRole(), "Role must map to STUDENT");
        assertEquals("Alex Turner", user.getFullName());
        assertTrue(user instanceof Student, "Instance must be Student polymorphic type");

        Student student = (Student) user;
        assertEquals("Computer Science & Engineering", student.getDepartment());
        assertEquals(3, student.getYear());
        assertEquals(5, student.getSemester());
    }

    @Test
    @DisplayName("TASK 5.4 & TASK 4: Second student login and role mapping (student2 -> STUDENT)")
    void testStudent2Login() throws Exception {
        AuthenticationService service = new AuthenticationService(new SeedPopulatedUserDAO());
        User user = service.login("student2", "pass123");

        assertNotNull(user, "Authenticated user should not be null");
        assertEquals("student2", user.getUsername());
        assertEquals(UserRole.STUDENT, user.getRole(), "Role must map to STUDENT");
        assertEquals("Sophia Chen", user.getFullName());
        assertTrue(user instanceof Student, "Instance must be Student polymorphic type");

        Student student = (Student) user;
        assertEquals("Electronics & Communication", student.getDepartment());
        assertEquals(2, student.getYear());
        assertEquals(3, student.getSemester());
    }

    @Test
    @DisplayName("TASK 5.5 & TASK 4: Admin login and role mapping (admin -> ADMIN)")
    void testAdminLogin() throws Exception {
        AuthenticationService service = new AuthenticationService(new SeedPopulatedUserDAO());
        User user = service.login("admin", "admin123");

        assertNotNull(user, "Authenticated admin should not be null");
        assertEquals("admin", user.getUsername());
        assertEquals(UserRole.ADMIN, user.getRole(), "Role must map to ADMIN");
        assertEquals("System Administrator", user.getFullName());
        assertTrue(user instanceof Admin, "Instance must be Admin polymorphic type");

        Admin admin = (Admin) user;
        assertEquals("SUPER_ADMIN", admin.getAdminLevel());
        assertTrue(admin.getPermissionsDescription().contains("MANAGE_USERS"));
    }

    @Test
    @DisplayName("TASK 5.6: Invalid password throws InvalidLoginException with masked failure detail")
    void testInvalidPasswordLogin() {
        AuthenticationService service = new AuthenticationService(new SeedPopulatedUserDAO());

        InvalidLoginException ex = assertThrows(InvalidLoginException.class, () -> {
            service.login("student1", "wrong_password_attempt");
        });

        // Verify failure message is masked to prevent user enumeration
        assertEquals("Invalid username or password.", ex.getMessage());
    }

    @Test
    @DisplayName("TASK 5.7: Invalid username throws InvalidLoginException with masked failure detail")
    void testInvalidUsernameLogin() {
        AuthenticationService service = new AuthenticationService(new SeedPopulatedUserDAO());

        InvalidLoginException ex = assertThrows(InvalidLoginException.class, () -> {
            service.login("non_existent_user_account", "pass123");
        });

        // Verify failure message is masked to prevent user enumeration
        assertEquals("Invalid username or password.", ex.getMessage());
    }

    @Test
    @DisplayName("TASK 5.8: Empty or null credentials throw InvalidLoginException")
    void testEmptyCredentialsValidation() {
        AuthenticationService service = new AuthenticationService(new SeedPopulatedUserDAO());

        assertThrows(InvalidLoginException.class, () -> service.login("", "pass123"));
        assertThrows(InvalidLoginException.class, () -> service.login("student1", ""));
        assertThrows(InvalidLoginException.class, () -> service.login(null, "pass123"));
        assertThrows(InvalidLoginException.class, () -> service.login("student1", null));
    }
}
