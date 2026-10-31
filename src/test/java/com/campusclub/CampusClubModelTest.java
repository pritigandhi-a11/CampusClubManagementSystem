package com.campusclub;

import com.campusclub.model.*;
import com.campusclub.recommendation.ClubRecommendationEngine;
import com.campusclub.util.PasswordUtil;
import com.campusclub.util.ValidationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Model classes, Validation, Password hashing, and Recommendation Engine.
 */
public class CampusClubModelTest {

    @Test
    @DisplayName("Test User Hierarchy and Polymorphism")
    void testUserHierarchy() {
        User student = new Student(1, "alex_c", "hash123", "Alex Chen", "alex@campus.edu",
                "1234567890", "1CC21CS001", "Computer Science", 3, 5, "Coding, AI");

        assertEquals(UserRole.STUDENT, student.getRole());
        assertTrue(student.getRoleDisplayName().contains("Student"));
        assertTrue(student instanceof User);

        User admin = new Admin(2, "admin", "hash456", "System Admin", "admin@campus.edu", "9876543210", "SUPER_ADMIN");
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertTrue(admin.getRoleDisplayName().contains("Administrator"));
    }

    @Test
    @DisplayName("Test Modern Salted Password Hashing (BCrypt & PBKDF2) and Salt Uniqueness")
    void testSaltedPasswordHashing() {
        String rawPassword = "mypassword2026";
        String hash1 = PasswordUtil.hashPassword(rawPassword);
        String hash2 = PasswordUtil.hashPassword(rawPassword);

        assertNotNull(hash1);
        assertNotNull(hash2);
        assertTrue(hash1.startsWith("$2a$") || hash1.startsWith("PBKDF2$"));
        // Different random salts produce different hashes for same password
        assertNotEquals(hash1, hash2);

        // Both verify successfully against the password
        assertTrue(PasswordUtil.verifyPassword(rawPassword, hash1));
        assertTrue(PasswordUtil.verifyPassword(rawPassword, hash2));
        assertFalse(PasswordUtil.verifyPassword("wrongpassword", hash1));

        // Also test backward-compatible PBKDF2 hashing
        String pbkdf2Hash = PasswordUtil.hashPasswordPbkdf2(rawPassword);
        assertTrue(pbkdf2Hash.startsWith("PBKDF2$"));
        assertTrue(PasswordUtil.verifyPassword(rawPassword, pbkdf2Hash));
    }

    @Test
    @DisplayName("Test Legacy Demo SHA-256 Seed Password Verification")
    void testLegacySeedPasswordVerification() {
        // SHA-256 hash for "password123" used in schema.sql demo seed data
        String seedHash = "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f";
        assertTrue(PasswordUtil.verifyPassword("password123", seedHash));
        assertFalse(PasswordUtil.verifyPassword("invalidPassword", seedHash));
    }

    @Test
    @DisplayName("Test Input Validation Utility")
    void testValidationUtil() {
        assertTrue(ValidationUtil.isValidEmail("student@campus.edu"));
        assertFalse(ValidationUtil.isValidEmail("invalid-email"));

        assertTrue(ValidationUtil.isValidUsername("john_doe123"));
        assertFalse(ValidationUtil.isValidUsername("ab")); // Too short

        assertTrue(ValidationUtil.isValidPassword("securePass1"));
        assertFalse(ValidationUtil.isValidPassword("123")); // Too short
    }

    @Test
    @DisplayName("Test Rule-Based Club Recommendation Engine")
    void testRecommendationEngine() {
        ClubRecommendationEngine engine = new ClubRecommendationEngine();

        Student student = new Student(10, "priya", "hash", "Priya S", "priya@campus.edu",
                "1234567890", "1CC22CS100", "Computer Science", 2, 4, "Coding, Robotics, AI");

        List<Club> clubs = new ArrayList<>();
        clubs.add(new Club(1, "Coding & Open Source Club", "Technical", "Competitive programming and AI hackathons", 1, LocalDate.now(), "ACTIVE"));
        clubs.add(new Club(2, "Music & Drama Society", "Cultural", "Acoustics and theatrical performances", 2, LocalDate.now(), "ACTIVE"));
        clubs.add(new Club(3, "Robotics Club", "Technical", "Hardware and IoT autonomous bots", 3, LocalDate.now(), "ACTIVE"));

        List<ClubRecommendationEngine.ClubRecommendation> recommendations = engine.recommendClubs(student, clubs, List.of());

        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.get(0).getScore() > 0);
        assertTrue(recommendations.stream().anyMatch(r -> r.getClub().getClubName().contains("Coding")));
    }
}
