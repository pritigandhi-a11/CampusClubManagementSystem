package com.campusclub.dao;

import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.model.EventRegistration;
import com.campusclub.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Event Registrations.
 */
public class EventRegistrationDAO {

    public boolean isAlreadyRegistered(int eventId, int studentId) throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM event_registrations r " +
                     "LEFT JOIN students s ON r.student_id = s.student_id " +
                     "WHERE r.event_id = ? AND (r.student_id = ? OR s.user_id = ? OR r.student_id = (SELECT s2.student_id FROM students s2 WHERE s2.user_id = ? LIMIT 1)) " +
                     "AND r.registration_status = 'REGISTERED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setInt(2, studentId);
            ps.setInt(3, studentId);
            ps.setInt(4, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error checking existing event registration", e);
        }
        return false;
    }

    public int getRegistrationCount(int eventId) throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM event_registrations WHERE event_id = ? AND registration_status = 'REGISTERED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error getting registration count for event: " + eventId, e);
        }
        return 0;
    }

    public Optional<EventRegistration> findByEventAndStudent(int eventId, int studentId) throws DatabaseOperationException {
        String sql = "SELECT r.*, e.event_name, u.full_name AS student_name, s.register_number " +
                     "FROM event_registrations r " +
                     "JOIN events e ON r.event_id = e.event_id " +
                     "JOIN students s ON r.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "WHERE r.event_id = ? AND (r.student_id = ? OR s.user_id = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setInt(2, studentId);
            ps.setInt(3, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRegistration(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error checking registration for event " + eventId + ", student " + studentId, e);
        }
        return Optional.empty();
    }

    public List<EventRegistration> findByStudentId(int studentId) throws DatabaseOperationException {
        List<EventRegistration> list = new ArrayList<>();
        String sql = "SELECT r.*, e.event_name, u.full_name AS student_name, s.register_number " +
                     "FROM event_registrations r " +
                     "JOIN events e ON r.event_id = e.event_id " +
                     "JOIN students s ON r.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "WHERE r.student_id = ? OR s.user_id = ? " +
                     "ORDER BY r.registered_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRegistration(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching registrations for student: " + studentId, e);
        }
        return list;
    }

    public List<EventRegistration> getEventParticipants(int eventId) throws DatabaseOperationException {
        List<EventRegistration> list = new ArrayList<>();
        String sql = "SELECT r.*, e.event_name, u.full_name AS student_name, s.register_number, u.email " +
                     "FROM event_registrations r " +
                     "JOIN events e ON r.event_id = e.event_id " +
                     "JOIN students s ON r.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "WHERE r.event_id = ? AND r.registration_status = 'REGISTERED' " +
                     "ORDER BY r.registered_at ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EventRegistration reg = mapResultSetToRegistration(rs);
                    reg.setStudentEmail(rs.getString("email"));
                    list.add(reg);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching participants for event: " + eventId, e);
        }
        return list;
    }

    public List<EventRegistration> findByEventId(int eventId) throws DatabaseOperationException {
        return getEventParticipants(eventId);
    }

    public int registerForEvent(int eventId, int studentId) throws DatabaseOperationException {
        EventRegistration reg = new EventRegistration();
        reg.setEventId(eventId);
        reg.setStudentId(studentId);
        reg.setRegistrationStatus("REGISTERED");
        reg.setAttended(false);
        return registerForEvent(reg);
    }

    private int resolveStudentTableId(Connection conn, int studentOrUserId) {
        String query = "SELECT student_id FROM students WHERE student_id = ? OR user_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, studentOrUserId);
            ps.setInt(2, studentOrUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ignored) {
        }
        return studentOrUserId;
    }

    public int registerForEvent(EventRegistration registration) throws DatabaseOperationException {
        String sql = "INSERT INTO event_registrations (event_id, student_id, registration_status, attended) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int actualStudentId = resolveStudentTableId(conn, registration.getStudentId());
            ps.setInt(1, registration.getEventId());
            ps.setInt(2, actualStudentId);
            ps.setString(3, registration.getRegistrationStatus() != null ? registration.getRegistrationStatus() : "REGISTERED");
            ps.setBoolean(4, registration.isAttended());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    registration.setRegistrationId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error registering for event", e);
        }
        return 0;
    }

    public boolean updateAttendance(int registrationId, boolean attended) throws DatabaseOperationException {
        String sql = "UPDATE event_registrations SET attended = ? WHERE registration_id = ?";
        String syncAttendanceSql = "INSERT INTO attendance (registration_id, event_id, student_id, status) " +
                                   "SELECT registration_id, event_id, student_id, ? FROM event_registrations WHERE registration_id = ? " +
                                   "ON DUPLICATE KEY UPDATE status = VALUES(status)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, attended);
            ps.setInt(2, registrationId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                try (PreparedStatement syncPs = conn.prepareStatement(syncAttendanceSql)) {
                    syncPs.setString(1, attended ? "PRESENT" : "ABSENT");
                    syncPs.setInt(2, registrationId);
                    syncPs.executeUpdate();
                } catch (Exception ignored) {
                    // Safe best-effort sync for standalone or mock environments
                }
            }
            return updated;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating attendance for registration: " + registrationId, e);
        }
    }

    public boolean cancelRegistration(int eventId, int studentId) throws DatabaseOperationException {
        String sql = "UPDATE event_registrations SET registration_status = 'CANCELLED' " +
                     "WHERE event_id = ? AND (student_id = ? OR student_id = (SELECT student_id FROM students WHERE user_id = ? LIMIT 1))";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setInt(2, studentId);
            ps.setInt(3, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error cancelling event registration", e);
        }
    }

    private EventRegistration mapResultSetToRegistration(ResultSet rs) throws SQLException {
        EventRegistration r = new EventRegistration();
        r.setRegistrationId(rs.getInt("registration_id"));
        r.setEventId(rs.getInt("event_id"));
        r.setStudentId(rs.getInt("student_id"));
        r.setEventTitle(rs.getString("event_name"));
        r.setStudentName(rs.getString("student_name"));
        r.setStudentRegisterNumber(rs.getString("register_number"));
        Timestamp ts = rs.getTimestamp("registered_at");
        if (ts != null) {
            r.setRegisteredAt(ts.toLocalDateTime());
        }
        r.setRegistrationStatus(rs.getString("registration_status"));
        r.setAttended(rs.getBoolean("attended"));
        return r;
    }
}
