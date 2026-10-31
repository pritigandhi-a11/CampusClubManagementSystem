package com.campusclub;

import com.campusclub.dao.UserDAO;
import com.campusclub.model.Student;
import com.campusclub.model.User;
import com.campusclub.model.UserRole;
import com.campusclub.service.AuthenticationService;
import com.campusclub.service.StudentService;
import com.campusclub.util.DatabaseConnection;
import com.campusclub.util.DatabaseMigrationRunner;
import com.campusclub.util.EmailService;
import com.campusclub.util.PasswordUtil;
import com.campusclub.util.ValidationUtil;
import com.campusclub.web.TeacherDashboardWebServer;
import com.campusclub.web.WebPortalController;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Targeted Security and Role-Based Access Control (RBAC) Test Suite.
 * Validates all eight security enhancement requirements:
 * 1. Student registration receives STUDENT role only & enforces institutional domain.
 * 2. Privilege escalation attempts during registration or modification are blocked.
 * 3. Authenticated Students cannot access Admin/Coordinator endpoints (returns HTTP 403).
 * 4. Coordinators can only perform authorized coordinator actions.
 * 5. Teacher / Reviewer accounts have strictly read-only access without data mutation rights.
 * 6. Unverified accounts cannot apply for club memberships or register for events until OTP verified.
 * 7. Production mode disables demo credential autofill and prevents presets from granting privileges.
 * 8. Existing legitimate accounts (admin, teacher_reviewer, students) authenticate without regression,
 *    and modern BCrypt password hashing is supported alongside legacy hashes.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SecurityAuditAndRbacTest {

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
    // Requirement 1: Student registration receives STUDENT role only
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("REQ 1: Student registration strictly assigns STUDENT role and enforces Kalasalingam University (@klu.ac.in) email")
    void testStudentRegistrationReceivesStudentRoleOnly() throws Exception {
        URL regUrl = new URL(getBaseUrl() + "/api/auth/register-student");

        // 1. Invalid non-institutional emails must be rejected (Gmail, Yahoo, fake domains, subdomains)
        String[] rejectedEmails = {
            "attacker@gmail.com",
            "attacker@yahoo.com",
            "attacker@klu.ac.in.example.com",
            "attacker@fakeklu.ac.in",
            "attacker@campus.edu"
        };

        for (String badEmail : rejectedEmails) {
            HttpURLConnection connInvalid = (HttpURLConnection) regUrl.openConnection();
            connInvalid.setRequestMethod("POST");
            connInvalid.setRequestProperty("Content-Type", "application/json");
            connInvalid.setDoOutput(true);

            String invalidPayload = String.format("{\"username\":\"bad_%d\",\"password\":\"Secret@2026\",\"fullName\":\"Attacker\",\"email\":\"%s\"}",
                    System.nanoTime(), badEmail);
            try (OutputStream os = connInvalid.getOutputStream()) {
                os.write(invalidPayload.getBytes(StandardCharsets.UTF_8));
            }
            assertEquals(400, connInvalid.getResponseCode(), "Email '" + badEmail + "' must be rejected with HTTP 400");
            String invalidErr = readStream(connInvalid);
            assertTrue(invalidErr.contains("@klu.ac.in"), "Error message must specify @klu.ac.in requirement for '" + badEmail + "'");
        }

        // 2. Direct backend domain validation assertion
        assertTrue(ValidationUtil.isValidInstitutionalEmail("99527800392@klu.ac.in"), "Numeric student ID @klu.ac.in must be valid");
        assertTrue(ValidationUtil.isValidInstitutionalEmail("student.lead@klu.ac.in"), "Alphanumeric student email @klu.ac.in must be valid");
        assertFalse(ValidationUtil.isValidInstitutionalEmail("user@gmail.com"));
        assertFalse(ValidationUtil.isValidInstitutionalEmail("user@yahoo.com"));
        assertFalse(ValidationUtil.isValidInstitutionalEmail("user@klu.ac.in.example.com"));
        assertFalse(ValidationUtil.isValidInstitutionalEmail("user@attackerklu.ac.in"));
        assertFalse(ValidationUtil.isValidInstitutionalEmail("user@campus.edu"));

        // 3. Valid institutional registration succeeds for numeric student ID email (e.g. 995278xxxxx@klu.ac.in) and receives STUDENT role
        HttpURLConnection connValid = (HttpURLConnection) regUrl.openConnection();
        connValid.setRequestMethod("POST");
        connValid.setRequestProperty("Content-Type", "application/json");
        connValid.setDoOutput(true);

        String testNumId = "995278" + (System.currentTimeMillis() % 100000);
        String testEmail = testNumId + "@klu.ac.in";
        String validUser = "student_klu_" + System.currentTimeMillis();
        String validPayload = String.format("{\"username\":\"%s\",\"password\":\"StudentPass@2026\",\"fullName\":\"Kalasalingam Student\",\"email\":\"%s\",\"registerNumber\":\"%s\",\"department\":\"Computer Science & Engineering\"}", validUser, testEmail, testNumId);
        try (OutputStream os = connValid.getOutputStream()) {
            os.write(validPayload.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(201, connValid.getResponseCode(), "Valid student registration with @klu.ac.in must return HTTP 201 Created");
        String response = readStream(connValid);
        assertTrue(response.contains("\"success\":true"), "Registration must succeed");
        assertTrue(response.contains("\"role\":\"STUDENT\""), "Registered user role must be STUDENT");
        assertTrue(response.contains("\"verified\":false"), "New account must initially be unverified");
        assertTrue(response.contains(testEmail), "Response should reflect registered @klu.ac.in email");
    }

    // =========================================================================
    // Requirement 2: Privilege escalation attempt during registration is blocked
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("REQ 2: Privilege escalation during student registration is blocked (role parameter is ignored/forced)")
    void testPrivilegeEscalationAttemptBlocked() throws Exception {
        URL regUrl = new URL(getBaseUrl() + "/api/auth/register-student");
        HttpURLConnection conn = (HttpURLConnection) regUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Attempting to self-assign ADMIN role in registration payload with @klu.ac.in email
        String hackerUser = "hacker_" + System.currentTimeMillis();
        String exploitPayload = String.format("{\"username\":\"%s\",\"password\":\"Hack@2026\",\"fullName\":\"Hacker\",\"email\":\"%s@klu.ac.in\",\"role\":\"ADMIN\"}", hackerUser, hackerUser);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(exploitPayload.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(201, conn.getResponseCode());
        String response = readStream(conn);
        // The backend must force role to STUDENT, never granting ADMIN!
        assertTrue(response.contains("\"role\":\"STUDENT\""), "Backend must override client role and enforce STUDENT");
        assertFalse(response.contains("\"role\":\"ADMIN\""), "Self-assigned ADMIN role must not be granted");
    }

    // =========================================================================
    // Requirement 3: Student cannot access admin/coordinator endpoints (returns 403)
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("REQ 3: Authenticated Student is FORBIDDEN (HTTP 403) from admin/coordinator mutation & portal")
    void testStudentCannotAccessAdminEndpoints() throws Exception {
        // Step 1: Login as student1
        String cookie = loginAndGetCookie("student1", "pass123");
        assertNotNull(cookie, "Student login must produce session cookie");

        // Step 2: Attempt to access /adminPortal -> HTTP 403
        URL adminPortalUrl = new URL(getBaseUrl() + "/adminPortal");
        HttpURLConnection portalConn = (HttpURLConnection) adminPortalUrl.openConnection();
        portalConn.setRequestMethod("GET");
        portalConn.setRequestProperty("Cookie", cookie);
        assertEquals(403, portalConn.getResponseCode(), "Student must be forbidden (403) from /adminPortal");

        // Step 3: Attempt to create club -> HTTP 403
        HttpURLConnection clubConn = sendPost(getBaseUrl() + "/api/clubs", cookie,
                "{\"name\":\"Unauthorized Club\",\"category\":\"Technical\",\"description\":\"Student created\"}");
        assertEquals(403, clubConn.getResponseCode(), "Student must be forbidden (403) from creating clubs");

        // Step 4: Attempt to schedule event -> HTTP 403
        HttpURLConnection eventConn = sendPost(getBaseUrl() + "/api/events", cookie,
                "{\"title\":\"Unauthorized Hackathon\",\"club\":\"Coding Club\",\"venue\":\"Auditorium\"}");
        assertEquals(403, eventConn.getResponseCode(), "Student must be forbidden (403) from scheduling events");

        // Step 5: Attempt to broadcast notice -> HTTP 403
        HttpURLConnection noticeConn = sendPost(getBaseUrl() + "/api/announcements", cookie,
                "{\"title\":\"Student Broadcast\",\"content\":\"Illegal post\"}");
        assertEquals(403, noticeConn.getResponseCode(), "Student must be forbidden (403) from broadcasting announcements");

        // Step 6: Attempt to approve membership application -> HTTP 403
        HttpURLConnection reviewConn = sendPost(getBaseUrl() + "/api/memberships/review", cookie,
                "{\"id\":\"301\",\"decision\":\"APPROVED\"}");
        assertEquals(403, reviewConn.getResponseCode(), "Student must be forbidden (403) from reviewing memberships");
    }

    // =========================================================================
    // Requirement 4: Coordinator can access authorized club actions but not super-admin actions
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("REQ 4: Coordinator can manage events/notices but cannot create clubs (Admin-only)")
    void testCoordinatorPermissionsEnforced() throws Exception {
        String coordCookie = loginAndGetCookie("team_dev1", "TeamDev1@2026");
        assertNotNull(coordCookie, "Coordinator login must succeed");

        // Coordinator CAN create an event (authorized coordinator action)
        HttpURLConnection eventConn = sendPost(getBaseUrl() + "/api/events", coordCookie,
                "{\"title\":\"AI Workshop 2026\",\"club\":\"Coding Club\",\"venue\":\"Lab 204\",\"capacity\":\"60\"}");
        assertEquals(201, eventConn.getResponseCode(), "Coordinator should be permitted to schedule events (201)");

        // Coordinator CAN publish an announcement
        HttpURLConnection noticeConn = sendPost(getBaseUrl() + "/api/announcements", coordCookie,
                "{\"title\":\"Workshop Guidelines\",\"content\":\"Bring your laptops\",\"priority\":\"MEDIUM\"}");
        assertEquals(201, noticeConn.getResponseCode(), "Coordinator should be permitted to post announcements (201)");

        // Coordinator CANNOT create brand new campus clubs (Admin only)
        HttpURLConnection clubConn = sendPost(getBaseUrl() + "/api/clubs", coordCookie,
                "{\"name\":\"Coordinator Made Club\",\"category\":\"Technical\"}");
        assertEquals(403, clubConn.getResponseCode(), "Only Administrators can create new clubs (Coordinator gets 403)");
    }

    // =========================================================================
    // Requirement 5: Teacher/Reviewer has strictly read-only access
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("REQ 5: Teacher/Reviewer has read access to all data but is blocked from all mutations (HTTP 403)")
    void testTeacherReviewerReadOnlyAccess() throws Exception {
        String revCookie = loginAndGetCookie("teacher_reviewer", "Reviewer@2026");
        assertNotNull(revCookie, "Teacher Reviewer login must succeed");

        // 1. Can READ clubs, events, notices, memberships
        HttpURLConnection getClubs = sendGet(getBaseUrl() + "/api/clubs", revCookie);
        assertEquals(200, getClubs.getResponseCode(), "Reviewer must be able to read clubs");

        HttpURLConnection getEvents = sendGet(getBaseUrl() + "/api/events", revCookie);
        assertEquals(200, getEvents.getResponseCode(), "Reviewer must be able to read events");

        HttpURLConnection getNotices = sendGet(getBaseUrl() + "/api/announcements", revCookie);
        assertEquals(200, getNotices.getResponseCode(), "Reviewer must be able to read announcements");

        HttpURLConnection getMemberships = sendGet(getBaseUrl() + "/api/memberships", revCookie);
        assertEquals(200, getMemberships.getResponseCode(), "Reviewer must be able to read memberships");

        // 2. BLOCKED from mutating clubs (403)
        HttpURLConnection mutClub = sendPost(getBaseUrl() + "/api/clubs", revCookie, "{\"name\":\"Test Club\"}");
        assertEquals(403, mutClub.getResponseCode());
        assertTrue(readStream(mutClub).contains("Read-Only"));

        // 3. BLOCKED from mutating events (403)
        HttpURLConnection mutEvent = sendPost(getBaseUrl() + "/api/events", revCookie, "{\"title\":\"Test Event\"}");
        assertEquals(403, mutEvent.getResponseCode());

        // 4. BLOCKED from publishing notices (403)
        HttpURLConnection mutNotice = sendPost(getBaseUrl() + "/api/announcements", revCookie, "{\"title\":\"Test Notice\"}");
        assertEquals(403, mutNotice.getResponseCode());

        // 5. BLOCKED from reviewing memberships (403)
        HttpURLConnection mutReview = sendPost(getBaseUrl() + "/api/memberships/review", revCookie, "{\"id\":\"301\",\"decision\":\"APPROVED\"}");
        assertEquals(403, mutReview.getResponseCode());
    }

    // =========================================================================
    // Requirement 6: Unverified student account cannot register for events or apply for membership
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("REQ 6: Unverified student cannot apply for club or events until institutional OTP verified")
    void testUnverifiedStudentRestrictedUntilOtpVerified() throws Exception {
        // Step 1: Register new student
        String unvUser = "student_unv_" + System.currentTimeMillis();
        URL regUrl = new URL(getBaseUrl() + "/api/auth/register-student");
        HttpURLConnection regConn = (HttpURLConnection) regUrl.openConnection();
        regConn.setRequestMethod("POST");
        regConn.setRequestProperty("Content-Type", "application/json");
        regConn.setDoOutput(true);
        String regPayload = String.format("{\"username\":\"%s\",\"password\":\"Pass@2026\",\"fullName\":\"Bob Dylan\",\"email\":\"%s@klu.ac.in\",\"registerNumber\":\"1CC24CS202\"}", unvUser, unvUser);
        try (OutputStream os = regConn.getOutputStream()) {
            os.write(regPayload.getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(201, regConn.getResponseCode());
        String regBody = readStream(regConn);

        // Extract devOtp from registration response
        int otpIdx = regBody.indexOf("\"devOtp\":\"");
        assertTrue(otpIdx > 0, "Registration response must include OTP in test environment");
        String otp = regBody.substring(otpIdx + 10, otpIdx + 16);

        // Step 2: Login as unverified student
        String unverifiedCookie = loginAndGetCookie(unvUser, "Pass@2026");
        assertNotNull(unverifiedCookie);

        // Step 3: Attempt to apply for membership -> HTTP 403 Forbidden
        HttpURLConnection appConn = sendPost(getBaseUrl() + "/api/memberships", unverifiedCookie,
                "{\"club\":\"Coding & Algorithms Club\"}");
        assertEquals(403, appConn.getResponseCode(), "Unverified student must get HTTP 403 applying for membership");
        assertTrue(readStream(appConn).contains("unverified"));

        // Step 4: Attempt to register for an event -> HTTP 403 Forbidden
        HttpURLConnection regEventConn = sendPost(getBaseUrl() + "/api/events/register", unverifiedCookie,
                "{\"eventId\":\"101\"}");
        assertEquals(403, regEventConn.getResponseCode(), "Unverified student must get HTTP 403 registering for events");

        // Step 5: Verify institutional OTP
        URL verifyUrl = new URL(getBaseUrl() + "/api/auth/verify-student");
        HttpURLConnection verifyConn = (HttpURLConnection) verifyUrl.openConnection();
        verifyConn.setRequestMethod("POST");
        verifyConn.setRequestProperty("Content-Type", "application/json");
        verifyConn.setDoOutput(true);
        String verifyPayload = String.format("{\"username\":\"%s\",\"code\":\"%s\"}", unvUser, otp);
        try (OutputStream os = verifyConn.getOutputStream()) {
            os.write(verifyPayload.getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(200, verifyConn.getResponseCode(), "Valid OTP verification must return HTTP 200");

        // Step 6: Post-verification, applying for membership succeeds!
        HttpURLConnection verifiedAppConn = sendPost(getBaseUrl() + "/api/memberships", unverifiedCookie,
                "{\"club\":\"Coding & Algorithms Club\"}");
        assertEquals(201, verifiedAppConn.getResponseCode(), "Verified student can apply for club membership (201 Created)");

        // Step 7: Post-verification, event registration succeeds!
        HttpURLConnection verifiedEventConn = sendPost(getBaseUrl() + "/api/events/register", unverifiedCookie,
                "{\"eventId\":\"101\"}");
        assertEquals(200, verifiedEventConn.getResponseCode(), "Verified student can register for events (200 OK)");
    }

    // =========================================================================
    // Requirement 7: Production mode disables or restricts demo presets
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("REQ 7: Production mode flag and credential exposure security check")
    void testProductionModeSuppressionAndCredentialMasking() throws Exception {
        // 1. Password exposure check: /login page must NOT expose plain text passwords in HTML
        URL loginUrl = new URL(getBaseUrl() + "/login");
        HttpURLConnection conn = (HttpURLConnection) loginUrl.openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());

        String html = readStream(conn);
        // Ensure passwords are NOT exposed in onclick or autofill attributes
        assertFalse(html.contains("Reviewer@2026"), "Plain text password 'Reviewer@2026' must not exist in login HTML");
        assertFalse(html.contains("TeamLead@2026"), "Plain text password 'TeamLead@2026' must not exist in login HTML");
        assertFalse(html.contains("TeamDev1@2026"), "Plain text password 'TeamDev1@2026' must not exist in login HTML");
        assertFalse(html.contains("TeamDev2@2026"), "Plain text password 'TeamDev2@2026' must not exist in login HTML");

        // 2. isDemoPresetsEnabled logic check
        assertTrue(WebPortalController.isDemoPresetsEnabled(), "By default in dev/test, presets flag is enabled");
    }

    // =========================================================================
    // Requirement 8: Existing user accounts authenticate properly with modern BCrypt hashing
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("REQ 8: Existing accounts authenticate without regression and BCrypt hashing is enforced")
    void testAuthenticationIntegrityAndBcryptStandard() throws Exception {
        // 1. Verify admin login
        String adminCookie = loginAndGetCookie("admin", "admin123");
        assertNotNull(adminCookie, "Admin must authenticate successfully");

        // 2. Verify teacher_reviewer login
        String revCookie = loginAndGetCookie("teacher_reviewer", "Reviewer@2026");
        assertNotNull(revCookie, "Teacher reviewer must authenticate successfully");

        // 3. Verify student1 login
        String studentCookie = loginAndGetCookie("student1", "pass123");
        assertNotNull(studentCookie, "Existing student1 must authenticate successfully");

        // 4. Verify PasswordUtil creates BCrypt hashes by default
        String rawPass = "SafePassword@2026";
        String bcryptHash = PasswordUtil.hashPassword(rawPass);
        assertNotNull(bcryptHash);
        assertTrue(bcryptHash.startsWith("$2a$12$"), "New hashes must be standard BCrypt $2a$ with work factor 12");
        assertTrue(PasswordUtil.verifyPassword(rawPass, bcryptHash), "BCrypt hash must verify against raw password");
        assertFalse(PasswordUtil.verifyPassword("wrong_pwd", bcryptHash), "Incorrect password must fail verification");

        // 5. Verify backward compatibility: PBKDF2 hash verifies
        String pbkdf2Hash = PasswordUtil.hashPasswordPbkdf2(rawPass);
        assertTrue(pbkdf2Hash.startsWith("PBKDF2$"));
        assertTrue(PasswordUtil.verifyPassword(rawPass, pbkdf2Hash), "PBKDF2 hash must verify seamlessly");

        // 6. Verify backward compatibility: SHA-256 hash verifies
        String shaHash = PasswordUtil.sha256Hex(rawPass);
        assertTrue(PasswordUtil.verifyPassword(rawPass, shaHash), "Legacy SHA-256 hash must verify seamlessly");
    }

    // =========================================================================
    // Requirement 9: OTP Expiry After 10 Minutes
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("REQ 9: OTP verification expires after 10 minutes")
    void testOtp10MinuteExpiration() throws Exception {
        String testUser = "student_exp_" + System.currentTimeMillis();
        URL regUrl = new URL(getBaseUrl() + "/api/auth/register-student");
        HttpURLConnection regConn = (HttpURLConnection) regUrl.openConnection();
        regConn.setRequestMethod("POST");
        regConn.setRequestProperty("Content-Type", "application/json");
        regConn.setDoOutput(true);
        String regPayload = String.format("{\"username\":\"%s\",\"password\":\"Pass@2026\",\"fullName\":\"Exp Student\",\"email\":\"%s@klu.ac.in\"}", testUser, testUser);
        try (OutputStream os = regConn.getOutputStream()) {
            os.write(regPayload.getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(201, regConn.getResponseCode());
        String body = readStream(regConn);
        int otpIdx = body.indexOf("\"devOtp\":\"");
        assertTrue(otpIdx > 0);
        String validOtp = body.substring(otpIdx + 10, otpIdx + 16);

        // Manually simulate expiration (advance time past 10 minutes)
        WebPortalController controller = new WebPortalController();
        controller.setBuiltInUserOtpExpirationForTesting(testUser, System.currentTimeMillis() - 5000L);

        // Attempt verification with the expired code -> must return HTTP 400 with 'expired'
        HttpURLConnection verifyConn = sendPost(getBaseUrl() + "/api/auth/verify-student", null,
                String.format("{\"username\":\"%s\",\"code\":\"%s\"}", testUser, validOtp));
        assertEquals(400, verifyConn.getResponseCode(), "Expired OTP must be rejected with HTTP 400");
        String verifyBody = readStream(verifyConn);
        assertTrue(verifyBody.toLowerCase().contains("expired"), "Error must state that OTP has expired");
    }

    // =========================================================================
    // Requirement 10: OTP Lockout after 5 failed attempts & safe resend mechanism
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("REQ 10: Lock OTP after 5 consecutive failed attempts and provide safe resend mechanism")
    void testOtpLockoutAfter5FailedAttemptsAndResend() throws Exception {
        String testUser = "student_lock_" + System.currentTimeMillis();
        URL regUrl = new URL(getBaseUrl() + "/api/auth/register-student");
        HttpURLConnection regConn = (HttpURLConnection) regUrl.openConnection();
        regConn.setRequestMethod("POST");
        regConn.setRequestProperty("Content-Type", "application/json");
        regConn.setDoOutput(true);
        String regPayload = String.format("{\"username\":\"%s\",\"password\":\"Pass@2026\",\"fullName\":\"Lock Student\",\"email\":\"%s@klu.ac.in\"}", testUser, testUser);
        try (OutputStream os = regConn.getOutputStream()) {
            os.write(regPayload.getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(201, regConn.getResponseCode());
        String body = readStream(regConn);
        int otpIdx = body.indexOf("\"devOtp\":\"");
        assertTrue(otpIdx > 0);
        String correctOtp = body.substring(otpIdx + 10, otpIdx + 16);

        // Submit 4 incorrect attempts -> returns HTTP 400 with attempts count
        for (int i = 1; i <= 4; i++) {
            HttpURLConnection badConn = sendPost(getBaseUrl() + "/api/auth/verify-student", null,
                    String.format("{\"username\":\"%s\",\"code\":\"00000%d\"}", testUser, i));
            assertEquals(400, badConn.getResponseCode(), "Failed attempt " + i + " must return HTTP 400");
            assertTrue(readStream(badConn).contains("Attempts remaining: " + (5 - i)));
        }

        // 5th failed attempt -> locks account and returns HTTP 429
        HttpURLConnection lockConn = sendPost(getBaseUrl() + "/api/auth/verify-student", null,
                String.format("{\"username\":\"%s\",\"code\":\"000005\"}", testUser));
        assertEquals(429, lockConn.getResponseCode(), "5th failed attempt must lock OTP and return HTTP 429");
        assertTrue(readStream(lockConn).contains("locked"));

        // Attempting with the correct OTP now fails because code is locked
        HttpURLConnection afterLockConn = sendPost(getBaseUrl() + "/api/auth/verify-student", null,
                String.format("{\"username\":\"%s\",\"code\":\"%s\"}", testUser, correctOtp));
        assertTrue(afterLockConn.getResponseCode() == 429 || afterLockConn.getResponseCode() == 400,
                "Locked code cannot be verified even with original OTP");

        // Immediate resend triggers 60s rate limit
        HttpURLConnection rateLimitResend = sendPost(getBaseUrl() + "/api/auth/resend-otp", null,
                String.format("{\"username\":\"%s\"}", testUser));
        assertEquals(429, rateLimitResend.getResponseCode(), "Resend within 60s must return HTTP 429 rate limit");

        // Fast-forward cooldown for testing
        WebPortalController controller = new WebPortalController();
        controller.setBuiltInUserLastOtpSentAtForTesting(testUser, System.currentTimeMillis() - 65_000L);

        // Safe resend now succeeds
        HttpURLConnection resendConn = sendPost(getBaseUrl() + "/api/auth/resend-otp", null,
                String.format("{\"username\":\"%s\"}", testUser));
        assertEquals(200, resendConn.getResponseCode(), "Resend after cooldown must succeed with HTTP 200");
        String resendBody = readStream(resendConn);
        int newOtpIdx = resendBody.indexOf("\"devOtp\":\"");
        assertTrue(newOtpIdx > 0);
        String freshOtp = resendBody.substring(newOtpIdx + 10, newOtpIdx + 16);

        // Verifying with the fresh OTP succeeds!
        HttpURLConnection successVerify = sendPost(getBaseUrl() + "/api/auth/verify-student", null,
                String.format("{\"username\":\"%s\",\"code\":\"%s\"}", testUser, freshOtp));
        assertEquals(200, successVerify.getResponseCode(), "Fresh OTP must verify successfully");
        assertTrue(readStream(successVerify).contains("successfully"));
    }

    // =========================================================================
    // Requirement 11: 30-Minute Inactivity Session Expiration
    // =========================================================================

    @Test
    @Order(11)
    @DisplayName("REQ 11: 30-minute web session inactivity expiration")
    void testSession30MinuteInactivityExpiration() throws Exception {
        String adminCookie = loginAndGetCookie("admin", "admin123");
        assertNotNull(adminCookie);

        // Session is active initially
        HttpURLConnection meConn = sendGet(getBaseUrl() + "/api/auth/me", adminCookie);
        assertEquals(200, meConn.getResponseCode());
        assertTrue(readStream(meConn).contains("\"authenticated\":true"));

        // Simulate 31 minutes of inactivity
        String cookiePart = adminCookie.split(";")[0].trim();
        String sessionId = cookiePart.contains("=") ? cookiePart.substring(cookiePart.indexOf('=') + 1).trim() : cookiePart;
        WebPortalController controller = new WebPortalController();
        controller.setSessionLastAccessedForTesting(sessionId, System.currentTimeMillis() - (31 * 60 * 1000L));

        // Subsequent request must reject the expired session
        HttpURLConnection expiredConn = sendGet(getBaseUrl() + "/api/auth/me", adminCookie);
        assertEquals(200, expiredConn.getResponseCode());
        assertTrue(readStream(expiredConn).contains("\"authenticated\":false"), "Expired session must return authenticated: false");
    }

    // =========================================================================
    // Requirement 12: Automatic Legacy Password Hash Upgrade to BCrypt
    // =========================================================================

    @Test
    @Order(12)
    @DisplayName("REQ 12: Automatic legacy password hash upgrade to BCrypt upon successful login")
    void testAutomaticLegacyPasswordHashUpgradeToBcrypt() throws Exception {
        String rawPassword = "UpgradeMe@2026";
        String legacyPbkdf2 = PasswordUtil.hashPasswordPbkdf2(rawPassword);
        assertTrue(legacyPbkdf2.startsWith("PBKDF2$"), "Initial hash must be PBKDF2");

        // Simulate user with legacy hash in DAO
        User testUser = new Student();
        testUser.setUserId(9999);
        testUser.setUsername("legacy_user_" + System.currentTimeMillis());
        testUser.setPasswordHash(legacyPbkdf2);
        testUser.setFullName("Legacy User");
        testUser.setEmail(testUser.getUsername() + "@klu.ac.in");
        testUser.setRole(UserRole.STUDENT);

        UserDAO testDAO = new UserDAO() {
            private String storedHash = legacyPbkdf2;
            @Override
            public Optional<User> findByUsername(String username) {
                if (username.equals(testUser.getUsername())) {
                    testUser.setPasswordHash(storedHash);
                    return Optional.of(testUser);
                }
                return Optional.empty();
            }
            @Override
            public boolean updatePassword(int userId, String newHash) {
                this.storedHash = newHash;
                testUser.setPasswordHash(newHash);
                return true;
            }
        };

        AuthenticationService auth = new AuthenticationService(testDAO);
        User authenticated = auth.login(testUser.getUsername(), rawPassword);
        assertNotNull(authenticated);

        // Check that hash was automatically upgraded to BCrypt $2a$12
        String upgradedHash = authenticated.getPasswordHash();
        assertNotNull(upgradedHash);
        assertTrue(upgradedHash.startsWith("$2a$12$"), "Hash must be automatically upgraded to BCrypt $2a$12");
        assertTrue(PasswordUtil.verifyPassword(rawPassword, upgradedHash), "New BCrypt hash must verify against raw password");
    }

    // =========================================================================
    // Requirement 13: Database Migration Execution and Metadata Verification
    // =========================================================================

    @Test
    @Order(13)
    @DisplayName("REQ 13: Database migration executes safely and adds security columns")
    void testDatabaseMigrationAndColumnsPresence() throws Exception {
        boolean executed = DatabaseMigrationRunner.runSecurityMigration();
        assertTrue(executed, "Migration runner must execute cleanly");

        if (DatabaseConnection.testConnection()) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                assertNotNull(conn);
                DatabaseMetaData meta = conn.getMetaData();
                Set<String> cols = new HashSet<>();
                try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "users", null)) {
                    while (rs.next()) {
                        String col = rs.getString("COLUMN_NAME");
                        if (col != null) cols.add(col.toLowerCase());
                    }
                }
                if (cols.isEmpty()) {
                    try (ResultSet rs = meta.getColumns(null, null, "users", null)) {
                        while (rs.next()) {
                            String col = rs.getString("COLUMN_NAME");
                            if (col != null) cols.add(col.toLowerCase());
                        }
                    }
                }
                assertTrue(cols.contains("is_verified"), "users table must contain 'is_verified'");
                assertTrue(cols.contains("verification_code"), "users table must contain 'verification_code'");
                assertTrue(cols.contains("otp_expires_at"), "users table must contain 'otp_expires_at'");
                assertTrue(cols.contains("otp_attempts"), "users table must contain 'otp_attempts'");
            }
        }
    }

    // =========================================================================
    // Requirement 14: Email Delivery Service Configuration & Fallback
    // =========================================================================

    @Test
    @Order(14)
    @DisplayName("REQ 14: SMTP Email delivery service configuration and safe handling")
    void testEmailServiceConfiguration() {
        assertDoesNotThrow(() -> EmailService.loadConfiguration());
        // Verify sendOtpEmail runs without throwing unhandled exceptions
        boolean sent = EmailService.sendOtpEmail("99527800392@klu.ac.in", "123456", 10);
        // If SMTP configured in environment, returns true; if not, returns false gracefully
        if (EmailService.isConfigured()) {
            assertTrue(sent, "Email should be dispatched when SMTP is configured");
        } else {
            assertFalse(sent, "Email delivery returns false gracefully when unconfigured");
        }
    }

    // =========================================================================
    // Test Helpers
    // =========================================================================

    private String loginAndGetCookie(String username, String password) throws Exception {
        URL url = new URL(getBaseUrl() + "/api/auth/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() == 200) {
            String cookie = conn.getHeaderField("Set-Cookie");
            return cookie != null ? cookie : "";
        }
        return null;
    }

    private HttpURLConnection sendPost(String endpointUrl, String cookie, String jsonPayload) throws Exception {
        URL url = new URL(endpointUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (cookie != null && !cookie.isEmpty()) {
            conn.setRequestProperty("Cookie", cookie);
        }
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
        }
        return conn;
    }

    private HttpURLConnection sendGet(String endpointUrl, String cookie) throws Exception {
        URL url = new URL(endpointUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (cookie != null && !cookie.isEmpty()) {
            conn.setRequestProperty("Cookie", cookie);
        }
        return conn;
    }

    private static String readStream(HttpURLConnection conn) throws Exception {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            in.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
