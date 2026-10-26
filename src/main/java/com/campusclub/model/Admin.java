package com.campusclub.model;

import java.util.Objects;

/**
 * [OOP: Inheritance]
 * Represents a System Administrator in the Campus Club Management System.
 * Inherits identity attributes from User with elevated administrative access.
 */
public class Admin extends User {

    // [OOP: Encapsulation - Private admin-specific attributes]
    private String adminLevel;

    public Admin() {
        super();
        setRole(UserRole.ADMIN);
        this.adminLevel = "SUPER_ADMIN";
    }

    public Admin(int userId, String username, String passwordHash, String fullName, String email,
                 String phoneNumber, String adminLevel) {
        super(userId, username, passwordHash, fullName, email, UserRole.ADMIN, phoneNumber);
        this.adminLevel = adminLevel != null ? adminLevel : "SUPER_ADMIN";
    }

    // ==========================================================
    // [OOP: Polymorphism - Overridden Methods]
    // ==========================================================

    @Override
    public String getRoleDisplayName() {
        return "Administrator (" + (adminLevel != null ? adminLevel : "Standard") + ")";
    }

    @Override
    public String getDashboardDescription() {
        return "Admin Control Panel: Register new campus clubs, manage user accounts, oversee campus-wide club activities, and configure platform settings.";
    }

    @Override
    public String getPermissionsDescription() {
        return "Permissions: [ALL_PERMISSIONS, REGISTER_CLUB, TOGGLE_CLUB_STATUS, MANAGE_USERS, VIEW_SYSTEM_LOGS, AUDIT_DATABASE]";
    }

    // ==========================================================
    // [OOP: Encapsulation - Accessors and Mutators]
    // ==========================================================

    public String getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Admin admin = (Admin) o;
        return Objects.equals(adminLevel, admin.adminLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), adminLevel);
    }

    @Override
    public String toString() {
        return String.format("Admin: %s [Level: %s, Email: %s]", getFullName(), adminLevel, getEmail());
    }
}
