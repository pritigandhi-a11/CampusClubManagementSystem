package com.campusclub.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * [OOP: Abstraction & Encapsulation]
 * Abstract base class representing a user in the Campus Club Management System.
 * Encapsulates common user identity attributes and declares abstract polymorphic operations.
 */
public abstract class User {

    // [OOP: Encapsulation - Private instance fields]
    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private UserRole role;
    private String phoneNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active = true;
    private boolean verified = true;
    private String verificationCode;
    private Long otpExpiresAt;
    private int otpAttempts = 0;
    private Long lastOtpSentAt;

    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
    }

    public User(int userId, String username, String passwordHash, String fullName, String email, UserRole role, String phoneNumber) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.phoneNumber = phoneNumber;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
    }

    public User(int userId, String username, String passwordHash, String fullName, String email,
                UserRole role, String phoneNumber, LocalDateTime createdAt, boolean active) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = active;
    }

    // ==========================================================
    // [OOP: Abstraction & Polymorphism - Abstract Methods]
    // ==========================================================

    /**
     * Polymorphic method to get the role-specific display name.
     */
    public abstract String getRoleDisplayName();

    /**
     * Polymorphic method providing dashboard summary description for the user.
     */
    public abstract String getDashboardDescription();

    /**
     * Polymorphic method describing the role-specific authorization permissions.
     */
    public abstract String getPermissionsDescription();

    // ==========================================================
    // [OOP: Encapsulation - Public Getters and Setters]
    // ==========================================================

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserRole getUserRole() {
        return role;
    }

    public void setUserRole(UserRole userRole) {
        this.role = userRole;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public Long getOtpExpiresAt() {
        return otpExpiresAt;
    }

    public void setOtpExpiresAt(Long otpExpiresAt) {
        this.otpExpiresAt = otpExpiresAt;
    }

    public int getOtpAttempts() {
        return otpAttempts;
    }

    public void setOtpAttempts(int otpAttempts) {
        this.otpAttempts = otpAttempts;
    }

    public Long getLastOtpSentAt() {
        return lastOtpSentAt;
    }

    public void setLastOtpSentAt(Long lastOtpSentAt) {
        this.lastOtpSentAt = lastOtpSentAt;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userId == user.userId && Objects.equals(username, user.username) && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username, email);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) - %s", role != null ? role.name() : "N/A", fullName, username, email);
    }
}
