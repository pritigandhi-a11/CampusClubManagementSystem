package com.campusclub.dao;

import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.model.Membership;
import com.campusclub.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Club Membership records.
 */
public class MembershipDAO {

    public boolean isAlreadyMember(int studentId, int clubId) throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM memberships m " +
                     "LEFT JOIN students s ON m.student_id = s.student_id " +
                     "WHERE (m.student_id = ? OR s.user_id = ? OR m.student_id = (SELECT s2.student_id FROM students s2 WHERE s2.user_id = ? LIMIT 1)) " +
                     "AND m.club_id = ? AND m.membership_status IN ('APPROVED', 'PENDING')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            ps.setInt(3, studentId);
            ps.setInt(4, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error checking existing membership", e);
        }
        return false;
    }

    public Optional<Membership> findByStudentAndClub(int studentId, int clubId) throws DatabaseOperationException {
        String sql = "SELECT m.*, u.full_name AS student_name, s.register_number, c.club_name " +
                     "FROM memberships m " +
                     "JOIN students s ON m.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN clubs c ON m.club_id = c.club_id " +
                     "WHERE (m.student_id = ? OR s.user_id = ?) AND m.club_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            ps.setInt(3, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMembership(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error checking membership for student " + studentId + " in club " + clubId, e);
        }
        return Optional.empty();
    }

    public List<Membership> findStudentMemberships(int studentId) throws DatabaseOperationException {
        List<Membership> memberships = new ArrayList<>();
        String sql = "SELECT m.*, u.full_name AS student_name, s.register_number, c.club_name " +
                     "FROM memberships m " +
                     "JOIN students s ON m.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN clubs c ON m.club_id = c.club_id " +
                     "WHERE m.student_id = ? OR s.user_id = ? " +
                     "ORDER BY m.joined_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    memberships.add(mapResultSetToMembership(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching memberships for student: " + studentId, e);
        }
        return memberships;
    }

    public List<Membership> findByStudentId(int studentId) throws DatabaseOperationException {
        return findStudentMemberships(studentId);
    }

    public List<Membership> findClubMembers(int clubId) throws DatabaseOperationException {
        List<Membership> memberships = new ArrayList<>();
        String sql = "SELECT m.*, u.full_name AS student_name, s.register_number, c.club_name " +
                     "FROM memberships m " +
                     "JOIN students s ON m.student_id = s.student_id " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "JOIN clubs c ON m.club_id = c.club_id " +
                     "WHERE m.club_id = ? ORDER BY m.joined_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    memberships.add(mapResultSetToMembership(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching members for club: " + clubId, e);
        }
        return memberships;
    }

    public List<Membership> findByClubId(int clubId) throws DatabaseOperationException {
        return findClubMembers(clubId);
    }

    public int joinClub(int studentId, int clubId) throws DatabaseOperationException {
        Membership m = new Membership();
        m.setStudentId(studentId);
        m.setClubId(clubId);
        m.setMembershipStatus("PENDING");
        m.setRoleInClub("MEMBER");
        return createMembership(m);
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

    public int createMembership(Membership membership) throws DatabaseOperationException {
        String sql = "INSERT INTO memberships (student_id, club_id, membership_status, role_in_club) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int actualStudentId = resolveStudentTableId(conn, membership.getStudentId());
            ps.setInt(1, actualStudentId);
            ps.setInt(2, membership.getClubId());
            ps.setString(3, membership.getMembershipStatus() != null ? membership.getMembershipStatus() : "PENDING");
            ps.setString(4, membership.getRoleInClub() != null ? membership.getRoleInClub() : "MEMBER");

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    membership.setMembershipId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error creating membership", e);
        }
        return 0;
    }

    public boolean leaveClub(int studentId, int clubId) throws DatabaseOperationException {
        String sql = "DELETE FROM memberships WHERE (student_id = ? OR student_id = (SELECT s.student_id FROM students s WHERE s.user_id = ? LIMIT 1)) AND club_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            ps.setInt(3, clubId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error leaving club", e);
        }
    }

    public boolean updateStatus(int membershipId, String newStatus) throws DatabaseOperationException {
        String sql = "UPDATE memberships SET membership_status = ? WHERE membership_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, membershipId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating membership status: " + membershipId, e);
        }
    }

    private Membership mapResultSetToMembership(ResultSet rs) throws SQLException {
        Membership m = new Membership();
        m.setMembershipId(rs.getInt("membership_id"));
        m.setStudentId(rs.getInt("student_id"));
        m.setClubId(rs.getInt("club_id"));
        m.setStudentName(rs.getString("student_name"));
        m.setStudentRegisterNumber(rs.getString("register_number"));
        m.setClubName(rs.getString("club_name"));
        Timestamp ts = rs.getTimestamp("joined_at");
        if (ts != null) {
            m.setJoinedAt(ts.toLocalDateTime());
        }
        m.setMembershipStatus(rs.getString("membership_status"));
        m.setRoleInClub(rs.getString("role_in_club"));
        return m;
    }
}
