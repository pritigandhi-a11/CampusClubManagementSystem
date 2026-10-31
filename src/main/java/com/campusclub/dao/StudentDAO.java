package com.campusclub.dao;

import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.model.Student;
import com.campusclub.model.UserRole;
import com.campusclub.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Student entity operations.
 */
public class StudentDAO {
    private final UserDAO userDAO = new UserDAO();

    public Optional<Student> findById(int studentId) throws DatabaseOperationException {
        String sql = "SELECT u.*, s.student_id AS student_table_id, s.register_number, s.department, s.year, s.semester, s.interests " +
                     "FROM students s " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "WHERE s.student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStudent(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding student by ID: " + studentId, e);
        }
        return Optional.empty();
    }

    public Optional<Student> findByUserId(int userId) throws DatabaseOperationException {
        String sql = "SELECT u.*, s.student_id AS student_table_id, s.register_number, s.department, s.year, s.semester, s.interests " +
                     "FROM students s " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "WHERE s.user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStudent(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding student by User ID: " + userId, e);
        }
        return Optional.empty();
    }

    public Optional<Student> findByRegisterNumber(String registerNumber) throws DatabaseOperationException {
        if (registerNumber == null) {
            return Optional.empty();
        }
        String sql = "SELECT u.*, s.student_id AS student_table_id, s.register_number, s.department, s.year, s.semester, s.interests " +
                     "FROM students s " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "WHERE s.register_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, registerNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStudent(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding student by Register Number: " + registerNumber, e);
        }
        return Optional.empty();
    }

    public List<Student> findAll() throws DatabaseOperationException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT u.*, s.student_id AS student_table_id, s.register_number, s.department, s.year, s.semester, s.interests " +
                     "FROM students s " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "ORDER BY u.full_name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                students.add(mapResultSetToStudent(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all students", e);
        }
        return students;
    }

    public List<Student> findAllStudents() throws DatabaseOperationException {
        return findAll();
    }

    public int createStudent(Student student) throws DatabaseOperationException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Create User record if userId <= 0
            int userId = student.getUserId();
            if (userId <= 0) {
                student.setRole(UserRole.STUDENT);
                userId = userDAO.createUser(student);
                student.setUserId(userId);
            }

            // 2. Insert into students table
            String sql = "INSERT INTO students (user_id, register_number, department, year, semester, phone, interests) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setString(2, student.getRegisterNumber());
                ps.setString(3, student.getDepartment());
                ps.setInt(4, student.getYear());
                ps.setInt(5, student.getSemester());
                ps.setString(6, student.getPhoneNumber());
                ps.setString(7, student.getInterests());

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int studentId = rs.getInt(1);
                        student.setStudentTableId(studentId);
                        conn.commit();
                        return studentId;
                    }
                }
            }
            conn.commit();
            return userId;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }
            }
            throw new DatabaseOperationException("Error creating student: " + student.getUsername(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    System.err.println("Warning: Failed to close connection cleanly: " + closeEx.getMessage());
                }
            }
        }
    }

    /**
     * Ensures that a student profile record exists in the students table for an existing user_id.
     * Prevents foreign key and duplicate key violations while ensuring relational consistency.
     */
    public boolean ensureStudentRecordExists(int userId, String registerNumber, String department, int year, int semester, String phone, String interests) {
        if (userId <= 0) return false;
        String checkSql = "SELECT student_id FROM students WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            psCheck.setInt(1, userId);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }

            String regNo = (registerNumber != null && !registerNumber.trim().isEmpty()) ? registerNumber.trim() : ("REG" + userId);
            String insertSql = "INSERT INTO students (user_id, register_number, department, year, semester, phone, interests) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, userId);
                ps.setString(2, regNo);
                ps.setString(3, department != null ? department : "Computer Science & Engineering");
                ps.setInt(4, year > 0 ? year : 1);
                ps.setInt(5, semester > 0 ? semester : 1);
                ps.setString(6, phone != null ? phone : "+91 9952780039");
                ps.setString(7, interests != null ? interests : "Coding");
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean updateStudent(Student student) throws DatabaseOperationException {
        String sql = "UPDATE students SET register_number = ?, department = ?, year = ?, semester = ?, phone = ?, interests = ? " +
                     "WHERE student_id = ? OR user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getRegisterNumber());
            ps.setString(2, student.getDepartment());
            ps.setInt(3, student.getYear());
            ps.setInt(4, student.getSemester());
            ps.setString(5, student.getPhoneNumber());
            ps.setString(6, student.getInterests());
            ps.setInt(7, student.getStudentTableId());
            ps.setInt(8, student.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating student details for ID: " + student.getUserId(), e);
        }
    }

    public boolean updateStudentProfile(Student student) throws DatabaseOperationException {
        return updateStudent(student);
    }

    private Student mapResultSetToStudent(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setUserId(rs.getInt("user_id"));
        s.setStudentTableId(rs.getInt("student_table_id"));
        s.setUsername(rs.getString("username"));
        s.setPasswordHash(rs.getString("password_hash"));
        s.setFullName(rs.getString("full_name"));
        s.setEmail(rs.getString("email"));
        s.setPhoneNumber(rs.getString("phone_number"));
        s.setRole(UserRole.STUDENT);
        s.setRegisterNumber(rs.getString("register_number"));
        s.setDepartment(rs.getString("department"));
        s.setYear(rs.getInt("year"));
        s.setSemester(rs.getInt("semester"));
        s.setInterests(rs.getString("interests"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            s.setCreatedAt(ts.toLocalDateTime());
        }
        return s;
    }
}
