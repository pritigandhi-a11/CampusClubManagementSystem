package com.campusclub.gui;

import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.exception.InvalidLoginException;
import com.campusclub.model.User;
import com.campusclub.service.AuthenticationService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Modern Swing Login Frame for the Campus Club Management System.
 * Handles credential entry, error reporting, demo accounts helper, and launches MainFrame.
 */
public class LoginFrame extends JFrame {
    private final AuthenticationService authService;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton clearButton;
    private JButton exitButton;
    private JLabel statusLabel;

    public LoginFrame() {
        this(new AuthenticationService());
    }

    public LoginFrame(AuthenticationService authService) {
        this.authService = authService != null ? authService : new AuthenticationService();
        initComponents();
    }

    private void initComponents() {
        setTitle("Campus Club Management System - Portal Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 520);
        setMinimumSize(new Dimension(480, 480));
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        mainPanel.setBackground(UITheme.BG_LIGHT);

        // Header Banner
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Campus Club Portal", SwingConstants.CENTER);
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.PRIMARY_NAVY);

        JLabel subtitleLabel = new JLabel("College Desktop Management & Activity Hub", SwingConstants.CENTER);
        subtitleLabel.setFont(UITheme.FONT_REGULAR);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);

        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);

        // Form Card Panel
        JPanel cardPanel = UITheme.createCardPanel();
        cardPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(UITheme.FONT_HEADER);
        userLabel.setForeground(UITheme.TEXT_PRIMARY);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        cardPanel.add(userLabel, gbc);

        usernameField = new JTextField(15);
        usernameField.setFont(UITheme.FONT_REGULAR);
        usernameField.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(6, 8, 6, 8)
        ));
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        cardPanel.add(usernameField, gbc);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(UITheme.FONT_HEADER);
        passLabel.setForeground(UITheme.TEXT_PRIMARY);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        cardPanel.add(passLabel, gbc);

        passwordField = new JPasswordField(15);
        passwordField.setFont(UITheme.FONT_REGULAR);
        passwordField.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(6, 8, 6, 8)
        ));
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        cardPanel.add(passwordField, gbc);

        // Demo Credentials Hint Box
        JPanel hintPanel = new JPanel(new GridLayout(3, 1, 2, 2));
        hintPanel.setOpaque(false);
        hintPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(224, 231, 255), 1, true),
                new EmptyBorder(6, 8, 6, 8)
        ));
        hintPanel.setBackground(new Color(238, 242, 255));

        JLabel demoTitle = new JLabel("💡 Quick Demo Login Credentials (Username / Password):");
        demoTitle.setFont(UITheme.FONT_SMALL);
        demoTitle.setForeground(UITheme.PRIMARY_BLUE);

        JLabel demoCoord = new JLabel("• Student: student1 / pass123  |  student2 / pass123");
        demoCoord.setFont(UITheme.FONT_SMALL);
        demoCoord.setForeground(UITheme.TEXT_MUTED);

        JLabel demoAdmin = new JLabel("• Coordinator: coord_coding / pass123  |  Admin: admin / admin123");
        demoAdmin.setFont(UITheme.FONT_SMALL);
        demoAdmin.setForeground(UITheme.TEXT_MUTED);

        hintPanel.add(demoTitle);
        hintPanel.add(demoCoord);
        hintPanel.add(demoAdmin);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 4, 4, 4);
        cardPanel.add(hintPanel, gbc);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        buttonPanel.setOpaque(false);

        loginButton = UITheme.createPrimaryButton("Sign In");
        loginButton.addActionListener(this::handleLogin);

        clearButton = UITheme.createSecondaryButton("Clear");
        clearButton.addActionListener(e -> {
            usernameField.setText("");
            passwordField.setText("");
            statusLabel.setText(" ");
        });

        exitButton = UITheme.createSecondaryButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));

        buttonPanel.add(loginButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(exitButton);

        // Center Container
        JPanel centerContainer = new JPanel(new BorderLayout(8, 8));
        centerContainer.setOpaque(false);
        centerContainer.add(cardPanel, BorderLayout.CENTER);
        centerContainer.add(buttonPanel, BorderLayout.SOUTH);

        // Status Label at bottom
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(UITheme.FONT_REGULAR);
        statusLabel.setForeground(UITheme.DANGER_RED);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(centerContainer, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        getRootPane().setDefaultButton(loginButton);
    }

    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setForeground(UITheme.DANGER_RED);
            statusLabel.setText("Please enter both username and password.");
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        statusLabel.setForeground(UITheme.PRIMARY_BLUE);
        statusLabel.setText("Authenticating credentials...");

        try {
            User user = authService.login(username, password);
            statusLabel.setForeground(UITheme.ACCENT_EMERALD);
            statusLabel.setText("Authentication successful! Loading dashboard...");

            SwingUtilities.invokeLater(() -> {
                dispose();
                MainFrame mainFrame = new MainFrame(user);
                mainFrame.setVisible(true);
            });
        } catch (InvalidLoginException ex) {
            statusLabel.setForeground(UITheme.DANGER_RED);
            statusLabel.setText(ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseOperationException ex) {
            statusLabel.setForeground(UITheme.DANGER_RED);
            statusLabel.setText("Database Connection Error. Check MySQL / db.properties.");
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Database Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            statusLabel.setForeground(UITheme.DANGER_RED);
            statusLabel.setText("System error: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Unexpected system error:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public JTextField getUsernameField() {
        return usernameField;
    }

    public JPasswordField getPasswordField() {
        return passwordField;
    }

    public JButton getLoginButton() {
        return loginButton;
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }
}
