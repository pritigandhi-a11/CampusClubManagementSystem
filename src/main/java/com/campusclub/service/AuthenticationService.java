package com.campusclub.service;

import com.campusclub.dao.UserDAO;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.exception.InvalidLoginException;
import com.campusclub.model.User;
import com.campusclub.model.UserRole;
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
            throw new InvalidLoginException("Username is required.");
        }
        if (ValidationUtil.isNullOrEmpty(password)) {
            throw new InvalidLoginException("Password is required.");
        }

        // 2. Query user from persistent storage (DatabaseOperationException thrown on DB fault)
        Optional<User> optionalUser = userDAO.findByUsername(username.trim());
        if (optionalUser.isEmpty()) {
            // Mask failure details: do not reveal that username does not exist
            throw new InvalidLoginException("Invalid username or password.");
        }

        User user = optionalUser.get();

        // 3. Verify Account Status
        if (!user.isActive()) {
            throw new InvalidLoginException("Account is deactivated. Please contact the administrator.");
        }

        // 4. Secure Password Verification (never log/print raw passwords or hashes)
        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            // Mask failure details: do not reveal that password was incorrect
            throw new InvalidLoginException("Invalid username or password.");
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
}
