package com.club.util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database connection utility facade.
 * Provides getConnection() using the active JDBC configuration and pool.
 */
public final class DatabaseConnection {

    private DatabaseConnection() {
        // Utility class
    }

    /**
     * Obtains an active JDBC database connection to MySQL.
     *
     * @return active java.sql.Connection instance
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return com.campusclub.util.DatabaseConnection.getConnection();
    }

    /**
     * Tests if the database connection is alive.
     *
     * @return true if connection succeeds, false otherwise
     */
    public static boolean testConnection() {
        return com.campusclub.util.DatabaseConnection.testConnection();
    }

    /**
     * Safely closes the given connection.
     *
     * @param connection Connection to close
     */
    public static void closeConnection(Connection connection) {
        com.campusclub.util.DatabaseConnection.close(connection);
    }
}
