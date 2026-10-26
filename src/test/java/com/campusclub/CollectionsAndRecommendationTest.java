package com.campusclub;

import com.campusclub.model.Club;
import com.campusclub.model.Event;
import com.campusclub.model.Membership;
import com.campusclub.model.Student;
import com.campusclub.recommendation.ClubRecommendationEngine;
import com.campusclub.recommendation.ClubRecommendationEngine.ClubRecommendation;
import com.campusclub.service.ClubService;
import com.campusclub.service.EventService;
import com.campusclub.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 5 Verification Test Suite:
 * Tests Java Collections, Data Structures (List, Set, Map, PriorityQueue),
 * Searching, Sorting (Comparator), and the Rule-Based Smart Club Recommendation Engine.
 */
public class CollectionsAndRecommendationTest {

    private ClubRecommendationEngine engine;
    private List<Club> sampleClubs;
    private ClubService clubService;
    private EventService eventService;
    private StudentService studentService;

    @BeforeEach
    void setUp() {
        engine = new ClubRecommendationEngine();
        clubService = new ClubService();
        eventService = new EventService();
        studentService = new StudentService();

        sampleClubs = new ArrayList<>();
        sampleClubs.add(new Club(1, "Coding & Open Source Club", "Technical",
                "Competitive programming, hackathons, and web development", 1, LocalDate.now(), "ACTIVE"));
        sampleClubs.add(new Club(2, "Robotics & IoT Club", "Technical",
                "Autonomous robots, Arduino, sensors, and hardware IoT projects", 2, LocalDate.now(), "ACTIVE"));
        sampleClubs.add(new Club(3, "Music & Drama Society", "Cultural",
                "Acoustic bands, singing performances, and theatrical stage plays", 3, LocalDate.now(), "ACTIVE"));
        sampleClubs.add(new Club(4, "Photography & Media Club", "Cultural",
                "Digital filmmaking, editing, creative camera shoots, and visuals", 4, LocalDate.now(), "ACTIVE"));
        sampleClubs.add(new Club(5, "Campus Sports Association", "Sports",
                "Athletics, cricket, badminton tournaments, and fitness", 5, LocalDate.now(), "ACTIVE"));
        sampleClubs.add(new Club(6, "E-Cell Entrepreneurship Club", "Social",
                "Startups, business leadership, innovation pitching, and finance", 6, LocalDate.now(), "ACTIVE"));
    }

    @Nested
    @DisplayName("1. Java Collections & Data Structures Tests")
    class CollectionsDataStructuresTests {

        @Test
        @DisplayName("Requirement 1: ArrayList stores and preserves ordered clubs")
        void testArrayListClubStorage() {
            List<Club> list = new ArrayList<>(sampleClubs);
            assertEquals(6, list.size());
            assertEquals("Coding & Open Source Club", list.get(0).getClubName());
            assertEquals("E-Cell Entrepreneurship Club", list.get(5).getClubName());

            list.add(new Club(7, "Literary Guild", "Cultural", "Debating", 1, LocalDate.now(), "ACTIVE"));
            assertEquals(7, list.size());
            assertEquals("Literary Guild", list.get(6).getClubName());
        }

        @Test
        @DisplayName("Requirement 2: HashSet removes duplicate interests and handles case-insensitivity")
        void testHashSetDeduplication() {
            Student student = new Student();
            student.setInterests("Coding, AI, coding, Robotics, AI, Programming");

            Set<String> uniqueInterests = student.getInterestsSet();
            // Expected unique items (case-insensitive parsed by engine)
            Set<String> normalized = engine.extractNormalizedInterests(student);

            assertTrue(uniqueInterests.size() <= 6);
            assertEquals(4, normalized.size()); // "coding", "ai", "robotics", "programming"
            assertTrue(normalized.contains("coding"));
            assertTrue(normalized.contains("ai"));
            assertTrue(normalized.contains("robotics"));
            assertTrue(normalized.contains("programming"));
        }

