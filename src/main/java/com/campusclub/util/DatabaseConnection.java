package com.campusclub.util;

import com.campusclub.exception.DatabaseOperationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class to manage JDBC Database Connections to MySQL 8 and TiDB Cloud Serverless.
 * Loads configuration from environment variables, .env file, or db.properties.
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

        // 1. Read defaults from db.properties
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

        // 2. Read from git-ignored .env file if present
        String envFileUrl = null;
        String envFileUser = null;
        String envFilePassword = null;
        File envFile = resolveEnvFile();
        if (envFile != null && envFile.exists() && envFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int idx = line.indexOf('=');
                    if (idx > 0) {
                        String k = line.substring(0, idx).trim();
                        String v = line.substring(idx + 1).trim();
                        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                            v = v.substring(1, v.length() - 1);
                        }
                        if ("CAMPUS_DB_URL".equalsIgnoreCase(k) || "DATABASE_URL".equalsIgnoreCase(k) || "MYSQL_URL".equalsIgnoreCase(k)) {
                            envFileUrl = v;
                        } else if ("CAMPUS_DB_USER".equalsIgnoreCase(k) || "DATABASE_USER".equalsIgnoreCase(k) || "MYSQL_USER".equalsIgnoreCase(k)) {
                            envFileUser = v;
                        } else if ("CAMPUS_DB_PASSWORD".equalsIgnoreCase(k) || "DATABASE_PASSWORD".equalsIgnoreCase(k) || "MYSQL_PASSWORD".equalsIgnoreCase(k)) {
                            envFilePassword = v;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Notice: Could not read .env: " + e.getMessage());
            }
        }

        // 3. System environment variables override .env and db.properties
        String envUrl = System.getenv("CAMPUS_DB_URL");
        if (envUrl == null || envUrl.trim().isEmpty()) envUrl = System.getenv("DATABASE_URL");
        if (envUrl == null || envUrl.trim().isEmpty()) envUrl = System.getenv("MYSQL_URL");
        if (envUrl == null || envUrl.trim().isEmpty()) envUrl = envFileUrl;

        String envUser = System.getenv("CAMPUS_DB_USER");
        if (envUser == null || envUser.trim().isEmpty()) envUser = System.getenv("DATABASE_USER");
        if (envUser == null || envUser.trim().isEmpty()) envUser = System.getenv("MYSQL_USER");
        if (envUser == null || envUser.trim().isEmpty()) envUser = envFileUser;

        String envPassword = System.getenv("CAMPUS_DB_PASSWORD");
        if (envPassword == null) envPassword = System.getenv("DATABASE_PASSWORD");
        if (envPassword == null) envPassword = System.getenv("MYSQL_PASSWORD");
        if (envPassword == null) envPassword = envFilePassword;

        if (envUser != null && !envUser.trim().isEmpty()) {
            username = envUser.trim();
        }
        if (envPassword != null) {
            password = envPassword;
        }
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            applyConnectionUrl(envUrl.trim());
        } else if (url != null && !url.trim().isEmpty()) {
            applyConnectionUrl(url.trim());
        }

        // 4. Fallback defaults for local development
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

    private static void applyConnectionUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return;
        String clean = rawUrl.trim();
        if (clean.startsWith("mysql://")) {
            try {
                java.net.URI uri = java.net.URI.create(clean);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    if (username == null || username.trim().isEmpty() || "root".equals(username)) {
                        username = java.net.URLDecoder.decode(parts[0], java.nio.charset.StandardCharsets.UTF_8);
                    }
                    if (password == null || password.trim().isEmpty() || "MyCampus@2026".equals(password)) {
                        password = java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
                int port = uri.getPort() > 0 ? uri.getPort() : 3306;
                String path = uri.getPath();
                String query = uri.getQuery();
                clean = "jdbc:mysql://" + uri.getHost() + ":" + port + (path != null ? path : "/campus_club_db") +
                        (query != null && !query.isEmpty() ? "?" + query : "");
            } catch (Exception ignored) {
                clean = "jdbc:" + clean;
            }
        } else if (!clean.startsWith("jdbc:")) {
            clean = "jdbc:mysql://" + clean;
        }

        // Normalize ssl-mode=REQUIRED to sslMode=VERIFY_IDENTITY for MySQL Connector/J
        if (clean.contains("ssl-mode=REQUIRED")) {
            clean = clean.replace("ssl-mode=REQUIRED", "sslMode=VERIFY_IDENTITY");
        }

        // Automatically optimize for TiDB Cloud Serverless endpoints
        if (clean.contains("tidbcloud.com") || clean.contains(":4000")) {
            if (!clean.contains("useSSL=") && !clean.contains("sslMode=")) {
                clean += (clean.contains("?") ? "&" : "?") + "useSSL=true&sslMode=VERIFY_IDENTITY";
            }
            if (!clean.contains("enabledTLSProtocols=")) {
                clean += (clean.contains("?") ? "&" : "?") + "enabledTLSProtocols=TLSv1.2,TLSv1.3";
            }
            if (!clean.contains("serverTimezone=")) {
                clean += (clean.contains("?") ? "&" : "?") + "serverTimezone=UTC";
            }
        }

        if (!clean.contains("connectTimeout=")) {
            clean += (clean.contains("?") ? "&" : "?") + "connectTimeout=3000&socketTimeout=5000";
        }

        url = clean;
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
     * Resolves the .env file across project directories and current working directory.
     */
    private static File resolveEnvFile() {
        File[] candidates = new File[] {
            new File(".env"),
            new File(System.getProperty("user.dir", "."), ".env"),
            new File("c:/Users/Preeti gandhi/Desktop/CampusClubManagementSystem/.env"),
            new File(System.getProperty("user.home", ""), "Desktop/CampusClubManagementSystem/.env")
        };
        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                return candidate;
            }
        }
        return null;
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

    /**
     * CLI entrypoint to verify database connection without revealing passwords.
     */
    public static void main(String[] args) {
        System.out.println("--- Campus Club Database Connection Test ---");
        String safeUrl = url != null ? url.replaceAll("(?i)password=[^&;]*", "password=***") : "null";
        System.out.println("Target JDBC URL : " + safeUrl);
        System.out.println("Target DB User  : " + username);
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("[PASS] Successfully connected to database!");
                System.out.println("Database Engine : " + conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion());
            } else {
                System.err.println("[FAIL] Connection is null or closed.");
            }
        } catch (Exception e) {
            System.err.println("[FAIL] " + e.getMessage());
        }
    }
}

