package com.campusclub.model;

import com.campusclub.service.ClubOperations;
import com.campusclub.service.ClubService;
import com.campusclub.service.EventOperations;
import com.campusclub.service.EventService;
import com.campusclub.service.MembershipOperations;
import com.campusclub.service.MembershipService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 3 OOP Demonstration and Verification Test Suite.
 * Explicitly tests and validates the 6 core Object-Oriented Programming principles:
 * 1. Encapsulation
 * 2. Inheritance
 * 3. Abstraction
 * 4. Polymorphism
 * 5. Composition
 * 6. Interfaces & Collections
 */
public class OOPConceptsTest {

    @Nested
    @DisplayName("1. Encapsulation Tests")
    class EncapsulationTests {

        @Test
        @DisplayName("Encapsulation: Fields are private and modified only via controlled getters/setters")
        void testPrivateFieldEncapsulation() {
            Student student = new Student();
            student.setStudentId(101);
            student.setUserId(201);
            student.setUsername("arun_kumar");
            student.setFullName("Arun Kumar");
            student.setEmail("arun@campus.edu");
            student.setDepartment("Computer Science");
            student.setAcademicYear(3);
            student.setCgpa(8.75);
            student.setActive(true);

            assertEquals(101, student.getStudentId());
            assertEquals(201, student.getUserId());
            assertEquals("arun_kumar", student.getUsername());
            assertEquals("Arun Kumar", student.getFullName());
            assertEquals("arun@campus.edu", student.getEmail());
            assertEquals("Computer Science", student.getDepartment());
            assertEquals(3, student.getAcademicYear());
            assertEquals(8.75, student.getCgpa(), 0.001);
            assertTrue(student.isActive());
        }

        @Test
        @DisplayName("Encapsulation: Defensive collection encapsulation via unmodifiable lists")
        void testDefensiveCollectionEncapsulation() {
            Club club = new Club();
            club.setClubId(1);
            club.setClubName("Coding Club");

            Event event = new Event();
            event.setEventId(10);
            event.setEventName("Hackathon 2026");

            club.addEvent(event);
            assertEquals(1, club.getEvents().size());

            // Attempting to modify the unmodifiable collection view should throw UnsupportedOperationException
            assertThrows(UnsupportedOperationException.class, () -> {
                club.getEvents().add(new Event());
            });
        }
    }

    @Nested
    @DisplayName("2. Inheritance Tests")
    class InheritanceTests {

        @Test
        @DisplayName("Inheritance: Student, ClubCoordinator, and Admin extend base User class")
        void testClassHierarchyInheritance() {
            Student student = new Student();
            ClubCoordinator coordinator = new ClubCoordinator();
            Admin admin = new Admin();

            // Verify 'is-a' inheritance relationship
            assertInstanceOf(User.class, student);
            assertInstanceOf(User.class, coordinator);
            assertInstanceOf(User.class, admin);

            // Verify inherited fields & behavior from User
            student.setUserId(1);
            student.setUsername("student1");
            student.setEmail("student1@campus.edu");
            student.setActive(true);

            coordinator.setUserId(2);
            coordinator.setUsername("coord1");
            coordinator.setEmail("coord1@campus.edu");

            admin.setUserId(3);
            admin.setUsername("admin1");
            admin.setEmail("admin1@campus.edu");

            assertEquals(UserRole.STUDENT, student.getUserRole());
            assertEquals(UserRole.COORDINATOR, coordinator.getUserRole());
            assertEquals(UserRole.ADMIN, admin.getUserRole());
        }

        @Test
        @DisplayName("Inheritance: Base class methods are reused across subclasses")
        void testBaseClassMethodReuse() {
            Student student = new Student(1, 10, "CS2023001", "Computer Science", 3, 8.5,
                    "Tech, AI", 10, "student_user", "hash123", "student@campus.edu",
                    "Student User", "9876543210", UserRole.STUDENT, LocalDateTime.now(), true);

            assertEquals(10, student.getUserId());
            assertEquals("student_user", student.getUsername());
            assertEquals("Student User", student.getFullName());
            assertTrue(student.isActive());
        }
    }

    @Nested
    @DisplayName("3. Abstraction Tests")
    class AbstractionTests {

        @Test
        @DisplayName("Abstraction: User is an abstract class with abstract methods implemented by subclasses")
        void testAbstractClassMethods() {
            User studentUser = new Student();
            User coordUser = new ClubCoordinator();
            User adminUser = new Admin();

            // Abstract method implementations
            assertNotNull(studentUser.getRoleDisplayName());
            assertNotNull(coordUser.getRoleDisplayName());
            assertNotNull(adminUser.getRoleDisplayName());

            assertNotNull(studentUser.getDashboardDescription());
            assertNotNull(coordUser.getDashboardDescription());
            assertNotNull(adminUser.getDashboardDescription());

            assertNotNull(studentUser.getPermissionsDescription());
            assertNotNull(coordUser.getPermissionsDescription());
            assertNotNull(adminUser.getPermissionsDescription());
        }

        @Test
        @DisplayName("Abstraction: Service interfaces decouple contract from implementation")
        void testServiceInterfaces() {
            ClubOperations clubOps = new ClubService();
            EventOperations eventOps = new EventService();
            MembershipOperations memOps = new MembershipService();

            assertNotNull(clubOps);
            assertNotNull(eventOps);
            assertNotNull(memOps);
        }
    }

    @Nested
    @DisplayName("4. Polymorphism Tests")
    class PolymorphismTests {