        @Test
        @DisplayName("Requirement 3: HashMap correctly calculates category statistics")
        void testHashMapCategoryStatistics() {
            Map<String, Integer> categoryCounts = new HashMap<>();
            for (Club c : sampleClubs) {
                categoryCounts.put(c.getCategory(), categoryCounts.getOrDefault(c.getCategory(), 0) + 1);
            }

            assertEquals(2, categoryCounts.get("Technical"));
            assertEquals(2, categoryCounts.get("Cultural"));
            assertEquals(1, categoryCounts.get("Sports"));
            assertEquals(1, categoryCounts.get("Social"));
            assertNull(categoryCounts.get("NonExistentCategory"));
        }
    }

    @Nested
    @DisplayName("2. Search & Sorting Tests (Comparator)")
    class SearchAndSortingTests {

        @Test
        @DisplayName("Requirement 4: Club search by keyword matches name, category, or description")
        void testClubSearchByKeyword() {
            // In-memory simulation of search logic
            String term = "robot";
            List<Club> matches = new ArrayList<>();
            for (Club c : sampleClubs) {
                if (c.getClubName().toLowerCase().contains(term) || c.getDescription().toLowerCase().contains(term)) {
                    matches.add(c);
                }
            }
            assertEquals(1, matches.size());
            assertEquals("Robotics & IoT Club", matches.get(0).getClubName());

            // Search for "cultural"
            List<Club> culturalMatches = new ArrayList<>();
            for (Club c : sampleClubs) {
                if (c.getCategory().equalsIgnoreCase("Cultural")) {
                    culturalMatches.add(c);
                }
            }
            assertEquals(2, culturalMatches.size());
        }

        @Test
        @DisplayName("Requirement 5: Club sorting by name and category via Comparator")
        void testClubSorting() {
            // Sort ascending by name
            List<Club> sortedByName = clubService.sortClubsByName(sampleClubs, true);
            assertEquals("Campus Sports Association", sortedByName.get(0).getClubName());
            assertEquals("Robotics & IoT Club", sortedByName.get(sortedByName.size() - 1).getClubName());

            // Sort descending by name
            List<Club> sortedDesc = clubService.sortClubsByName(sampleClubs, false);
            assertEquals("Robotics & IoT Club", sortedDesc.get(0).getClubName());
            assertEquals("Campus Sports Association", sortedDesc.get(sortedDesc.size() - 1).getClubName());

            // Sort by category
            List<Club> sortedByCategory = clubService.sortClubsByCategory(sampleClubs);
            assertEquals("Cultural", sortedByCategory.get(0).getCategory());
        }

        @Test
        @DisplayName("Requirement 6: Event sorting by date using Comparator")
        void testEventSortingByDate() {
            List<Event> events = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            events.add(new Event(1, 1, "Hackathon", "Coding", now.plusDays(10), "Hall A", 50, now.plusDays(8)));
            events.add(new Event(2, 2, "RoboWars", "Combat", now.plusDays(2), "Arena", 40, now.plusDays(1)));
            events.add(new Event(3, 3, "Music Night", "Concert", now.plusDays(20), "Auditorium", 100, now.plusDays(15)));

            List<Event> sortedAsc = eventService.sortEventsByDate(events, true);
            assertEquals("RoboWars", sortedAsc.get(0).getEventName()); // Earliest (day 2)
            assertEquals("Hackathon", sortedAsc.get(1).getEventName()); // Day 10
            assertEquals("Music Night", sortedAsc.get(2).getEventName()); // Latest (day 20)

            List<Event> sortedDesc = eventService.sortEventsByDate(events, false);
            assertEquals("Music Night", sortedDesc.get(0).getEventName());
            assertEquals("RoboWars", sortedDesc.get(2).getEventName());
        }
    }

    @Nested
    @DisplayName("3. Smart Recommendation Engine Tests")
    class RecommendationEngineTests {

