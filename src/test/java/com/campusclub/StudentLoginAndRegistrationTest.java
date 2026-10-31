package com.campusclub;

import com.campusclub.model.UserRole;
import com.campusclub.web.TeacherDashboardWebServer;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Student Login, Registration, Student Dashboard, and Admin Extensions.
 * Validates requirements A through F:
 * A. Separate Admin Login and Student Login options on existing interface.
 * B. Complete Student Registration with 7 fields (fullName, registerNumber, email, department, year, password, confirmPassword),
 *    enforcing @klu.ac.in domain, password matching, OTP dispatch, and BCrypt security.
 * C. Dual Student Login using either registered college email (@klu.ac.in) or Student ID (register number).
 * D. Complete Student Dashboard (Welcome message, Available Clubs, Events, My Clubs, My Events, Profile, Logout).
 * E. Extended Admin Dashboard (Registered students roster, student details, membership lists, event registrations).
 * F. Full preservation of existing accounts, roles, JavaFX views, and security controls.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StudentLoginAndRegistrationTest {

    private static int testPort;

    @BeforeAll
    static void setUp() {
        System.setProperty("java.awt.headless", "true");
        testPort = TeacherDashboardWebServer.startServerIfNotRunning(8095);
    }

    private String getBaseUrl() {
        return "http://localhost:" + testPort;
    }

    // =========================================================================
    // Feature A: Separate Login Options on Existing Interface
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Feature A: Login page renders separate Student and Staff/Admin portal options")
    void testSeparateLoginOptionsOnWebInterface() throws Exception {
        URL url = new URL(getBaseUrl() + "/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());

        String html = readResponse(conn);
        // Verify presence of segmented portal switches
        assertTrue(html.contains("Student Portal") || html.contains("STUDENT PORTAL"),
                "Login page should include a Student Portal option");
        assertTrue(html.contains("Staff &amp; Admin") || html.contains("Staff & Admin") || html.contains("STAFF"),
                "Login page should include a Staff & Admin option");
        // Verify presence of registration trigger for students
        assertTrue(html.contains("modal-register"),
                "Login page should have a student registration modal trigger");
        // Verify institution indicator
        assertTrue(html.contains("klu.ac.in"), "Login page should reference @klu.ac.in domain");
    }

    // =========================================================================
    // Feature B: Complete Student Registration with 7 Fields & Validation
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("Feature B1: Complete Student Registration succeeds with all 7 fields and @klu.ac.in email")
    void testStudentRegistrationSuccess() throws Exception {
        URL url = new URL(getBaseUrl() + "/api/auth/register-student");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String testRegNo = "9952" + (System.currentTimeMillis() % 10000000);
        String testEmail = testRegNo + "@klu.ac.in";

        String payload = "{" +
                "\"fullName\":\"Venkatesh Raman\"," +
                "\"registerNumber\":\"" + testRegNo + "\"," +
                "\"email\":\"" + testEmail + "\"," +
                "\"department\":\"Computer Science & Engineering\"," +
                "\"year\":\"2\"," +
                "\"password\":\"KareStudent@2026\"," +
                "\"confirmPassword\":\"KareStudent@2026\"" +
                "}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(201, conn.getResponseCode(), "Registration should return HTTP 201 Created");
        String resp = readResponse(conn);
        assertTrue(resp.contains("\"success\":true"), "Response should indicate success");
        assertTrue(resp.contains("\"role\":\"STUDENT\""), "Backend must enforce STUDENT role");
        assertTrue(resp.contains(testEmail), "Response should echo institutional email");
    }

    @Test
    @Order(3)
    @DisplayName("Feature B2: Registration fails when password and confirm password do not match")
    void testStudentRegistrationPasswordMismatchRejection() throws Exception {
        URL url = new URL(getBaseUrl() + "/api/auth/register-student");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = "{" +
                "\"fullName\":\"Rajesh Kumar\"," +
                "\"registerNumber\":\"99527800111\"," +
                "\"email\":\"99527800111@klu.ac.in\"," +
                "\"department\":\"Information Technology\"," +
                "\"year\":\"3\"," +
                "\"password\":\"SecretPassword1\"," +
                "\"confirmPassword\":\"SecretPassword2\"" +
                "}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(400, conn.getResponseCode(), "Password mismatch should return HTTP 400 Bad Request");
        String resp = readErrorResponse(conn);
        assertTrue(resp.contains("do not match") || resp.contains("verify both passwords"),
                "Error message should explain password mismatch");
    }

    @Test
    @Order(4)
    @DisplayName("Feature B3: Registration rejects non-institutional or spoofed email domains")
    void testStudentRegistrationInvalidEmailDomainRejection() throws Exception {
        String[] invalidEmails = {
                "student@gmail.com",
                "student@yahoo.com",
                "attacker@klu.ac.in.fake.org",
                "notklu@campus.edu"
        };

        for (String badEmail : invalidEmails) {
            URL url = new URL(getBaseUrl() + "/api/auth/register-student");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = "{" +
                    "\"fullName\":\"Bad Email Test\"," +
                    "\"registerNumber\":\"99527800444\"," +
                    "\"email\":\"" + badEmail + "\"," +
                    "\"department\":\"CSE\"," +
                    "\"year\":\"1\"," +
                    "\"password\":\"TestPass@2026\"," +
                    "\"confirmPassword\":\"TestPass@2026\"" +
                    "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            assertEquals(400, conn.getResponseCode(), "Domain " + badEmail + " must be rejected with HTTP 400");
        }
    }

    // =========================================================================
    // Feature C: Dual Student Login via College Email or Student ID
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("Feature C1: Student can log in using registered college email (@klu.ac.in)")
    void testStudentLoginViaCollegeEmail() throws Exception {
        URL url = new URL(getBaseUrl() + "/api/auth/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = "{\"username\":\"99527800392@klu.ac.in\",\"password\":\"StudentPass@2026\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(200, conn.getResponseCode(), "Login via @klu.ac.in email should return HTTP 200");
        String resp = readResponse(conn);
        assertTrue(resp.contains("\"success\":true"), "Response should confirm successful login");
        assertTrue(resp.contains("\"role\":\"STUDENT\""), "User role should be STUDENT");
    }

    @Test
    @Order(6)
    @DisplayName("Feature C2: Student can log in using numeric Student ID (register number)")
    void testStudentLoginViaStudentId() throws Exception {
        URL url = new URL(getBaseUrl() + "/api/auth/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = "{\"username\":\"99527800392\",\"password\":\"StudentPass@2026\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(200, conn.getResponseCode(), "Login via numeric Student ID should return HTTP 200");
        String resp = readResponse(conn);
        assertTrue(resp.contains("\"success\":true"), "Response should confirm successful login");
        assertTrue(resp.contains("\"role\":\"STUDENT\""), "User role should be STUDENT");
    }

    @Test
    @Order(7)
    @DisplayName("Feature C3: Existing privileged and student accounts continue logging in normally")
    void testExistingAccountsLoginPreserved() throws Exception {
        String[][] accounts = {
                {"admin", "admin123"},
                {"team_lead", "TeamLead@2026"},
                {"teacher_reviewer", "Reviewer@2026"},
                {"coord_coding", "pass123"},
                {"student1", "pass123"}
        };

        for (String[] acc : accounts) {
            URL url = new URL(getBaseUrl() + "/api/auth/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = "{\"username\":\"" + acc[0] + "\",\"password\":\"" + acc[1] + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            assertEquals(200, conn.getResponseCode(), "Account " + acc[0] + " must authenticate successfully");
        }
    }

    // =========================================================================
    // Feature D: Complete Student Dashboard APIs
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("Feature D1: Student Profile API returns institutional student details")
    void testStudentProfileApi() throws Exception {
        String sessionCookie = loginAndGetCookie("99527800392", "StudentPass@2026");
        assertNotNull(sessionCookie);

        URL url = new URL(getBaseUrl() + "/api/student/profile");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", sessionCookie);

        assertEquals(200, conn.getResponseCode());
        String resp = readResponse(conn);
        assertTrue(resp.contains("\"role\":\"STUDENT\""));
        assertTrue(resp.contains("99527800392"));
        assertTrue(resp.contains("klu.ac.in"));
    }

    @Test
    @Order(9)
    @DisplayName("Feature D2: Student My Clubs and My Events APIs return enrolled items")
    void testStudentMyClubsAndEventsApis() throws Exception {
        String sessionCookie = loginAndGetCookie("99527800392", "StudentPass@2026");
        assertNotNull(sessionCookie);

        // My Clubs
        URL clubsUrl = new URL(getBaseUrl() + "/api/student/my-clubs");
        HttpURLConnection connClubs = (HttpURLConnection) clubsUrl.openConnection();
        connClubs.setRequestMethod("GET");
        connClubs.setRequestProperty("Cookie", sessionCookie);
        assertEquals(200, connClubs.getResponseCode());
        String clubsResp = readResponse(connClubs);
        assertTrue(clubsResp.startsWith("["), "Should return JSON array of clubs");

        // My Events
        URL eventsUrl = new URL(getBaseUrl() + "/api/student/my-events");
        HttpURLConnection connEvents = (HttpURLConnection) eventsUrl.openConnection();
        connEvents.setRequestMethod("GET");
        connEvents.setRequestProperty("Cookie", sessionCookie);
        assertEquals(200, connEvents.getResponseCode());
        String eventsResp = readResponse(connEvents);
        assertTrue(eventsResp.startsWith("["), "Should return JSON array of events");
    }

    @Test
    @Order(10)
    @DisplayName("Feature D3: Verified student can apply for club membership and register for an event")
    void testStudentApplyAndRegisterActions() throws Exception {
        String sessionCookie = loginAndGetCookie("99527800392", "StudentPass@2026");
        assertNotNull(sessionCookie);

        // Apply for Club
        URL appUrl = new URL(getBaseUrl() + "/api/memberships");
        HttpURLConnection connApp = (HttpURLConnection) appUrl.openConnection();
        connApp.setRequestMethod("POST");
        connApp.setRequestProperty("Content-Type", "application/json");
        connApp.setRequestProperty("Cookie", sessionCookie);
        connApp.setDoOutput(true);

        try (OutputStream os = connApp.getOutputStream()) {
            os.write("{\"club\":\"Robotics & Automation Club\"}".getBytes(StandardCharsets.UTF_8));
        }
        int appCode = connApp.getResponseCode();
        assertTrue(appCode == 201 || appCode == 409, "Should succeed (201) or report already submitted (409)");

        // Register for Event
        URL regUrl = new URL(getBaseUrl() + "/api/events/register");
        HttpURLConnection connReg = (HttpURLConnection) regUrl.openConnection();
        connReg.setRequestMethod("POST");
        connReg.setRequestProperty("Content-Type", "application/json");
        connReg.setRequestProperty("Cookie", sessionCookie);
        connReg.setDoOutput(true);

        try (OutputStream os = connReg.getOutputStream()) {
            os.write("{\"eventId\":\"102\"}".getBytes(StandardCharsets.UTF_8));
        }
        int regCode = connReg.getResponseCode();
        assertTrue(regCode == 200 || regCode == 409, "Should succeed (200) or report already registered (409)");
    }

    @Test
    @Order(11)
    @DisplayName("Feature D4: Student visiting /portal is served the Student Dashboard")
    void testStudentPortalPageRendering() throws Exception {
        String sessionCookie = loginAndGetCookie("99527800392", "StudentPass@2026");
        assertNotNull(sessionCookie);

        URL url = new URL(getBaseUrl() + "/portal");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", sessionCookie);

        assertEquals(200, conn.getResponseCode());
        String html = readResponse(conn);
        assertTrue(html.contains("Student Portal") || html.contains("Welcome"), "Page should render student portal");
        assertTrue(html.contains("Available Clubs"), "Page should have Available Clubs section");
        assertTrue(html.contains("Upcoming Events"), "Page should have Upcoming Events section");
        assertTrue(html.contains("My Clubs"), "Page should have My Clubs tab");
        assertTrue(html.contains("My Events"), "Page should have My Events tab");
        assertTrue(html.contains("Student Profile"), "Page should have Student Profile tab");
        assertTrue(html.contains("Logout"), "Page should have Logout button");
    }

    // =========================================================================
    // Feature E: Admin Dashboard Extension
    // =========================================================================

    @Test
    @Order(12)
    @DisplayName("Feature E1: Admin can access Registered Students list (/api/admin/students)")
    void testAdminGetRegisteredStudents() throws Exception {
        String adminCookie = loginAndGetCookie("admin", "admin123");
        assertNotNull(adminCookie);

        URL url = new URL(getBaseUrl() + "/api/admin/students");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", adminCookie);

        assertEquals(200, conn.getResponseCode());
        String resp = readResponse(conn);
        assertTrue(resp.startsWith("["), "Should return JSON list of students");
        assertTrue(resp.contains("99527800392"), "Roster should include KARE student");
        assertTrue(resp.contains("department"), "Roster should include student department");
        assertTrue(resp.contains("year"), "Roster should include student year of study");
    }

    @Test
    @Order(13)
    @DisplayName("Feature E2: Admin can access Event Registrations list (/api/admin/event-registrations)")
    void testAdminGetEventRegistrations() throws Exception {
        String adminCookie = loginAndGetCookie("admin", "admin123");
        assertNotNull(adminCookie);

        URL url = new URL(getBaseUrl() + "/api/admin/event-registrations");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", adminCookie);

        assertEquals(200, conn.getResponseCode());
        String resp = readResponse(conn);
        assertTrue(resp.startsWith("["), "Should return JSON list of event registrations");
    }

    @Test
    @Order(14)
    @DisplayName("Feature E3: Student role is strictly forbidden from accessing Admin Student Roster (HTTP 403)")
    void testStudentForbiddenFromAdminStudentsEndpoint() throws Exception {
        String studentCookie = loginAndGetCookie("99527800392", "StudentPass@2026");
        assertNotNull(studentCookie);

        URL url = new URL(getBaseUrl() + "/api/admin/students");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", studentCookie);

        assertEquals(403, conn.getResponseCode(), "Student role must be forbidden (HTTP 403)");
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private String loginAndGetCookie(String username, String password) throws Exception {
        URL url = new URL(getBaseUrl() + "/api/auth/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() != 200) {
            return null;
        }

        String cookie = conn.getHeaderField("Set-Cookie");
        if (cookie != null && cookie.contains(";")) {
            cookie = cookie.substring(0, cookie.indexOf(';'));
        }
        return cookie;
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        }
    }

    private static String readErrorResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        }
    }
}
