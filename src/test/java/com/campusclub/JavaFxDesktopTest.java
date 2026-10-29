package com.campusclub;

import com.campusclub.javafx.*;
import com.campusclub.model.Admin;
import com.campusclub.model.ClubCoordinator;
import com.campusclub.model.Student;
import com.campusclub.model.UserRole;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit & Integration tests for JavaFX Desktop Components, Styling,
 * Polymorphic Navigation, and Resource Integrity.
 */
public class JavaFxDesktopTest {

    private static boolean fxToolkitAvailable = false;

    @BeforeAll
    static void initJavaFx() {
        try {
            System.setProperty("prism.order", "sw"); // Software pipeline for headless / CI safety
            Platform.startup(() -> {});
            fxToolkitAvailable = true;
        } catch (IllegalStateException e) {
            // Toolkit already started
            fxToolkitAvailable = true;
        } catch (Throwable t) {
            System.out.println("[JavaFxDesktopTest] Notice: JavaFX toolkit running in mock/restricted environment: " + t.getMessage());
            fxToolkitAvailable = false;
        }
    }

    @Test
    @DisplayName("Verify JavaFX Application Entrypoint Class Hierarchy")
    void testAppHierarchy() {
        assertTrue(javafx.application.Application.class.isAssignableFrom(CampusClubFxApp.class),
                "CampusClubFxApp must extend javafx.application.Application");
    }

    @Test
    @DisplayName("Verify /campusclub.css Exists and Contains Core CCMS Design Rules")
    void testCssStylesheetResource() throws Exception {
        URL cssUrl = getClass().getResource("/campusclub.css");
        assertNotNull(cssUrl, "Resource /campusclub.css must be available on classpath");

        try (InputStream is = getClass().getResourceAsStream("/campusclub.css")) {
            assertNotNull(is, "Cannot open stream to /campusclub.css");
            byte[] bytes = is.readAllBytes();
            String css = new String(bytes);

            assertTrue(css.contains(".kpi-card"), "Stylesheet must define .kpi-card");
            assertTrue(css.contains(".btn-primary"), "Stylesheet must define .btn-primary");
            assertTrue(css.contains(".table-view"), "Stylesheet must define .table-view");
            assertTrue(css.contains(".top-bar"), "Stylesheet must define .top-bar");
            assertTrue(css.contains(".role-pill"), "Stylesheet must define .role-pill");
        }
    }

    @Test
    @DisplayName("Verify FxUITheme Role Pill Generation")
    void testRolePills() {
        if (!fxToolkitAvailable) return;

        assertNotNull(FxUITheme.createRolePill(UserRole.ADMIN));
        assertNotNull(FxUITheme.createRolePill(UserRole.COORDINATOR));
        assertNotNull(FxUITheme.createRolePill(UserRole.TEACHER));
        assertNotNull(FxUITheme.createRolePill(UserRole.STUDENT));
    }

    @Test
    @DisplayName("Verify FxUITheme Button Creators")
    void testButtonStyling() {
        if (!fxToolkitAvailable) return;

        var primary = FxUITheme.createPrimaryButton("Action");
        assertTrue(primary.getStyleClass().contains("btn-primary"));

        var danger = FxUITheme.createDangerButton("Delete");
        assertTrue(danger.getStyleClass().contains("btn-danger"));

        var success = FxUITheme.createSuccessButton("Save");
        assertTrue(success.getStyleClass().contains("btn-success"));
    }

    @Test
    @DisplayName("Verify Polymorphic Role Dashboard Resolution in FxMainView")
    void testPolymorphicDashboardRouting() {
        if (!fxToolkitAvailable) return;

        Admin admin = new Admin();
        admin.setUserId(1);
        admin.setFullName("Admin Test");
        var adminNode = FxMainView.createRoleDashboard(admin);
        assertTrue(adminNode instanceof FxAdminDashboard, "Admin role must resolve to FxAdminDashboard");

        ClubCoordinator coord = new ClubCoordinator();
        coord.setUserId(2);
        coord.setFullName("Coord Test");
        coord.setRole(UserRole.COORDINATOR);
        var coordNode = FxMainView.createRoleDashboard(coord);
        assertTrue(coordNode instanceof FxCoordinatorDashboard, "Coordinator role must resolve to FxCoordinatorDashboard");

        Student student = new Student();
        student.setUserId(3);
        student.setFullName("Student Test");
        var studentNode = FxMainView.createRoleDashboard(student);
        assertTrue(studentNode instanceof FxStudentDashboard, "Student role must resolve to FxStudentDashboard");
    }

    @Test
    @DisplayName("Verify Quick Preset Roles on Login View")
    void testLoginViewInit() {
        if (!fxToolkitAvailable) return;

        FxLoginView loginView = new FxLoginView(user -> {});
        assertNotNull(loginView);
    }
}
