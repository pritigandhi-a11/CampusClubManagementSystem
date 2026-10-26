package com.campusclub.gui;

import com.campusclub.model.ClubCoordinator;
import com.campusclub.service.ClubService;
import com.campusclub.service.EventService;
import com.campusclub.service.MembershipService;
import com.campusclub.web.TeacherDashboardWebServer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;

/**
 * Teacher / Faculty Coordinator Dashboard View.
 * Extends CoordinatorDashboard to render teacher-specific club management,
 * profile data, pending student requests, and announcements.
 * Also provides seamless access to the /teacherDashboard web frontend route.
 */
public class TeacherDashboard extends CoordinatorDashboard {

    public TeacherDashboard(ClubCoordinator coordinator) {
        this(coordinator, new ClubService(), new MembershipService(), new EventService());
    }

    public TeacherDashboard(ClubCoordinator coordinator, ClubService clubService, MembershipService membershipService, EventService eventService) {
        super(coordinator, clubService, membershipService, eventService);
        addWebDashboardToolbar();
    }

    /**
     * Adds an integrated toolbar offering 1-click access to the /teacherDashboard web route.
     */
    private void addWebDashboardToolbar() {
        JPanel toolbarPanel = new JPanel(new BorderLayout(10, 0));
        toolbarPanel.setOpaque(false);
        toolbarPanel.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel leftBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftBadge.setOpaque(false);

        JLabel statusLabel = new JLabel("👨‍🏫 Teacher / Faculty Coordinator Portal Active");
        statusLabel.setFont(UITheme.FONT_SUBTITLE);
        statusLabel.setForeground(UITheme.PRIMARY_NAVY);
        leftBadge.add(statusLabel);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        JButton openWebBtn = UITheme.createPrimaryButton("🌐 Open Web View (/teacherDashboard)");
        openWebBtn.setToolTipText("Open the web-based Teacher Dashboard in your default browser");
        openWebBtn.addActionListener(e -> openWebDashboard());

        rightActions.add(openWebBtn);

        toolbarPanel.add(leftBadge, BorderLayout.WEST);
        toolbarPanel.add(rightActions, BorderLayout.EAST);

        // Add to top of the dashboard
        add(toolbarPanel, BorderLayout.NORTH);
    }

    private void openWebDashboard() {
        try {
            // Ensure embedded web server is running on port 8080
            int port = TeacherDashboardWebServer.startServerIfNotRunning();
            String url = "http://localhost:" + port + "/teacherDashboard";

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                JOptionPane.showMessageDialog(this,
                        "Web Dashboard Route is active at:\n" + url,
                        "Web Route Mapped",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Web Route active at http://localhost:8080/teacherDashboard\n(Could not launch browser automatically: " + ex.getMessage() + ")",
                    "Teacher Dashboard Route",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
