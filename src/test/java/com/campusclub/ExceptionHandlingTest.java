package com.campusclub;

import com.campusclub.dao.ClubDAO;
import com.campusclub.dao.EventDAO;
import com.campusclub.dao.EventRegistrationDAO;
import com.campusclub.dao.MembershipDAO;
import com.campusclub.dao.UserDAO;
import com.campusclub.exception.ClubNotFoundException;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.exception.DuplicateMembershipException;
import com.campusclub.exception.EventRegistrationException;
import com.campusclub.exception.InvalidLoginException;
import com.campusclub.model.Club;
import com.campusclub.model.Event;
import com.campusclub.model.EventRegistration;
import com.campusclub.model.Membership;
import com.campusclub.model.User;
import com.campusclub.service.AuthenticationService;
import com.campusclub.service.ClubService;
import com.campusclub.service.EventService;
import com.campusclub.service.MembershipService;
import com.campusclub.util.ValidationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 4 Verification Test Suite:
 * Tests custom exceptions, service-layer error handling, input validation,
 * boundary conditions, and database exception encapsulation.
 */
public class ExceptionHandlingTest {

    @Nested
    @DisplayName("1. Custom Exception Class Tests")
    class CustomExceptionTests {

        @Test
        @DisplayName("InvalidLoginException constructors and hierarchy")
        void testInvalidLoginException() {
            InvalidLoginException ex1 = new InvalidLoginException("Bad credentials");
            assertEquals("Bad credentials", ex1.getMessage());
            assertInstanceOf(Exception.class, ex1);

            Throwable cause = new RuntimeException("Root cause");
            InvalidLoginException ex2 = new InvalidLoginException("Auth failure", cause);
            assertEquals("Auth failure", ex2.getMessage());
            assertEquals(cause, ex2.getCause());

            InvalidLoginException ex3 = new InvalidLoginException(cause);
            assertEquals(cause, ex3.getCause());
        }

        @Test
        @DisplayName("DuplicateMembershipException constructors and hierarchy")
        void testDuplicateMembershipException() {
            DuplicateMembershipException ex1 = new DuplicateMembershipException("Already joined");
            assertEquals("Already joined", ex1.getMessage());
            assertInstanceOf(Exception.class, ex1);

            Throwable cause = new RuntimeException("DB unique constraint");
            DuplicateMembershipException ex2 = new DuplicateMembershipException("Duplicate entry", cause);
            assertEquals("Duplicate entry", ex2.getMessage());
            assertEquals(cause, ex2.getCause());
        }

        @Test
        @DisplayName("ClubNotFoundException constructors and hierarchy")
        void testClubNotFoundException() {
            ClubNotFoundException ex = new ClubNotFoundException("Club 999 not found");
            assertEquals("Club 999 not found", ex.getMessage());
            assertInstanceOf(Exception.class, ex);
        }

        @Test
        @DisplayName("EventRegistrationException constructors and hierarchy")
        void testEventRegistrationException() {
            EventRegistrationException ex = new EventRegistrationException("Event full");
            assertEquals("Event full", ex.getMessage());
            assertInstanceOf(Exception.class, ex);
        }

        @Test
        @DisplayName("DatabaseOperationException constructors and hierarchy (Unchecked)")
        void testDatabaseOperationException() {
            DatabaseOperationException ex1 = new DatabaseOperationException("SQL failure");
            assertEquals("SQL failure", ex1.getMessage());
            assertInstanceOf(RuntimeException.class, ex1);

            Throwable cause = new java.sql.SQLException("Connection timeout");
            DatabaseOperationException ex2 = new DatabaseOperationException("Query failed", cause);
            assertEquals("Query failed", ex2.getMessage());
            assertEquals(cause, ex2.getCause());
        }
    }

    @Nested
    @DisplayName("2. Authentication Exception Handling Tests")
    class AuthenticationExceptionTests {

        @Test
        @DisplayName("Requirement 1: Invalid login credentials throw InvalidLoginException")
        void testInvalidLoginCredentials() {
            // Mock DAO returning empty for unknown user
            UserDAO mockDAO = new UserDAO() {
                @Override
                public Optional<User> findByUsername(String username) {
                    return Optional.empty();
                }
            };
            AuthenticationService authService = new AuthenticationService(mockDAO);

            InvalidLoginException ex = assertThrows(InvalidLoginException.class, () -> {
                authService.login("non_existent_user", "password123");
            });
            assertEquals("Invalid username or password.", ex.getMessage());
        }

        @Test
        @DisplayName("Requirement 2: Empty login inputs are rejected")
        void testEmptyLoginInputs() {
            AuthenticationService authService = new AuthenticationService(new UserDAO());

            // Empty username
            InvalidLoginException exUser = assertThrows(InvalidLoginException.class, () -> {
                authService.login("", "password123");
            });
            assertTrue(exUser.getMessage().contains("Username"));

            // Empty password
            InvalidLoginException exPass = assertThrows(InvalidLoginException.class, () -> {
                authService.login("admin", "");
            });
            assertTrue(exPass.getMessage().contains("Password"));

            // Null username
            assertThrows(InvalidLoginException.class, () -> {
                authService.login(null, "password123");
            });
        }
    }