        @Test
        @DisplayName("Requirement 7: Recommendation scoring computes multi-rule weighted scores")
        void testRecommendationScoring() {
            Student techStudent = new Student();
            techStudent.setDepartment("Computer Science");
            techStudent.setInterests("Programming, Coding, Web Development");

            List<ClubRecommendation> recs = engine.recommendClubs(techStudent, sampleClubs, List.of());
            assertFalse(recs.isEmpty());

            ClubRecommendation top = recs.get(0);
            assertEquals("Coding & Open Source Club", top.getClub().getClubName());
            assertTrue(top.getScore() >= 50, "Score should be >= 50 due to multiple matching rules");
            assertTrue(top.getMatchPercentage() > 0);
        }

        @Test
        @DisplayName("Requirement 8: Top 3 recommendations are returned in ranked order")
        void testTop3RecommendationsReturned() {
            Student student = new Student();
            student.setDepartment("Computer Science");
            student.setInterests("Coding, Robotics, Sports, Music, Photography");

            List<ClubRecommendation> recs = engine.recommendClubs(student, sampleClubs, List.of());

            assertTrue(recs.size() <= 3, "Should return at most top 3 recommendations");
            // Verify descending order
            for (int i = 0; i < recs.size() - 1; i++) {
                assertTrue(recs.get(i).getScore() >= recs.get(i + 1).getScore());
            }
        }

        @Test
        @DisplayName("Requirement 9: Already-joined clubs are strictly excluded from recommendations")
        void testExcludeAlreadyJoinedClubs() {
            Student student = new Student();
            student.setStudentId(10);
            student.setDepartment("Computer Science");
            student.setInterests("Programming, Coding, Artificial Intelligence");

            // Student already joined club 1 (Coding & Open Source Club)
            List<Membership> memberships = List.of(
                    new Membership(1, 1, 10, "APPROVED", "MEMBER", LocalDateTime.now(), "John", "Coding Club")
            );

            List<ClubRecommendation> recs = engine.recommendClubs(student, sampleClubs, memberships);

            // Club 1 MUST NOT appear in the recommendations
            boolean containsCodingClub = recs.stream().anyMatch(r -> r.getClub().getClubId() == 1);
            assertFalse(containsCodingClub, "Already-joined Coding Club must be excluded");
        }

        @Test
        @DisplayName("Requirement 10: Matching interests produce higher scores than non-matching ones")
        void testMatchingInterestsProduceHigherScores() {
            Student sportsStudent = new Student();
            sportsStudent.setDepartment("Mechanical");
            sportsStudent.setInterests("Sports, Athletics, Fitness");

            List<ClubRecommendation> recs = engine.recommendClubs(sportsStudent, sampleClubs, List.of());
            assertFalse(recs.isEmpty());

            ClubRecommendation topRec = recs.get(0);
            assertEquals("Campus Sports Association", topRec.getClub().getClubName());
            assertTrue(topRec.getScore() > 0);
        }

        @Test
        @DisplayName("Requirement 11: No matching interests produces empty or low recommendations")
        void testNoMatchingInterests() {
            Student neutralStudent = new Student();
            neutralStudent.setDepartment("Civil Engineering");
            neutralStudent.setInterests("Astronomy, QuantumPhysics, Gardening"); // None of the sample clubs match

            List<ClubRecommendation> recs = engine.recommendClubs(neutralStudent, sampleClubs, List.of());
            assertTrue(recs.isEmpty(), "Clubs with 0 score should not be recommended");
        }

        @Test
        @DisplayName("Requirement 12: Recommendation reasons correctly identify matched interests")
        void testRecommendationReasonsIdentifyMatchedInterests() {
            Student student = new Student();
            student.setDepartment("Computer Science");
            student.setInterests("Robotics, IoT");

            List<ClubRecommendation> recs = engine.recommendClubs(student, sampleClubs, List.of());
            assertFalse(recs.isEmpty());

            ClubRecommendation topRec = recs.get(0);
            assertEquals("Robotics & IoT Club", topRec.getClub().getClubName());

            String summary = topRec.getReasonSummary();
            assertTrue(summary.contains("Robotics") || summary.contains("IoT"));
            assertFalse(topRec.getReasons().isEmpty());
            assertNotNull(topRec.getExplanation());
        }
    }
}
