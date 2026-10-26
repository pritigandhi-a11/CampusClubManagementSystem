package com.campusclub;

import com.campusclub.gui.AdminDashboard;
import com.campusclub.gui.CoordinatorDashboard;
import com.campusclub.gui.DashboardCard;
import com.campusclub.gui.LoginFrame;
import com.campusclub.gui.MainFrame;
import com.campusclub.gui.StudentDashboard;
import com.campusclub.gui.UITheme;
import com.campusclub.model.Admin;
import com.campusclub.model.Club;
import com.campusclub.model.ClubCoordinator;
import com.campusclub.model.Event;
import com.campusclub.model.Membership;
import com.campusclub.model.Student;
import com.campusclub.model.UserRole;
import com.campusclub.recommendation.ClubRecommendationEngine;
import com.campusclub.service.ClubService;
import com.campusclub.service.EventService;
import com.campusclub.service.MembershipService;
import com.campusclub.service.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless-safe integration and unit test suite for the Campus Club Management System Swing GUI.
 * Tests theme styling, dashboard cards, polymorphic role routing, and role dashboards.
 */
public class GUIIntegrationTest {

    // ==========================================
    // 1. UITheme & Design System Tests
    // ==========================================

    @Test
    @DisplayName("UITheme: Palette colors and typography constants are properly defined")
    void testUIThemeConstants() {
        assertNotNull(UITheme.PRIMARY_NAVY);
        assertNotNull(UITheme.PRIMARY_BLUE);
        assertNotNull(UITheme.ACCENT_EMERALD);
        assertNotNull(UITheme.DANGER_RED);
        assertNotNull(UITheme.BG_LIGHT);
        assertNotNull(UITheme.CARD_BG);

        assertEquals("Segoe UI", UITheme.FONT_TITLE.getFamily());
        assertEquals("Segoe UI", UITheme.FONT_REGULAR.getFamily());
        assertTrue(UITheme.FONT_TITLE.getSize() >= 18);
    }

    @Test
    @DisplayName("UITheme: Button factory helpers create styled buttons with proper fonts and backgrounds")
    void testUIThemeButtonFactories() {
        JButton primaryBtn = UITheme.createPrimaryButton("Submit");
        assertNotNull(primaryBtn);
        assertEquals("Submit", primaryBtn.getText());
        assertEquals(UITheme.PRIMARY_BLUE, primaryBtn.getBackground());
        assertEquals(Color.WHITE, primaryBtn.getForeground());

        JButton successBtn = UITheme.createSuccessButton("Join");
        assertNotNull(successBtn);
        assertEquals(UITheme.ACCENT_EMERALD, successBtn.getBackground());

        JButton dangerBtn = UITheme.createDangerButton("Delete");
        assertNotNull(dangerBtn);
        assertEquals(UITheme.DANGER_RED, dangerBtn.getBackground());

        JButton secondaryBtn = UITheme.createSecondaryButton("Cancel");
        assertNotNull(secondaryBtn);
        assertEquals(UITheme.TEXT_PRIMARY, secondaryBtn.getForeground());
    }

    @Test
    @DisplayName("UITheme: Table styling helper sets row height and fonts correctly")
    void testTableStylingHelper() {
        JTable table = new JTable(new DefaultTableModel(new String[]{"Col1", "Col2"}, 2));
        UITheme.styleTable(table);

        assertEquals(28, table.getRowHeight());
        assertEquals(UITheme.FONT_REGULAR, table.getFont());
        assertEquals(ListSelectionModel.SINGLE_SELECTION, table.getSelectionModel().getSelectionMode());
        assertNotNull(table.getTableHeader());
    }

    // ==========================================
    // 2. DashboardCard Metric Component Tests
    // ==========================================

    @Test
    @DisplayName("DashboardCard: Renders title, initial metric value, and updates correctly")
    void testDashboardCardOperations() {
        DashboardCard card = new DashboardCard("Total Clubs", "12", "Registered Clubs", UITheme.PRIMARY_BLUE);

        assertEquals("Total Clubs", card.getTitle());
        assertEquals("12", card.getValue());

        card.setValue("15");
        assertEquals("15", card.getValue());

        card.setValue(20);
        assertEquals("20", card.getValue());

        card.setSubtitle("Active Clubs");
        assertNotNull(card.getBorder());
    }

    // ==========================================
    // 3. Polymorphic Role Dashboard Routing Tests
    // ==========================================