    @Nested
    @DisplayName("3. Membership Exception Handling Tests")
    class MembershipExceptionTests {

        @Test
        @DisplayName("Requirement 3: Duplicate membership throws DuplicateMembershipException")
        void testDuplicateMembershipThrows() {
            ClubDAO mockClubDAO = new ClubDAO() {
                @Override
                public Optional<Club> findById(int clubId) {
                    Club club = new Club();
                    club.setClubId(clubId);
                    club.setClubName("Coding Club");
                    return Optional.of(club);
                }
            };

            MembershipDAO mockMembershipDAO = new MembershipDAO() {
                @Override
                public boolean isAlreadyMember(int studentId, int clubId) {
                    return true; // Already registered
                }
            };

            MembershipService service = new MembershipService(mockMembershipDAO, mockClubDAO);

            DuplicateMembershipException ex = assertThrows(DuplicateMembershipException.class, () -> {
                service.applyForMembership(1, 1);
            });
            assertTrue(ex.getMessage().contains("already have an active or pending membership"));
        }

        @Test
        @DisplayName("Requirement 4: Missing club throws ClubNotFoundException")
        void testMissingClubThrows() {
            ClubDAO mockClubDAO = new ClubDAO() {
                @Override
                public Optional<Club> findById(int clubId) {
                    return Optional.empty(); // Not found
                }
            };
            MembershipDAO mockMembershipDAO = new MembershipDAO();
            MembershipService service = new MembershipService(mockMembershipDAO, mockClubDAO);

            ClubNotFoundException ex = assertThrows(ClubNotFoundException.class, () -> {
                service.applyForMembership(1, 9999);
            });
            assertTrue(ex.getMessage().contains("Club ID 9999 does not exist"));
        }
    }

    @Nested
    @DisplayName("4. Club Exception Handling Tests")
    class ClubExceptionTests {

        @Test
        @DisplayName("ClubService getClubById throws ClubNotFoundException for non-existent club")
        void testClubNotFoundInService() {
            ClubDAO mockClubDAO = new ClubDAO() {
                @Override
                public Optional<Club> findById(int clubId) {
                    return Optional.empty();
                }
            };
            ClubService service = new ClubService(mockClubDAO);

            assertThrows(ClubNotFoundException.class, () -> {
                service.getClubById(404);
            });
        }

        @Test
        @DisplayName("ClubService validates inputs strictly")
        void testClubServiceInputValidation() {
            ClubService service = new ClubService();

            assertThrows(IllegalArgumentException.class, () -> {
                service.getClubById(-1);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                service.createClub(null);
            });

            Club invalidClub = new Club();
            invalidClub.setClubName("");
            assertThrows(IllegalArgumentException.class, () -> {
                service.createClub(invalidClub);
            });
        }
    }

    @Nested
    @DisplayName("5. Event Exception Handling Tests")
    class EventExceptionTests {

        @Test
        @DisplayName("Requirement 5: Full event throws EventRegistrationException")
        void testFullEventThrows() {
            EventDAO mockEventDAO = new EventDAO() {
                @Override
                public Optional<Event> findById(int eventId) {
                    Event e = new Event();
                    e.setEventId(eventId);
                    e.setEventName("Workshop");
                    e.setCapacity(10);
                    e.setStatus("UPCOMING");
                    e.setRegistrationDeadline(LocalDateTime.now().plusDays(5));
                    return Optional.of(e);
                }
            };

            EventRegistrationDAO mockRegDAO = new EventRegistrationDAO() {
                @Override
                public boolean isAlreadyRegistered(int eventId, int studentId) {
                    return false;
                }

                @Override
                public int getRegistrationCount(int eventId) {
                    return 10; // Exactly at capacity
                }
            };

            EventService service = new EventService(mockEventDAO, mockRegDAO, null, null);

            EventRegistrationException ex = assertThrows(EventRegistrationException.class, () -> {
                service.registerForEvent(1, 100);
            });
            assertTrue(ex.getMessage().contains("maximum capacity"));
        }

        @Test
        @DisplayName("Requirement 6: Duplicate event registration throws EventRegistrationException")
        void testDuplicateEventRegistrationThrows() {
            EventDAO mockEventDAO = new EventDAO() {
                @Override
                public Optional<Event> findById(int eventId) {
                    Event e = new Event();
                    e.setEventId(eventId);
                    e.setCapacity(50);
                    e.setStatus("UPCOMING");
                    e.setRegistrationDeadline(LocalDateTime.now().plusDays(5));
                    return Optional.of(e);
                }
            };

            EventRegistrationDAO mockRegDAO = new EventRegistrationDAO() {
                @Override
                public boolean isAlreadyRegistered(int eventId, int studentId) {
                    return true; // Already registered
                }
            };

            EventService service = new EventService(mockEventDAO, mockRegDAO, null, null);

            EventRegistrationException ex = assertThrows(EventRegistrationException.class, () -> {
                service.registerForEvent(1, 100);
            });
            assertTrue(ex.getMessage().contains("already registered"));
        }

