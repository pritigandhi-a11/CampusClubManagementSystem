package com.campusclub.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * JavaFX Design System and Theme Utilities for Campus Club Management System.
 * Matches the existing CCMS aesthetic (Navy, Royal Blue, Emerald, Light Slate).
 */
public final class FxUITheme {

    private FxUITheme() {}

    public static final String STYLESHEET_PATH = "/campusclub.css";

    public static VBox createKpiCard(String title, String value, String desc) {
        VBox card = new VBox(4);
        card.getStyleClass().add("kpi-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("kpi-title");

        Label lblValue = new Label(value);
        lblValue.getStyleClass().add("kpi-value");

        Label lblDesc = new Label(desc);
        lblDesc.getStyleClass().add("kpi-desc");

        card.getChildren().addAll(lblTitle, lblValue, lblDesc);
        return card;
    }

    public static Button createPrimaryButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-primary");
        return btn;
    }

    public static Button createSecondaryButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-secondary");
        return btn;
    }

    public static Button createDangerButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-danger");
        return btn;
    }

    public static Button createSuccessButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-success");
        return btn;
    }

    public static Button createQuickRoleButton(String roleTitle, String userText) {
        Button btn = new Button(roleTitle + "\n(" + userText + ")");
        btn.getStyleClass().add("btn-quick-role");
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    public static Label createRolePill(String roleName, boolean isReviewer) {
        Label pill = new Label();
        if (isReviewer || "TEACHER".equalsIgnoreCase(roleName)) {
            pill.setText("REVIEWER");
            pill.getStyleClass().add("role-pill-reviewer");
        } else if ("ADMIN".equalsIgnoreCase(roleName)) {
            pill.setText("ADMIN");
            pill.getStyleClass().add("role-pill-admin");
        } else if ("COORDINATOR".equalsIgnoreCase(roleName)) {
            pill.setText("COORDINATOR");
            pill.getStyleClass().add("role-pill-coord");
        } else {
            pill.setText("STUDENT");
            pill.getStyleClass().add("role-pill-student");
        }
        return pill;
    }

    public static Label createRolePill(com.campusclub.model.UserRole role) {
        if (role == null) {
            return createRolePill("STUDENT", false);
        }
        return createRolePill(role.name(), role == com.campusclub.model.UserRole.TEACHER);
    }

    public static void updateKpiCard(VBox card, String val) {
        if (card != null && card.getChildren().size() > 1 && card.getChildren().get(1) instanceof Label l) {
            l.setText(val);
        }
    }

    public static void showNotification(javafx.scene.control.Alert.AlertType type, String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type, message, javafx.scene.control.ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    public static VBox createSectionCard(String title, Node content) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        if (title != null && !title.trim().isEmpty()) {
            Label lblHeader = new Label(title);
            lblHeader.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
            card.getChildren().add(lblHeader);
        }

        if (content != null) {
            VBox.setVgrow(content, Priority.ALWAYS);
            card.getChildren().add(content);
        }
        return card;
    }
}
