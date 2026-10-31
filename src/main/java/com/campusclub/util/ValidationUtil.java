package com.campusclub.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Utility class for validating user input data throughout the application.
 * Provides reusable boolean checks as well as strict assertion methods that
 * throw {@link IllegalArgumentException} upon validation failure.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9]{10,15}$");

    private static final Pattern REGISTER_NUMBER_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_-]{3,30}$");

    private static final Pattern INSTITUTIONAL_EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@klu\\.ac\\.in$", Pattern.CASE_INSENSITIVE);

    private ValidationUtil() {
        // Utility class - non-instantiable
    }

    // ==========================================================
    // Boolean Verification Methods
    // ==========================================================

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        if (isNullOrEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidUsername(String username) {
        if (isNullOrEmpty(username)) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.trim().length() >= 6;
    }

    public static boolean isValidPhone(String phone) {
        if (isNullOrEmpty(phone)) {
            return false;
        }
        String digits = phone.trim().replaceAll("[\\s\\-+()]", "");
        return PHONE_PATTERN.matcher(digits).matches();
    }

    public static boolean isValidRegisterNumber(String regNo) {
        if (isNullOrEmpty(regNo)) {
            return false;
        }
        return REGISTER_NUMBER_PATTERN.matcher(regNo.trim()).matches();
    }

    public static boolean isValidInstitutionalEmail(String email) {
        if (isNullOrEmpty(email)) {
            return false;
        }
        return INSTITUTIONAL_EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static void validateInstitutionalEmail(String email) {
        if (!isValidInstitutionalEmail(email)) {
            throw new IllegalArgumentException("Registration requires an official institutional email ending exactly in @klu.ac.in (e.g. 99527800392@klu.ac.in).");
        }
    }

    public static boolean isPositiveInteger(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }
        try {
            int val = Integer.parseInt(str.trim());
            return val > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ==========================================================
    // Strict Assertion / Validation Methods (Throwing)
    // ==========================================================

    /**
     * Validates that a string field is not null or blank.
     */
    public static void validateRequired(String value, String fieldName) {
        if (isNullOrEmpty(value)) {
            throw new IllegalArgumentException(fieldName + " is required and cannot be blank.");
        }
    }

    /**
     * Validates an email address.
     */
    public static void validateEmail(String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format: '" + email + "'. Must be a valid email (e.g. user@domain.com).");
        }
    }

    /**
     * Validates a username format.
     */
    public static void validateUsername(String username) {
        if (!isValidUsername(username)) {
            throw new IllegalArgumentException("Invalid username: '" + username + "'. Must be 3-30 alphanumeric characters or underscores.");
        }
    }

    /**
     * Validates a password format.
     */
    public static void validatePassword(String password) {
        if (!isValidPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
    }

    /**
     * Validates a phone number.
     */
    public static void validatePhone(String phone) {
        if (!isValidPhone(phone)) {
            throw new IllegalArgumentException("Invalid phone number format: '" + phone + "'. Must contain 10-15 digits.");
        }
    }

    /**
     * Validates a college student register number / roll number.
     */
    public static void validateStudentRegisterNumber(String regNo) {
        if (!isValidRegisterNumber(regNo)) {
            throw new IllegalArgumentException("Invalid student register number: '" + regNo + "'. Must be 3-30 alphanumeric characters.");
        }
    }

    /**
     * Validates positive integer bounds.
     */
    public static void validatePositiveNumber(int number, String fieldName) {
        if (number <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive number greater than zero.");
        }
    }

    /**
     * Validates positive double bounds.
     */
    public static void validatePositiveNumber(double number, String fieldName) {
        if (number <= 0.0) {
            throw new IllegalArgumentException(fieldName + " must be a positive number greater than zero.");
        }
    }

    /**
     * Validates event seat capacity.
     */
    public static void validateEventCapacity(int capacity) {
        if (capacity < 1 || capacity > 10000) {
            throw new IllegalArgumentException("Event capacity must be between 1 and 10,000 seats.");
        }
    }

    /**
     * Validates date presence.
     */
    public static void validateDate(LocalDateTime date, String fieldName) {
        if (date == null) {
            throw new IllegalArgumentException(fieldName + " date and time cannot be null.");
        }
    }

    /**
     * Validates date presence.
     */
    public static void validateDate(LocalDate date, String fieldName) {
        if (date == null) {
            throw new IllegalArgumentException(fieldName + " date cannot be null.");
        }
    }

    /**
     * Validates that a date is in the future.
     */
    public static void validateFutureDate(LocalDateTime date, String fieldName) {
        validateDate(date, fieldName);
        if (date.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(fieldName + " must be scheduled for a future date and time.");
        }
    }
}
