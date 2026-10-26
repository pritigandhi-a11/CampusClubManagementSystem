package com.campusclub.model;

import java.time.LocalDateTime;

/**
 * Represents an announcement broadcast by a club.
 * Demonstrates Encapsulation.
 */
public class Announcement {
    private int announcementId;
    private int clubId;
    private String clubName;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private String priority; // LOW, MEDIUM, HIGH

    public Announcement() {
        this.createdAt = LocalDateTime.now();
        this.priority = "MEDIUM";
    }

    public Announcement(int announcementId, int clubId, String title, String message, LocalDateTime createdAt, String priority) {
        this.announcementId = announcementId;
        this.clubId = clubId;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.priority = priority != null ? priority : "MEDIUM";
    }

    // Getters and Setters
    public int getAnnouncementId() {
        return announcementId;
    }

    public void setAnnouncementId(int announcementId) {
        this.announcementId = announcementId;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // Message / Content aliases
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContent() {
        return message;
    }

    public void setContent(String content) {
        this.message = content;
    }

    // CreatedAt / PostedDate aliases
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPostedDate() {
        return createdAt;
    }

    public void setPostedDate(LocalDateTime postedDate) {
        this.createdAt = postedDate;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Announcement that = (Announcement) o;
        if (announcementId > 0 && that.announcementId > 0) {
            return announcementId == that.announcementId;
        }
        return clubId == that.clubId && java.util.Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return announcementId > 0 ? java.util.Objects.hash(announcementId) : java.util.Objects.hash(clubId, title);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s", priority, title, clubName != null ? clubName : clubId);
    }
}
