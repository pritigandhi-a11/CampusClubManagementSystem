# PowerPoint (PPT) Presentation Guide & Slide-by-Slide Content

**Project**: Campus Club Management System (CCMS)  
**Type**: Java Desktop Application (Java 17, Java Swing, JDBC, MySQL 8)

---

## Slide 1: Title Slide
- **Title**: Campus Club Management System
- **Subtitle**: A Java Desktop Application for Extracurricular Activity & Club Administration
- **Technology Stack**: Java 17 | Java Swing | JDBC | MySQL 8 | Apache Maven
- **Presented by**: [Your Name & Register Number / USN]
- **Department**: Department of Computer Science & Engineering [or your department]
- **Institution**: [Your College / University Name]

---

## Slide 2: Introduction & Problem Statement
- **Background**: Extracurricular activities and student clubs are vital for student growth, technical upskilling, and campus culture.
- **Existing Problems**:
  - Fragmented communication across WhatsApp/Telegram groups and physical notice boards.
  - Manual, error-prone tracking of memberships and event registrations in spreadsheets.
  - Lack of business constraint enforcement (seat limits, deadlines, duplicate registrations).
  - No smart discovery mechanism to recommend clubs matching a student’s academic department and personal interests.
- **Proposed Solution**: A centralized, secure Java Swing desktop application connected to MySQL via JDBC.

---

## Slide 3: Project Objectives
1. **Centralize Operations**: Provide dedicated portals for Students, Club Coordinators, and Administrators.
2. **Automate Workflows**: Seamlessly handle membership approvals, event signups, attendee rosters, and campus broadcasts.
3. **Enforce Data Integrity**: Apply database-level constraints (`UNIQUE`, foreign keys, capacity caps) and application-level validations.
4. **Smart Club Recommendation**: Implement a client-side rule-based recommendation engine powered by Java Collections and a Max-Heap `PriorityQueue`.
5. **Demonstrate OOP Rigor**: Implement clean modular architecture emphasizing the core pillars of Object-Oriented Software Engineering.

---

## Slide 4: System Architecture (Layered / MVC)
- **Visual Diagram**:
  $$\text{Java Swing GUI} \longrightarrow \text{Service Layer} \longrightarrow \text{DAO Layer} \longrightarrow \text{JDBC} \longrightarrow \text{MySQL 8 Database}$$
- **Key Architectural Guarantees**:
  - **Zero SQL in GUI**: Presentation layer is completely decoupled from database queries.
  - **Single Responsibility**: DAO handles persistence; Service handles business rules; GUI handles presentation.
  - **Centralized Connection**: `DatabaseConnection` singleton reads credentials from `db.properties` and uses parameterized `PreparedStatement` queries.

---

## Slide 5: Object-Oriented Programming (OOP) Implementation
- **Encapsulation**:
  - Private attributes across all domain entities (`User`, `Student`, `Club`, `Event`, etc.).
  - Validated getters/setters and defensive collection copying (`Collections.unmodifiableList()`).
- **Inheritance**:
  - Abstract base class `User` extended by `Student`, `ClubCoordinator`, and `Admin`.
- **Abstraction**:
  - Abstract contracts on `User` (`getRoleDisplayName()`, `getDashboardDescription()`).
  - Service interfaces (`ClubOperations`, `EventOperations`, `MembershipOperations`).
- **Polymorphism**:
  - Dynamic method dispatch for role display and runtime polymorphic dashboard loading in `MainFrame`.
  - Method overloading across constructors and method overriding (`equals`, `hashCode`, `toString`).
- **Composition**:
  - `Club` composes events, memberships, and announcements ("Has-A" relationship).

---

## Slide 6: Database Design & Schema
- **Database**: `campus_club_db` (MySQL 8)
- **Key Relational Tables (7 Tables)**:
  1. `users`: Base credentials, roles (`STUDENT`, `COORDINATOR`, `ADMIN`), and timestamps.
  2. `students`: Academic details (USN, department, semester, interests) linked to `users`.
  3. `clubs`: Club profiles, categories, status (`ACTIVE`/`INACTIVE`), and coordinator reference.
  4. `memberships`: Student club enrollments with constraint `UNIQUE(student_id, club_id)`.
  5. `events`: Event schedules, venues, capacities, and deadlines.
  6. `event_registrations`: Attendee signups with constraint `UNIQUE(event_id, student_id)`.
  7. `announcements`: Priority broadcasts (`HIGH`, `MEDIUM`, `LOW`).

---

## Slide 7: Exception Handling & Custom Exceptions
- **Multi-Tier Exception Design**:
  - **Checked Exceptions (`extends Exception`)**: For recoverable business scenarios:
    - `InvalidLoginException`: Mismatched credentials or inactive accounts.
    - `DuplicateMembershipException`: Prevents students from joining the same club twice.
    - `ClubNotFoundException`: Triggered on invalid club references.
    - `EventRegistrationException`: Handles capacity limits, expired deadlines, or double signups.
  - **Unchecked Exceptions (`extends RuntimeException`)**:
    - `DatabaseOperationException`: Wraps low-level JDBC `SQLException` to protect presentation layer.
    - `IllegalArgumentException`: Thrown by `ValidationUtil` on invalid method inputs.
- **User-Friendly Error Boundaries**: Swing GUI catches domain exceptions and displays clean `JOptionPane` alert dialogs without exposing raw stack traces.

---

## Slide 8: Collections & Smart Recommendation Engine
- **Java Collections Used**:
  - `ArrayList`: Ordered storage for table models, event rosters, and search results.
  - `HashSet` / `LinkedHashSet`: $O(1)$ fast set lookup to exclude already-joined clubs.
  - `HashMap` / `LinkedHashMap`: Taxonomy mappings, synonym dictionaries, and metric calculations.
  - `PriorityQueue` (Max-Heap): Top-K ranking based on recommendation scores.
