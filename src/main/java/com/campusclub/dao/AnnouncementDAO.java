package com.campusclub.dao;

import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.model.Announcement;
import com.campusclub.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Club Announcements.
 */
public class AnnouncementDAO {

    public List<Announcement> findAll() throws DatabaseOperationException {
        List<Announcement> list = new ArrayList<>();
        String sql = "SELECT a.*, c.club_name FROM announcements a " +
                     "JOIN clubs c ON a.club_id = c.club_id " +
                     "ORDER BY a.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToAnnouncement(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching all announcements", e);
        }
        return list;
    }

    public List<Announcement> findLatestAnnouncements(int limit) throws DatabaseOperationException {
        List<Announcement> list = new ArrayList<>();
        String sql = "SELECT a.*, c.club_name FROM announcements a " +
                     "JOIN clubs c ON a.club_id = c.club_id " +
                     "ORDER BY a.created_at DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 10);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAnnouncement(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching latest announcements", e);
        }
        return list;
    }

    public List<Announcement> findByClub(int clubId) throws DatabaseOperationException {
        List<Announcement> list = new ArrayList<>();
        String sql = "SELECT a.*, c.club_name FROM announcements a " +
                     "JOIN clubs c ON a.club_id = c.club_id " +
                     "WHERE a.club_id = ? ORDER BY a.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAnnouncement(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching announcements for club: " + clubId, e);
        }
        return list;
    }

    public List<Announcement> findByClubId(int clubId) throws DatabaseOperationException {
        return findByClub(clubId);
    }

    public int createAnnouncement(Announcement announcement) throws DatabaseOperationException {
        String sql = "INSERT INTO announcements (club_id, title, message, priority) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, announcement.getClubId());
            ps.setString(2, announcement.getTitle());
            ps.setString(3, announcement.getMessage());
            ps.setString(4, announcement.getPriority() != null ? announcement.getPriority() : "MEDIUM");

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    announcement.setAnnouncementId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error creating announcement", e);
        }
        return 0;
    }

    private Announcement mapResultSetToAnnouncement(ResultSet rs) throws SQLException {
        Announcement a = new Announcement();
        a.setAnnouncementId(rs.getInt("announcement_id"));
        a.setClubId(rs.getInt("club_id"));
        a.setClubName(rs.getString("club_name"));
        a.setTitle(rs.getString("title"));
        a.setMessage(rs.getString("message"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            a.setCreatedAt(ts.toLocalDateTime());
        }
        a.setPriority(rs.getString("priority"));
        return a;
    }
}
