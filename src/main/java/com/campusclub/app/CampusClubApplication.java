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

        // Start Web Server for /teacherDashboard route
        try {
            int port = com.campusclub.web.TeacherDashboardWebServer.startServerIfNotRunning();
            System.out.println("[CCMS] Teacher Dashboard web route active at http://0.0.0.0:" + port + "/teacherDashboard");
        } catch (Exception e) {
            System.err.println("[CCMS] Notice: Could not start web route: " + e.getMessage());
        }

        // Launch Desktop GUI only if display environment is present
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            boolean useSwing = false;
            for (String arg : args) {
                if ("--swing".equalsIgnoreCase(arg)) {
                    useSwing = true;
                    break;
                }
            }

            if (useSwing) {
                System.out.println("[CCMS] Launching Legacy Java Swing Desktop GUI (--swing flag detected)...");
                SwingUtilities.invokeLater(() -> {
                    LoginFrame loginFrame = new LoginFrame();
                    loginFrame.setVisible(true);
                });
            } else {
                System.out.println("[CCMS] Launching Modern JavaFX Desktop GUI (Default)...");
                try {
                    javafx.application.Application.launch(com.campusclub.javafx.CampusClubFxApp.class, args);
                } catch (Throwable t) {
                    System.err.println("[CCMS] JavaFX launch encountered error: " + t.getMessage());
                    System.err.println("[CCMS] Falling back to Java Swing Desktop GUI...");
                    SwingUtilities.invokeLater(() -> {
                        LoginFrame loginFrame = new LoginFrame();
                        loginFrame.setVisible(true);
                    });
                }
            }
        } else {
            System.out.println("[CCMS] Headless environment detected: Running in standalone Web Server mode.");
            // Keep container process alive in cloud deployment (e.g. Render/Docker)
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                System.out.println("[CCMS] Server shutting down...");
            }
        }
    }
}
