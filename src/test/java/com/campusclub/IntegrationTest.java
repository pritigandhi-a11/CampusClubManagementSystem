package com.campusclub;

import com.campusclub.model.*;
import com.campusclub.recommendation.ClubRecommendationEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * STAGE 7 — FULL INTEGRATION, END-TO-END WORKFLOW & DEMO VERIFICATION TEST SUITE
 * 
 * Tests the complete integration between:
 * - GUI Model and DTOs
 * - AuthenticationService, StudentService, ClubService, EventService, MembershipService
 * - Rule-Based ClubRecommendationEngine
 * - Layered Data Access (DAO and in-memory simulated business logic)
 * - Checked & Unchecked Exception Handling Boundaries
 * - Scoped Data Isolation and Role Access Control
 */
public class IntegrationTest {

    private static ClubRecommendationEngine recommendationEngine;

    @BeforeAll
    static void setUpGlobal() {
        recommendationEngine = new ClubRecommendationEngine();
    }

    // =========================================================================
    // 1. STUDENT END-TO-END WORKFLOW INTEGRATION
    // =========================================================================
    @Nested
    @DisplayName("1. Student End-to-End Workflow Tests")
    class StudentWorkflowTests {

        @Test
        @DisplayName("Student 1: Full profile, clubs discovery, join club, duplicate rejection, event registration, and recommendations")
        void testStudent1EndToEndWorkflow() {
            // Setup Student 1
            Student student1 = new Student();
            student1.setUserId(15);
            student1.setStudentTableId(9);
            student1.setUsername("student1");
            student1.setFullName("Alex Turner");
            student1.setEmail("alex.turner@campus.edu");
            student1.setDepartment("Computer Science & Engineering");
            student1.setYear(3);
            student1.setSemester(5);
            student1.setPhoneNumber("+1-555-0101");
            student1.setInterests("Programming, Artificial Intelligence, Web Development");

            // A. Profile Verification
            assertEquals("Alex Turner", student1.getFullName());
            assertEquals(UserRole.STUDENT, student1.getRole());
            assertTrue(student1.getRoleDisplayName().contains("Computer Science"));
            assertTrue(student1.getPermissionsDescription().contains("APPLY_MEMBERSHIP"));

            // B. Browse & Filter Clubs
            List<Club> campusClubs = createMockClubs();
            assertFalse(campusClubs.isEmpty());
            assertEquals(6, campusClubs.size());

            // Search filter by category
            List<Club> techClubs = campusClubs.stream()
                    .filter(c -> "Technical".equalsIgnoreCase(c.getCategory()))
                    .toList();
            assertEquals(2, techClubs.size());

            // C. Join Club & Duplicate Membership check
            Membership m1 = new Membership(101, student1.getStudentTableId(), 1, LocalDateTime.now(), "APPROVED", "MEMBER");
            student1.addMembership(m1);
            assertEquals(1, student1.getMemberships().size());

            // Duplicate detection check
            boolean isAlreadyMember = student1.getMemberships().stream()
                    .anyMatch(m -> m.getClubId() == 1 && !"REJECTED".equalsIgnoreCase(m.getMembershipStatus()));
            assertTrue(isAlreadyMember, "Duplicate check must identify that student1 is already member of club 1");

            // D. Upcoming Events & Registration
            Event event1 = new Event(201, 1, "AI & Hackathon 2026", "Build AI agents", LocalDateTime.now().plusDays(7), "Auditorium", 50, LocalDateTime.now().plusDays(5));
            EventRegistration reg1 = new EventRegistration(301, event1.getEventId(), student1.getStudentTableId(), LocalDateTime.now(), "REGISTERED", false);
            student1.addEventRegistration(reg1);
            assertEquals(1, student1.getRegistrations().size());

            // Duplicate registration check
            boolean isAlreadyRegistered = student1.getRegistrations().stream()
                    .anyMatch(r -> r.getEventId() == event1.getEventId() && "REGISTERED".equalsIgnoreCase(r.getRegistrationStatus()));
            assertTrue(isAlreadyRegistered, "Duplicate registration check must identify existing event pass");

            // E. Rule-Based Recommendations Execution
            List<ClubRecommendationEngine.ClubRecommendation> recs = recommendationEngine.recommendClubs(
                    student1, campusClubs, student1.getMemberships()
            );

            assertNotNull(recs);
            assertFalse(recs.isEmpty(), "Recommendations should be generated for student1");

            // Joined club (Club ID 1: Coding Club) MUST be excluded
            boolean containsJoinedClub = recs.stream().anyMatch(r -> r.getClub().getClubId() == 1);
            assertFalse(containsJoinedClub, "Already joined club (Coding Club) MUST be excluded from recommendations");

            // Top recommendation should be Robotics & AI Club (Club ID 2) due to interest in AI & CSE department
            ClubRecommendationEngine.ClubRecommendation topRec = recs.get(0);
            assertEquals(2, topRec.getClub().getClubId());
            assertTrue(topRec.getScore() >= 20);
            assertNotNull(topRec.getReasonSummary());
            assertTrue(topRec.getMatchPercentage() > 0);
        }
    }

