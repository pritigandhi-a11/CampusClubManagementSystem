package com.campusclub.dao;

import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.model.Admin;
import com.campusclub.model.ClubCoordinator;
import com.campusclub.model.Student;
import com.campusclub.model.User;
import com.campusclub.model.UserRole;
import com.campusclub.util.DatabaseConnection;
import com.campusclub.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for base User entities and authentication.
 * Demonstrates Abstraction and Data Layer Separation.
 */
public class UserDAO {

    public Optional<User> findByUsername(String username) throws DatabaseOperationException {
        if (username == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(conn, rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by username: " + username, e);
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) throws DatabaseOperationException {
        if (email == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(conn, rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by email: " + email, e);
        }
        return Optional.empty();
    }

    public Optional<User> findById(int userId) throws DatabaseOperationException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(conn, rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by id: " + userId, e);
        }
        return Optional.empty();
    }

    public Optional<User> authenticateUser(String username, String rawPassword) throws DatabaseOperationException {
        Optional<User> optionalUser = findByUsername(username);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (PasswordUtil.verifyPassword(rawPassword, user.getPasswordHash())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public List<User> findAll() throws DatabaseOperationException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY user_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapResultSetToUser(conn, rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all users", e);
        }
        return users;
    }

    public int createUser(User user) throws DatabaseOperationException {
        String sql = "INSERT INTO users (username, full_name, email, password_hash, role, phone_number) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole().name());
            ps.setString(6, user.getPhoneNumber());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseOperationException("Creating user failed, no rows affected.");
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    user.setUserId(generatedId);
                    return generatedId;
                } else {
                    throw new DatabaseOperationException("Creating user failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error creating user: " + user.getUsername(), e);
        }
    }

    public boolean updatePassword(int userId, String newPasswordHash) throws DatabaseOperationException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating password for userId: " + userId, e);
        }
    }

    public boolean updateUserRole(int userId, UserRole role) throws DatabaseOperationException {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating role for userId: " + userId, e);
        }
    }

    public boolean deleteUser(int userId) throws DatabaseOperationException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting user: " + userId, e);
        }
    }

    protected User mapResultSetToUser(Connection conn, ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String roleStr = rs.getString("role");
        String phoneNumber = rs.getString("phone_number");
        UserRole role = UserRole.fromString(roleStr);

        switch (role) {
            case STUDENT:
                Student student = new Student();
                populateBaseUser(student, userId, username, passwordHash, fullName, email, role, phoneNumber, rs);
                loadStudentDetails(conn, student);
                return student;

            case COORDINATOR:
            case TEACHER:
                ClubCoordinator coordinator = new ClubCoordinator();
                populateBaseUser(coordinator, userId, username, passwordHash, fullName, email, role, phoneNumber, rs);
                loadCoordinatorDetails(conn, coordinator);
                return coordinator;

            case ADMIN:
                Admin admin = new Admin();
                populateBaseUser(admin, userId, username, passwordHash, fullName, email, role, phoneNumber, rs);
                return admin;

            default:
                throw new IllegalStateException("Unsupported user role: " + role);
        }
    }

    private void populateBaseUser(User user, int id, String uname, String pwd, String name, String mail, UserRole role, String phone, ResultSet rs) throws SQLException {
        user.setUserId(id);
        user.setUsername(uname);
        user.setPasswordHash(pwd);
        user.setFullName(name);
        user.setEmail(mail);
        user.setRole(role);
        user.setPhoneNumber(phone);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
    }

    private void loadStudentDetails(Connection conn, Student student) throws SQLException {
        String sql = "SELECT * FROM students WHERE user_id = ? OR student_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, student.getUserId());
            ps.setInt(2, student.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    student.setStudentTableId(rs.getInt("student_id"));
                    student.setRegisterNumber(rs.getString("register_number"));
                    student.setDepartment(rs.getString("department"));
                    student.setYear(rs.getInt("year"));
                    student.setSemester(rs.getInt("semester"));
                    student.setInterests(rs.getString("interests"));
                }
            }
        }
    }

    private void loadCoordinatorDetails(Connection conn, ClubCoordinator coordinator) throws SQLException {
        String sql = "SELECT c.club_id, c.club_name FROM clubs c WHERE c.coordinator_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, coordinator.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    coordinator.setManagedClubId(rs.getInt("club_id"));
                    coordinator.setManagedClubName(rs.getString("club_name"));
                }
            }
        }
    }
}