        @Test
        @DisplayName("Polymorphism: Dynamic method dispatch across different User subtypes")
        void testDynamicMethodDispatch() {
            List<User> users = new ArrayList<>();
            users.add(new Student());
            users.add(new ClubCoordinator());
            users.add(new Admin());

            List<String> roleNames = new ArrayList<>();
            for (User u : users) {
                // Dynamic polymorphism: specific getRoleDisplayName() is invoked at runtime
                roleNames.add(u.getRoleDisplayName());
            }

            assertTrue(roleNames.get(0).startsWith("Student"));
            assertTrue(roleNames.get(1).contains("Coordinator"));
            assertTrue(roleNames.get(2).contains("Administrator"));
        }

        @Test
        @DisplayName("Polymorphism: Overridden toString, equals, and hashCode")
        void testOverriddenMethods() {
            Student s1 = new Student();
            s1.setStudentId(5);
            s1.setRegisterNumber("REG001");

            Student s2 = new Student();
            s2.setStudentId(5);
            s2.setRegisterNumber("REG001");

            Student s3 = new Student();
            s3.setStudentId(6);
            s3.setRegisterNumber("REG002");

            assertEquals(s1, s2);
            assertNotEquals(s1, s3);
            assertEquals(s1.hashCode(), s2.hashCode());
            assertTrue(s1.toString().contains("REG001"));
        }
    }

    @Nested
    @DisplayName("5. Composition Tests")
    class CompositionTests {

        @Test
        @DisplayName("Composition: Club has-a collection of Events, Memberships, and Announcements")
        void testClubComposition() {
            Club club = new Club(1, "Robotics Club", "Robotics and Automation", "Technical",
                    10, "Alex Rivera", 50, "ACTIVE", LocalDateTime.now());

            Event event1 = new Event(101, 1, "Robotics Workshop", "Intro to ROS",
                    LocalDateTime.now().plusDays(7), "Lab 2", 40, LocalDateTime.now().plusDays(5),
                    "Alex Rivera", "UPCOMING");

            Membership membership1 = new Membership(201, 1, 10, "APPROVED",
                    "MEMBER", LocalDateTime.now(), "Alex Rivera", "Robotics Club");

            Announcement announcement1 = new Announcement(301, 1, "Hackathon Announcement",
                    "Team registrations open.", LocalDateTime.now(), "HIGH");

            // Build composition
            club.addEvent(event1);
            club.addMembership(membership1);
            club.addAnnouncement(announcement1);

            assertEquals(1, club.getEvents().size());
            assertEquals(1, club.getMemberships().size());
            assertEquals(1, club.getAnnouncements().size());

            assertEquals("Robotics Workshop", club.getEvents().get(0).getEventName());
            assertEquals("APPROVED", club.getMemberships().get(0).getMembershipStatus());
            assertEquals("Hackathon Announcement", club.getAnnouncements().get(0).getTitle());
        }

        @Test
        @DisplayName("Composition: Student has-a collection of Memberships and EventRegistrations")
        void testStudentComposition() {
            Student student = new Student();
            student.setStudentId(10);
            student.setFullName("John Doe");

            Membership m1 = new Membership(1, 1, 10, "APPROVED", "MEMBER", LocalDateTime.now(), "John Doe", "Coding Club");
            EventRegistration r1 = new EventRegistration(1, 101, 10, LocalDateTime.now(), "REGISTERED", false);

            student.addMembership(m1);
            student.addEventRegistration(r1);

            assertEquals(1, student.getMemberships().size());
            assertEquals(1, student.getEventRegistrations().size());
            assertEquals(1, student.getMemberships().get(0).getClubId());
            assertEquals(101, student.getEventRegistrations().get(0).getEventId());
        }

        @Test
        @DisplayName("Composition: Event has-a collection of EventRegistrations")
        void testEventComposition() {
            Event event = new Event();
            event.setEventId(50);
            event.setEventName("AI Summit");
            event.setCapacity(100);

            EventRegistration reg1 = new EventRegistration(1, 50, 10, LocalDateTime.now(), "REGISTERED", false);
            EventRegistration reg2 = new EventRegistration(2, 50, 11, LocalDateTime.now(), "REGISTERED", false);

            event.addRegistration(reg1);
            event.addRegistration(reg2);

            assertEquals(2, event.getRegistrations().size());
        }
    }

    @Nested
    @DisplayName("6. Interfaces and Collections Tests")
    class InterfacesAndCollectionsTests {

        @Test
        @DisplayName("Interfaces: Verification of Service Interface Implementation")
        void testInterfaceImplementationTypes() {
            ClubService clubService = new ClubService();
            EventService eventService = new EventService();
            MembershipService membershipService = new MembershipService();

            assertInstanceOf(ClubOperations.class, clubService);
            assertInstanceOf(EventOperations.class, eventService);
            assertInstanceOf(MembershipOperations.class, membershipService);
        }

        @Test
        @DisplayName("Collections: List manipulation and type safety")
        void testCollectionsTypeSafety() {
            Club club = new Club();
            List<Event> events = List.of(
                    new Event(1, 1, "E1", "Desc 1", LocalDateTime.now(), "Venue 1", 30, LocalDateTime.now(), "Coord 1", "UPCOMING"),
                    new Event(2, 1, "E2", "Desc 2", LocalDateTime.now(), "Venue 2", 50, LocalDateTime.now(), "Coord 1", "UPCOMING")
            );
            club.setEvents(events);

            assertEquals(2, club.getEvents().size());
            assertEquals("E1", club.getEvents().get(0).getEventName());
            assertEquals("E2", club.getEvents().get(1).getEventName());
        }
    }
}
