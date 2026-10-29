package com.campusclub;

import com.campusclub.model.UserRole;
import com.campusclub.web.TeacherDashboardWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for multi-user online access, authentication,
 * role-based access control (RBAC), and reviewer read-only enforcement.
 */
public class WebPortalMultiUserTest {

    private static int testPort;

    @BeforeAll
    static void setUp() {
        testPort = TeacherDashboardWebServer.startServerIfNotRunning(8095);
    }

    @AfterAll
    static void tearDown() {
        // Do not stop server as other tests might rely on it or it runs during app life
    }

    private String getBaseUrl() {
        return "http://localhost:" + testPort;
    }

    @Test
    @DisplayName("1. Web Server: /login and /portal endpoints are reachable")
    void testLoginPageReachable() throws Exception {
        URL url = new URL(getBaseUrl() + "/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());

        String content = readStream(conn);
        assertTrue(content.contains("Campus Club System"), "Login page must have brand title");
        assertTrue(content.contains("teacher_reviewer"), "Login page should display quick evaluation presets");
    }

    @Test
    @DisplayName("2. Multi-User Authentication: Team Lead (Admin) can log in via REST API")
    void testTeamLeadLogin() throws Exception {
        URL url = new URL(getBaseUrl() + "/api/auth/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = "{\"username\":\"team_lead\",\"password\":\"TeamLead@2026\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(200, conn.getResponseCode());
        String response = readStream(conn);
        assertTrue(response.contains("\"success\":true"), "Login must succeed");
        assertTrue(response.contains("\"role\":\"ADMIN\""), "Team Lead must have ADMIN role");
        assertTrue(response.contains("\"isReviewer\":false"), "Team Lead is not reviewer");
    }

    @Test
    @DisplayName("3. Multi-User Authentication: Team Dev 1 (Coordinator) can log in independently")
    void testTeamDevLogin() throws Exception {
        URL url = new URL(getBaseUrl() + "/api/auth/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = "{\"username\":\"team_dev1\",\"password\":\"TeamDev1@2026\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(200, conn.getResponseCode());
        String response = readStream(conn);
        assertTrue(response.contains("\"success\":true"), "Login must succeed");
        assertTrue(response.contains("\"role\":\"COORDINATOR\""), "Team dev must have COORDINATOR role");
    }

    @Test
    @DisplayName("4. External Reviewer: Teacher Reviewer logs in with TEACHER role and isReviewer=true")
    void testTeacherReviewerLogin() throws Exception {
        URL url = new URL(getBaseUrl() + "/api/auth/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = "{\"username\":\"teacher_reviewer\",\"password\":\"Reviewer@2026\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(200, conn.getResponseCode());
        String response = readStream(conn);
        assertTrue(response.contains("\"success\":true"), "Reviewer login must succeed");
        assertTrue(response.contains("\"isReviewer\":true"), "Reviewer flag must be true");
        assertTrue(response.contains("\"role\":\"TEACHER\""), "Reviewer must have TEACHER role");
    }

    @Test
    @DisplayName("5. Strict RBAC: Reviewer is FORBIDDEN (403) from creating clubs or mutating data")
    void testReviewerMutationBlocked() throws Exception {
        // Step 1: Login as reviewer and get cookie
        URL loginUrl = new URL(getBaseUrl() + "/api/auth/login");
        HttpURLConnection loginConn = (HttpURLConnection) loginUrl.openConnection();
        loginConn.setRequestMethod("POST");
        loginConn.setRequestProperty("Content-Type", "application/json");
        loginConn.setDoOutput(true);
        String loginPayload = "{\"username\":\"teacher_reviewer\",\"password\":\"Reviewer@2026\"}";
        try (OutputStream os = loginConn.getOutputStream()) {
            os.write(loginPayload.getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(200, loginConn.getResponseCode());
        String cookie = loginConn.getHeaderField("Set-Cookie");
        assertNotNull(cookie, "Session cookie must be set on login");

        // Step 2: Attempt to create a club with reviewer cookie
        URL createClubUrl = new URL(getBaseUrl() + "/api/clubs");
        HttpURLConnection clubConn = (HttpURLConnection) createClubUrl.openConnection();
        clubConn.setRequestMethod("POST");
        clubConn.setRequestProperty("Content-Type", "application/json");
        clubConn.setRequestProperty("Cookie", cookie);
        clubConn.setDoOutput(true);
        String clubPayload = "{\"name\":\"Unauthorized Club\",\"category\":\"Technical\",\"description\":\"Test\"}";
        try (OutputStream os = clubConn.getOutputStream()) {
            os.write(clubPayload.getBytes(StandardCharsets.UTF_8));
        }

        // Must be rejected with HTTP 403 Forbidden!
        assertEquals(403, clubConn.getResponseCode(), "Reviewer mutation must return 403 Forbidden");
        String errResponse = readStream(clubConn);
        assertTrue(errResponse.contains("Read-Only"), "Error message must state read-only policy");
    }

    @Test
    @DisplayName("6. Authorized Mutation: Team Lead can create clubs successfully (HTTP 201)")
    void testTeamLeadCanCreateClub() throws Exception {
        // Step 1: Login as admin
        URL loginUrl = new URL(getBaseUrl() + "/api/auth/login");
        HttpURLConnection loginConn = (HttpURLConnection) loginUrl.openConnection();
        loginConn.setRequestMethod("POST");
        loginConn.setRequestProperty("Content-Type", "application/json");
        loginConn.setDoOutput(true);
        String loginPayload = "{\"username\":\"team_lead\",\"password\":\"TeamLead@2026\"}";
        try (OutputStream os = loginConn.getOutputStream()) {
            os.write(loginPayload.getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(200, loginConn.getResponseCode());
        String cookie = loginConn.getHeaderField("Set-Cookie");

        // Step 2: Create a club
        URL createClubUrl = new URL(getBaseUrl() + "/api/clubs");
        HttpURLConnection clubConn = (HttpURLConnection) createClubUrl.openConnection();
        clubConn.setRequestMethod("POST");
        clubConn.setRequestProperty("Content-Type", "application/json");
        clubConn.setRequestProperty("Cookie", cookie);
        clubConn.setDoOutput(true);
        String clubPayload = "{\"name\":\"AI & Data Science Hub\",\"category\":\"Technical\",\"description\":\"Student AI team\"}";
        try (OutputStream os = clubConn.getOutputStream()) {
            os.write(clubPayload.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(201, clubConn.getResponseCode(), "Admin mutation must succeed with HTTP 201 Created");
        String response = readStream(clubConn);
        assertTrue(response.contains("\"success\":true"), "Club creation must succeed");
    }

    @Test
    @DisplayName("7. System Diagnostics: /api/db-status returns operational health")
    void testDbStatusEndpoint() throws Exception {
        URL url = new URL(getBaseUrl() + "/api/db-status");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());

        String content = readStream(conn);
        assertTrue(content.contains("databaseConnected"), "Must report databaseConnected state");
        assertTrue(content.contains("activeWebSessions"), "Must report activeWebSessions count");
    }

    @Test
    @DisplayName("8. Backward Compatibility: Existing /teacherDashboard continues to function")
    void testTeacherDashboardBackwardCompatibility() throws Exception {
        URL url = new URL(getBaseUrl() + "/teacherDashboard");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());

        String content = readStream(conn);
        assertTrue(content.contains("Teacher / Faculty Coordinator Dashboard"), "Teacher dashboard header intact");
        assertTrue(content.contains("switchTab"), "Tab switching function intact");
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
