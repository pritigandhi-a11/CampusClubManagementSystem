package com.campusclub.service;

import com.campusclub.dao.AnnouncementDAO;
import com.campusclub.dao.ClubDAO;
import com.campusclub.dao.EventDAO;
import com.campusclub.dao.EventRegistrationDAO;
import com.campusclub.exception.ClubNotFoundException;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.exception.EventRegistrationException;
import com.campusclub.model.Announcement;
import com.campusclub.model.Event;
import com.campusclub.model.EventRegistration;
import com.campusclub.util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service managing Event schedules, registrations, capacity constraints, and Announcements.
 * Implements EventOperations interface (Abstraction).
 * Demonstrates Collections usage: ArrayList, HashMap, and Comparator.
 */
public class EventService implements EventOperations {
    private final EventDAO eventDAO;
    private final EventRegistrationDAO registrationDAO;
    private final AnnouncementDAO announcementDAO;
    private final ClubDAO clubDAO;

    public EventService() {
        this.eventDAO = new EventDAO();
        this.registrationDAO = new EventRegistrationDAO();
        this.announcementDAO = new AnnouncementDAO();
        this.clubDAO = new ClubDAO();
    }

    public EventService(EventDAO eventDAO, EventRegistrationDAO registrationDAO, AnnouncementDAO announcementDAO, ClubDAO clubDAO) {
        this.eventDAO = eventDAO;
        this.registrationDAO = registrationDAO;
        this.announcementDAO = announcementDAO;
        this.clubDAO = clubDAO;
    }

    @Override
    public List<Event> getAllEvents() throws DatabaseOperationException {
        return eventDAO.findAll();
    }

    @Override
    public List<Event> getUpcomingEvents() throws DatabaseOperationException {
        return eventDAO.findUpcomingEvents();
    }

    @Override
    public List<Event> getEventsByClub(int clubId) throws DatabaseOperationException {
        if (clubId <= 0) {
            throw new IllegalArgumentException("Invalid club ID: " + clubId);
        }
        return eventDAO.findEventsByClub(clubId);
    }

