package com.campusclub.javafx;

import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.exception.InvalidLoginException;
import com.campusclub.model.User;
import com.campusclub.service.AuthenticationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

/**
 * JavaFX Login View for the Campus Club Management System.
 * Connects to the AuthenticationService and supports quick role presets for evaluator review.
 */
public class FxLoginView extends StackPane {

    private final AuthenticationService authService;
    private final Consumer<User> onLoginSuccess;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label statusLabel;
    private Button loginButton;

    public FxLoginView(Consumer<User> onLoginSuccess) {
        this(new AuthenticationService(), onLoginSuccess);
    }

    public FxLoginView(AuthenticationService authService, Consumer<User> onLoginSuccess) {
        this.authService = authService != null ? authService : new AuthenticationService();
        this.onLoginSuccess = onLoginSuccess;
        initComponents();
    }

    private void initComponents() {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #0F172A;");

        // Main Login Card
        VBox card = new VBox(16);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(460);
        card.setAlignment(Pos.CENTER);

        // Header
        Label iconLabel = new Label("🏛️");
        iconLabel.setStyle("-fx-font-size: 38px;");

        Label titleLabel = new Label("Campus Club System");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label subLabel = new Label("JavaFX Desktop Management & Review Hub");
        subLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        VBox headerBox = new VBox(4, iconLabel, titleLabel, subLabel);
        headerBox.setAlignment(Pos.CENTER);

        // Form Fields
        VBox formBox = new VBox(12);
        formBox.setAlignment(Pos.CENTER_LEFT);

        Label userLabel = new Label("USERNAME");
        userLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        usernameField = new TextField();
        usernameField.setPromptText("e.g. teacher_reviewer or team_lead");
        usernameField.setPrefHeight(38);

        Label passLabel = new Label("PASSWORD");
        passLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter account password");
        passwordField.setPrefHeight(38);
        passwordField.setOnAction(e -> handleLogin());

        formBox.getChildren().addAll(userLabel, usernameField, passLabel, passwordField);

        // Status Label
        statusLabel = new Label(" ");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #EF4444;");
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);

        // Action Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        loginButton = FxUITheme.createPrimaryButton("Sign In to Desktop Portal →");
        loginButton.setPrefHeight(40);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(loginButton, Priority.ALWAYS);
        loginButton.setOnAction(e -> handleLogin());

        Button clearButton = FxUITheme.createSecondaryButton("Clear");
        clearButton.setPrefHeight(40);
        clearButton.setOnAction(e -> {
            usernameField.clear();
            passwordField.clear();
            statusLabel.setText(" ");
        });

        buttonBox.getChildren().addAll(loginButton, clearButton);

        // Quick Evaluation Presets
        VBox presetsBox = new VBox(8);
        presetsBox.setStyle("-fx-border-color: #E2E8F0 transparent transparent transparent; -fx-padding: 14px 0 0 0;");
        presetsBox.setAlignment(Pos.CENTER);

        Label presetsTitle = new Label("QUICK EVALUATION PRESETS (CLICK TO AUTOFILL)");
        presetsTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748B;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);

        Button btnRev = FxUITheme.createQuickRoleButton("🔍 Teacher Reviewer", "teacher_reviewer");
        btnRev.setOnAction(e -> setCredentials("teacher_reviewer", "Reviewer@2026"));

        Button btnLead = FxUITheme.createQuickRoleButton("👑 Team Lead (Admin)", "team_lead");
        btnLead.setOnAction(e -> setCredentials("team_lead", "TeamLead@2026"));

        Button btnDev = FxUITheme.createQuickRoleButton("💻 Member 2 (Tech)", "team_dev1");
        btnDev.setOnAction(e -> setCredentials("team_dev1", "TeamDev1@2026"));

        Button btnAdmin = FxUITheme.createQuickRoleButton("⚙️ Desktop Admin", "admin");
        btnAdmin.setOnAction(e -> setCredentials("admin", "admin123"));

        grid.add(btnRev, 0, 0);
        grid.add(btnLead, 1, 0);
        grid.add(btnDev, 0, 1);
        grid.add(btnAdmin, 1, 1);

        presetsBox.getChildren().addAll(presetsTitle, grid);

        card.getChildren().addAll(headerBox, formBox, statusLabel, buttonBox, presetsBox);
        getChildren().add(card);
    }

    private void setCredentials(String username, String password) {
        usernameField.setText(username);
        passwordField.setText(password);
        statusLabel.setText(" ");
    }

    private void handleLogin() {
        String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText() : "";

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2563EB;");
        statusLabel.setText("Authenticating credentials...");

        try {
            User user = authService.login(username, password);
            if (user != null) {
                statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #10B981;");
                statusLabel.setText("Authentication successful! Loading dashboard...");
                if (onLoginSuccess != null) {
                    onLoginSuccess.accept(user);
                }
            } else {
                showError("Invalid username or password.");
            }
        } catch (InvalidLoginException ex) {
            showError(ex.getMessage());
        } catch (DatabaseOperationException ex) {
            showError("Database connection error. Verify cloud connection or local MySQL.");
        } catch (Exception ex) {
            showError("System error: " + ex.getMessage());
        }
    }

    private void showError(String msg) {
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #EF4444;");
        statusLabel.setText(msg);
    }

    public TextField getUsernameField() {
        return usernameField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }

    public Button getLoginButton() {
        return loginButton;
    }
}