        @Test
        @DisplayName("Requirement 7: Expired deadline or non-existent event throws EventRegistrationException")
        void testExpiredOrMissingEventRegistrationThrows() {
            EventDAO mockEventDAO = new EventDAO() {
                @Override
                public Optional<Event> findById(int eventId) {
                    if (eventId == 999) return Optional.empty();
                    Event e = new Event();
                    e.setEventId(eventId);
                    e.setCapacity(50);
                    e.setStatus("UPCOMING");
                    e.setRegistrationDeadline(LocalDateTime.now().minusDays(1)); // Expired yesterday
                    return Optional.of(e);
                }
            };

            EventRegistrationDAO mockRegDAO = new EventRegistrationDAO() {
                @Override
                public boolean isAlreadyRegistered(int eventId, int studentId) {
                    return false;
                }
            };

            EventService service = new EventService(mockEventDAO, mockRegDAO, null, null);

            // Missing event
            EventRegistrationException exMissing = assertThrows(EventRegistrationException.class, () -> {
                service.registerForEvent(999, 100);
            });
            assertTrue(exMissing.getMessage().contains("Event does not exist"));

            // Expired deadline
            EventRegistrationException exExpired = assertThrows(EventRegistrationException.class, () -> {
                service.registerForEvent(1, 100);
            });
            assertTrue(exExpired.getMessage().contains("Registration closed"));

            // Invalid IDs
            assertThrows(EventRegistrationException.class, () -> {
                service.registerForEvent(-1, 100);
            });
        }
    }

    @Nested
    @DisplayName("6. ValidationUtil Tests")
    class ValidationUtilTests {

        @Test
        @DisplayName("Requirement 8: ValidationUtil rejects invalid emails")
        void testInvalidEmailValidation() {
            assertFalse(ValidationUtil.isValidEmail("plainaddress"));
            assertFalse(ValidationUtil.isValidEmail("@missingusername.com"));
            assertFalse(ValidationUtil.isValidEmail("username@.com"));
            assertFalse(ValidationUtil.isValidEmail(""));
            assertFalse(ValidationUtil.isValidEmail(null));

            assertTrue(ValidationUtil.isValidEmail("student@campus.edu"));
            assertTrue(ValidationUtil.isValidEmail("user.name+tag@example.co.in"));

            assertThrows(IllegalArgumentException.class, () -> {
                ValidationUtil.validateEmail("invalid-email-format");
            });
        }

        @Test
        @DisplayName("Requirement 9: ValidationUtil rejects invalid phone numbers")
        void testInvalidPhoneValidation() {
            assertFalse(ValidationUtil.isValidPhone("123")); // Too short
            assertFalse(ValidationUtil.isValidPhone("abc1234567")); // Contains letters
            assertFalse(ValidationUtil.isValidPhone("12345678901234567890")); // Too long
            assertFalse(ValidationUtil.isValidPhone(""));
            assertFalse(ValidationUtil.isValidPhone(null));

            assertTrue(ValidationUtil.isValidPhone("9876543210"));
            assertTrue(ValidationUtil.isValidPhone("+91-9876543210"));

            assertThrows(IllegalArgumentException.class, () -> {
                ValidationUtil.validatePhone("invalid-phone");
            });
        }

        @Test
        @DisplayName("ValidationUtil validates usernames, register numbers, and capacity")
        void testOtherValidationUtilMethods() {
            assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateRequired("", "Club Name"));
            assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateUsername("ab")); // Too short
            assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validatePassword("123")); // Too short
            assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateStudentRegisterNumber(""));
            assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateEventCapacity(0));
            assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateEventCapacity(10001));
            assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validatePositiveNumber(-5, "Count"));
        }
    }

    @Nested
    @DisplayName("7. Database Exception Encapsulation Tests")
    class DatabaseExceptionTests {

        @Test
        @DisplayName("Requirement 10: Database failures are wrapped in DatabaseOperationException")
        void testDatabaseExceptionWrapping() {
            java.sql.SQLException sqlEx = new java.sql.SQLException("Connection refused by host");
            DatabaseOperationException dbEx = new DatabaseOperationException("Failed to load clubs", sqlEx);

            assertEquals("Failed to load clubs", dbEx.getMessage());
            assertEquals(sqlEx, dbEx.getCause());
            assertTrue(dbEx.getCause() instanceof java.sql.SQLException);
        }
    }
}
