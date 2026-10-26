# CAMPUS CLUB MANAGEMENT SYSTEM (CCMS)
## Academic Project Report & Technical Documentation

**Degree / Course**: Bachelor of Engineering / Technology (B.E. / B.Tech)  
**Department**: Computer Science and Engineering  
**Academic Year**: 2025 – 2026  
**Technology Stack**: Java 17 | Java Swing | JDBC | MySQL 8.4 | Apache Maven 3.10  

---

## TABLE OF CONTENTS
1. [Introduction](#1-introduction)
2. [Existing System vs. Proposed System](#2-existing-system-vs-proposed-system)
3. [System Requirements](#3-system-requirements)
4. [System Architecture and Design](#4-system-architecture-and-design)
5. [Object-Oriented Programming (OOP) Implementation](#5-object-oriented-programming-oop-implementation)
6. [Exception Handling & Custom Exceptions](#6-exception-handling--custom-exceptions)
7. [Collections, Data Structures & Recommendation Algorithm](#7-collections-data-structures--recommendation-algorithm)
8. [JDBC & Database Design](#8-jdbc--database-design)
9. [Graphical User Interface (Java Swing)](#9-graphical-user-interface-java-swing)
10. [Implementation Details](#10-implementation-details)
11. [Software Testing & Quality Assurance](#11-software-testing--quality-assurance)
12. [Results & Visual Evidence](#12-results--visual-evidence)
13. [Conclusion](#13-conclusion)
14. [Future Enhancements](#14-future-enhancements)

---

## 1. Introduction
Extracurricular activities and student organizations form the cornerstone of experiential learning, personal leadership, and technical networking in modern university environments. In an academic institution, student-led clubs organize hackathons, robotics challenges, sports leagues, cultural festivals, and technical workshops.

However, coordinating student activities, tracking membership applications, scheduling venue-capped events, and communicating club announcements across multiple departments is often chaotic. The **Campus Club Management System (CCMS)** is an integrated desktop software application engineered in pure **Java 17 (Java Swing)** with a **MySQL 8** relational database backend connected via **JDBC**. CCMS unifies student engagement, club coordinator management, and institutional administration under a secure, role-based desktop architecture.

### Objectives:
- Provide a centralized management platform with three dedicated user roles: **Student**, **Club Coordinator**, and **Administrator**.
- Automate club applications, event signups, participant capacity checking, and announcement broadcasts.
- Introduce an intelligent, rule-based **Smart Club Recommendation Engine** running client-side to match students with clubs according to their academic department and personal interests.
- Demonstrate rigorous software engineering practices: MVC layered architecture, complete OOP principles, comprehensive multi-tier exception handling, collections data structures, parameterized SQL security, and automated JUnit 5 testing.

---

## 2. Existing System vs. Proposed System

### 2.1 The Existing System (Manual / Ad-hoc)
In many academic institutions, club operations rely on disconnected, informal tools:
- **Communication Fragmentation**: Notices and event posters are broadcasted across informal WhatsApp/Telegram groups or physical noticeboards, causing students to miss deadlines.
- **Spreadsheet Errors**: Club coordinators record signups and attendance in unlinked Google Sheets or Excel workbooks, frequently causing duplicate registrations and accidental data overwrites.
- **Capacity Overflows**: Events exceed room capacities because manual signup forms lack automated real-time seat limits.
- **No Role Boundaries**: Coordinators and club leads have uncontrolled access to student data or lack oversight from college administrators.
- **No Guided Discovery**: Students often do not know which clubs exist or which organizations align with their skills and career goals.

### 2.2 The Proposed System (Campus Club Management System)
CCMS replaces manual and fragmented tracking with a structured, automated client-desktop solution:
- **Centralized ACID-Compliant Storage**: Relational MySQL database guarantees transactional consistency and data integrity.
- **Role-Based Security**: Polymorphic authentication grants students, coordinators, and administrators customized dashboards with strict permission isolation.
- **Automated Business Rules**: Java service layer and database unique constraints enforce seat limits, prevent duplicate memberships, and stop double event signups.
- **Client-Side Smart Recommendation**: A rule-based algorithm ranks clubs using a Max-Heap `PriorityQueue` without sending student data to external third-party AI APIs.
- **Professional Desktop UI**: Built with Java Swing and a unified `UITheme` design system for seamless navigation across all 15 functional tabs.

### 2.3 Comparative Summary Table

| Parameter | Existing System | Proposed CCMS |
|:---|:---|:---|
| **Platform** | WhatsApp / Google Forms / Paper Spreadsheets | Standalone Java Swing Desktop Application |
| **Data Integrity** | High risk of duplicate or corrupted rows | Enforced via Relational Constraints & Java Service Layer |
| **Access Control** | Unrestricted / Shared link access | Strict SHA-256 Authentication & Role-Based UI Routing |
| **Event Capacity** | Manual tallying; frequent hall overfills | Real-time seat tallying; automatic cap rejection |
| **Club Discovery** | Word of mouth; physical posters | Keyword Search, Category Filter & Max-Heap Recommendations |
| **Data Privacy** | PII shared across open spreadsheets | Localized desktop storage with hidden password hashes |

---

## 3. System Requirements

### 3.1 Hardware Requirements
- **Processor**: Intel Core i3 / AMD Ryzen 3 or higher (Dual-Core 2.0 GHz minimum).
- **RAM**: Minimum 4 GB RAM (8 GB recommended for simultaneous IDE, Maven, and MySQL server execution).
- **Hard Disk Storage**: 500 MB free hard disk space (including JDK, Maven repository, database tables, and log files).
- **Display**: Minimum $1280 \times 720$ resolution for desktop Swing window rendering ($1920 \times 1080$ recommended).

### 3.2 Software Requirements
- **Operating System**: Windows 10 / 11, Linux (Ubuntu 20.04+), or macOS 12+.
- **Java Development Kit (JDK)**: Java SE 17 LTS (Code target: Java 17; compiled and runtime tested on JDK 17 to JDK 26).
- **Build & Dependency Tool**: Apache Maven 3.8.0 or higher (Tested with Maven 3.10.0).
- **Database Engine**: MySQL Server 8.0 or higher (Tested with MySQL 8.4.11 Community Server).
- **JDBC Driver**: `com.mysql:mysql-connector-j` version 8.3.0 / 8.4.0.
- **Testing Framework**: JUnit 5 (Jupiter 5.10.2).

---

## 4. System Architecture and Design

### 4.1 Layered Architecture Overview
CCMS follows a clean, 4-tier layered architectural pattern ensuring separation of concerns:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       PRESENTATION LAYER (JAVA SWING)                       │
│     CampusClubApplication | LoginFrame | MainFrame (Dynamic Router)         │
│     StudentDashboard (7 Tabs) | CoordinatorDashboard (4 Tabs) | Admin (4)   │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Calls Service Operations
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       BUSINESS LOGIC & SERVICE LAYER                        │
│     AuthenticationService | ClubService | EventService | MembershipService  │
│     StudentService | ClubRecommendationEngine | ValidationUtil              │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Invokes DAO Queries
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       DATA ACCESS OBJECT (DAO) LAYER                        │
│     UserDAO | StudentDAO | ClubDAO | EventDAO | MembershipDAO               │
│     EventRegistrationDAO | AnnouncementDAO                                  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Uses java.sql API
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PERSISTENCE TIER (JDBC & MySQL)                    │
│     DatabaseConnection (Connection Pool / Singleton)                        │
│     PreparedStatement / ResultSet / Transactions                            │
│     MySQL 8.4 Database: campus_club_db                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Architectural Guarantees:
1. **Zero SQL in GUI**: Presentation classes do not contain SQL strings, queries, or database connection handles. All data retrieval and mutation flows through service interfaces.
2. **Polymorphic UI Router**: `MainFrame.createRoleDashboard(User user)` inspects the polymorphic subtype of `User` and renders role-specific dashboard panels.
3. **Database Independence for Models**: Domain classes in `com.campusclub.model` are pure Java POJOs containing no JDBC or SQL dependencies.

---

## 5. Object-Oriented Programming (OOP) Implementation

The project illustrates the core pillars of Object-Oriented Software Engineering:

### 5.1 Encapsulation
- **Private Fields**: All entity attributes across `User`, `Student`, `ClubCoordinator`, `Admin`, `Club`, `Event`, `Membership`, and `EventRegistration` are marked `private`.
- **Validation in Accessors/Mutators**: Setters enforce logical invariants (e.g., `Student.setYear()` restricts input between 1 and 5; `Event.setCapacity()` validates positive seating limits).
- **Defensive Copying**: Collections are exposed through `Collections.unmodifiableList()`, preventing external tampering with internal states.

### 5.2 Inheritance
- **Base Abstract Entity**: `com.campusclub.model.User` provides shared identity attributes (`userId`, `username`, `fullName`, `email`, `role`, `phoneNumber`, `createdAt`).
- **Specialized Subclasses**:
  - `Student extends User`: Adds register number (USN), department, year, semester, CGPA, and personal interests.
  - `ClubCoordinator extends User`: Adds academic title/designation, department, and associated managed club reference.
  - `Admin extends User`: Adds administrative authority levels (`SUPER_ADMIN`).
- **GUI Hierarchy**: `LoginFrame` and `MainFrame` extend `javax.swing.JFrame`; dashboard panels extend `javax.swing.JPanel`.

```
User (Abstract Class)
 ├── Student (Specialized academic entity)
 ├── ClubCoordinator (Specialized club coordinator entity)
 └── Admin (Specialized system administrator entity)
```

### 5.3 Abstraction
- **Abstract Contracts**: `User` declares abstract polymorphic methods:
  - `public abstract String getRoleDisplayName();`
  - `public abstract String getDashboardDescription();`
  - `public abstract String getPermissionsDescription();`
- **Service Layer Interfaces**: Interfaces isolate high-level business contracts from implementation details:
  - `ClubOperations` implemented by `ClubService`
  - `EventOperations` implemented by `EventService`
  - `MembershipOperations` implemented by `MembershipService`

### 5.4 Polymorphism
- **Dynamic Method Dispatch (Runtime Polymorphism)**: Calling `user.getRoleDisplayName()` on a base `User` reference dispatches at runtime to the correct concrete subclass.
- **Polymorphic UI Dispatch**: `MainFrame` accepts a `User` reference and mounts `StudentDashboard`, `CoordinatorDashboard`, or `AdminDashboard` dynamically.
- **Method Overriding**: Entities cleanly override `toString()`, `equals()`, and `hashCode()` for object identity and set hashing.
- **Method Overloading (Compile-Time Polymorphism)**: Multiple overloaded constructor signatures throughout model and service classes accommodate various initialization contexts.

### 5.5 Composition ("Has-A" Relationships)
- `Club` composes `List<Event>`, `List<Membership>`, and `List<Announcement>`.
- `Student` composes `List<Membership>` and `List<EventRegistration>`.
- `Event` composes `List<EventRegistration>` and enforces capacity caps (`isFull()`, `getRemainingSeats()`).
- `ClubCoordinator` composes a reference to their managed `Club`.

---

## 6. Exception Handling & Custom Exceptions

CCMS implements a multi-tier exception hierarchy distinguishing between recoverable domain errors and system infrastructure failures:

```
                  java.lang.Throwable
                           │
                  java.lang.Exception
                 ┌─────────┴─────────┐
       (Checked Exceptions)   (Unchecked Exceptions)
                 │                   │
  ┌──────────────┴──────────────┐   java.lang.RuntimeException
  │ - InvalidLoginException     │            │
  │ - DuplicateMembershipException           │
  │ - ClubNotFoundException         DatabaseOperationException
  │ - EventRegistrationException
```

### 6.1 Custom Exception Catalog
1. **`InvalidLoginException` (Checked)**: Thrown by `AuthenticationService` on bad credentials, empty inputs, or deactivated accounts. Protects against user enumeration by returning a standardized message: *"Invalid username or password."*
2. **`DuplicateMembershipException` (Checked)**: Thrown by `MembershipService` when a student applies for a club in which they already hold membership.
3. **`ClubNotFoundException` (Checked)**: Thrown by `ClubService` when an operation targets an invalid or deleted club ID.
4. **`EventRegistrationException` (Checked)**: Thrown by `EventService` when an event reaches maximum capacity, when the registration deadline has passed, or when a student attempts double registration.
5. **`DatabaseOperationException` (Unchecked `RuntimeException`)**: Thrown by the DAO layer to wrap underlying `java.sql.SQLException` instances. It isolates persistence errors and formats safe user-facing diagnostic codes (Error 1045, Error 1049, Error 2003) without leaking raw passwords or schema details.

### 6.2 GUI Error Boundaries
When an exception reaches the presentation layer, the GUI catches the domain exception and displays a structured `JOptionPane` warning/error dialog. Raw stack traces are never exposed to the end user.

---

## 7. Collections, Data Structures & Recommendation Algorithm

### 7.1 Java Collections Framework Usage
- **`ArrayList<T>`**: Provides indexed, dynamic-resizing storage for table models, event rosters, user directories, and search query results.
- **`HashSet<T>` & `LinkedHashSet<T>`**: Provides $O(1)$ fast set lookup to check whether a student has already joined a club and ensures uniqueness when tokenizing interest keywords.
- **`HashMap<K, V>` & `LinkedHashMap<K, V>`**: Maintains category taxonomy maps, domain synonym dictionaries, and aggregated dashboard metrics (e.g., counting active members per club).
- **`PriorityQueue<T>` (Max-Heap)**: Stores and ranks candidate club recommendations in descending order of score using a custom `Comparator`.

### 7.2 Smart Club Recommendation Algorithm
The recommendation module (`ClubRecommendationEngine.java`) is a client-side, rule-based heuristic scoring engine that runs locally on the JVM:

```
[Candidate Active Clubs]
           │
           ▼
[Filter out Already-Joined Clubs via HashSet O(1)]
           │
           ▼
[Score Remaining Candidate Clubs via Rules]
  • Category exact match with interest:  +50 points
  • Title contains interest keyword:      +25 points
  • Description contains interest keyword:+15 points
  • Domain synonym match:                 +10 points
  • Department alignment bonus:           +15 points
           │
           ▼
[Insert Scored Clubs into Max-Heap PriorityQueue]
           │
           ▼
[Poll Top-K Recommendations with Human-Readable Rationale]
```

### 7.3 Explainable Output
Each recommendation object yields a numerical score, a percentage match, and an explainable rationale:
> *"Coding Club [Score: 90 | Match: 90%] — Recommended because you selected Programming and Web Development, and your department is Computer Science."*

---

## 8. JDBC & Database Design

### 8.1 Database Schema (`campus_club_db`)
The relational schema consists of 7 normalized tables in 3rd Normal Form (3NF):

```
┌──────────────────┐       1:1       ┌──────────────────┐
│      users       ├─────────────────┤     students     │
│  (user_id PK)    │                 │  (student_id PK) │
└────────┬─────────┘                 └────────┬─────────┘
         │ 1:N (Coordinator)                  │ 1:N
         ▼                                    ▼
┌──────────────────┐       1:N       ┌──────────────────┐
│      clubs       ├─────────────────┤   memberships    │
│  (club_id PK)    │                 │ (uq_student_club)│
└────────┬─────────┘                 └──────────────────┘
         │ 1:N
         ├────────────────────────────────────┐
         ▼ 1:N                                ▼ 1:N
┌──────────────────┐       1:N       ┌──────────────────┐
│      events      ├─────────────────┤  announcements   │
│  (event_id PK)   │                 │(announcement_id) │
└────────┬─────────┘                 └──────────────────┘
         │ 1:N
         ▼
┌──────────────────┐
│event_registrations│
│(uq_event_student)│
└──────────────────┘
```

### 8.2 Relational Integrity & Key Constraints
- **Foreign Key Cascades**:
  - `students.user_id` $\rightarrow$ `users.user_id` (`ON DELETE CASCADE`)
  - `clubs.coordinator_id` $\rightarrow$ `users.user_id` (`ON DELETE SET NULL`)
  - `memberships.club_id` $\rightarrow$ `clubs.club_id` (`ON DELETE CASCADE`)
  - `events.club_id` $\rightarrow$ `clubs.club_id` (`ON DELETE CASCADE`)
- **Composite Unique Keys**:
  - `memberships`: `CONSTRAINT uq_student_club UNIQUE (student_id, club_id)`
  - `event_registrations`: `CONSTRAINT uq_event_student UNIQUE (event_id, student_id)`

### 8.3 JDBC Best Practices
- **Parameterized Queries**: All SQL statements use `java.sql.PreparedStatement` to ensure SQL injection immunity.
- **Resource Management**: Queries execute inside Java `try-with-resources` blocks, guaranteeing deterministic closure of `Connection`, `PreparedStatement`, and `ResultSet` handles.
- **Centralized Connection**: `DatabaseConnection.java` manages connections using parameters loaded from `src/main/resources/db.properties`.

---

## 9. Graphical User Interface (Java Swing)

### 9.1 Design System (`UITheme.java`)
The interface utilizes standard Java Swing enhanced with a cohesive palette and typography:
- **Primary Navy (`#192A56`)**: Window headers, branding banner, and title labels.
- **Primary Blue (`#2563EB`)**: Action buttons, search filters, and primary submit controls.
- **Accent Emerald (`#10B981`)**: Approval actions, join buttons, and success tags.
- **Danger Red (`#EF4444`)**: Logout, rejection, club removal, and cancellation buttons.
- **Canvas Slate (`#F8FAFC`)**: Card background panels with subtle compound borders.

### 9.2 Navigation Structure
1. **`LoginFrame` ($520 \times 520$)**: Card-based authentication window with demo account hints and inline status feedback.
2. **`MainFrame` ($1050 \times 720$)**: Master window with application header, user identity badge, logout confirmation button, and a central container dynamically loading role-specific dashboard panels.

### 9.3 Role Dashboard Breakdowns
- **Student Dashboard (7 Tabs)**:
  1. *Discover Clubs*: Real-time keyword search, category filter, and one-click join.
  2. *My Memberships*: Membership status table (`APPROVED` / `PENDING`) and leave club button.
  3. *Upcoming Events*: Schedules with venue, remaining seats, and registration deadline.
  4. *My Event Passes*: Active registration passes and one-click pass cancellation.
  5. *Announcements*: Broadcast notices with priority tags (`HIGH`, `MEDIUM`, `LOW`).
  6. *Smart Recommendations*: Ranked recommendation list with match scores and direct apply.
  7. *My Profile*: Academic info view and editable phone number/interest tags.
- **Coordinator Dashboard (4 Tabs)**:
  1. *Club Details*: Overview and editable club description.
  2. *Membership Requests*: Applicant list with filter and Approve / Reject controls.
  3. *Event Management*: Create event modal dialog, attendee roster viewer, and delete option.
  4. *Broadcast Announcements*: Multi-line broadcast composer with priority selector.
- **Admin Dashboard (4 Tabs)**:
  1. *Live Metric KPI Cards*: Real-time counters for Total Users, Campus Clubs, Active Clubs, and Scheduled Events.
  2. *User Registry*: User directory with search and role filter (`STUDENT`, `COORDINATOR`, `ADMIN`).
  3. *Manage Clubs*: Register club dialog, toggle status (`ACTIVE`/`INACTIVE`), and delete club.
  4. *Campus Events Overview*: Cross-club schedule monitor and cancellation control.

---

## 10. Implementation Details

### 10.1 Package Layout
The codebase is structured under `com.campusclub`:
- `com.campusclub.app`: Application entry point (`CampusClubApplication.java`).
- `com.campusclub.model`: Domain entities (`User`, `Student`, `ClubCoordinator`, `Admin`, `Club`, `Event`, `Membership`, `EventRegistration`, `Announcement`, `UserRole`).
- `com.campusclub.service`: Business logic interfaces and service implementations (`AuthenticationService`, `ClubService`, `EventService`, `MembershipService`, `StudentService`).
- `com.campusclub.dao`: Data access objects with JDBC queries (`UserDAO`, `StudentDAO`, `ClubDAO`, `EventDAO`, `MembershipDAO`, `EventRegistrationDAO`, `AnnouncementDAO`).
- `com.campusclub.exception`: Custom checked and unchecked exceptions (`InvalidLoginException`, `DuplicateMembershipException`, `ClubNotFoundException`, `EventRegistrationException`, `DatabaseOperationException`).
- `com.campusclub.gui`: Swing UI frames, dashboard tabs, metric cards, and theme styling (`LoginFrame`, `MainFrame`, `StudentDashboard`, `CoordinatorDashboard`, `AdminDashboard`, `DashboardCard`, `UITheme`).
- `com.campusclub.recommendation`: Rule-based recommendation engine (`ClubRecommendationEngine.java`).
- `com.campusclub.util`: Utility classes (`DatabaseConnection`, `PasswordUtil`, `ValidationUtil`).

### 10.2 Security Implementation
- **SHA-256 Hashing**: `PasswordUtil.hashPassword()` digests raw passwords with SHA-256 before database insertion or comparison.
- **Protection Against Injections**: 100% of DAO operations bind parameters through `PreparedStatement.setObject()` or typed setters.
- **Session Isolation**: Closing the application or clicking "Logout" safely disposes the `MainFrame` and reinstantiates a clean `LoginFrame`.

---

## 11. Software Testing & Quality Assurance

### 11.1 Test Suite Breakdown
Testing was executed through Maven Surefire and JUnit 5:

```
-------------------------------------------------------------------------------
Test Suite Class                        Tests  Passed  Failed  Errors  Skipped
-------------------------------------------------------------------------------
com.campusclub.model.OOPConceptsTest       11      11       0       0        0
com.campusclub.ExceptionHandlingTest       11      11       0       0        0
com.campusclub.CollectionsAndRecommendationTest 31 31       0       0        0
com.campusclub.CredentialsConsistencyTest   5       5       0       0        0
com.campusclub.GUIIntegrationTest          11      11       0       0        0
com.campusclub.IntegrationTest              7       7       0       0        0
com.campusclub.CampusClubModelTest          4       4       0       0        0
com.campusclub.LiveDatabaseTest            11       0       0       0       11*
-------------------------------------------------------------------------------
TOTAL EXECUTION:                           91      80       0       0       11
-------------------------------------------------------------------------------
```

### 11.2 Explanation of the 11 Conditionally Skipped Tests
The 11 skipped tests reside in `LiveDatabaseTest.java`. They use JUnit 5's `@BeforeEach` conditional check:
```java
assumeTrue(DatabaseConnection.testConnection(), "MySQL server not reachable");
```
When running in an offline or CI build environment where the local MySQL socket is not bound with default development credentials, the test suite skips these 11 tests gracefully rather than breaking the build. Once a live MySQL daemon is running with matching credentials in `db.properties` or `$env:CAMPUS_DB_PASSWORD`, all 11 tests execute against live database tables.

---

## 12. Results & Visual Evidence

The build system generates a standalone executable JAR:
```
target/campus-club-management-system-1.0.0.jar (154 KB)
```
The application executes directly using:
```bash
mvn exec:java
```
or
```bash
java -jar target/campus-club-management-system-1.0.0.jar
```

### Visual Evidence & Screenshot Gallery

The following 20 figures document the operational capabilities of the system. Place your captured application images in `docs/screenshots/` matching the referenced filenames:

#### Figure 1: Portal Login Screen (`LoginFrame`)
![Figure 1: Portal Login Screen](screenshots/01_login_screen.png)
*Description: Initial authentication window displaying username and password fields, quick credentials hint box, and real-time status label.*

#### Figure 2: Student Dashboard Overview (`StudentDashboard`)
![Figure 2: Student Dashboard Overview](screenshots/02_student_dashboard_overview.png)
*Description: Main Student Hub displaying the top four KPI metric cards (My Clubs, My Events, Campus Events, Smart Matches) and the 7 functional tabs.*

#### Figure 3: Discover Clubs Search & Filter
![Figure 3: Discover Clubs Search and Filter](screenshots/03_discover_clubs_filter.png)
*Description: Tab 1 filtered by category 'Technical' and keyword 'Robotics' showing instant tabular search.*

#### Figure 4: Club Join Application Confirmation
![Figure 4: Club Join Application Confirmation](screenshots/04_join_club_dialog.png)
*Description: Modal confirmation dialog displayed upon submitting a membership application for a campus club.*

#### Figure 5: Student Active Memberships Table
![Figure 5: Student Memberships View](screenshots/05_student_memberships_table.png)
*Description: Tab 2 displaying student's enrolled clubs with APPROVED and PENDING statuses and Leave Club action.*

#### Figure 6: Upcoming Campus Events Schedule
![Figure 6: Upcoming Campus Events](screenshots/06_upcoming_events_table.png)
*Description: Tab 3 showing event dates, venues, capacities, and live remaining seat counts.*

#### Figure 7: Event Registration Confirmation Dialog
![Figure 7: Event Registration Confirmation](screenshots/07_event_registration_dialog.png)
*Description: Success dialog confirming event pass generation and seat reservation.*

#### Figure 8: My Event Passes Management
![Figure 8: My Event Passes](screenshots/08_my_event_passes_table.png)
*Description: Tab 4 displaying registered event passes with REGISTERED status and Cancel Pass capability.*

#### Figure 9: Campus & Club Announcements Feed
![Figure 9: Campus and Club Announcements](screenshots/09_announcements_table.png)
*Description: Tab 5 showing broadcast notices from club coordinators with HIGH, MEDIUM, and LOW priority tags.*

#### Figure 10: Smart Club Recommendations (Max-Heap)
![Figure 10: Smart Recommendations](screenshots/10_smart_recommendations.png)
*Description: Tab 6 displaying Max-Heap prioritized recommendations with match percentages and explainable match rationale.*

#### Figure 11: Student Profile & Interests Editor
![Figure 11: Student Profile and Interests](screenshots/11_student_profile_tab.png)
*Description: Tab 7 displaying USN, Department, Semester, and editable contact info and interest tags.*

#### Figure 12: Coordinator Dashboard Overview (`CoordinatorDashboard`)
![Figure 12: Coordinator Dashboard Overview](screenshots/12_coordinator_dashboard_overview.png)
*Description: Management hub scoped to the coordinator's assigned club with 4 KPI cards and 4 management tabs.*

#### Figure 13: Membership Requests Approval Panel
![Figure 13: Membership Requests Approval](screenshots/13_membership_request_approval.png)
*Description: Tab 2 displaying pending applicant roster with Approve and Reject control buttons.*

#### Figure 14: Event Creation Modal Dialog
![Figure 14: Event Creation Modal Dialog](screenshots/14_create_event_modal.png)
*Description: Event scheduling form with Event Name, Venue, Date/Time, Capacity limit, and Deadline.*

#### Figure 15: Event Attendee Roster Viewer
![Figure 15: Event Attendee Roster](screenshots/15_event_attendee_roster.png)
*Description: Attendee roster dialog showing registered students, USNs, and registration timestamps.*

#### Figure 16: Broadcast Announcement Composer
![Figure 16: Broadcast Announcement Dialog](screenshots/16_broadcast_announcement_form.png)
*Description: Broadcast composer with title, priority dropdown, and message text area.*

#### Figure 17: Admin Dashboard Overview (Live System KPIs)
![Figure 17: Admin Dashboard Overview](screenshots/17_admin_dashboard_kpis.png)
*Description: System administrator dashboard showing real-time counters for Total Users, Campus Clubs, Active Clubs, and Scheduled Events.*

#### Figure 18: Admin User Registry & Role Filtering
![Figure 18: Admin User Registry](screenshots/18_admin_user_registry_table.png)
*Description: User directory filtered by role (STUDENT, COORDINATOR, ADMIN) with zero password hash leakage.*

#### Figure 19: Admin Manage Clubs & Status Toggle
![Figure 19: Admin Manage Clubs](screenshots/19_admin_manage_clubs.png)
*Description: Club lifecycle management table showing Register Club, Status Toggle (ACTIVE/INACTIVE), and Delete controls.*

#### Figure 20: Build & Database Verification Proof
![Figure 20: Build and MySQL Evidence](screenshots/20_build_and_mysql_evidence.png)
*Description: Command terminal showing Maven BUILD SUCCESS alongside MySQL SHOW TABLES output verifying all 7 core tables.*

---

## 13. Conclusion
The **Campus Club Management System** demonstrates a complete, reliable, and secure Java desktop application. By integrating:
- A pure **Java Swing** presentation layer,
- A decoupled **Service** and **DAO** architecture,
- An ACID-compliant **MySQL 8** relational database,
- Complete **Object-Oriented Programming** principles,
- Comprehensive **Exception Handling**, and
- A **Max-Heap Rule-Based Recommendation Engine**,

the project successfully fulfills all academic and functional requirements of a collegiate software engineering capstone.

---

## 14. Future Enhancements
1. **Asynchronous Background Workers**: Wrap JDBC calls in `javax.swing.SwingWorker` background threads with loading spinners to maintain high responsiveness on slow networks.
2. **Automated PDF Certificate Generation**: Integrate Apache PDFBox or iText to export participation certificates for completed club events.
3. **Automated Email Notifications**: Integrate the JavaMail API to send registration confirmations and broadcast digests directly to student mailboxes.
4. **QR Code Attendance System**: Implement QR code check-in scanning so coordinators can record real-time event attendance on campus.
