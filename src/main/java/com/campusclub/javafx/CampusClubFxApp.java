package com.campusclub.javafx;

import com.campusclub.model.User;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Main JavaFX Application entrypoint for Campus Club Management System.
 * Manages the primary stage, CSS styling, and screen transitions between:
 * - FxLoginView (Authentication & Quick Evaluation presets)
 * - FxMainView (Role-specific dashboards for Admin, Coordinator/Teacher, Student)
 */
public class CampusClubFxApp extends Application {

    private Stage primaryStage;
    private static final int DEFAULT_WIDTH = 1120;
    private static final int DEFAULT_HEIGHT = 760;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.primaryStage.setTitle("Campus Club Management System — JavaFX Desktop Portal");
        this.primaryStage.setMinWidth(960);
        this.primaryStage.setMinHeight(640);

        showLoginScreen();
        this.primaryStage.show();
    }

    /**
     * Renders the login screen with evaluation preset credentials and database authentication.
     */
    public void showLoginScreen() {
        FxLoginView loginView = new FxLoginView(this::showMainDashboard);
        Scene scene = new Scene(loginView, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        applyStylesheet(scene);

        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    /**
     * Renders the role-specific main dashboard after authenticated login.
     */
    public void showMainDashboard(User authenticatedUser) {
        FxMainView mainView = new FxMainView(authenticatedUser, this::showLoginScreen);
        Scene scene = new Scene(mainView, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        applyStylesheet(scene);

        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    private void applyStylesheet(Scene scene) {
        URL cssResource = getClass().getResource("/campusclub.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        } else {
            System.err.println("Warning: /campusclub.css not found on classpath.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
