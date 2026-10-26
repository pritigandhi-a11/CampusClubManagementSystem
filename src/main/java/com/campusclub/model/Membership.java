package com.campusclub.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * [OOP: Encapsulation & Composition]
 * Represents a student's membership enrollment record in a campus club.
 */
public class Membership {

    // [OOP: Encapsulation - Private fields]
    private int membershipId;
    private int studentId;
    private int clubId;
    private String studentName;
    private String studentRegisterNumber;
    private String studentDepartment;
    private String clubName;
    private String clubCategory;
    private LocalDateTime joinedAt;
    private String membershipStatus; // PENDING, APPROVED, REJECTED
    private String roleInClub; // MEMBER, LEAD, SECRETARY, etc.

    public Membership() {
        this.joinedAt = LocalDateTime.now();
        this.membershipStatus = "PENDING";
        this.roleInClub = "MEMBER";
    }

    public Membership(int membershipId, int studentId, int clubId, LocalDateTime joinedAt, String membershipStatus, String roleInClub) {
        this.membershipId = membershipId;
        this.studentId = studentId;
        this.clubId = clubId;
        this.joinedAt = joinedAt != null ? joinedAt : LocalDateTime.now();
        this.membershipStatus = membershipStatus != null ? membershipStatus : "PENDING";
        this.roleInClub = roleInClub != null ? roleInClub : "MEMBER";
    }

    public Membership(int membershipId, int clubId, int studentId, String membershipStatus, String roleInClub,
                      LocalDateTime joinedAt, String studentName, String clubName) {
        this.membershipId = membershipId;
        this.clubId = clubId;
        this.studentId = studentId;
        this.membershipStatus = membershipStatus != null ? membershipStatus : "PENDING";
        this.roleInClub = roleInClub != null ? roleInClub : "MEMBER";
        this.joinedAt = joinedAt != null ? joinedAt : LocalDateTime.now();
        this.studentName = studentName;
        this.clubName = clubName;
    }

    // ==========================================================
    // [OOP: Encapsulation - Accessors and Mutators]
    // ==========================================================

    public int getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(int membershipId) {
        this.membershipId = membershipId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getClubId() {
        return clubId;
    }

    public void setClubId(int clubId) {
        this.clubId = clubId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentRegisterNumber() {
        return studentRegisterNumber;
    }

    public void setStudentRegisterNumber(String studentRegisterNumber) {
        this.studentRegisterNumber = studentRegisterNumber;
    }

    public String getStudentUsn() {
        return studentRegisterNumber;
    }

    public void setStudentUsn(String studentUsn) {
        this.studentRegisterNumber = studentUsn;
    }

    public String getStudentDepartment() {
        return studentDepartment;
    }

    public void setStudentDepartment(String studentDepartment) {
        this.studentDepartment = studentDepartment;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public String getClubCategory() {
        return clubCategory;
    }

    public void setClubCategory(String clubCategory) {
        this.clubCategory = clubCategory;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getJoinDate() {
        return joinedAt;
    }

    public void setJoinDate(LocalDateTime joinDate) {
        this.joinedAt = joinDate;
    }

    public String getMembershipStatus() {
        return membershipStatus;
    }

    public void setMembershipStatus(String membershipStatus) {
        this.membershipStatus = membershipStatus;
    }

    public String getStatus() {
        return membershipStatus;
    }

    public void setStatus(String status) {
        this.membershipStatus = status;
    }

    public String getRoleInClub() {
        return roleInClub;
    }

    public void setRoleInClub(String roleInClub) {
        this.roleInClub = roleInClub;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Membership that = (Membership) o;
        return studentId == that.studentId && clubId == that.clubId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, clubId);
    }

    @Override
    public String toString() {
        return String.format("Membership [ID: %d, Student: %s, Club: %s, Status: %s, Role: %s]",
                membershipId, studentName != null ? studentName : studentId, clubName != null ? clubName : clubId, membershipStatus, roleInClub);
    }
}
