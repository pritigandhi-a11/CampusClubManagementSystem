package com.campusclub.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * [OOP: Encapsulation & Composition]
 * Represents a student organization/club on campus.
 * Composes associated events, member memberships, and broadcast announcements.
 */
public class Club {

    // [OOP: Encapsulation - Private fields]
    private int clubId;
    private String clubName;
    private String category;
    private String description;
    private Integer coordinatorId;
    private String coordinatorName;
    private LocalDate establishedDate;
    private String status; // ACTIVE, INACTIVE
    private LocalDateTime createdAt;
    private int memberCount;

    // [OOP: Composition - Club contains events, memberships, and announcements]
    private final List<Event> events = new ArrayList<>();
    private final List<Membership> members = new ArrayList<>();
    private final List<Announcement> announcements = new ArrayList<>();

    public Club() {
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.establishedDate = LocalDate.now();
    }

    public Club(int clubId, String clubName, String category, String description,
                Integer coordinatorId, LocalDate establishedDate, String status) {
        this.clubId = clubId;
        this.clubName = clubName;
        this.category = category;
        this.description = description;
        this.coordinatorId = coordinatorId;
        this.establishedDate = establishedDate != null ? establishedDate : LocalDate.now();
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public Club(int clubId, String clubName, String description, String category,
                Integer coordinatorId, String coordinatorName, int memberCount, String status, LocalDateTime createdAt) {
        this.clubId = clubId;
        this.clubName = clubName;
        this.description = description;
        this.category = category;
        this.coordinatorId = coordinatorId;
        this.coordinatorName = coordinatorName;
        this.memberCount = memberCount;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.establishedDate = LocalDate.now();
    }

    // ==========================================================
    // [OOP: Composition - Managing Child Collections]
    // ==========================================================

    public void addEvent(Event event) {
        if (event != null && !events.contains(event)) {
            events.add(event);
            event.setClubId(this.clubId);
            event.setClubName(this.clubName);
        }
    }

    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public void setEvents(List<Event> newEvents) {
        this.events.clear();
        if (newEvents != null) {
            for (Event e : newEvents) {
                addEvent(e);
            }
        }
    }

    public void addMembership(Membership membership) {
        if (membership != null && !members.contains(membership)) {
            members.add(membership);
            membership.setClubId(this.clubId);
            membership.setClubName(this.clubName);
        }
    }

    public List<Membership> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public List<Membership> getMemberships() {
        return Collections.unmodifiableList(members);
    }

    public void setMembers(List<Membership> newMembers) {
        this.members.clear();
        if (newMembers != null) {
            for (Membership m : newMembers) {
                addMembership(m);
            }
        }
    }

    public void setMemberships(List<Membership> newMembers) {
        setMembers(newMembers);
    }

    public void addAnnouncement(Announcement announcement) {
        if (announcement != null && !announcements.contains(announcement)) {
            announcements.add(announcement);
            announcement.setClubId(this.clubId);
            announcement.setClubName(this.clubName);
        }
    }

    public List<Announcement> getAnnouncements() {
        return Collections.unmodifiableList(announcements);
    }

    public void setAnnouncements(List<Announcement> newAnnouncements) {
        this.announcements.clear();
        if (newAnnouncements != null) {
            for (Announcement a : newAnnouncements) {
                addAnnouncement(a);
            }
        }
    }

    // ==========================================================
    // [OOP: Encapsulation - Accessors and Mutators]
    // ==========================================================

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCoordinatorId() {
        return coordinatorId;
    }

    public void setCoordinatorId(Integer coordinatorId) {
        this.coordinatorId = coordinatorId;
    }

    public String getCoordinatorName() {
        return coordinatorName;
    }

    public void setCoordinatorName(String coordinatorName) {
        this.coordinatorName = coordinatorName;
    }

    public LocalDate getEstablishedDate() {
        return establishedDate;
    }

    public void setEstablishedDate(LocalDate establishedDate) {
        this.establishedDate = establishedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Club club = (Club) o;
        return clubId == club.clubId && Objects.equals(clubName, club.clubName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clubId, clubName);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %s", clubName, category, status);
    }
}