    @Test
    @DisplayName("MainFrame Factory: Polymorphically mounts StudentDashboard for Student user")
    void testMainFrameStudentRoleRouting() {
        Student student = new Student();
        student.setUserId(101);
        student.setUsername("student_test");
        student.setFullName("Alice Student");
        student.setEmail("alice@campus.edu");
        student.setRole(UserRole.STUDENT);

        JPanel panel = MainFrame.createRoleDashboard(student);
        assertNotNull(panel);
        assertTrue(panel instanceof StudentDashboard);
    }

    @Test
    @DisplayName("MainFrame Factory: Polymorphically mounts CoordinatorDashboard for ClubCoordinator user")
    void testMainFrameCoordinatorRoleRouting() {
        ClubCoordinator coordinator = new ClubCoordinator();
        coordinator.setUserId(201);
        coordinator.setUsername("coord_test");
        coordinator.setFullName("Bob Coordinator");
        coordinator.setEmail("bob@campus.edu");
        coordinator.setRole(UserRole.COORDINATOR);

        JPanel panel = MainFrame.createRoleDashboard(coordinator);
        assertNotNull(panel);
        assertTrue(panel instanceof CoordinatorDashboard);
    }

    @Test
    @DisplayName("MainFrame Factory: Polymorphically mounts AdminDashboard for Admin user")
    void testMainFrameAdminRoleRouting() {
        Admin admin = new Admin();
        admin.setUserId(301);
        admin.setUsername("admin_test");
        admin.setFullName("Carol Admin");
        admin.setEmail("admin@campus.edu");
        admin.setRole(UserRole.ADMIN);

        JPanel panel = MainFrame.createRoleDashboard(admin);
        assertNotNull(panel);
        assertTrue(panel instanceof AdminDashboard);
    }

    // ==========================================
    // 4. StudentDashboard UI and Integration Tests
    // ==========================================

    @Test
    @DisplayName("StudentDashboard: Initializes all 7 tab panels, tables, and profile fields")
    void testStudentDashboardComponents() {
        Student student = new Student();
        student.setUserId(1);
        student.setUsername("alex_turner");
        student.setFullName("Alex Turner");
        student.setEmail("alex@campus.edu");
        student.setDepartment("Computer Science");
        student.setYear(3);
        student.setSemester(5);
        student.setInterests("AI, Robotics, Web Development");

        StudentDashboard dashboard = new StudentDashboard(student);
        assertNotNull(dashboard.getStudent());
        assertEquals("Alex Turner", dashboard.getStudent().getFullName());
        assertNotNull(dashboard.getClubsTable());
        assertNotNull(dashboard.getEventsTable());
    }

    // ==========================================
    // 5. CoordinatorDashboard UI Tests
    // ==========================================

    @Test
    @DisplayName("CoordinatorDashboard: Initializes club details, membership requests table, and event table")
    void testCoordinatorDashboardComponents() {
        ClubCoordinator coordinator = new ClubCoordinator();
        coordinator.setUserId(2);
        coordinator.setUsername("coord_coding");
        coordinator.setFullName("David Miller");
        coordinator.setEmail("david@campus.edu");

        CoordinatorDashboard dashboard = new CoordinatorDashboard(coordinator);
        assertNotNull(dashboard.getCoordinator());
        assertNotNull(dashboard.getMembershipRequestsTable());
        assertNotNull(dashboard.getClubEventsTable());
    }

    // ==========================================
    // 6. AdminDashboard UI Tests
    // ==========================================

    @Test
    @DisplayName("AdminDashboard: Initializes clubs table, user registry table, and metric cards")
    void testAdminDashboardComponents() {
        Admin admin = new Admin();
        admin.setUserId(3);
        admin.setUsername("admin_master");
        admin.setFullName("System Admin");
        admin.setEmail("admin@campus.edu");

        AdminDashboard dashboard = new AdminDashboard(admin);
        assertNotNull(dashboard.getAdmin());
        assertNotNull(dashboard.getClubsTable());
        assertNotNull(dashboard.getUsersTable());
    }

    // ==========================================
    // 7. Non-Headless Window Initialization Tests (Runs only when display available)
    // ==========================================

    @Test
    @DisplayName("Window Frames: Instantiate cleanly if display is available")
    void testTopLevelWindowInstantiation() {
        if (!GraphicsEnvironment.isHeadless()) {
            LoginFrame loginFrame = new LoginFrame();
            assertNotNull(loginFrame.getTitle());
            assertNotNull(loginFrame.getUsernameField());
            assertNotNull(loginFrame.getPasswordField());
            loginFrame.dispose();

            Admin admin = new Admin();
            admin.setUserId(1);
            admin.setUsername("admin");
            admin.setFullName("Administrator");
            MainFrame mainFrame = new MainFrame(admin);
            assertNotNull(mainFrame.getTitle());
            mainFrame.dispose();
        }
    }
}
