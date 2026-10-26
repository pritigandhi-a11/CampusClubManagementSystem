# Campus Club Management System

A comprehensive College Desktop Management and Activity Platform built using **Java 17**, **Java Swing**, **Maven**, **JDBC**, and **MySQL 8**.

---

## 1. Project Title
**Campus Club Management System (CCMS)** — *A Desktop Extracurricular Activity & Club Administration Suite*

---

## 2. Problem Statement
In modern college campuses, club management, student extracurricular participation, and event scheduling are often fragmented across disparate chat groups, manual spreadsheets, and physical notice boards. This leads to:
- Lack of centralized visibility for students regarding available clubs and campus events.
- Inefficient, error-prone manual tracking of membership applications and event registrations.
- Difficulties in enforcing business constraints such as seat capacities, registration deadlines, and duplicate membership prevention.
- Absence of interest-aligned recommendations to help students discover clubs relevant to their academic discipline and hobbies.

The Campus Club Management System resolves these challenges through a unified, secure, and robust Java Swing desktop platform.

---

## 3. Objectives
1. **Centralized Platform**: Provide a single desktop hub for students, coordinators, and administrators.
2. **Automated Workflows**: Streamline club membership approvals, event registrations, and campus broadcasts.
3. **Strict Business Integrity**: Enforce data consistency, capacity limitations, and anti-duplication rules at both application and database layers.
4. **Smart Discovery**: Implement a client-side, rule-based recommendation engine matching student interests to campus clubs.
5. **Educational OOP Showcase**: Demonstrate clean Object-Oriented Architecture, custom exception handling, Java Collections Framework usage, and JDBC persistence.

---

## 4. Features

### Student Dashboard (7 Tabs)
1. **Club Discovery**: Search clubs by keywords, filter by category (*Technical, Cultural, Sports, Academic*), and inspect details.
2. **My Memberships**: Apply for clubs, view membership status (*PENDING, APPROVED, REJECTED*), and track active roles.
3. **Upcoming Events**: View scheduled events, dates, venues, capacities, and available seats with instant registration.
4. **My Event Passes**: View registration confirmation, unique cryptographic event pass IDs (`CCMS-XXXX-XXXX`), and pass details.
5. **Announcements**: Display relevant campus and club announcements with priority indicators (*HIGH, MEDIUM, LOW*).
6. **Smart Recommendations**: Tailored recommendations using a Java `PriorityQueue` Max-Heap with transparent ranking criteria and match scores.
7. **Profile**: View USN, department, semester, phone, and update permitted profile interests.

### Coordinator Dashboard (4 Tabs)
1. **Managed Club Overview**: View assigned club details, active membership statistics, and upcoming activities.
2. **Membership Requests**: Review, approve, or reject student applications with persistent database updates.
3. **Event Management**: Create, edit, and cancel authorized club events; manage capacity, view attendee rosters, and record attendance (`Mark Selected Attended / Absent`).
4. **Broadcast Announcements**: Publish announcements for the appropriate club members and campus feed.

### Administrator Dashboard (4 Tabs)
1. **System KPI Metrics and Analytics**: Display database-backed totals for users, clubs, memberships, events, and registrations.
2. **User Registry and RBAC**: Search, view, create users, assign/edit roles (*STUDENT, COORDINATOR, ADMIN*), and deactivate/delete accounts.
3. **Manage Clubs and Coordinators**: Create and update clubs, assign coordinators, and toggle active/inactive status.
4. **Campus Events Oversight**: Review all campus events, monitor registration capacity, and cancel/delete events.

---

## 5. Technology Stack
- **Language**: Java 17+ (JDK 26 runtime supported)
- **GUI Toolkit**: Java Swing (Nimbus / System Look & Feel)
- **Database**: MySQL 8.x (`campus_club_db`)
- **Persistence**: Java Database Connectivity (JDBC) via `mysql-connector-j 8.4.0`
- **Build & Dependency Tool**: Apache Maven 3.10.0
- **Testing Framework**: JUnit 5 (Jupiter 5.10.2)

