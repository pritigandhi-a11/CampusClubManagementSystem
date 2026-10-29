package com.campusclub.model;

/**
 * Enumeration representing user roles in the Campus Club Management System.
 */
public enum UserRole {
    STUDENT("Student"),
    COORDINATOR("Club Coordinator"),
    TEACHER("Teacher Coordinator"),
    ADMIN("Administrator");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static UserRole fromString(String roleStr) {
        if (roleStr == null) {
            return null;
        }
        for (UserRole role : UserRole.values()) {
            if (role.name().equalsIgnoreCase(roleStr) || role.displayName.equalsIgnoreCase(roleStr)) {
                return role;
            }
        }
        if (roleStr.equalsIgnoreCase("FACULTY")
                || roleStr.equalsIgnoreCase("TEACHER_COORDINATOR")
                || roleStr.equalsIgnoreCase("TEACHER COORDINATOR")
                || roleStr.equalsIgnoreCase("TEACHER")
                || roleStr.equalsIgnoreCase("REVIEWER")
                || roleStr.equalsIgnoreCase("EVALUATOR")
                || roleStr.equalsIgnoreCase("AUDITOR")) {
            return TEACHER;
        }
        if (roleStr.equalsIgnoreCase("TEAM_MEMBER")
                || roleStr.equalsIgnoreCase("TEAM MEMBER")
                || roleStr.equalsIgnoreCase("MEMBER_COORDINATOR")) {
            return COORDINATOR;
        }
        throw new IllegalArgumentException("Unknown user role: " + roleStr);
    }
}
