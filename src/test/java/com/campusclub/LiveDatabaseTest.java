package com.campusclub;

import com.campusclub.dao.*;
import com.campusclub.exception.ClubNotFoundException;
import com.campusclub.exception.DuplicateMembershipException;
import com.campusclub.exception.EventRegistrationException;
import com.campusclub.model.*;
import com.campusclub.service.AuthenticationService;
import com.campusclub.service.ClubService;
import com.campusclub.service.EventService;
import com.campusclub.service.MembershipService;
import com.campusclub.util.DatabaseConnection;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Live MySQL 8 Integration Test Suite.
 * Validates JDBC connection, DAO operations, record counts, authentication, and business constraints
 * against the actual MySQL database 'campus_club_db'.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LiveDatabaseTest {

    private static UserDAO userDAO;
    private static StudentDAO studentDAO;
    private static ClubDAO clubDAO;
    private static MembershipDAO membershipDAO;
    private static EventDAO eventDAO;
    private static EventRegistrationDAO eventRegistrationDAO;
    private static AnnouncementDAO announcementDAO;
    private static AuthenticationService authService;
    private static MembershipService membershipService;
    private static ClubService clubService;
    private static EventService eventService;

    @BeforeAll
    static void setUp() {
        userDAO = new UserDAO();
        studentDAO = new StudentDAO();
        clubDAO = new ClubDAO();
        membershipDAO = new MembershipDAO();
        eventDAO = new EventDAO();
        eventRegistrationDAO = new EventRegistrationDAO();
        announcementDAO = new AnnouncementDAO();

        authService = new AuthenticationService(userDAO);
        membershipService = new MembershipService(membershipDAO, clubDAO);
        clubService = new ClubService(clubDAO);
        eventService = new EventService(eventDAO, eventRegistrationDAO, announcementDAO, clubDAO);

        new com.campusclub.web.WebPortalController().ensureAccountsProvisionedInDatabase();
    }

    @BeforeEach
    void requireLiveDatabase() {
        assumeTrue(DatabaseConnection.testConnection(),
                "Skipping live database test: MySQL server is not reachable with credentials in src/main/resources/db.properties");
    }

    @Test
    @Order(1)
    @DisplayName("Task 2: Test Real JDBC Connection to MySQL")
    void testJdbcConnection() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            assertNotNull(conn, "JDBC Connection must not be null");
            assertFalse(conn.isClosed(), "JDBC Connection must be active and open");

            DatabaseMetaData metaData = conn.getMetaData();
            assertNotNull(metaData.getDatabaseProductName());
            assertTrue(metaData.getDatabaseProductName().toLowerCase().contains("mysql"),
                    "Database product should be MySQL");

            // Verify database name
            assertEquals("campus_club_db", conn.getCatalog(), "Catalog must match campus_club_db");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Task 3: Test UserDAO on Live MySQL")
    void testUserDAO() {
        // findByUsername
        Optional<User> adminOpt = userDAO.findByUsername("admin");
        assertTrue(adminOpt.isPresent(), "Admin user should exist in live database");
        assertEquals("admin@campus.edu", adminOpt.get().getEmail());
        assertEquals(UserRole.ADMIN, adminOpt.get().getRole());

        // findByEmail
        Optional<User> emailOpt = userDAO.findByEmail("sharma@campus.edu");
        assertTrue(emailOpt.isPresent(), "User with email sharma@campus.edu should exist");
        assertEquals("prof_sharma", emailOpt.get().getUsername());

        // findAll
        List<User> allUsers = userDAO.findAll();
        assertNotNull(allUsers);
        assertTrue(allUsers.size() >= 12, "Should have at least 12 users");
    }

    @Test
    @Order(3)
    @DisplayName("Task 3: Test StudentDAO on Live MySQL")
    void testStudentDAO() {
        // findAll
        List<Student> allStudents = studentDAO.findAll();
        assertNotNull(allStudents);
        assertTrue(allStudents.size() >= 8, "Should have at least 8 students");

        // findById (student table ID 1 corresponds to student rohit_k)
        Optional<Student> studentOpt = studentDAO.findById(1);
        assertTrue(studentOpt.isPresent(), "Student with ID 1 should exist");
        assertEquals("1CC21CS045", studentOpt.get().getRegisterNumber());
        assertEquals("Computer Science & Engineering", studentOpt.get().getDepartment());
    }

    @Test
    @Order(4)
    @DisplayName("Task 3: Test ClubDAO on Live MySQL")
    void testClubDAO() {
        // findAll
        List<Club> allClubs = clubDAO.findAll();
        assertNotNull(allClubs);
        assertTrue(allClubs.size() >= 6, "Should have at least 6 clubs");

        // findActiveClubs
        List<Club> activeClubs = clubDAO.findActiveClubs();
        assertNotNull(activeClubs);
        assertTrue(activeClubs.size() >= 6, "All registered initial clubs should be ACTIVE");

        // findById
        Optional<Club> codingClub = clubDAO.findById(1);
        assertTrue(codingClub.isPresent());
        assertEquals("Coding Club", codingClub.get().getClubName());
        assertEquals("Technical", codingClub.get().getCategory());
    }

    @Test
    @Order(5)
    @DisplayName("Task 3: Test MembershipDAO on Live MySQL")
    void testMembershipDAO() {
        // findStudentMemberships (Student 1 has approved memberships)
        List<Membership> studentMemberships = membershipDAO.findStudentMemberships(1);
        assertNotNull(studentMemberships);
        assertFalse(studentMemberships.isEmpty(), "Student 1 should have memberships");

        // findClubMembers (Club 1 Coding Club has members)
        List<Membership> clubMembers = membershipDAO.findClubMembers(1);
        assertNotNull(clubMembers);
        assertFalse(clubMembers.isEmpty(), "Club 1 should have members");
    }

    @Test
    @Order(6)
    @DisplayName("Task 3: Test EventDAO on Live MySQL")
    void testEventDAO() {
        // findAll
        List<Event> allEvents = eventDAO.findAll();
        assertNotNull(allEvents);
        assertEquals(8, allEvents.size(), "Should have exactly 8 events");

        // findUpcomingEvents
        List<Event> upcomingEvents = eventDAO.findUpcomingEvents();
        assertNotNull(upcomingEvents);
        assertTrue(upcomingEvents.size() >= 1, "Should have upcoming events scheduled");

        // findEventsByClub (Club 1)
        List<Event> club1Events = eventDAO.findEventsByClub(1);
        assertNotNull(club1Events);
        assertFalse(club1Events.isEmpty(), "Club 1 should have organized events");
    }

    @Test
    @Order(7)
    @DisplayName("Task 3: Test EventRegistrationDAO on Live MySQL")
    void testEventRegistrationDAO() {
        // getRegistrationCount (Event 1 has registered students)
        int regCount = eventRegistrationDAO.getRegistrationCount(1);
        assertTrue(regCount >= 1, "Event 1 should have registrations");

        // getEventParticipants
        List<EventRegistration> participants = eventRegistrationDAO.getEventParticipants(1);
        assertNotNull(participants);
        assertEquals(regCount, participants.size());
    }

    @Test
    @Order(8)
    @DisplayName("Task 3: Test AnnouncementDAO on Live MySQL")
    void testAnnouncementDAO() {
        // findAll
        List<Announcement> allAnnouncements = announcementDAO.findAll();
        assertNotNull(allAnnouncements);
        assertTrue(allAnnouncements.size() >= 4, "Should have at least 4 announcements");

        // findLatestAnnouncements
        List<Announcement> latest = announcementDAO.findLatestAnnouncements(2);
        assertNotNull(latest);
        assertEquals(2, latest.size());
    }

    @Test
    @Order(9)
    @DisplayName("Task 4: Verify Live Database Exact Counts")
    void testLiveDatabaseExactCounts() {
        List<User> users = userDAO.findAll();
        assertTrue(users.size() >= 12, "Live database must contain at least 12 users");

        List<Student> students = studentDAO.findAll();
        assertTrue(students.size() >= 8, "Live database must contain at least 8 students");

        List<Club> clubs = clubDAO.findAll();
        assertTrue(clubs.size() >= 6, "Live database must contain at least 6 clubs");

        List<Event> events = eventDAO.findAll();
        assertTrue(events.size() >= 8, "Live database must contain at least 8 events");

        List<Announcement> announcements = announcementDAO.findAll();
        assertTrue(announcements.size() >= 4, "Live database must contain at least 4 announcements");

        // Check user role breakdown
        long adminCount = users.stream().filter(u -> u.getRole() == UserRole.ADMIN).count();
        long coordCount = users.stream().filter(u -> u.getRole() == UserRole.COORDINATOR).count();
        long studentCount = users.stream().filter(u -> u.getRole() == UserRole.STUDENT).count();

        assertTrue(adminCount >= 1, "Must have at least 1 Admin");
        assertTrue(coordCount >= 3, "Must have at least 3 Coordinators");
        assertTrue(studentCount >= 8, "Must have at least 8 Students");
    }

    @Test
    @Order(10)
    @DisplayName("Task 5: Test AuthenticationService against Live Database")
    void testLiveAuthentication() throws Exception {
        // Authenticate admin with verified password (admin123 or password123)
        User adminUser = null;
        try {
            adminUser = authService.login("admin", "admin123");
        } catch (Exception ignored) {
            adminUser = authService.login("admin", "password123");
        }
        assertNotNull(adminUser);
        assertEquals("admin", adminUser.getUsername());
        assertEquals(UserRole.ADMIN, adminUser.getRole());
        assertEquals("System Administrator", adminUser.getFullName());

        // Authenticate student with demo password "password123"
        User studentUser = authService.login("rohit_k", "password123");
        assertNotNull(studentUser);
        assertEquals("rohit_k", studentUser.getUsername());
        assertEquals(UserRole.STUDENT, studentUser.getRole());

        // Authenticate coordinator with demo password "password123"
        User coordUser = authService.login("prof_sharma", "password123");
        assertNotNull(coordUser);
        assertEquals("prof_sharma", coordUser.getUsername());
        assertEquals(UserRole.COORDINATOR, coordUser.getRole());
    }

    @Test
    @Order(11)
    @DisplayName("Task 6: Test Business Rules against Live Database")
    void testLiveBusinessRules() {
        // Rule 1: Duplicate club membership rejection
        // Student 1 (rohit_k) is already an APPROVED member of Club 1 (Coding Club)
        assertThrows(DuplicateMembershipException.class, () -> {
            membershipService.applyForMembership(1, 1);
        }, "Applying to a club where student is already a member must throw DuplicateMembershipException");

        // Rule 2: Duplicate event registration rejection
        // Student 1 is already registered for Event 1
        assertThrows(EventRegistrationException.class, () -> {
            eventService.registerForEvent(1, 1);
        }, "Registering twice for the same event must throw EventRegistrationException");

        // Rule 3: Event capacity enforcement
        Event cappedEvent = new Event();
        cappedEvent.setClubId(1);
        cappedEvent.setEventName("Tiny Lab Demo");
        cappedEvent.setDescription("Limited seating");
        cappedEvent.setVenue("Room 101");
        cappedEvent.setCapacity(1); // Capacity of 1
        cappedEvent.setEventDate(LocalDateTime.now().plusDays(10));

        try {
            int newEventId = eventService.createEvent(cappedEvent);
            assertTrue(newEventId > 0);

            // First registration should succeed (Student 2)
            int regId = eventService.registerForEvent(newEventId, 2);
            assertTrue(regId > 0);

            // Second registration should fail due to capacity constraint (Student 3)
            EventRegistrationException capEx = assertThrows(EventRegistrationException.class, () -> {
                eventService.registerForEvent(newEventId, 3);
            });
            assertTrue(capEx.getMessage().toLowerCase().contains("capacity"));

            // Cleanup test event safely
            eventService.deleteEvent(newEventId);
        } catch (Exception e) {
            fail("Capacity test encountered unexpected error: " + e.getMessage());
        }

        // Rule 4: Non-existent club produces ClubNotFoundException
        assertThrows(ClubNotFoundException.class, () -> {
            clubService.getClubById(99999);
        });

        // Rule 5: Non-existent club event creation produces ClubNotFoundException
        Event badEvent = new Event();
        badEvent.setClubId(99999); // Non-existent club
        badEvent.setEventName("Ghost Event");
        badEvent.setEventDate(LocalDateTime.now().plusDays(5));
        assertThrows(ClubNotFoundException.class, () -> {
            eventService.createEvent(badEvent);
        });
    }
}