- **Rule-Based Scoring Logic**:
  - **+50 pts**: Category matches student interest.
  - **+25 pts**: Club title contains interest keyword.
  - **+15 pts**: Club description contains interest keyword.
  - **+10 pts**: Synonym / related domain match (e.g. `Coding` $\leftrightarrow$ `Web Dev`).
  - **+15 pts**: Academic department alignment bonus (e.g. CSE $\rightarrow$ Technical).
- **Client-Side & Explainable**: Runs entirely on the local JVM; outputs transparent human-readable match rationale.

---

## Slide 9: Module 1 — Student Portal
- **7 Integrated Tabs**:
  1. **Discover Clubs**: Keyword search + Category filter dropdown + One-click "Join Club" action.
  2. **My Memberships**: Status tracking (`APPROVED` / `PENDING`) and "Leave Club" capability.
  3. **Upcoming Events**: Event list showing date, venue, capacity, and remaining available seats.
  4. **My Event Passes**: Active event passes with one-click cancellation.
  5. **Announcements**: Campus & club broadcasts with priority badges (`HIGH`, `MEDIUM`, `LOW`).
  6. **Smart Recommendations**: Max-Heap recommendations with match percentage & direct apply.
  7. **My Profile**: View academic records and update contact info/interests with live recommendation refresh.

---

## Slide 10: Module 2 — Club Coordinator Portal
- **Scoped Management (Restricted to Assigned Club)**:
  1. **Club Overview**: Update club description, category, and foundation profile.
  2. **Membership Requests**: Review applicant roster with status filters; Approve or Reject requests.
  3. **Event Management**: Create new events with capacity caps and registration deadlines; view live attendee rosters.
  4. **Broadcast Announcements**: Publish priority announcements directly to members and the campus feed.

---

## Slide 11: Module 3 — Administrator Portal
- **Campus-Wide Oversight**:
  1. **Live KPI Metric Cards**: Real-time counts for Total Users, Campus Clubs, Active Clubs, and Scheduled Events.
  2. **User Registry**: Search and filter all users by role (`STUDENT`, `COORDINATOR`, `ADMIN`) with zero password hash leakage.
  3. **Manage Clubs**: Register new clubs, assign coordinators, toggle status (`ACTIVE` / `INACTIVE`), and delete clubs.
  4. **Campus Events & Broadcasts**: Supervise all scheduled campus events and announcements.

---

## Slide 12: Security & Data Integrity
- **Password Security**: Passwords are never stored or compared in plaintext; hashed using industry-standard **SHA-256**.
- **SQL Injection Immunity**: 100% of database queries use parameterized `PreparedStatement` instances.
- **Role Isolation**: Polymorphic router in `MainFrame` enforces strict access boundaries; coordinators cannot view other clubs.
- **Double-Layer Integrity**: Business rules enforced in Java Service layer and reinforced via MySQL relational constraints.

---

## Slide 13: Testing & Quality Assurance
- **Testing Framework**: JUnit 5 (Jupiter)
- **Test Metrics**:
  - **Total Tests Executed**: 91 tests
  - **Passed Tests**: 80 tests (0 Failures, 0 Errors)
  - **Skipped Tests**: 11 conditional tests (Live MySQL socket tests)
  - **Build Status**: Maven `BUILD SUCCESS` (42 source files compiled)
- **Test Suites**:
  - `OOPConceptsTest` (Encapsulation, Inheritance, Abstraction, Polymorphism, Composition)
  - `ExceptionHandlingTest` (Checked exceptions, validation boundaries)
  - `CollectionsAndRecommendationTest` (Collections, Max-Heap PriorityQueue, Scoring rules)
  - `CredentialsConsistencyTest` (Authentication and credential mapping)
  - `GUIIntegrationTest` (Headless-safe Swing UI integration)
  - `IntegrationTest` (Multi-role end-to-end workflows)

---

## Slide 14: Demonstration Workflow Summary
- **Live Demo Flow**:
  1. **Student Demo**: Log in as `student1` $\rightarrow$ Discover clubs $\rightarrow$ Join club $\rightarrow$ Register for event $\rightarrow$ View recommendation match $\rightarrow$ Update interests $\rightarrow$ Log out.
  2. **Coordinator Demo**: Log in as `coord_coding` $\rightarrow$ Approve pending member $\rightarrow$ Schedule new hackathon event $\rightarrow$ View attendee list $\rightarrow$ Broadcast high-priority notice $\rightarrow$ Log out.
  3. **Admin Demo**: Log in as `admin` $\rightarrow$ View system KPI metrics $\rightarrow$ Search User Registry $\rightarrow$ Register a new campus club $\rightarrow$ Toggle club status $\rightarrow$ Log out.

---

## Slide 15: Limitations & Future Enhancements
- **Current Limitations**:
  - Java Swing desktop application (requires local Java runtime).
  - Synchronous JDBC operations on Event Dispatch Thread.
  - Deterministic heuristic recommendation rules (no machine learning model).
- **Future Enhancements**:
  - Asynchronous background tasks with `SwingWorker` for high-latency remote database setups.
  - Automated PDF participation certificate generation (using iText).
  - JavaMail API integration for automated email notifications and reminders.
  - Mobile QR code attendance scanner for coordinators.

---

## Slide 16: Conclusion & Thank You
- **Summary**: The Campus Club Management System successfully provides a robust, secure, and user-friendly desktop solution for extracurricular college management.
- **Key Takeaway**: Demonstrates practical software engineering through Layered Architecture, OOP principles, Custom Exceptions, Java Collections, and JDBC MySQL integration.
- **Questions?**: Open for Viva Voce and Q&A.
