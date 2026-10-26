package com.campusclub.model;

import java.util.Objects;

/**
 * [OOP: Inheritance & Composition]
 * Represents a Club Coordinator (Faculty or Senior Lead).
 * Inherits base user attributes from User and maintains an association with their managed Club.
 */
public class ClubCoordinator extends User {

    // [OOP: Encapsulation - Private coordinator-specific fields]
    private String department;
    private String designation;
    private Integer managedClubId;
    private String managedClubName;

    // [OOP: Composition - Optional reference to managed Club entity]
    private Club managedClub;

    public ClubCoordinator() {
        super();
        setRole(UserRole.COORDINATOR);
    }

    public ClubCoordinator(int userId, String username, String passwordHash, String fullName, String email,
                           String phoneNumber, String department, String designation) {
        super(userId, username, passwordHash, fullName, email, UserRole.COORDINATOR, phoneNumber);
        this.department = department;
        this.designation = designation;
    }

    // ==========================================================
    // [OOP: Polymorphism - Overridden Methods]
    // ==========================================================

    @Override
    public String getRoleDisplayName() {
        return "Club Coordinator (" + (designation != null ? designation : "Faculty") + ")";
    }

    @Override
    public String getDashboardDescription() {
        return "Coordinator Workspace: Review membership applications, schedule club events, manage attendee capacities, and broadcast announcements.";
    }

    @Override
    public String getPermissionsDescription() {
        return "Permissions: [APPROVE_MEMBERSHIP, REJECT_MEMBERSHIP, CREATE_EVENT, EDIT_EVENT, BROADCAST_ANNOUNCEMENTS, MANAGE_CLUB_PROFILE]";
    }

    // ==========================================================
    // [OOP: Encapsulation - Accessors and Mutators]
    // ==========================================================

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public Integer getManagedClubId() {
        return managedClubId;
    }

    public void setManagedClubId(Integer managedClubId) {
        this.managedClubId = managedClubId;
    }

    public String getManagedClubName() {
        return managedClubName;
    }

    public void setManagedClubName(String managedClubName) {
        this.managedClubName = managedClubName;
    }

    public Club getManagedClub() {
        return managedClub;
    }

    public void setManagedClub(Club managedClub) {
        this.managedClub = managedClub;
        if (managedClub != null) {
            this.managedClubId = managedClub.getClubId();
            this.managedClubName = managedClub.getClubName();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ClubCoordinator that = (ClubCoordinator) o;
        return Objects.equals(managedClubId, that.managedClubId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), managedClubId);
    }

    @Override
    public String toString() {
        return String.format("Coordinator: %s [%s, %s - Managed Club: %s]",
                getFullName(), designation, department, managedClubName != null ? managedClubName : "Unassigned");
    }
}
