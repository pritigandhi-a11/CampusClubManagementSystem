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

    /**
     * Looks up a user by student register number (Student ID) from the joined students table.
     */
    public Optional<User> findByStudentRegisterNumber(String registerNumber) throws DatabaseOperationException {
        if (registerNumber == null || registerNumber.trim().isEmpty()) {
            return Optional.empty();
        }
        String sql = "SELECT u.* FROM users u JOIN students s ON u.user_id = s.user_id WHERE s.register_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, registerNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(conn, rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by student ID: " + registerNumber, e);
        }
        return Optional.empty();
    }

    /**
     * Looks up a user by either username, email address, or student register number (Student ID).
     * Normalizes institutional college emails (@klu.ac.in), legacy emails (@campus.edu),
     * and numeric / alphanumeric student IDs across both the users and students relational tables.
     */
    public Optional<User> findByIdentifier(String identifier) throws DatabaseOperationException {
        if (identifier == null || identifier.trim().isEmpty()) {
            return Optional.empty();
        }
        String clean = identifier.trim();

        // 1. Direct username check
        Optional<User> byUser = findByUsername(clean);
        if (byUser.isPresent()) return byUser;

        // 2. Direct email check
        Optional<User> byEmail = findByEmail(clean);
        if (byEmail.isPresent()) return byEmail;

        // 3. Student register_number (Student ID) in students table
        Optional<User> byReg = findByStudentRegisterNumber(clean);
        if (byReg.isPresent()) return byReg;

        // 4. If identifier contains an email domain
        if (clean.contains("@")) {
            int atIdx = clean.indexOf('@');
            String localPart = clean.substring(0, atIdx).trim();
            String domain = clean.substring(atIdx + 1).trim().toLowerCase();

            // Check if local part is an existing username (e.g. student1@klu.ac.in -> username student1)
            Optional<User> byUserLocal = findByUsername(localPart);
            if (byUserLocal.isPresent()) return byUserLocal;

            // Check if local part is a student register number (e.g. 1CC21CS101@klu.ac.in -> regNo 1CC21CS101)
            Optional<User> byRegLocal = findByStudentRegisterNumber(localPart);
            if (byRegLocal.isPresent()) return byRegLocal;

            // Check institutional / legacy cross-domain email aliases
            if ("klu.ac.in".equalsIgnoreCase(domain)) {
                Optional<User> byCampus = findByEmail(localPart + "@campus.edu");
                if (byCampus.isPresent()) return byCampus;
            } else if ("campus.edu".equalsIgnoreCase(domain)) {
                Optional<User> byKlu = findByEmail(localPart + "@klu.ac.in");
                if (byKlu.isPresent()) return byKlu;
            }
        } else {
            // 5. If identifier is a Student ID / username without '@'
            // Check if user has an institutional email: <clean>@klu.ac.in (e.g. Student ID 99527800392 -> 99527800392@klu.ac.in)
            Optional<User> byKluEmail = findByEmail(clean + "@klu.ac.in");
            if (byKluEmail.isPresent()) return byKluEmail;

            // Check if user has legacy email: <clean>@campus.edu
            Optional<User> byCampusEmail = findByEmail(clean + "@campus.edu");
            if (byCampusEmail.isPresent()) return byCampusEmail;
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

    public boolean updateVerificationStatus(int userId, boolean verified) throws DatabaseOperationException {
        String sql = "UPDATE users SET is_verified = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, verified);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Gracefully handle case where database column does not yet exist
            return false;
        }
    }

    public boolean updateOtpDetails(int userId, String code, Long expiresAt, int attempts) {
        String sql = "UPDATE users SET verification_code = ?, otp_expires_at = ?, otp_attempts = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            if (expiresAt != null) {
                ps.setTimestamp(2, new Timestamp(expiresAt));
            } else {
                ps.setNull(2, Types.TIMESTAMP);
            }
            ps.setInt(3, attempts);
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Gracefully handle case where database columns do not yet exist
            return false;
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
        try {
            user.setVerified(rs.getBoolean("is_verified"));
            if (rs.wasNull()) {
                user.setVerified(true);
            }
        } catch (SQLException ignored) {
            user.setVerified(true);
        }
        try {
            user.setVerificationCode(rs.getString("verification_code"));
        } catch (SQLException ignored) {
            user.setVerificationCode(null);
        }
        try {
            Timestamp expTs = rs.getTimestamp("otp_expires_at");
            if (expTs != null) {
                user.setOtpExpiresAt(expTs.getTime());
            }
        } catch (SQLException ignored) {}
        try {
            user.setOtpAttempts(rs.getInt("otp_attempts"));
        } catch (SQLException ignored) {}
    }

    private void loadStudentDetails(Connection conn, Student student) throws SQLException {
        String sql = "SELECT * FROM students WHERE user_id = ? OR student_id = ?";
        boolean found = false;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, student.getUserId());
            ps.setInt(2, student.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    found = true;
                    student.setStudentTableId(rs.getInt("student_id"));
                    student.setRegisterNumber(rs.getString("register_number"));
                    student.setDepartment(rs.getString("department"));
                    student.setYear(rs.getInt("year"));
                    student.setSemester(rs.getInt("semester"));
                    student.setInterests(rs.getString("interests"));
                }
            }
        }
        if (!found && student.getUserId() > 0) {
            String regNo = student.getUsername();
            if (student.getEmail() != null && student.getEmail().contains("@")) {
                String local = student.getEmail().substring(0, student.getEmail().indexOf('@')).trim();
                if (!local.isEmpty()) {
                    regNo = local;
                }
            }
            if (regNo == null || regNo.trim().isEmpty()) {
                regNo = "REG" + student.getUserId();
            }
            try (PreparedStatement insertPs = conn.prepareStatement(
                    "INSERT INTO students (user_id, register_number, department, year, semester, phone, interests) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                insertPs.setInt(1, student.getUserId());
                insertPs.setString(2, regNo);
                insertPs.setString(3, "Computer Science & Engineering");
                insertPs.setInt(4, 3);
                insertPs.setInt(5, 5);
                insertPs.setString(6, student.getPhoneNumber() != null ? student.getPhoneNumber() : "+91 9952780039");
                insertPs.setString(7, "Coding, Web Development");
                insertPs.executeUpdate();
                try (ResultSet genRs = insertPs.getGeneratedKeys()) {
                    if (genRs.next()) {
                        student.setStudentTableId(genRs.getInt(1));
                    }
                }
                student.setRegisterNumber(regNo);
                student.setDepartment("Computer Science & Engineering");
                student.setYear(3);
                student.setSemester(5);
                student.setInterests("Coding, Web Development");
            } catch (SQLException ignored) {
                // Ignore if concurrent insertion completed
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
