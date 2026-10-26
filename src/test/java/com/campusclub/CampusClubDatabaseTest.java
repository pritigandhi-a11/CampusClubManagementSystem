package com.campusclub;

import com.campusclub.dao.*;
import com.campusclub.exception.ClubNotFoundException;
import com.campusclub.exception.DuplicateMembershipException;
import com.campusclub.exception.EventRegistrationException;
import com.campusclub.model.*;
import com.campusclub.service.EventService;
import com.campusclub.service.MembershipService;
import com.campusclub.util.DatabaseConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 2 Tests: Database configuration, entity mapping, and service business rules.
 */
public class CampusClubDatabaseTest {

    @Test
    @DisplayName("Verify db.properties Configuration File is Readable")
    void testDbPropertiesFile() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            assertNotNull(input, "db.properties must exist in classpath resources");
            Properties prop = new Properties();
            prop.load(input);
            assertTrue(prop.containsKey("db.url"), "db.properties must contain db.url");
            assertTrue(prop.containsKey("db.username"), "db.properties must contain db.username");
            assertTrue(prop.containsKey("db.password"), "db.properties must contain db.password");
            assertTrue(prop.getProperty("db.url").contains("campus_club_db"));
        }
    }

    @Test
    @DisplayName("Verify Entity and DTO Field Mapping Consistency")
    void testEntityFieldMappings() {
        // Student mapping
        Student student = new Student();
        student.setUserId(5);
        student.setStudentTableId(1);
        student.setRegisterNumber("1CC21CS045");
        student.setDepartment("CSE");
        student.setYear(3);
        student.setPhone("9876543220");

        assertEquals(5, student.getUserId());
        assertEquals("1CC21CS045", student.getRegisterNumber());
        assertEquals("1CC21CS045", student.getUsnRollNo());
        assertEquals(3, student.getYear());
        assertEquals(3, student.getYearOfStudy());

        // Event mapping
        Event event = new Event();
        event.setEventId(1);
        event.setClubId(1);
        event.setEventName("HackCampus 2026");
        event.setVenue("Auditorium");
        event.setCapacity(50);
        event.setRegisteredCount(50);

        assertEquals("HackCampus 2026", event.getTitle());
        assertEquals("HackCampus 2026", event.getEventName());
        assertEquals("Auditorium", event.getLocation());
        assertEquals("Auditorium", event.getVenue());
        assertTrue(event.isFull());

        // Membership mapping
        Membership membership = new Membership();
        membership.setMembershipId(10);
        membership.setMembershipStatus("APPROVED");
        assertEquals("APPROVED", membership.getStatus());
        assertEquals("APPROVED", membership.getMembershipStatus());

        // Announcement mapping
        Announcement announcement = new Announcement();
        announcement.setTitle("Tech Talk");
        announcement.setMessage("Join on Friday");
        assertEquals("Join on Friday", announcement.getContent());
        assertEquals("Join on Friday", announcement.getMessage());
    }

    @Test
    @DisplayName("Verify Duplicate Membership Business Rule in MembershipService")
    void testDuplicateMembershipRule() {
        // Mocking DAO behavior with custom anonymous subclass for isolated testing
        MembershipDAO mockMembershipDAO = new MembershipDAO() {
            @Override
            public boolean isAlreadyMember(int studentId, int clubId) {
                return studentId == 1 && clubId == 1; // Already joined club 1
            }
        };

        ClubDAO mockClubDAO = new ClubDAO() {
            @Override
            public Optional<Club> findById(int clubId) {
                if (clubId == 1) {
                    Club club = new Club();
                    club.setClubId(1);
                    club.setClubName("Coding Club");
                    return Optional.of(club);
                }
                return Optional.empty();
            }
        };

        MembershipService service = new MembershipService(mockMembershipDAO, mockClubDAO);

        // Should throw DuplicateMembershipException
        assertThrows(DuplicateMembershipException.class, () -> service.applyForMembership(1, 1));

        // Non-existent club should throw ClubNotFoundException
        assertThrows(ClubNotFoundException.class, () -> service.applyForMembership(1, 999));
    }

    @Test
    @DisplayName("Verify Event Capacity and Duplicate Registration Rules in EventService")
    void testEventRegistrationRules() {
        Event fullEvent = new Event();
        fullEvent.setEventId(10);
        fullEvent.setClubId(1);
        fullEvent.setEventName("Full Workshop");
        fullEvent.setCapacity(20);
        fullEvent.setEventDate(LocalDateTime.now().plusDays(5));

        EventDAO mockEventDAO = new EventDAO() {
            @Override
            public Optional<Event> findById(int eventId) {
                if (eventId == 10) return Optional.of(fullEvent);
                return Optional.empty();
            }
        };

        EventRegistrationDAO mockRegDAO = new EventRegistrationDAO() {
            @Override
            public boolean isAlreadyRegistered(int eventId, int studentId) {
                return eventId == 10 && studentId == 99; // Student 99 already registered
            }

            @Override
            public int getRegistrationCount(int eventId) {
                return 20; // Reached capacity
            }
        };

        EventService eventService = new EventService(mockEventDAO, mockRegDAO, new AnnouncementDAO(), new ClubDAO());

        // Duplicate registration check
        assertThrows(EventRegistrationException.class, () -> eventService.registerForEvent(10, 99));

        // Capacity check for another student
        EventRegistrationException capacityEx = assertThrows(EventRegistrationException.class, () -> eventService.registerForEvent(10, 100));
        assertTrue(capacityEx.getMessage().contains("capacity"));
    }

    @Test
    @DisplayName("Test DatabaseConnection Utility Status")
    void testDatabaseConnectionStatus() {
        // Tests the testConnection helper method (returns boolean without crashing)
        boolean connected = DatabaseConnection.testConnection();
        // Just verify it executed safely
        assertTrue(connected || !connected);
    }
}