---

## 6. Architecture & System Flow
The application enforces strict **Layered / MVC-style Architecture**:

```
Swing Desktop GUI  (LoginFrame, MainFrame, Student/Coord/Admin Dashboards)
        │
        ▼
Service Layer      (ClubService, EventService, MembershipService, RecommendationEngine)
        │
        ▼
DAO Layer          (UserDAO, StudentDAO, ClubDAO, MembershipDAO, EventDAO, EventRegDAO)
        │
        ▼
JDBC Layer         (DatabaseConnection, PreparedStatements, Connection Pooling)
        │
        ▼
MySQL 8 Database   (campus_club_db relational schema)
```

Detailed architectural documentation is available in [ARCHITECTURE.md](file:///c:/Users/Preeti%20gandhi/Desktop/CampusClubManagementSystem/docs/ARCHITECTURE.md) and [final-architecture.md](file:///c:/Users/Preeti%20gandhi/Desktop/CampusClubManagementSystem/docs/final-architecture.md).

---

## 7. Object-Oriented Programming (OOP) Concepts
1. **Encapsulation**: Private fields across all model classes with validated getters/setters and defensive collection copying (`Collections.unmodifiableList()`).
2. **Inheritance**: `User` abstract base class extended by `Student`, `ClubCoordinator`, and `Admin`.
3. **Abstraction**: Abstract methods on `User` and service contracts (`ClubOperations`, `EventOperations`, `MembershipOperations`).
4. **Polymorphism**: Dynamic method dispatch for role display and dashboard routing; method overloading and overriding (`equals`, `hashCode`, `toString`).
5. **Composition**: `Club` composes events, memberships, and announcements; `Student` composes memberships and event passes.

Detailed OOP documentation is available in [OOP.md](file:///c:/Users/Preeti%20gandhi/Desktop/CampusClubManagementSystem/docs/OOP.md).

---

## 8. Exception Handling & Custom Exceptions
Multi-tier exception architecture isolating database errors and providing clear GUI dialogs:
- **Checked Exceptions**: `InvalidLoginException`, `DuplicateMembershipException`, `ClubNotFoundException`, `EventRegistrationException`.
- **Unchecked Exceptions**: `DatabaseOperationException` (wrapping SQL exceptions) and `IllegalArgumentException` (validation).

Detailed exception documentation is available in [EXCEPTION_HANDLING.md](file:///c:/Users/Preeti%20gandhi/Desktop/CampusClubManagementSystem/docs/EXCEPTION_HANDLING.md).

---

## 9. Collections & Data Structures
- **`ArrayList`**: Dynamic lists for table models, event rosters, and search results.
- **`HashSet` & `LinkedHashSet`**: Fast $O(1)$ set membership for already-joined clubs and unique interest tokens.
- **`HashMap` & `LinkedHashMap`**: Category taxonomy mapping and dashboard metric aggregations.
- **`PriorityQueue`**: Max-Heap implementation for score-based top-K club recommendations.

Detailed collections documentation is available in [COLLECTIONS_AND_RECOMMENDATION.md](file:///c:/Users/Preeti%20gandhi/Desktop/CampusClubManagementSystem/docs/COLLECTIONS_AND_RECOMMENDATION.md).

---

## 10. Smart Club Recommendation Engine
A client-side rule-based recommendation system that scores clubs against student interests:
- **+50 pts**: Exact category match.
- **+25 pts**: Title keyword match.
- **+15 pts**: Description keyword match.
- **+10 pts**: Synonym/related term match.
- **+15 pts**: Academic department alignment bonus.
- Already-joined clubs are filtered out in $O(1)$ time.

---

## 11. JDBC & MySQL Integration
- Central connection manager: `DatabaseConnection.java` reading properties from `src/main/resources/db.properties`.
- Parameterized `PreparedStatement` usage across all DAOs to guarantee SQL injection immunity.
- Atomic multi-table transactions for complex workflows.

---

## 12. Graphical User Interface (GUI)
- Built with pure Java Swing.
- Standardized design system via `UITheme.java` (Navy `#192A56`, Blue `#2563EB`, Emerald `#10B981`, Slate `#F8FAFC`).
- Non-editable JTable components with custom header renderers and row padding.
- Headless-safe integration test coverage.

Detailed GUI documentation is available in [GUI.md](file:///c:/Users/Preeti%20gandhi/Desktop/CampusClubManagementSystem/docs/GUI.md).

---

## 13. User Roles & Permissions

| Role | Permissions & Scope |
|:---|:---|
| **Student** | Browse clubs, submit join requests, register for events, view broadcasts, receive recommendations, manage profile. |
| **Coordinator** | Manage assigned club details, approve/reject membership requests, schedule events, view attendee rosters, post announcements. |
| **Administrator** | View campus metrics, manage user directory, register/edit/toggle/delete clubs, view all campus events and announcements. |

---

## 14. How to Configure MySQL

1. Ensure MySQL Server 8.x is installed and running on port `3306`.
2. Import the schema and seed data:
   ```bash
   mysql -u root -p < database/schema.sql
   ```
3. Update connection properties in `src/main/resources/db.properties` if using custom credentials:
   ```properties
   db.url=jdbc:mysql://localhost:3306/campus_club_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   db.user=root
   db.password=root
   ```

---

## 15. How to Build and Run

### Compile
```bash
mvn clean compile
```

### Run Tests
```bash
mvn test
```

### Package JAR
```bash
mvn clean package
```

### Launch Desktop Application
```bash
mvn exec:java
```
Or run `com.campusclub.app.CampusClubApplication` directly in your IDE.

---

## 16. Demo Credentials

| Role | Username | Password | Notes / Assigned Department |
|:---|:---|:---|:---|
| **Administrator** | `admin` | `admin123` | System Super Administrator |
| **Coordinator** | `coord_coding` | `pass123` | Coding Club Coordinator |
| **Coordinator** | `coord_robotics` | `pass123` | Robotics Club Coordinator |
| **Student** | `student1` | `pass123` | Alex Turner (CSE, 3rd Year) |
| **Student** | `student2` | `pass123` | Sophia Chen (ECE, 2nd Year) |

---

## 17. Testing Status
- **Total Tests Configured**: 100
- **Unit & Model Tests**: 15 passed
- **Exception Handling Tests**: 11 passed
- **Collections & Recommendation Tests**: 31 passed
- **Credential Consistency Tests**: 5 passed
- **GUI Integration Tests**: 11 passed
- **End-to-End Workflow Tests**: 7 passed
- **OOP Hierarchy & Polymorphism Tests**: 11 passed
- **Teacher Dashboard Web Routing Tests**: 9 passed
- **Live Database Tests**: 11 passed (connected directly to active local MySQL 8.4 service)
- **Total Tests Passed**: **100 passed, 0 failures, 0 errors, 0 skipped**
- **Build Result**: `BUILD SUCCESS` (Compiled with Maven `javac [debug release 17]`)

Detailed test documentation is available in [TESTING.md](file:///c:/Users/Preeti%20gandhi/Desktop/CampusClubManagementSystem/docs/TESTING.md).

---

## 18. Known Limitations
1. **Desktop Client Scope**: Built as a native desktop application (Swing GUI); web view (`http://localhost:8080/teacherDashboard`) operates as a demonstration companion view.
2. **Synchronous Network Calls**: Standard JDBC calls execute directly; high-latency remote database environments would benefit from asynchronous `SwingWorker` threads.
3. **Deterministic Recommendation Rules**: Uses multi-factor Max-Heap scoring rather than black-box machine-learning embeddings.

---

## 19. Future Enhancements
1. **Asynchronous SwingWorkers**: Background worker threads with progress rings for remote network resilience.
2. **PDF Certificate Export**: Automated generation of branded event participation certificates via iText/PDFBox.
3. **Email Notifications**: Integration with JavaMail API for automatic event reminders.
4. **QR Code Attendance**: Mobile/camera QR barcode scanning module for instant physical check-in.
