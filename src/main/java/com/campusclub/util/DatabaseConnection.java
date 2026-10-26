package com.campusclub.util;

import com.campusclub.exception.DatabaseOperationException;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class to manage JDBC Database Connections to MySQL 8.
 * Loads configuration from db.properties and provides connection lifecycle management.
 */
public final class DatabaseConnection {
    private static String url;
    private static String username;
    private static String password;
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    static {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found in classpath: " + e.getMessage());
        }
        loadConfiguration();
    }

    private static void loadConfiguration() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception e) {
            System.err.println("Notice: Could not load db.properties: " + e.getMessage());
        }

        // 1. Read from db.properties
        url = properties.getProperty("db.url");
        if (url == null || url.trim().isEmpty()) {
            url = properties.getProperty("url");
        }

        username = properties.getProperty("db.username");
        if (username == null || username.trim().isEmpty()) {
            username = properties.getProperty("db.user");
        }
        if (username == null || username.trim().isEmpty()) {
            username = properties.getProperty("user");
        }

        password = properties.getProperty("db.password");
        if (password == null) {
            password = properties.getProperty("db.pass");
        }
        if (password == null) {
            password = properties.getProperty("password");
        }
        // 2. Environment variables override db.properties when provided
        String envUrl = System.getenv("CAMPUS_DB_URL");
        String envUser = System.getenv("CAMPUS_DB_USER");
        String envPassword = System.getenv("CAMPUS_DB_PASSWORD");

        if (envUrl != null && !envUrl.trim().isEmpty()) {
            url = envUrl;
        }
        if (envUser != null && !envUser.trim().isEmpty()) {
            username = envUser;
        }
        if (envPassword != null) {
            password = envPassword;
        }

        // 3. Fallback defaults for local development
        if (url == null || url.trim().isEmpty()) {
            url = "jdbc:mysql://localhost:3306/campus_club_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        }
        if (username == null || username.trim().isEmpty()) {
            username = "root";
        }
        if (password == null) {
            password = "MyCampus@2026";
        }
        }



    /**
     * Reloads configuration from db.properties and environment variables.
     */
    public static void reloadConfiguration() {
        loadConfiguration();
    }

    /**
     * Gets a new JDBC Connection to MySQL.
     *
     * @return active java.sql.Connection instance
     * @throws DatabaseOperationException if connection fails
     */
    public static Connection getConnection() throws DatabaseOperationException {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            String safeUrl = url != null ? url.replaceAll("(?i)password=[^&;]*", "password=***") : "null";
            String errorMsg;
            int errorCode = e.getErrorCode();
            String sqlState = e.getSQLState();

            if (errorCode == 1045 || "28000".equals(sqlState)) {
                errorMsg = "MySQL Authentication Failed (Error 1045, SQLState 28000):\n" +
                        "Access denied for user '" + username + "' at " + safeUrl + ".\n" +
                        "Please verify your MySQL password in 'src/main/resources/db.properties' or set the 'CAMPUS_DB_PASSWORD' environment variable.";
            } else if (errorCode == 1049 || "42000".equals(sqlState)) {
                errorMsg = "MySQL Database Not Found (Error 1049, SQLState 42000):\n" +
                        "The database 'campus_club_db' does not exist.\n" +
                        "Please run 'database/schema.sql' in MySQL Workbench/CLI to initialize the database.";
            } else if (errorCode == 0 || (sqlState != null && sqlState.startsWith("08"))) {
                errorMsg = "MySQL Server Unreachable (SQLState " + sqlState + ", Error " + errorCode + "):\n" +
                        "Could not connect to MySQL server at " + safeUrl + ".\n" +
                        "Please verify that the MySQL service (e.g. MySQL84) is running on port 3306.";
            } else {
                errorMsg = "MySQL Connection Error (SQLState " + sqlState + ", Error " + errorCode + "):\n" +
                        e.getMessage() + "\nURL: " + safeUrl;
            }

            throw new DatabaseOperationException(errorMsg, e);
        }
    }

    /**
     * Tests if the database connection can be established.
     *
     * @return true if connection succeeds and is valid, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            System.err.println("JDBC Connection Check: " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper to configure connection parameters dynamically (e.g. for custom test setups).
     */
    public static void configure(String dbUrl, String user, String pass) {
        url = dbUrl;
        username = user;
        password = pass;
    }

    /**
     * Safely closes an AutoCloseable resource (Connection, Statement, ResultSet).
     */
    public static void close(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                System.err.println("Warning: Error closing database resource: " + e.getMessage());
            }
        }
    }
}
