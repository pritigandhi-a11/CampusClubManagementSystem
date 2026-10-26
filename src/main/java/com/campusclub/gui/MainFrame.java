package com.campusclub.gui;

import com.campusclub.model.Admin;
import com.campusclub.model.ClubCoordinator;
import com.campusclub.model.Student;
import com.campusclub.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Main Application Window hosting role-specific dashboards.
 * Demonstrates Polymorphism and MVC-style Java Swing Architecture.
 */
public class MainFrame extends JFrame {
    private final User currentUser;
    private JPanel dynamicDashboardPanel;

    public MainFrame(User currentUser) {
        this.currentUser = currentUser != null ? currentUser : new Admin();
        initComponents();
    }

    private void initComponents() {
        setTitle("Campus Club Management System - [" + currentUser.getRole().getDisplayName() + " Portal]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 720);
        setMinimumSize(new Dimension(920, 620));
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(UITheme.BG_LIGHT);

        // Top Navigation Header Bar
        JPanel topBar = new JPanel(new BorderLayout(15, 0));
        topBar.setBackground(UITheme.PRIMARY_NAVY);
        topBar.setBorder(new EmptyBorder(12, 20, 12, 20));

        // Brand Title
        JPanel brandPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        brandPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("🎓 Campus Club Management System");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel("College Activity & Extracurricular Management Platform");
        subLabel.setFont(UITheme.FONT_SMALL);
        subLabel.setForeground(new Color(203, 213, 225));

        brandPanel.add(titleLabel);
        brandPanel.add(subLabel);

        // User Info & Logout Pill
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        userPanel.setOpaque(false);

        String roleBadge = "[" + currentUser.getRoleDisplayName() + "]";
        JLabel userInfoLabel = new JLabel("👤 " + currentUser.getFullName() + "  " + roleBadge);
        userInfoLabel.setFont(UITheme.FONT_HEADER);
        userInfoLabel.setForeground(new Color(241, 245, 249));

        JButton logoutButton = UITheme.createDangerButton("Logout");
        logoutButton.setFont(UITheme.FONT_BOLD);
        logoutButton.addActionListener(e -> handleLogout());

        userPanel.add(userInfoLabel);
        userPanel.add(logoutButton);

        topBar.add(brandPanel, BorderLayout.WEST);
        topBar.add(userPanel, BorderLayout.EAST);

        contentPane.add(topBar, BorderLayout.NORTH);

        // Dynamic Role-Based Dashboard Loading (Polymorphic View Loading)
        dynamicDashboardPanel = createRoleDashboard();
        contentPane.add(dynamicDashboardPanel, BorderLayout.CENTER);

        setContentPane(contentPane);
    }

    private JPanel createRoleDashboard() {
        return createRoleDashboard(this.currentUser);
    }

    public static JPanel createRoleDashboard(User user) {
        if (user == null) {
            JPanel errorPanel = new JPanel(new FlowLayout());
            errorPanel.add(new JLabel("User cannot be null."));
            return errorPanel;
        }

        switch (user.getRole()) {
            case STUDENT:
                if (user instanceof Student student) {
                    return new StudentDashboard(student);
                } else {
                    Student s = new Student();
                    s.setUserId(user.getUserId());
                    s.setFullName(user.getFullName());
                    s.setUsername(user.getUsername());
                    s.setEmail(user.getEmail());
                    s.setPhoneNumber(user.getPhoneNumber());
                    return new StudentDashboard(s);
                }

            case COORDINATOR:
            case TEACHER:
                if (user instanceof ClubCoordinator coordinator) {
                    return new TeacherDashboard(coordinator);
                } else {
                    ClubCoordinator c = new ClubCoordinator();
                    c.setUserId(user.getUserId());
                    c.setFullName(user.getFullName());
                    c.setUsername(user.getUsername());
                    c.setEmail(user.getEmail());
                    return new TeacherDashboard(c);
                }

            case ADMIN:
                if (user instanceof Admin admin) {
                    return new AdminDashboard(admin);
                } else {
                    Admin a = new Admin();
                    a.setUserId(user.getUserId());
                    a.setFullName(user.getFullName());
                    a.setUsername(user.getUsername());
                    a.setEmail(user.getEmail());
                    return new AdminDashboard(a);
                }

            default:
                JPanel errorPanel = new JPanel(new FlowLayout());
                errorPanel.add(new JLabel("Unrecognized User Role."));
                return errorPanel;
        }
    }

    public void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out from Campus Club Portal?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public JPanel getDynamicDashboardPanel() {
        return dynamicDashboardPanel;
    }
}
