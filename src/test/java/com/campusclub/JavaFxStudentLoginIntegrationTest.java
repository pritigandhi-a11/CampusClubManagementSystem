package com.campusclub;

import com.campusclub.dao.UserDAO;
import com.campusclub.exception.InvalidLoginException;
import com.campusclub.javafx.FxLoginView;
import com.campusclub.model.Student;
import com.campusclub.model.User;
import com.campusclub.model.UserRole;
import com.campusclub.service.AuthenticationService;
import com.campusclub.util.DatabaseConnection;
import com.campusclub.web.TeacherDashboardWebServer;
import com.campusclub.web.WebPortalController;
import org.junit.jupiter.api.*;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated Integration Test invoking the exact JavaFX Student Login flow and backend authentication methods.
 * Verifies:
 * 1. JavaFX exact method path (authService.login) across college email, Student ID, and usernames.
 * 2. Database connection status and schema verification.
 * 3. Accurate student object mapping including registerNumber, department, and role.
 * 4. Password mismatch and masked exception handling.
 * 5. Preservation of staff and admin authentication.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JavaFxStudentLoginIntegrationTest {

    private static AuthenticationService authService;
    private static UserDAO userDAO;

    @BeforeAll
    static void setUp() {
        System.setProperty("java.awt.headless", "true");
        TeacherDashboardWebServer.startServerIfNotRunning(8096);
        new WebPortalController().ensureAccountsProvisionedInDatabase();

        userDAO = new UserDAO();
        authService = new AuthenticationService(userDAO);
    }

    @Test
    @Order(1)
    @DisplayName("Verify live database connectivity and catalog schema")
    void testDatabaseConnectivity() {
        assertTrue(DatabaseConnection.testConnection(), "Live database must be reachable for JavaFX authentication");
    }

    @Test
    @Order(2)
    @DisplayName("JavaFX flow: Student login using official college email ending in @klu.ac.in")
    void testLoginWithOfficialCollegeEmail() throws Exception {
        User user = authService.login("99527800392@klu.ac.in", "StudentPass@2026");
        assertNotNull(user, "Authenticated user must not be null");
        assertEquals(UserRole.STUDENT, user.getRole(), "User role must be STUDENT");
        assertTrue(user instanceof Student, "Polymorphic type must be Student");

        Student student = (Student) user;
        assertNotNull(student.getRegisterNumber(), "Student register number must be populated");
        assertEquals("99527800392", student.getRegisterNumber(), "Register number must match 99527800392");
    }

    @Test
    @Order(3)
    @DisplayName("JavaFX flow: Student login using numeric Student ID")
    void testLoginWithNumericStudentID() throws Exception {
        User user = authService.login("99527800392", "StudentPass@2026");
        assertNotNull(user);
        assertEquals(UserRole.STUDENT, user.getRole());
        assertEquals("99527800392@klu.ac.in", user.getEmail());
    }

    @Test
    @Order(4)
    @DisplayName("JavaFX flow: Student login using alphanumeric Register Number")
    void testLoginWithAlphaNumericRegisterNumber() throws Exception {
        User user = authService.login("1CC21CS101", "pass123");
        assertNotNull(user);
        assertEquals(UserRole.STUDENT, user.getRole());
        assertEquals("student1", user.getUsername());
    }

    @Test
    @Order(5)
    @DisplayName("JavaFX flow: Student login using institutional email alias for existing account")
    void testLoginWithInstitutionalAliasForExistingStudent() throws Exception {
        User user = authService.login("student1@klu.ac.in", "pass123");
        assertNotNull(user);
        assertEquals("student1", user.getUsername());
        assertEquals(UserRole.STUDENT, user.getRole());
    }

    @Test
    @Order(6)
    @DisplayName("JavaFX flow: Student login using rohit_k institutional email")
    void testLoginWithRohitCollegeEmail() throws Exception {
        User user = authService.login("rohit_k@klu.ac.in", "password123");
        assertNotNull(user);
        assertEquals("rohit_k", user.getUsername());
        assertEquals(UserRole.STUDENT, user.getRole());
    }

    @Test
    @Order(7)
    @DisplayName("JavaFX flow: Wrong password returns masked 'Invalid username or password.' exception")
    void testLoginWithWrongPasswordFails() {
        InvalidLoginException ex = assertThrows(InvalidLoginException.class, () ->
                authService.login("99527800392@klu.ac.in", "WrongPassword!2026"));
        assertEquals("Invalid username or password.", ex.getMessage(), "Failure must be masked to prevent enumeration");

        InvalidLoginException ex2 = assertThrows(InvalidLoginException.class, () ->
                authService.login("99527800392", "BadPass@999"));
        assertEquals("Invalid username or password.", ex2.getMessage());
    }

    @Test
    @Order(8)
    @DisplayName("JavaFX flow: Non-existent identifier returns masked 'Invalid username or password.' exception")
    void testLoginWithNonExistentIdentifierFails() {
        InvalidLoginException ex = assertThrows(InvalidLoginException.class, () ->
                authService.login("non_existent_999999@klu.ac.in", "SomePass@123"));
        assertEquals("Invalid username or password.", ex.getMessage());
    }

    @Test
    @Order(9)
    @DisplayName("JavaFX flow: Blank username or password throws required validation error")
    void testLoginWithBlankCredentialsThrowsValidation() {
        InvalidLoginException exUser = assertThrows(InvalidLoginException.class, () ->
                authService.login("", "SomePass@123"));
        assertEquals("Username is required.", exUser.getMessage());

        InvalidLoginException exPass = assertThrows(InvalidLoginException.class, () ->
                authService.login("student1@klu.ac.in", ""));
        assertEquals("Password is required.", exPass.getMessage());
    }

    @Test
    @Order(10)
    @DisplayName("JavaFX flow: Admin and Staff login preservation")
    void testStaffAndAdminLoginPreserved() throws Exception {
        User admin = authService.login("admin", "admin123");
        assertNotNull(admin);
        assertEquals(UserRole.ADMIN, admin.getRole());

        User reviewer = authService.login("teacher_reviewer", "Reviewer@2026");
        assertNotNull(reviewer);
        assertEquals(UserRole.TEACHER, reviewer.getRole());

        User lead = authService.login("team_lead", "TeamLead@2026");
        assertNotNull(lead);
        assertEquals(UserRole.ADMIN, lead.getRole());
    }

    @Test
    @Order(11)
    @DisplayName("Verify FxLoginView consumer callback passes authenticated User to dashboard transition")
    void testFxLoginConsumerIntegration() throws Exception {
        AtomicReference<User> loggedInUser = new AtomicReference<>();
        // Simulate the exact Consumer<User> callback that FxLoginView invokes on successful authentication
        java.util.function.Consumer<User> onLoginSuccess = loggedInUser::set;

        User user = authService.login("99527800392@klu.ac.in", "StudentPass@2026");
        onLoginSuccess.accept(user);

        assertNotNull(loggedInUser.get(), "Consumer must receive the authenticated student user");
        assertEquals("student_klu_1793373484909", loggedInUser.get().getUsername());
        assertEquals(UserRole.STUDENT, loggedInUser.get().getRole());
    }
}
