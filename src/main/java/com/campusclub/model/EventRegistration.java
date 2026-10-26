package com.campusclub.model;

import java.time.LocalDateTime;

/**
 * Represents a student's registration for a specific event.
 * Demonstrates Composition and Encapsulation.
 */
public class EventRegistration {
    private int registrationId;
    private int eventId;
    private int studentId;
    private String eventTitle;
    private String clubName;
    private LocalDateTime eventDate;
    private String location;
    private String studentName;
    private String studentEmail;
    private String studentRegisterNumber;
    private LocalDateTime registeredAt;
    private String registrationStatus; // REGISTERED, CANCELLED, WAITLISTED
    private boolean attended;

    public EventRegistration() {
        this.registeredAt = LocalDateTime.now();
        this.registrationStatus = "REGISTERED";
        this.attended = false;
    }

    public EventRegistration(int registrationId, int eventId, int studentId,
                             LocalDateTime registeredAt, String registrationStatus, boolean attended) {
        this.registrationId = registrationId;
        this.eventId = eventId;
        this.studentId = studentId;
        this.registeredAt = registeredAt != null ? registeredAt : LocalDateTime.now();
        this.registrationStatus = registrationStatus != null ? registrationStatus : "REGISTERED";
        this.attended = attended;
    }

    // Getters and Setters
    public int getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(int registrationId) {
        this.registrationId = registrationId;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    // Event title / name aliases
    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public String getEventName() {
        return eventTitle;
    }

    public void setEventName(String eventName) {
        this.eventTitle = eventName;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getStudentRegisterNumber() {
        return studentRegisterNumber;
    }

    public void setStudentRegisterNumber(String studentRegisterNumber) {
        this.studentRegisterNumber = studentRegisterNumber;
    }

    public String getStudentUsn() {
        return studentRegisterNumber;
    }

    public void setStudentUsn(String studentUsn) {
        this.studentRegisterNumber = studentUsn;
    }

    // RegisteredAt / RegistrationDate aliases
    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public LocalDateTime getRegistrationDate() {
        return registeredAt;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registeredAt = registrationDate;
    }

    // RegistrationStatus / Status aliases
    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public String getStatus() {
        return registrationStatus;
    }

    public void setStatus(String status) {
        this.registrationStatus = status;
    }

    public boolean isAttended() {
        return attended;
    }

    public void setAttended(boolean attended) {
        this.attended = attended;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventRegistration that = (EventRegistration) o;
        if (registrationId > 0 && that.registrationId > 0) {
            return registrationId == that.registrationId;
        }
        return eventId == that.eventId && studentId == that.studentId;
    }

    @Override
    public int hashCode() {
        return registrationId > 0 ? java.util.Objects.hash(registrationId) : java.util.Objects.hash(eventId, studentId);
    }

    @Override
    public String toString() {
        return String.format("EventRegistration [ID: %d, Event: %s, Student: %s, Status: %s]",
                registrationId, eventTitle != null ? eventTitle : eventId, studentName != null ? studentName : studentId, registrationStatus);
    }
}
