package com.campusclub.service;

import com.campusclub.dao.UserDAO;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.exception.InvalidLoginException;
import com.campusclub.model.User;
import com.campusclub.model.UserRole;
import com.campusclub.util.DatabaseConnection;
import com.campusclub.util.PasswordUtil;
import com.campusclub.util.ValidationUtil;

import java.util.Optional;

/**
 * Service handling user authentication, credentials verification, and session operations.
 * Enforces secure credential validation and error masking to prevent user enumeration.
 */
public class AuthenticationService {
    private final UserDAO userDAO;

    public AuthenticationService() {
        this.userDAO = new UserDAO();
    }

    public AuthenticationService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates a user with username and password.
     *
     * @param username user login username
     * @param password raw plain text password
     * @return authenticated User entity with role populated
     * @throws InvalidLoginException if credentials are bad or account inactive
     * @throws DatabaseOperationException if database query fails
     */
    public User login(String username, String password) throws InvalidLoginException, DatabaseOperationException {
        return authenticateUser(username, password);
    }

    /**
     * Authenticates user credentials against the secure persistent store.
     *
     * @param username plain username
     * @param password plain password
     * @return validated User object
     * @throws InvalidLoginException on validation or credential failure
     * @throws DatabaseOperationException on database access error
     */
    public User authenticateUser(String username, String password) throws InvalidLoginException, DatabaseOperationException {
        // 1. Input Validation
        if (ValidationUtil.isNullOrEmpty(username)) {
            System.out.println("[AUTH-DIAGNOSTIC] Authentication failed: Username is required.");
            throw new InvalidLoginException("Username is required.");
        }
        if (ValidationUtil.isNullOrEmpty(password)) {
            System.out.println("[AUTH-DIAGNOSTIC] Authentication failed: Password is required.");
            throw new InvalidLoginException("Password is required.");
        }

        String cleanIdentifier = username.trim();
        System.out.println("[AUTH-DIAGNOSTIC] === User Authentication Initiated ===");
        System.out.println("[AUTH-DIAGNOSTIC] Entered Identifier: '" + cleanIdentifier + "'");

        // 2. Query user from persistent storage by username, email, or student ID
        Optional<User> optionalUser = Optional.empty();
        String lookupMethod = "NONE";
        try {
            optionalUser = userDAO.findByIdentifier(cleanIdentifier);
            if (optionalUser.isPresent()) {
                lookupMethod = "DATABASE (users/students table)";
            }
        } catch (DatabaseOperationException e) {
            System.out.println("[AUTH-DIAGNOSTIC] Database query encountered error: " + e.getMessage());
            optionalUser = findBuiltInUser(cleanIdentifier);
            if (optionalUser.isPresent()) {
                lookupMethod = "BUILT-IN FALLBACK (after DB exception)";
            } else {
                System.out.println("[AUTH-DIAGNOSTIC] Lookup failed and no fallback available. Rethrowing DatabaseOperationException.");
                throw e;
            }
        }

        // If not found in DB, check built-in fallback accounts
        if (optionalUser.isEmpty()) {
            optionalUser = findBuiltInUser(cleanIdentifier);
            if (optionalUser.isPresent()) {
                lookupMethod = "BUILT-IN IN-MEMORY FALLBACK";
            }
        }

        if (optionalUser.isEmpty()) {
            System.out.println("[AUTH-DIAGNOSTIC] Account Lookup: NOT FOUND (Identifier: '" + cleanIdentifier + "'). Raising masked InvalidLoginException.");
            throw new InvalidLoginException("Invalid username or password.");
        }

        User user = optionalUser.get();
        System.out.println("[AUTH-DIAGNOSTIC] Account Lookup: FOUND (Resolved Username: '" + user.getUsername() + 
                "', Role: " + user.getRole() + ", Lookup Method: " + lookupMethod + ", Verified: " + user.isVerified() + ")");

        // 3. Verify Account Status
        if (!user.isActive()) {
            System.out.println("[AUTH-DIAGNOSTIC] Account Status: DEACTIVATED for username: '" + user.getUsername() + "'");
            throw new InvalidLoginException("Account is deactivated. Please contact the administrator.");
        }

        // 4. Secure Password Verification (never log/print raw passwords or hashes)
        boolean passwordMatches = PasswordUtil.verifyPassword(password, user.getPasswordHash());
        System.out.println("[AUTH-DIAGNOSTIC] Password Verification: " + (passwordMatches ? "MATCH SUCCESS" : "MISMATCH (Password does not match stored hash)"));

        if (!passwordMatches) {
            System.out.println("[AUTH-DIAGNOSTIC] Authentication Result: FAILED (Password mismatch for user: '" + user.getUsername() + "'). Raising masked InvalidLoginException.");
            throw new InvalidLoginException("Invalid username or password.");
        }

        System.out.println("[AUTH-DIAGNOSTIC] Authentication Result: SUCCESS for user: '" + user.getUsername() + "' (" + user.getRole() + ")");

        // Automatic legacy password hash upgrade to BCrypt ($2a$12) upon successful verification
        String currentHash = user.getPasswordHash();
        if (currentHash != null && !currentHash.startsWith("$2a$") && !currentHash.startsWith("$2b$") && !currentHash.startsWith("$2y$")) {
            try {
                String upgradedHash = PasswordUtil.hashPassword(password);
                userDAO.updatePassword(user.getUserId(), upgradedHash);
                user.setPasswordHash(upgradedHash);
                System.out.println("[AUTH-DIAGNOSTIC] Upgraded legacy password hash to BCrypt for user: '" + user.getUsername() + "'");
            } catch (Exception e) {
                // Non-blocking: log notice and allow login to proceed seamlessly
                System.err.println("Notice: Could not automatically upgrade password hash to BCrypt: " + e.getMessage());
            }
        }

        return user;
    }

