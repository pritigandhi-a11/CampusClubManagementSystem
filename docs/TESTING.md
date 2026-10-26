# Testing & Verification Specification

**Campus Club Management System** — *Test Architecture, Suites & Verification Results*

---

## 1. Test Architecture Overview

The system includes a multi-layered JUnit 5 testing framework ensuring that all OOP concepts, custom exceptions, collections, rule-based recommendation logic, credential consistency, GUI integration, and database operations function correctly.

```
src/test/java/com/campusclub/
├── CampusClubModelTest.java             # Unit tests for domain models & entities
├── model/
│   └── OOPConceptsTest.java            # Comprehensive OOP verification suite
├── ExceptionHandlingTest.java          # Multi-tier exception & validation test suite
├── CollectionsAndRecommendationTest.java # Collections, Max-Heap & algorithm tests
├── CredentialsConsistencyTest.java     # Authentication & credential consistency suite
├── GUIIntegrationTest.java             # Headless-safe Swing UI & view integration tests
├── IntegrationTest.java                # Multi-role end-to-end integration workflows
├── CampusClubDatabaseTest.java         # JDBC Mock/DAO isolation test suite
└── LiveDatabaseTest.java               # Live MySQL integration tests (executed against active DB)
```

---

## 2. Test Suite Breakdown

### 2.1 OOP Concepts Suite (`OOPConceptsTest.java`)
- **Total Tests**: 11
- **Focus**:
  - Encapsulation: Validates property protection, invalid input bounds (e.g., negative capacity, invalid academic semester), and defensive collection copying.
  - Inheritance: Verifies `Student`, `ClubCoordinator`, and `Admin` specialization from `User`.
  - Abstraction: Validates abstract contracts on `User` and service interfaces (`ClubOperations`, `EventOperations`, `MembershipOperations`).
  - Polymorphism: Tests runtime polymorphic dispatch, method overriding (`toString`, `equals`, `hashCode`), and constructor overloading.
  - Composition: Tests "has-a" relationship integrity between `Club`, `Event`, `Membership`, and `EventRegistration`.

### 2.2 Exception Handling Suite (`ExceptionHandlingTest.java`)
- **Total Tests**: 11
- **Focus**:
  - `InvalidLoginException` during bad credential attempts.
  - `DuplicateMembershipException` when enrolling twice in the same club.
  - `ClubNotFoundException` on missing club references.
  - `EventRegistrationException` on capacity exhaustion, past deadlines, or duplicate registrations.
  - `DatabaseOperationException` wrapping SQL exceptions.
  - `ValidationUtil` input constraint checks.

### 2.3 Collections & Smart Recommendation Suite (`CollectionsAndRecommendationTest.java`)
- **Total Tests**: 31
- **Focus**:
  - Ordered indexing and iteration in `ArrayList`.
  - Fast $O(1)$ lookup and set deduplication in `HashSet` and `LinkedHashSet`.
  - Key-value mapping, frequency counting, and categorization in `HashMap` and `LinkedHashMap`.
  - Max-Heap sorting via `PriorityQueue` with custom `Comparator`.
  - Rule-based scoring: Interest match (+50), keyword in title (+25), keyword in description (+15), domain synonym (+10), department match (+15).
  - Already-joined club exclusion filter.

### 2.4 Credential Consistency Suite (`CredentialsConsistencyTest.java`)
- **Total Tests**: 5
- **Focus**:
  - Verifies that all documented demo credentials (`admin / admin123`, `coord_coding / pass123`, `coord_robotics / pass123`, `student1 / pass123`, `student2 / pass123`) authenticate through `AuthenticationService` and resolve to correct `UserRole` instances.

### 2.5 Headless-Safe GUI Integration Suite (`GUIIntegrationTest.java`)
- **Total Tests**: 11
- **Focus**:
  - Instantiates `LoginFrame`, `MainFrame`, `StudentDashboard`, `CoordinatorDashboard`, and `AdminDashboard` in a headless environment.
  - Validates component creation, tab initialization, layout boundaries, and polymorphic view dispatch.

### 2.6 Full Multi-Role Integration Suite (`IntegrationTest.java`)
- **Total Tests**: 7
- **Focus**:
  - Student 1 Workflow: Discover clubs, join club, register for events, inspect recommendations.
  - Student 2 Workflow: Departmental recommendations, event pass management.
  - Coordinator Workflow: Scoped club dashboard, approve/reject requests, schedule events, attendee rosters, announcements.
  - Admin Workflow: Platform metrics, user registry filtering, club status toggling, campus event management.
  - Role Isolation: Restricts unauthorized dashboard access between roles.

### 2.7 Live Database Integration Suite (`LiveDatabaseTest.java`)
- **Total Tests**: 11 (Conditional)
- **Focus**:
  - Verifies connection to live MySQL 8.4 database (`campus_club_db`).
  - Validates table schemas and row count minimums ($\ge 12$ users, $\ge 8$ students, $\ge 6$ clubs, $\ge 8$ events, $\ge 4$ announcements).
  - **Execution Note**: Automatically skipped during offline Maven builds if local MySQL service is offline, preventing build failures in non-database environments.

---

## 3. Test Execution Summary

```
-------------------------------------------------------
 T E S T S   E X E C U T I O N   R E S U L T S
-------------------------------------------------------
Running com.campusclub.model.OOPConceptsTest
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0

Running com.campusclub.ExceptionHandlingTest
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0

Running com.campusclub.CollectionsAndRecommendationTest
Tests run: 31, Failures: 0, Errors: 0, Skipped: 0

Running com.campusclub.CredentialsConsistencyTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0

Running com.campusclub.GUIIntegrationTest
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0

Running com.campusclub.IntegrationTest
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0

Running com.campusclub.CampusClubModelTest
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0

Running com.campusclub.LiveDatabaseTest
Tests run: 11, Failures: 0, Errors: 0, Skipped: 11 (Offline unit-test run)

Results :
Tests run: 91, Failures: 0, Errors: 0, Skipped: 11 (80 Passed, 0 Failed, 0 Errors, 11 Skipped)
```

---

## 4. How to Run Tests

### Run All Unit & Integration Tests:
```bash
mvn test
```

### Run a Specific Test Suite:
```bash
mvn test -Dtest=OOPConceptsTest
mvn test -Dtest=CollectionsAndRecommendationTest
mvn test -Dtest=IntegrationTest
```