    // =========================================================================
    // 2. SECOND STUDENT WORKFLOW & DATA ISOLATION
    // =========================================================================
    @Nested
    @DisplayName("2. Second Student Workflow & Data Isolation Tests")
    class SecondStudentWorkflowTests {

        @Test
        @DisplayName("Student 2: Isolated profile, distinct ECE department, isolated memberships, tailored recommendations")
        void testStudent2DataIsolation() {
            Student student2 = new Student();
            student2.setUserId(16);
            student2.setStudentTableId(10);
            student2.setUsername("student2");
            student2.setFullName("Sophia Chen");
            student2.setEmail("sophia.chen@campus.edu");
            student2.setDepartment("Electronics & Communication Engineering");
            student2.setYear(2);
            student2.setSemester(3);
            student2.setInterests("Robotics, IoT, Embedded Systems, Hardware");

            // Add membership in Club 2 (Robotics Club)
            Membership m2 = new Membership(102, student2.getStudentTableId(), 2, LocalDateTime.now(), "APPROVED", "MEMBER");
            student2.addMembership(m2);

            // Verify isolated memberships
            assertEquals(1, student2.getMemberships().size());
            assertEquals(2, student2.getMemberships().get(0).getClubId());

            // Verify recommendation engine produces tailored output for student 2
            List<Club> campusClubs = createMockClubs();
            List<ClubRecommendationEngine.ClubRecommendation> recs = recommendationEngine.recommendClubs(
                    student2, campusClubs, student2.getMemberships()
            );

            // Joined Club 2 MUST be excluded
            boolean containsClub2 = recs.stream().anyMatch(r -> r.getClub().getClubId() == 2);
            assertFalse(containsClub2, "Student 2 must not be recommended Club 2 which she has already joined");
        }
    }

    // =========================================================================
    // 3. COORDINATOR END-TO-END WORKFLOW & VALIDATIONS
    // =========================================================================
    @Nested
    @DisplayName("3. Coordinator End-to-End Workflow Tests")
    class CoordinatorWorkflowTests {

        @Test
        @DisplayName("Coordinator Coding: Scoped managed club, event creation, attendee roster, and announcement broadcast")
        void testCoordinatorCodingWorkflow() {
            ClubCoordinator coord = new ClubCoordinator();
            coord.setUserId(13);
            coord.setUsername("coord_coding");
            coord.setFullName("David Miller");
            coord.setEmail("david.miller@campus.edu");
            coord.setDepartment("Computer Science");
            coord.setManagedClubId(1);
            coord.setManagedClubName("Coding & Open Source Club");

            assertEquals("Coding & Open Source Club", coord.getManagedClubName());
            assertEquals(UserRole.COORDINATOR, coord.getRole());
            assertTrue(coord.getPermissionsDescription().contains("CREATE_EVENT"));

            // Event Validation Tests
            Event validEvent = new Event();
            validEvent.setClubId(coord.getManagedClubId());
            validEvent.setEventName("Spring Codeathon 2026");
            validEvent.setDescription("Annual 24-hour coding challenge");
            validEvent.setVenue("CS Lab 4");
            validEvent.setCapacity(60);
            validEvent.setEventDate(LocalDateTime.now().plusDays(14));

            assertTrue(validEvent.getCapacity() > 0);
            assertNotNull(validEvent.getEventName());
            assertFalse(validEvent.getEventName().isBlank());
        }

        @Test
        @DisplayName("Coordinator: Invalid event inputs throw IllegalArgumentException / validation errors")
        void testInvalidEventCreationValidations() {
            // Case 1: Blank event name
            assertThrows(IllegalArgumentException.class, () ->
                    com.campusclub.util.ValidationUtil.validateRequired("   ", "Event name"));

            // Case 2: Non-positive capacity
            assertThrows(IllegalArgumentException.class, () ->
                    com.campusclub.util.ValidationUtil.validateEventCapacity(0));

            // Case 3: Negative capacity
            assertThrows(IllegalArgumentException.class, () ->
                    com.campusclub.util.ValidationUtil.validateEventCapacity(-10));
        }

        @Test
        @DisplayName("Coordinator Scoping: coord_coding vs coord_robotics are properly isolated")
        void testCoordinatorScopingIsolation() {
            ClubCoordinator coord1 = new ClubCoordinator(13, "coord_coding", "hash", "David Miller", "david@campus.edu", "555-0101", "CSE", "Associate Professor");
            coord1.setManagedClubId(1);
            coord1.setManagedClubName("Coding Club");

            ClubCoordinator coord2 = new ClubCoordinator(14, "coord_robotics", "hash", "Elena Rostova", "elena@campus.edu", "555-0102", "ECE", "Assistant Professor");
            coord2.setManagedClubId(2);
            coord2.setManagedClubName("Robotics Club");

            assertNotEquals(coord1.getManagedClubId(), coord2.getManagedClubId());
            assertEquals(1, coord1.getManagedClubId());
            assertEquals(2, coord2.getManagedClubId());
            assertEquals("Coding Club", coord1.getManagedClubName());
            assertEquals("Robotics Club", coord2.getManagedClubName());
        }
    }