    /**
     * Identifies the role of the user.
     */
    public UserRole getUserRole(User user) {
        return user != null ? user.getRole() : null;
    }

    /**
     * Updates a user's password securely.
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword)
            throws InvalidLoginException, DatabaseOperationException {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID: " + userId);
        }
        if (ValidationUtil.isNullOrEmpty(oldPassword)) {
            throw new InvalidLoginException("Current password is required.");
        }
        if (!ValidationUtil.isValidPassword(newPassword)) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        Optional<User> optionalUser = userDAO.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new InvalidLoginException("User account not found.");
        }

        User user = optionalUser.get();
        if (!PasswordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
            throw new InvalidLoginException("Current password does not match.");
        }

        String newHash = PasswordUtil.hashPassword(newPassword);
        return userDAO.updatePassword(userId, newHash);
    }

    /**
     * Finds built-in/fallback accounts by username, email, register number, or @klu.ac.in prefix.
     */
    public static Optional<User> findBuiltInUser(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return Optional.empty();
        }
        String clean = identifier.trim();
        java.util.Map<String, User> map;
        try {
            map = com.campusclub.web.WebPortalController.getBuiltInUsers();
        } catch (Throwable t) {
            return Optional.empty();
        }
        if (map == null || map.isEmpty()) {
            return Optional.empty();
        }

        // Direct lookup by lowercase key
        User u = map.get(clean.toLowerCase());
        if (u != null) return Optional.of(u);

        // Check if @klu.ac.in prefix
        if (clean.toLowerCase().endsWith("@klu.ac.in")) {
            String prefix = clean.substring(0, clean.indexOf('@')).trim().toLowerCase();
            u = map.get(prefix);
            if (u != null) return Optional.of(u);
            u = map.get(prefix + "@campus.edu");
            if (u != null) return Optional.of(u);
        }

        // Scan values
        for (User user : map.values()) {
            if (clean.equalsIgnoreCase(user.getUsername()) || clean.equalsIgnoreCase(user.getEmail())) {
                return Optional.of(user);
            }
            if (user instanceof com.campusclub.model.Student s && s.getRegisterNumber() != null) {
                if (clean.equalsIgnoreCase(s.getRegisterNumber())) {
                    return Optional.of(user);
                }
            }
        }

        return Optional.empty();
    }
}
