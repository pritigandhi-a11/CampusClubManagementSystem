package com.campusclub.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * [OOP: Encapsulation & Composition]
 * Represents an organized campus event by a club.
 * Composes student event registrations and enforces event capacity limits.
 */
public class Event {

    // [OOP: Encapsulation - Private fields]
    private int eventId;
    private int clubId;
    private String clubName;
    private String eventName;
    private String description;
    private LocalDateTime eventDate;
    private String venue;
    private int capacity;
    private LocalDateTime registrationDeadline;
    private LocalDateTime createdAt;
    private int registeredCount;

    private String status = "UPCOMING"; // UPCOMING, ONGOING, COMPLETED, CANCELLED
    private String coordinatorName;

    // [OOP: Composition - Event maintains registered attendees]
    private final List<EventRegistration> registrations = new ArrayList<>();

    public Event() {
        this.createdAt = LocalDateTime.now();
        this.capacity = 100;
        this.status = "UPCOMING";
    }

    public Event(int eventId, int clubId, String eventName, String description,
                 LocalDateTime eventDate, String venue, int capacity, LocalDateTime registrationDeadline) {
        this.eventId = eventId;
        this.clubId = clubId;
        this.eventName = eventName;
        this.description = description;
        this.eventDate = eventDate;
        this.venue = venue;
        this.capacity = capacity;
        this.registrationDeadline = registrationDeadline;
        this.createdAt = LocalDateTime.now();
        this.status = "UPCOMING";
    }

    public Event(int eventId, int clubId, String eventName, String description,
                 LocalDateTime eventDate, String venue, int capacity, LocalDateTime registrationDeadline,
                 String coordinatorName, String status) {
        this.eventId = eventId;
        this.clubId = clubId;
        this.eventName = eventName;
        this.description = description;
        this.eventDate = eventDate;
        this.venue = venue;
        this.capacity = capacity;
        this.registrationDeadline = registrationDeadline;
        this.coordinatorName = coordinatorName;
        this.status = status != null ? status : "UPCOMING";
        this.createdAt = LocalDateTime.now();
    }

    // ==========================================================
    // [OOP: Composition - Managing Registrations]
    // ==========================================================

    public void addRegistration(EventRegistration registration) {
        if (registration != null && !registrations.contains(registration)) {
            registrations.add(registration);
            this.registeredCount = registrations.size();
        }
    }

    public List<EventRegistration> getRegistrations() {
        return Collections.unmodifiableList(registrations);
    }

    public void setRegistrations(List<EventRegistration> newRegistrations) {
        this.registrations.clear();
        if (newRegistrations != null) {
            for (EventRegistration reg : newRegistrations) {
                addRegistration(reg);
            }
        }
    }

    public boolean isFull() {
        return (registeredCount >= capacity) || (registrations.size() >= capacity);
    }

    // ==========================================================
    // [OOP: Encapsulation - Accessors and Mutators]
    // ==========================================================

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getClubId() {
        return clubId;
    }

    public void setClubId(int clubId) {
        this.clubId = clubId;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getTitle() {
        return eventName;
    }

    public void setTitle(String title) {
        this.eventName = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getLocation() {
        return venue;
    }

    public void setLocation(String location) {
        this.venue = location;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        if (capacity > 0) {
            this.capacity = capacity;
        }
    }

    public int getMaxCapacity() {
        return capacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        setCapacity(maxCapacity);
    }

    public LocalDateTime getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDateTime registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getRegisteredCount() {
        return Math.max(registeredCount, registrations.size());
    }

    public void setRegisteredCount(int registeredCount) {
        this.registeredCount = registeredCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCoordinatorName() {
        return coordinatorName;
    }

    public void setCoordinatorName(String coordinatorName) {
        this.coordinatorName = coordinatorName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return eventId == event.eventId && clubId == event.clubId && Objects.equals(eventName, event.eventName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, clubId, eventName);
    }

    @Override
    public String toString() {
        return String.format("%s [Club: %s, Date: %s, Venue: %s, Capacity: %d/%d]",
                eventName, clubName != null ? clubName : clubId, eventDate, venue, getRegisteredCount(), capacity);
    }
}
