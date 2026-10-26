package com.campusclub.app;

import com.campusclub.gui.LoginFrame;

import javax.swing.*;

/**
 * Main application entry point for the Campus Club Management System.
 * Starts the Java Swing Desktop GUI.
 */
public class CampusClubApplication {

    public static void main(String[] args) {
        // Set modern Look & Feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        // Start background Web Server for /teacherDashboard route
        new Thread(() -> {
            try {
                int port = com.campusclub.web.TeacherDashboardWebServer.startServerIfNotRunning();
                System.out.println("[CCMS] Teacher Dashboard web route active at http://localhost:" + port + "/teacherDashboard");
            } catch (Exception e) {
                System.err.println("[CCMS] Notice: Could not start web route: " + e.getMessage());
            }
        }, "CCMS-WebServer-Thread").start();

        // Launch the Login UI on Event Dispatch Thread (EDT) only if display environment is present
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
        } else {
            System.out.println("[CCMS] Headless environment detected: Running in standalone Web Server mode.");
        }
    }
}
