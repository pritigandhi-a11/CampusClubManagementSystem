package com.campusclub.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility to safely and idempotently execute database schema migrations.
 * Verifies column presence before executing DDL to prevent SQL errors.
 */
public final class DatabaseMigrationRunner {

    private static volatile boolean migrationExecuted = false;

    private DatabaseMigrationRunner() {
        // Utility
    }

    public static synchronized boolean runSecurityMigration() {
        if (migrationExecuted) {
            return true;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                return false;
            }

            String catalog = conn.getCatalog();
            DatabaseMetaData metaData = conn.getMetaData();

            // Collect existing columns for table 'users'
            Set<String> existingColumns = new HashSet<>();
            try (ResultSet rs = metaData.getColumns(catalog, null, "users", null)) {
                while (rs.next()) {
                    String col = rs.getString("COLUMN_NAME");
                    if (col != null) {
                        existingColumns.add(col.toLowerCase());
                    }
                }
            }

            if (existingColumns.isEmpty()) {
                // Table might not exist yet or catalog name mismatch; try without catalog
                try (ResultSet rs = metaData.getColumns(null, null, "users", null)) {
                    while (rs.next()) {
                        String col = rs.getString("COLUMN_NAME");
                        if (col != null) {
                            existingColumns.add(col.toLowerCase());
                        }
                    }
                }
            }

            try (Statement stmt = conn.createStatement()) {
                if (!existingColumns.contains("is_verified")) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN is_verified BOOLEAN DEFAULT TRUE");
                    System.out.println("[MIGRATION] Added column 'is_verified' to table 'users'.");
                }
                if (!existingColumns.contains("verification_code")) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN verification_code VARCHAR(10) DEFAULT NULL");
                    System.out.println("[MIGRATION] Added column 'verification_code' to table 'users'.");
                }
                if (!existingColumns.contains("otp_expires_at")) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN otp_expires_at TIMESTAMP NULL DEFAULT NULL");
                    System.out.println("[MIGRATION] Added column 'otp_expires_at' to table 'users'.");
                }
                if (!existingColumns.contains("otp_attempts")) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN otp_attempts INT DEFAULT 0");
                    System.out.println("[MIGRATION] Added column 'otp_attempts' to table 'users'.");
                }
            }

            migrationExecuted = true;
            return true;
        } catch (Exception e) {
            System.err.println("[MIGRATION] Migration notice: " + e.getMessage());
            return false;
        }
    }
}
