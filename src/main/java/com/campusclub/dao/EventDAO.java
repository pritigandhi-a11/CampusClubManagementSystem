package com.campusclub.dao;

import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.model.Event;
import com.campusclub.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Event management.
 */
public class EventDAO {

    public Optional<Event> findById(int eventId) throws DatabaseOperationException {
        String sql = "SELECT e.*, c.club_name, " +
                     "(SELECT COUNT(*) FROM event_registrations r WHERE r.event_id = e.event_id AND r.registration_status = 'REGISTERED') AS reg_count " +
                     "FROM events e " +
                     "JOIN clubs c ON e.club_id = c.club_id " +
                     "WHERE e.event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEvent(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding event by ID: " + eventId, e);
        }
        return Optional.empty();
    }

    public List<Event> findAll() throws DatabaseOperationException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT e.*, c.club_name, " +
                     "(SELECT COUNT(*) FROM event_registrations r WHERE r.event_id = e.event_id AND r.registration_status = 'REGISTERED') AS reg_count " +
                     "FROM events e " +
                     "JOIN clubs c ON e.club_id = c.club_id " +
                     "ORDER BY e.event_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all events", e);
        }
        return events;
    }

    public List<Event> findUpcomingEvents() throws DatabaseOperationException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT e.*, c.club_name, " +
                     "(SELECT COUNT(*) FROM event_registrations r WHERE r.event_id = e.event_id AND r.registration_status = 'REGISTERED') AS reg_count " +
                     "FROM events e " +
                     "JOIN clubs c ON e.club_id = c.club_id " +
                     "WHERE e.event_date >= NOW() " +
                     "ORDER BY e.event_date ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving upcoming events", e);
        }
        return events;
    }

    public List<Event> findEventsByClub(int clubId) throws DatabaseOperationException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT e.*, c.club_name, " +
                     "(SELECT COUNT(*) FROM event_registrations r WHERE r.event_id = e.event_id AND r.registration_status = 'REGISTERED') AS reg_count " +
                     "FROM events e " +
                     "JOIN clubs c ON e.club_id = c.club_id " +
                     "WHERE e.club_id = ? " +
                     "ORDER BY e.event_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvent(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving events for club: " + clubId, e);
        }
        return events;
    }

    public List<Event> findByClubId(int clubId) throws DatabaseOperationException {
        return findEventsByClub(clubId);
    }

    public int createEvent(Event event) throws DatabaseOperationException {
        String sql = "INSERT INTO events (club_id, event_name, description, event_date, venue, capacity, registration_deadline) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, event.getClubId());
            ps.setString(2, event.getEventName());
            ps.setString(3, event.getDescription());
            ps.setTimestamp(4, Timestamp.valueOf(event.getEventDate()));
            ps.setString(5, event.getVenue());
            ps.setInt(6, event.getCapacity());
            if (event.getRegistrationDeadline() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(event.getRegistrationDeadline()));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    event.setEventId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error creating event: " + event.getEventName(), e);
        }
        return 0;
    }

    public boolean updateEvent(Event event) throws DatabaseOperationException {
        String sql = "UPDATE events SET club_id = ?, event_name = ?, description = ?, event_date = ?, venue = ?, capacity = ?, registration_deadline = ? " +
                     "WHERE event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, event.getClubId());
            ps.setString(2, event.getEventName());
            ps.setString(3, event.getDescription());
            ps.setTimestamp(4, Timestamp.valueOf(event.getEventDate()));
            ps.setString(5, event.getVenue());
            ps.setInt(6, event.getCapacity());
            if (event.getRegistrationDeadline() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(event.getRegistrationDeadline()));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }
            ps.setInt(8, event.getEventId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating event ID: " + event.getEventId(), e);
        }
    }

    public boolean deleteEvent(int eventId) throws DatabaseOperationException {
        String sql = "DELETE FROM events WHERE event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting event ID: " + eventId, e);
        }
    }

    private Event mapResultSetToEvent(ResultSet rs) throws SQLException {
        Event e = new Event();
        e.setEventId(rs.getInt("event_id"));
        e.setClubId(rs.getInt("club_id"));
        e.setClubName(rs.getString("club_name"));
        e.setEventName(rs.getString("event_name"));
        e.setDescription(rs.getString("description"));
        Timestamp dateTs = rs.getTimestamp("event_date");
        if (dateTs != null) {
            e.setEventDate(dateTs.toLocalDateTime());
        }
        e.setVenue(rs.getString("venue"));
        e.setCapacity(rs.getInt("capacity"));
        Timestamp deadlineTs = rs.getTimestamp("registration_deadline");
        if (deadlineTs != null) {
            e.setRegistrationDeadline(deadlineTs.toLocalDateTime());
        }
        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            e.setCreatedAt(createdTs.toLocalDateTime());
        }
        e.setRegisteredCount(rs.getInt("reg_count"));
        return e;
    }
}
