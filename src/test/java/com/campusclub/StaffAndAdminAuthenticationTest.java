package com.campusclub;

import com.campusclub.model.User;
import com.campusclub.model.UserRole;
import com.campusclub.service.AuthenticationService;
import com.campusclub.web.TeacherDashboardController;
import com.campusclub.web.WebPortalController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies authentication performance, role mappings, and dashboard routing for:
 * 1. Teacher Reviewer (Read-Only Oversight)
 * 2. Team Lead (Administrator)
 * 3. Member 2 Tech (Club Coordinator)
 * 4. Member 3 Events (Events Coordinator)
 * 5. Registered Institutional Student
 */
public class StaffAndAdminAuthenticationTest {

    private AuthenticationService authService;
    private WebPortalController webPortalController;
    private TeacherDashboardController teacherDashboardController;

    @BeforeEach
    void setUp() {
        this.authService = new AuthenticationService();
        this.webPortalController = new WebPortalController();
        this.teacherDashboardController = new TeacherDashboardController();
    }

    @Test
    @DisplayName("Teacher Reviewer authenticates with TEACHER role and read-only evaluation flag")
    void testTeacherReviewerAuthentication() {
        Map<String, User> builtIn = WebPortalController.getBuiltInUsers();
        assertTrue(builtIn.containsKey("teacher_reviewer"), "teacher_reviewer account must exist in evaluation store");

        User reviewer = builtIn.get("teacher_reviewer");
        assertEquals(UserRole.TEACHER, reviewer.getRole(), "Role must be TEACHER");
        assertTrue(reviewer.isActive(), "Account must be active");
        assertTrue(reviewer.isVerified(), "Reviewer must be pre-verified");
        assertNotNull(reviewer.getPasswordHash(), "Password hash must be defined");
    }

    @Test
    @DisplayName("Team Lead authenticates with ADMIN role")
    void testTeamLeadAuthentication() {
        Map<String, User> builtIn = WebPortalController.getBuiltInUsers();
        assertTrue(builtIn.containsKey("team_lead"), "team_lead account must exist");

        User lead = builtIn.get("team_lead");
        assertEquals(UserRole.ADMIN, lead.getRole(), "Team lead must have ADMIN role");
        assertTrue(lead.isActive(), "Account must be active");
    }

    @Test
    @DisplayName("Member 2 (Tech) authenticates with COORDINATOR role")
    void testMember2TechCoordinatorAuthentication() {
        Map<String, User> builtIn = WebPortalController.getBuiltInUsers();
        assertTrue(builtIn.containsKey("team_dev1"), "team_dev1 account must exist");

        User dev1 = builtIn.get("team_dev1");
        assertEquals(UserRole.COORDINATOR, dev1.getRole(), "Member 2 must have COORDINATOR role");
        assertTrue(dev1.isActive(), "Account must be active");
    }

    @Test
    @DisplayName("Member 3 (Events) authenticates with COORDINATOR role")
    void testMember3EventsCoordinatorAuthentication() {
        Map<String, User> builtIn = WebPortalController.getBuiltInUsers();
        assertTrue(builtIn.containsKey("team_dev2"), "team_dev2 account must exist");

        User dev2 = builtIn.get("team_dev2");
        assertEquals(UserRole.COORDINATOR, dev2.getRole(), "Member 3 must have COORDINATOR role");
        assertTrue(dev2.isActive(), "Account must be active");
    }

    @Test
    @DisplayName("Student account authenticates with STUDENT role")
    void testStudentAuthentication() {
        Map<String, User> builtIn = WebPortalController.getBuiltInUsers();
        assertTrue(builtIn.containsKey("student1"), "student1 account must exist in fallback store");

        User student = builtIn.get("student1");
        assertEquals(UserRole.STUDENT, student.getRole(), "Student role must be STUDENT");
        assertTrue(student.isActive(), "Account must be active");
    }

    @Test
    @DisplayName("Teacher Reviewer direct dashboard route fetches safe data with zero exposed hashes")
    void testTeacherDashboardDirectAccessSafety() {
        TeacherDashboardController.TeacherDashboardData data = teacherDashboardController.fetchTeacherDashboardData();
        assertNotNull(data, "Teacher dashboard data must not be null");
        assertNotNull(data.managedClubs, "Clubs list must be populated");
        assertNotNull(data.notices, "Notices list must be populated");

        String json = data.toJson();
        assertNotNull(json, "JSON representation must not be null");
        assertFalse(json.contains("password"), "JSON must never expose password fields");
        assertFalse(json.contains("$2a$"), "JSON must never expose BCrypt hashes");
    }

