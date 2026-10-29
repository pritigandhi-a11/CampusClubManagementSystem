package com.campusclub.javafx;

import com.campusclub.model.Admin;
import com.campusclub.model.ClubCoordinator;
import com.campusclub.model.Student;
import com.campusclub.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

/**
 * Main application container view for JavaFX desktop frontend.
 * Provides:
 * - Top header with branding, active user name, role badge pill, and Logout action.
 * - Center content area dynamically rendering role-based dashboards (Admin, Coordinator/Teacher, Student).
 */
public class FxMainView extends BorderPane {

    private final User currentUser;
    private final Runnable onLogout;
    private Node currentDashboard;

    public FxMainView(User currentUser, Runnable onLogout) {
        this.currentUser = currentUser != null ? currentUser : new Admin();
        this.onLogout = onLogout != null ? onLogout : () -> {};

        setStyle("-fx-background-color: #F8FAFC;");

        setTop(buildHeaderBar());
        setCenter(buildCenterContent());
    }

    private Node buildHeaderBar() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 24, 12, 24));
        header.getStyleClass().add("top-bar");

        // Branding
        VBox brandBox = new VBox(2);
        Label titleLbl = new Label("🎓 Campus Club Management System");
        titleLbl.getStyleClass().add("top-bar-title");

        Label subLbl = new Label("College Activity & Extracurricular Platform  •  Desktop JavaFX Edition");
        subLbl.getStyleClass().add("top-bar-subtitle");

        brandBox.getChildren().addAll(titleLbl, subLbl);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // User info & Role Pill
        HBox userBox = new HBox(12);
        userBox.setAlignment(Pos.CENTER_RIGHT);

        Label userLbl = new Label("👤 " + currentUser.getFullName());
        userLbl.setStyle("-fx-text-fill: #F1F5F9; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label rolePill = FxUITheme.createRolePill(currentUser.getRole());

        Button logoutBtn = FxUITheme.createDangerButton("Logout");
        logoutBtn.setOnAction(e -> handleLogoutAction());

        userBox.getChildren().addAll(userLbl, rolePill, logoutBtn);

        header.getChildren().addAll(brandBox, spacer, userBox);
        return header;
    }

    private Node buildCenterContent() {
        currentDashboard = createRoleDashboard(this.currentUser);

        ScrollPane scrollPane = new ScrollPane(currentDashboard);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        return scrollPane;
    }

    public static Node createRoleDashboard(User user) {
        if (user == null) {
            Label err = new Label("User cannot be null.");
            err.setStyle("-fx-text-fill: red; -fx-padding: 20px;");
            return err;
        }

        switch (user.getRole()) {
            case STUDENT:
                if (user instanceof Student student) {
                    return new FxStudentDashboard(student);
                } else {
                    Student s = new Student();
                    s.setUserId(user.getUserId());
                    s.setFullName(user.getFullName());
                    s.setUsername(user.getUsername());
                    s.setEmail(user.getEmail());
                    s.setPhoneNumber(user.getPhoneNumber());
                    return new FxStudentDashboard(s);
                }

            case COORDINATOR:
            case TEACHER:
                if (user instanceof ClubCoordinator coordinator) {
                    return new FxCoordinatorDashboard(coordinator);
                } else {
                    ClubCoordinator c = new ClubCoordinator();
                    c.setUserId(user.getUserId());
                    c.setFullName(user.getFullName());
                    c.setUsername(user.getUsername());
                    c.setEmail(user.getEmail());
                    c.setRole(user.getRole());
                    return new FxCoordinatorDashboard(c);
                }

            case ADMIN:
                if (user instanceof Admin admin) {
                    return new FxAdminDashboard(admin);
                } else {
                    Admin a = new Admin();
                    a.setUserId(user.getUserId());
                    a.setFullName(user.getFullName());
                    a.setUsername(user.getUsername());
                    a.setEmail(user.getEmail());
                    return new FxAdminDashboard(a);
                }

            default:
                Label err = new Label("Unrecognized role: " + user.getRole());
                err.setStyle("-fx-padding: 20px;");
                return err;
        }
    }

    private void handleLogoutAction() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Logout");
        confirm.setHeaderText("Log out of Campus Club Management System?");
        confirm.setContentText("You will be returned to the login screen.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            onLogout.run();
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Node getCurrentDashboard() {
        return currentDashboard;
    }
}
