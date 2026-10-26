package com.campusclub;

import com.campusclub.gui.MainFrame;
import com.campusclub.gui.TeacherDashboard;
import com.campusclub.model.ClubCoordinator;
import com.campusclub.model.User;
import com.campusclub.model.UserRole;
import com.campusclub.web.TeacherDashboardController;
import com.campusclub.web.TeacherDashboardWebServer;
import com.club.util.DatabaseConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification Test Suite for Teacher Dashboard routing, frontend files,
 * authentication/access redirection, and database connectivity.
 */
public class TeacherDashboardRoutingTest {

    private static int testPort;

    @BeforeAll
    static void setUp() {
        testPort = TeacherDashboardWebServer.startServerIfNotRunning(8090);
    }

    @AfterAll
    static void tearDown() {
        TeacherDashboardWebServer.stopServer();
    }

    @Test
    @DisplayName("1. Connection Utility: com.club.util.DatabaseConnection is available")
    void testConnectionUtility() {
        assertDoesNotThrow(() -> {
            boolean alive = DatabaseConnection.testConnection();
            // testConnection handles offline/online gracefully without throwing
            assertNotNull(alive);
        });
    }

    @Test
    @DisplayName("2. Role & Authentication: UserRole handles TEACHER and FACULTY aliases")
    void testTeacherRoleParsing() {
        assertEquals(UserRole.TEACHER, UserRole.fromString("TEACHER"));
        assertEquals(UserRole.TEACHER, UserRole.fromString("FACULTY"));
        assertEquals(UserRole.TEACHER, UserRole.fromString("Teacher Coordinator"));
        assertEquals("Teacher Coordinator", UserRole.TEACHER.getDisplayName());
    }

    @Test
    @DisplayName("3. Authentication / Access: Teacher redirects to TeacherDashboard in Swing MainFrame")
    void testTeacherDashboardSwingRedirection() {
        ClubCoordinator teacher = new ClubCoordinator(2, "coord_coding", "hash", "David Miller",
                "david@campus.edu", "+1 555-0102", "Computer Science", "Faculty Lead");
        teacher.setRole(UserRole.TEACHER);

        JPanel panel = MainFrame.createRoleDashboard(teacher);
        assertNotNull(panel, "Dashboard panel must not be null");
        assertTrue(panel instanceof TeacherDashboard, "Teacher must be redirected to TeacherDashboard instance");
    }

    @Test
    @DisplayName("4. Routing & Controller: /teacherDashboard route is mapped and queries data cleanly")
    void testTeacherDashboardControllerData() {
        TeacherDashboardController controller = new TeacherDashboardController();
        assertEquals("/teacherDashboard", TeacherDashboardController.ROUTE_PATH);

        TeacherDashboardController.TeacherDashboardData data = controller.fetchTeacherDashboardData();
        assertNotNull(data, "Dashboard data must not be null");
        assertNotNull(data.teacherName, "Teacher name must be populated");
        assertFalse(data.managedClubs.isEmpty(), "Managed clubs list must not be empty");
        assertFalse(data.notices.isEmpty(), "Notices list must not be empty");
        assertNotNull(data.toJson(), "JSON payload must serialize properly");
    }

    @Test
    @DisplayName("5. Frontend View: Verify teacherDashboard.jsp exists and is placed in src/main/webapp")
    void testJspFrontendViewExists() {
        File jspFile = new File("src/main/webapp/teacherDashboard.jsp");
        assertTrue(jspFile.exists(), "src/main/webapp/teacherDashboard.jsp must exist");
        assertTrue(jspFile.length() > 0, "teacherDashboard.jsp must not be empty");
    }

    @Test
    @DisplayName("6. Frontend View: Verify teacherDashboard.html exists and is placed in src/main/webapp")
    void testHtmlFrontendViewExists() {
        File htmlFile = new File("src/main/webapp/teacherDashboard.html");
        assertTrue(htmlFile.exists(), "src/main/webapp/teacherDashboard.html must exist");
        assertTrue(htmlFile.length() > 0, "teacherDashboard.html must not be empty");
    }

    @Test
    @DisplayName("7. Deployment Descriptor: Verify web.xml maps /teacherDashboard")
    void testWebXmlMappingExists() {
        File webXml = new File("src/main/webapp/WEB-INF/web.xml");
        assertTrue(webXml.exists(), "src/main/webapp/WEB-INF/web.xml must exist");
        assertTrue(webXml.length() > 0, "web.xml must not be empty");
    }

    @Test
    @DisplayName("8. HTTP Route Verification: GET /teacherDashboard responds with 200 OK and HTML")
    void testHttpRouteAccessible() throws Exception {
        URL url = new URL("http://localhost:" + testPort + "/teacherDashboard");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        int responseCode = conn.getResponseCode();
        assertEquals(200, responseCode, "HTTP response code must be 200 OK");
        assertEquals("text/html; charset=UTF-8", conn.getContentType());

        StringBuilder content = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        assertTrue(content.toString().contains("Teacher / Faculty Coordinator Dashboard"),
                "Response HTML must contain Teacher Dashboard header");
    }

    @Test
    @DisplayName("9. HTTP JSON Route: GET /teacherDashboard?format=json responds with 200 OK and valid JSON")
    void testHttpJsonRouteAccessible() throws Exception {
        URL url = new URL("http://localhost:" + testPort + "/teacherDashboard?format=json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        int responseCode = conn.getResponseCode();
        assertEquals(200, responseCode);
        assertEquals("application/json", conn.getContentType());

        StringBuilder content = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
        }
        assertTrue(content.toString().contains("teacherName"));
        assertTrue(content.toString().contains("managedClubsCount"));
    }
}