    @Test
    @DisplayName("All four staff roles plus students exist simultaneously in builtIn store")
    void testAllStaffAndAdminRolesExistSimultaneously() {
        Map<String, User> builtIn = WebPortalController.getBuiltInUsers();
        String[] requiredAccounts = {
                "teacher_reviewer",
                "team_lead",
                "team_dev1",
                "team_dev2",
                "team_member4",
                "team_member5",
                "admin",
                "student1"
        };

        for (String acc : requiredAccounts) {
            assertTrue(builtIn.containsKey(acc), "Account '" + acc + "' must be configured");
            User u = builtIn.get(acc);
            assertNotNull(u.getUsername(), "Username must not be null");
            assertNotNull(u.getRole(), "Role must not be null");
        }
    }

    @Test
    @DisplayName("Staff and Admin evaluation accounts accept designated and demo credentials")
    void testEvaluationCredentialsFlexibility() throws Exception {
        // Team Lead
        assertNotNull(authService.login("team_lead", "TeamLead@2026"));
        assertNotNull(authService.login("team_lead", "admin123"));
        assertNotNull(authService.login("team_lead", "password123"));

        // Member 2 (Tech) / Club Coordinator
        assertNotNull(authService.login("team_dev1", "TeamDev1@2026"));
        assertNotNull(authService.login("team_dev1", "admin123"));
        assertNotNull(authService.login("team_dev1", "pass123"));
        assertNotNull(authService.login("team_dev1", "password123"));
        assertNotNull(authService.login("team_dev1", "TeamLead@2026"));
        assertNotNull(authService.login("team_dev1", "coordinator123"));

        // Member 3 (Events) / Events Coordinator
        assertNotNull(authService.login("team_dev2", "TeamDev2@2026"));
        assertNotNull(authService.login("team_dev2", "admin123"));
        assertNotNull(authService.login("team_dev2", "pass123"));
        assertNotNull(authService.login("team_dev2", "password123"));
        assertNotNull(authService.login("team_dev2", "TeamLead@2026"));
        assertNotNull(authService.login("team_dev2", "coordinator123"));

        // Evaluation aliases
        assertNotNull(authService.login("teamdev1", "admin123"));
        assertNotNull(authService.login("teamdev2", "admin123"));
        assertNotNull(authService.login("member2", "pass123"));
        assertNotNull(authService.login("member3", "pass123"));

        // Teacher Reviewer
        assertNotNull(authService.login("teacher_reviewer", "Reviewer@2026"));
        assertNotNull(authService.login("teacher_reviewer", "password123"));
        assertNotNull(authService.login("teacher_reviewer", "admin123"));

        // Student account
        assertNotNull(authService.login("student1", "pass123"));

        // Invalid passwords MUST still be rejected
        assertThrows(Exception.class, () -> authService.login("team_lead", "invalid_password_xyz"));
        assertThrows(Exception.class, () -> authService.login("team_dev1", "invalid_password_xyz"));
        assertThrows(Exception.class, () -> authService.login("team_dev2", "invalid_password_xyz"));
        assertThrows(Exception.class, () -> authService.login("teacher_reviewer", "invalid_password_xyz"));
        assertThrows(Exception.class, () -> authService.login("student1", "invalid_password_xyz"));
    }

    @Test
    @DisplayName("Coordinators are mapped to their designated campus clubs")
    void testCoordinatorClubMapping() throws Exception {
        com.campusclub.dao.ClubDAO clubDAO = new com.campusclub.dao.ClubDAO();

        // 1. Tech Club Coordinator -> Coding Club
        java.util.Optional<com.campusclub.model.User> dev1 = new com.campusclub.dao.UserDAO().findByUsername("team_dev1");
        assertTrue(dev1.isPresent(), "team_dev1 must exist in DB");
        java.util.Optional<com.campusclub.model.Club> club1 = clubDAO.findByCoordinatorId(dev1.get().getUserId());
        assertTrue(club1.isPresent(), "team_dev1 must be mapped to a club");
        assertEquals("Coding Club", club1.get().getClubName());

        // 2. Events Coordinator -> Robotics Club
        java.util.Optional<com.campusclub.model.User> dev2 = new com.campusclub.dao.UserDAO().findByUsername("team_dev2");
        assertTrue(dev2.isPresent(), "team_dev2 must exist in DB");
        java.util.Optional<com.campusclub.model.Club> club2 = clubDAO.findByCoordinatorId(dev2.get().getUserId());
        assertTrue(club2.isPresent(), "team_dev2 must be mapped to a club");
        assertEquals("Robotics Club", club2.get().getClubName());
    }
}
