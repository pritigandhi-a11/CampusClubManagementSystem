package com.campusclub.service;

import com.campusclub.exception.ClubNotFoundException;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.exception.EventRegistrationException;
import com.campusclub.model.Announcement;
import com.campusclub.model.Event;
import com.campusclub.model.EventRegistration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface defining operations on Events, Event Registrations, and Announcements.
 * Demonstrates Abstraction and Interface Segregation in OOP.
 */
public interface EventOperations {
    List<Event> getAllEvents() throws DatabaseOperationException;
    List<Event> getUpcomingEvents() throws DatabaseOperationException;
    List<Event> getEventsByClub(int clubId) throws DatabaseOperationException;
    Optional<Event> getEventById(int eventId) throws DatabaseOperationException;
    int createEvent(Event event) throws ClubNotFoundException, DatabaseOperationException;
    boolean updateEvent(Event event) throws DatabaseOperationException;
    boolean deleteEvent(int eventId) throws DatabaseOperationException;
    int registerForEvent(int eventId, int studentId) throws EventRegistrationException, DatabaseOperationException;
    boolean cancelRegistration(int eventId, int studentId) throws DatabaseOperationException;
    List<EventRegistration> getStudentRegistrations(int studentId) throws DatabaseOperationException;
    List<EventRegistration> getEventParticipants(int eventId) throws DatabaseOperationException;
    boolean markAttendance(int registrationId, boolean attended) throws DatabaseOperationException;
    List<Announcement> getAllAnnouncements() throws DatabaseOperationException;
    List<Announcement> getLatestAnnouncements(int limit) throws DatabaseOperationException;
    List<Announcement> getAnnouncementsForClub(int clubId) throws DatabaseOperationException;
    int postAnnouncement(Announcement announcement) throws DatabaseOperationException;

    // Search, Sort & Statistics (Data Structures: List, Map, Comparator)
    List<Event> searchEvents(String keyword) throws DatabaseOperationException;
    List<Event> sortEventsByDate(List<Event> events, boolean ascending);
    Map<Integer, Integer> getEventCountPerClub() throws DatabaseOperationException;
}