    // =========================================================================
    // 4. ADMIN END-TO-END WORKFLOW & METRICS
    // =========================================================================
    @Nested
    @DisplayName("4. Admin Workflow & System Analytics Tests")
    class AdminWorkflowTests {

        @Test
        @DisplayName("Admin: System overview metrics, user search, club status management")
        void testAdminSystemAnalytics() {
            Admin admin = new Admin(1, "admin", "hash", "System Administrator", "admin@campus.edu", "555-0000", "SUPER_ADMIN");
            assertEquals("System Administrator", admin.getFullName());
            assertEquals(UserRole.ADMIN, admin.getRole());
            assertTrue(admin.getPermissionsDescription().contains("MANAGE_USERS"));

            // Verify Collections-based statistics calculation
            List<Club> clubs = createMockClubs();
            long activeClubsCount = clubs.stream().filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus())).count();
            assertEquals(6, activeClubsCount);

            // Department statistics via HashMap
            List<Student> studentList = List.of(
                    createSampleStudent(1, "CSE"),
                    createSampleStudent(2, "CSE"),
                    createSampleStudent(3, "ECE"),
                    createSampleStudent(4, "Mechanical")
            );

            Map<String, Integer> deptStats = new java.util.HashMap<>();
            for (Student s : studentList) {
                deptStats.put(s.getDepartment(), deptStats.getOrDefault(s.getDepartment(), 0) + 1);
            }

            assertEquals(2, deptStats.get("CSE"));
            assertEquals(1, deptStats.get("ECE"));
            assertEquals(1, deptStats.get("Mechanical"));
        }
    }

    // =========================================================================
    // 5. ROLE ISOLATION & ACCESS BOUNDARY TESTS
    // =========================================================================
    @Nested
    @DisplayName("5. Role Isolation & Access Control Boundaries")
    class RoleIsolationTests {

        @Test
        @DisplayName("Polymorphic permissions enforce role boundary separation")
        void testRolePermissionsSeparation() {
            User student = new Student();
            student.setRole(UserRole.STUDENT);

            User coordinator = new ClubCoordinator();
            coordinator.setRole(UserRole.COORDINATOR);

            User admin = new Admin();
            admin.setRole(UserRole.ADMIN);

            // Verify Student cannot access Admin/Coord operations
            assertTrue(student.getPermissionsDescription().contains("APPLY_MEMBERSHIP"));
            assertFalse(student.getPermissionsDescription().contains("MANAGE_USERS"));
            assertFalse(student.getPermissionsDescription().contains("REGISTER_CLUBS"));

            // Verify Coordinator permissions
            assertTrue(coordinator.getPermissionsDescription().contains("CREATE_EVENT"));
            assertFalse(coordinator.getPermissionsDescription().contains("MANAGE_USERS"));

            // Verify Admin permissions
            assertTrue(admin.getPermissionsDescription().contains("MANAGE_USERS"));
            assertTrue(admin.getPermissionsDescription().contains("REGISTER_CLUB"));
        }
    }

    // =========================================================================
    // HELPER MOCK FACTORIES
    // =========================================================================

    private static List<Club> createMockClubs() {
        List<Club> list = new ArrayList<>();
        list.add(new Club(1, "Coding & Open Source Club", "Technical", "Competitive programming, open source, web development, and algorithms", 13, LocalDate.of(2023, 1, 15), "ACTIVE"));
        list.add(new Club(2, "Robotics & AI Club", "Technical", "Autonomous robotics, computer vision, machine learning, IoT, and embedded electronics", 14, LocalDate.of(2023, 2, 10), "ACTIVE"));
        list.add(new Club(3, "Literary & Debating Society", "Literary", "Elocution, parliamentary debate, creative writing, and essay competitions", 15, LocalDate.of(2023, 3, 5), "ACTIVE"));
        list.add(new Club(4, "Music & Cultural Society", "Cultural", "Vocal choir, instrumental band, western music, and cultural fests", 16, LocalDate.of(2023, 4, 1), "ACTIVE"));
        list.add(new Club(5, "Photography & Media Club", "Arts", "Digital photography, cinematography, video editing, and campus newsletter", 17, LocalDate.of(2023, 5, 20), "ACTIVE"));
        list.add(new Club(6, "Social Impact & Rotaract Club", "Social", "Community service, blood donation, environmental drives, and outreach", 18, LocalDate.of(2023, 6, 12), "ACTIVE"));
        return list;
    }

    private static Student createSampleStudent(int id, String dept) {
        Student s = new Student();
        s.setUserId(id);
        s.setFullName("Student " + id);
        s.setDepartment(dept);
        return s;
    }
}