    @Override
    public Optional<Event> getEventById(int eventId) throws DatabaseOperationException {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Invalid event ID: " + eventId);
        }
        return eventDAO.findById(eventId);
    }

    @Override
    public int createEvent(Event event) throws ClubNotFoundException, DatabaseOperationException {
        if (event == null) {
            throw new IllegalArgumentException("Event object cannot be null.");
        }
        ValidationUtil.validateRequired(event.getEventName(), "Event name");
        ValidationUtil.validatePositiveNumber(event.getClubId(), "Club ID");
        ValidationUtil.validateDate(event.getEventDate(), "Event date");
        ValidationUtil.validateEventCapacity(event.getCapacity());

        if (clubDAO.findById(event.getClubId()).isEmpty()) {
            throw new ClubNotFoundException("Cannot create event. Club ID " + event.getClubId() + " does not exist.");
        }
        return eventDAO.createEvent(event);
    }

    @Override
    public boolean updateEvent(Event event) throws DatabaseOperationException {
        if (event == null || event.getEventId() <= 0) {
            throw new IllegalArgumentException("Invalid event details for update.");
        }
        ValidationUtil.validateRequired(event.getEventName(), "Event name");
        ValidationUtil.validateDate(event.getEventDate(), "Event date");
        ValidationUtil.validateEventCapacity(event.getCapacity());

        return eventDAO.updateEvent(event);
    }

    @Override
    public boolean deleteEvent(int eventId) throws DatabaseOperationException {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Invalid event ID for deletion: " + eventId);
        }
        return eventDAO.deleteEvent(eventId);
    }

    @Override
    public int registerForEvent(int eventId, int studentId)
            throws EventRegistrationException, DatabaseOperationException {
        if (eventId <= 0 || studentId <= 0) {
            throw new EventRegistrationException("Invalid event or student ID.");
        }

        Optional<Event> optionalEvent = eventDAO.findById(eventId);
        if (optionalEvent.isEmpty()) {
            throw new EventRegistrationException("Event does not exist (ID: " + eventId + ").");
        }
        Event event = optionalEvent.get();

        if ("CANCELLED".equalsIgnoreCase(event.getStatus()) || "COMPLETED".equalsIgnoreCase(event.getStatus())) {
            throw new EventRegistrationException("Registration is closed. Event is " + event.getStatus() + ".");
        }

        if (event.getRegistrationDeadline() != null && LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new EventRegistrationException("Registration closed for this event on " + event.getRegistrationDeadline());
        }

        if (registrationDAO.isAlreadyRegistered(eventId, studentId)) {
            throw new EventRegistrationException("You are already registered for this event.");
        }

        int currentRegistrations = registrationDAO.getRegistrationCount(eventId);
        if (currentRegistrations >= event.getCapacity()) {
            throw new EventRegistrationException("Event has reached maximum capacity (" + event.getCapacity() + " seats).");
        }

        EventRegistration registration = new EventRegistration();
        registration.setEventId(eventId);
        registration.setStudentId(studentId);
        registration.setRegisteredAt(LocalDateTime.now());
        registration.setRegistrationStatus("REGISTERED");
        registration.setAttended(false);

        return registrationDAO.registerForEvent(registration);
    }

    @Override
    public boolean cancelRegistration(int eventId, int studentId) throws DatabaseOperationException {
        if (eventId <= 0 || studentId <= 0) {
            throw new IllegalArgumentException("Invalid event ID or student ID for cancelling registration.");
        }
        return registrationDAO.cancelRegistration(eventId, studentId);
    }

    @Override
    public List<EventRegistration> getStudentRegistrations(int studentId) throws DatabaseOperationException {
        if (studentId <= 0) {
            throw new IllegalArgumentException("Invalid student ID: " + studentId);
        }
        return registrationDAO.findByStudentId(studentId);
    }

    @Override
    public List<EventRegistration> getEventParticipants(int eventId) throws DatabaseOperationException {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Invalid event ID: " + eventId);
        }
        return registrationDAO.getEventParticipants(eventId);
    }

    @Override
    public boolean markAttendance(int registrationId, boolean attended) throws DatabaseOperationException {
        if (registrationId <= 0) {
            throw new IllegalArgumentException("Invalid registration ID: " + registrationId);
        }
        return registrationDAO.updateAttendance(registrationId, attended);
    }

    @Override
    public List<Announcement> getAllAnnouncements() throws DatabaseOperationException {
        return announcementDAO.findAll();
    }

    @Override
    public List<Announcement> getLatestAnnouncements(int limit) throws DatabaseOperationException {
        if (limit <= 0) {
            limit = 10;
        }
        return announcementDAO.findLatestAnnouncements(limit);
    }

    @Override
    public List<Announcement> getAnnouncementsForClub(int clubId) throws DatabaseOperationException {
        if (clubId <= 0) {
            throw new IllegalArgumentException("Invalid club ID: " + clubId);
        }
        return announcementDAO.findByClub(clubId);
    }

    @Override
    public int postAnnouncement(Announcement announcement) throws DatabaseOperationException {
        if (announcement == null) {
            throw new IllegalArgumentException("Announcement object cannot be null.");
        }
        ValidationUtil.validateRequired(announcement.getTitle(), "Announcement title");
        ValidationUtil.validateRequired(announcement.getMessage(), "Announcement message");
        ValidationUtil.validatePositiveNumber(announcement.getClubId(), "Club ID");

        return announcementDAO.createAnnouncement(announcement);
    }

    // ==========================================================
    // Collections, Searching, Sorting & Statistics
    // ==========================================================

    @Override
    public List<Event> searchEvents(String keyword) throws DatabaseOperationException {
        List<Event> all = getAllEvents();
        if (keyword == null || keyword.trim().isEmpty()) {
            return all;
        }
        String term = keyword.trim().toLowerCase();
        List<Event> results = new ArrayList<>();
        for (Event e : all) {
            boolean nameMatch = e.getEventName() != null && e.getEventName().toLowerCase().contains(term);
            boolean venueMatch = e.getVenue() != null && e.getVenue().toLowerCase().contains(term);
            boolean descMatch = e.getDescription() != null && e.getDescription().toLowerCase().contains(term);
            boolean clubMatch = e.getClubName() != null && e.getClubName().toLowerCase().contains(term);
            if (nameMatch || venueMatch || descMatch || clubMatch) {
                results.add(e);
            }
        }
        return results;
    }

    @Override
    public List<Event> sortEventsByDate(List<Event> events, boolean ascending) {
        if (events == null) return Collections.emptyList();
        List<Event> sorted = new ArrayList<>(events);
        Comparator<Event> comparator = Comparator.comparing(
                e -> e.getEventDate() != null ? e.getEventDate() : LocalDateTime.MIN
        );
        if (!ascending) {
            comparator = comparator.reversed();
        }
        sorted.sort(comparator);
        return sorted;
    }

    @Override
    public Map<Integer, Integer> getEventCountPerClub() throws DatabaseOperationException {
        List<Event> events = getAllEvents();
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Event e : events) {
            countMap.put(e.getClubId(), countMap.getOrDefault(e.getClubId(), 0) + 1);
        }
        return countMap;
    }
}
