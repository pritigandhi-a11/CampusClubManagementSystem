# Final Viva Examination Guide & Question Bank

**Campus Club Management System** — *Academic Viva Preparation & Code Answers*

---

### Q1. Where is encapsulation used in the project?
**Answer**:
Encapsulation is implemented across all domain model classes in the `com.campusclub.model` package (`User`, `Student`, `ClubCoordinator`, `Admin`, `Club`, `Event`, `Membership`, `EventRegistration`, `Announcement`).
1. All entity state attributes are declared `private`.
2. Public getters and setters enforce domain invariants and bounds checking (e.g., `Student.setYear()` restricts input to 1–5; `Event.setCapacity()` validates positive seating).
3. Defensive copying is used for collections via `Collections.unmodifiableList()` (e.g., `Club.getEvents()`, `Student.getMemberships()`), preventing external callers from modifying internal list states directly.

---

### Q2. Where is inheritance used?
**Answer**:
Inheritance is implemented in the user domain hierarchy:
- `com.campusclub.model.User` is the abstract base class containing common fields (`userId`, `username`, `fullName`, `email`, `role`, `phoneNumber`, `createdAt`).
- Three specialized subclasses inherit from `User`:
  - `Student extends User`: Adds register number (USN), department, year, semester, CGPA, and interest attributes.
  - `ClubCoordinator extends User`: Adds academic title/designation, department, and associated managed club reference.
  - `Admin extends User`: Adds super administrator authority levels and permission scopes.
- In the Swing GUI layer, `LoginFrame` and `MainFrame` inherit from `javax.swing.JFrame`, while `StudentDashboard`, `CoordinatorDashboard`, and `AdminDashboard` inherit from `javax.swing.JPanel`.

---

### Q3. Where is abstraction used?
**Answer**:
Abstraction is applied at two key architectural levels:
1. **Abstract Base Class**: `User` is an abstract class that cannot be directly instantiated. It defines abstract polymorphic contracts: `getRoleDisplayName()`, `getDashboardDescription()`, and `getPermissionsDescription()`.
2. **Service Interfaces**: Service-layer behavior is decoupled from implementation classes via interfaces:
   - `ClubOperations` implemented by `ClubService`
   - `EventOperations` implemented by `EventService`
   - `MembershipOperations` implemented by `MembershipService`
This ensures caller components (such as Swing GUI dashboards) interact strictly with high-level contracts rather than concrete DAO/database implementations.

---

### Q4. Where is polymorphism used?
**Answer**:
Polymorphism is demonstrated in both compile-time and runtime forms:
1. **Runtime Polymorphism (Dynamic Method Dispatch)**:
   - Calling `user.getRoleDisplayName()` or `user.getDashboardDescription()` on a `User` reference resolves dynamically to `Student`, `ClubCoordinator`, or `Admin`.
   - In `MainFrame.createRoleDashboard(User user)`, the UI dynamically instantiates and mounts the correct role-specific dashboard based on the polymorphic subtype of `User`.
   - Entities override `toString()`, `equals()`, and `hashCode()`.
2. **Compile-Time Polymorphism (Method Overloading)**:
   - Multiple overloaded constructors across `Club`, `Event`, `Student`, and `Membership` support varied initialization patterns (e.g., default no-arg constructors vs. fully-parameterized domain constructors).
   - Method overloading in service layers (e.g., `searchClubs(keyword)` and `searchClubs(keyword, category)`).

---

### Q5. Where is composition used?
**Answer**:
Composition ("Has-A" relationship) is used throughout the domain model:
- `Club` composes `List<Event>`, `List<Membership>`, and `List<Announcement>`.
- `Student` composes `List<Membership>` and `List<EventRegistration>`.
- `Event` composes `List<EventRegistration>` and provides helper methods like `isFull()` and `getRemainingSeats()`.
- `ClubCoordinator` composes a reference to the managed `Club`.
- In the GUI layer, `MainFrame` composes `DashboardCard` components, `JTabbedPane`, and role-specific dashboard panels.

---

### Q6. Why did you use JDBC?
**Answer**:
Java Database Connectivity (JDBC) provides low-level, direct control over SQL query execution, transaction management, and connection handling:
1. It avoids heavy ORM overhead (such as Hibernate or JPA), making it ideal for a focused desktop application.
2. It allows explicit control over `PreparedStatement` parameters to prevent SQL injection attacks.
3. It enables clean transaction handling (`setAutoCommit(false)`, `commit()`, `rollback()`) across multi-table operations (e.g., creating a student record and linked user record atomically).

---

### Q7. Why MySQL?
**Answer**:
MySQL 8.x is a reliable, industry-standard Relational Database Management System (RDBMS):
1. It provides strong ACID compliance and relational integrity through Foreign Key constraints with `ON DELETE CASCADE` and `ON DELETE SET NULL`.
2. It enforces uniqueness constraints at the database level (`UNIQUE(student_id, club_id)` and `UNIQUE(event_id, student_id)`), guaranteeing zero duplicate memberships or double event registrations.
3. Standard ANSI SQL compatibility and support for `ENUM` types (`STUDENT`, `COORDINATOR`, `ADMIN`).

---

### Q8. Where are custom exceptions used?
**Answer**:
Custom user-defined exceptions reside in `com.campusclub.exception`:
- `InvalidLoginException` (Checked): Thrown during failed authentication, empty credentials, or disabled accounts.
- `DuplicateMembershipException` (Checked): Thrown by `MembershipService` when a student attempts to join a club they are already a member of.
- `ClubNotFoundException` (Checked): Thrown when an operation targets a non-existent club ID.
- `EventRegistrationException` (Checked): Thrown when registering for an event that has reached full capacity, whose registration deadline has passed, or where the student is already registered.
- `DatabaseOperationException` (Unchecked `RuntimeException`): Wraps underlying JDBC `SQLException` instances to isolate persistence errors from business logic.

---

### Q9. Which Java collections are used and why?
**Answer**:
The project utilizes multiple components of the Java Collections Framework:
1. `ArrayList`: For indexed, ordered sequences of clubs, events, registrations, and tabular UI data.
2. `HashSet` & `LinkedHashSet`: For fast $O(1)$ duplicate checking (e.g., student interest tokens and set of already-joined club IDs).
3. `HashMap` & `LinkedHashMap`: For key-value mappings, category taxonomy maps, and computing metric aggregations (e.g., clubs per category, events per club).
4. `PriorityQueue`: Configured as a Max-Heap using a custom `Comparator` to rank and retrieve top-K club recommendations based on score.
5. `Collections.unmodifiableList()`: For defensive encapsulation of internal domain collections.

---

### Q10. How does the recommendation engine work?
**Answer**:
`ClubRecommendationEngine` is a **client-side rule-based recommendation system** running locally on the JVM without external AI/ML APIs:
1. **Filtering**: Fetches all active clubs and excludes clubs the student has already joined via a `HashSet` lookup ($O(1)$).
2. **Rule-Based Scoring**: Evaluates clubs against student profile data:
   - **+50 pts**: Exact match between student interest and club category.
   - **+25 pts**: Club title contains interest keyword.
   - **+15 pts**: Club description contains interest keyword.
   - **+10 pts**: Domain synonym match (e.g., `Coding` $\leftrightarrow$ `Web Development`, `Robotics` $\leftrightarrow$ `IoT`).
   - **+15 pts**: Department alignment bonus (e.g., CSE $\rightarrow$ Technical Club).
3. **Max-Heap Ranking**: Scored clubs are inserted into a `PriorityQueue` ordered descending by score.
4. **Top-K Extraction**: The top-K scoring clubs are extracted with human-readable explanations.

---

### Q11. How does the Swing GUI communicate with the database?
**Answer**:
The application enforces strict **Layered / MVC Separation**:
$$\text{Swing GUI} \xrightarrow{\text{calls}} \text{Service Layer} \xrightarrow{\text{invokes}} \text{DAO Layer} \xrightarrow{\text{JDBC PreparedStatement}} \text{MySQL 8}$$
- GUI classes (`StudentDashboard`, `CoordinatorDashboard`, `AdminDashboard`) never contain SQL queries or connection strings.
- GUI calls high-level service methods (`clubService.getAllClubs()`, `eventService.registerForEvent()`).
- Services perform validation via `ValidationUtil` and invoke DAOs.
- DAOs execute SQL using `DatabaseConnection.getConnection()` and map `ResultSet` rows to domain model objects.

---

### Q12. How is duplicate membership prevented?
**Answer**:
Duplicate membership is prevented at two independent layers:
1. **Application / Service Layer**: `MembershipService.applyForMembership()` calls `MembershipDAO.isAlreadyMember(studentId, clubId)`. If true, it immediately throws `DuplicateMembershipException`.
2. **Database Schema Layer**: The `memberships` table enforces a composite unique key constraint:
   `CONSTRAINT uq_student_club UNIQUE (student_id, club_id)`.

---

### Q13. How is event capacity enforced?
**Answer**:
Event capacity is validated before any registration is committed:
1. `EventService.registerForEvent(eventId, studentId)` retrieves the `Event` entity.
2. It queries `EventRegistrationDAO.getRegistrationCount(eventId)` to determine active registrations.
3. If `activeRegistrations >= event.getCapacity()`, it throws `EventRegistrationException("Event has reached maximum seat capacity.")`.
4. It also validates `event.getRegistrationDeadline()` against the current timestamp to ensure registration has not closed.

---

### Q14. How are user roles separated?
**Answer**:
Role separation is enforced via the `UserRole` enum (`STUDENT`, `COORDINATOR`, `ADMIN`):
1. **Database**: The `users.role` column is an `ENUM('STUDENT', 'COORDINATOR', 'ADMIN')`.
2. **Authentication**: `AuthenticationService.login()` reads the role and instantiates the concrete domain subclass (`Student`, `ClubCoordinator`, or `Admin`).
3. **UI Dispatch**: `MainFrame.createRoleDashboard(user)` inspects the role and loads only the permitted dashboard panel.
4. **Scoping**: `CoordinatorDashboard` strictly filters data by `coordinator.getManagedClub().getClubId()`, preventing coordinators from viewing or editing other clubs.

---

### Q15. How are passwords handled?
**Answer**:
1. Passwords are never stored in plaintext.
2. `PasswordUtil.hashPassword()` uses industry-standard **SHA-256 cryptographic hashing**.
3. When a user authenticates, their input password is encrypted with SHA-256 and compared against the stored `password_hash` in MySQL.
4. Plaintext passwords and hash strings are never displayed in the UI, printed to console logs, or stored in GUI memory.

---

### Q16. What are the limitations of the current implementation?
**Answer**:
1. **Single JVM Desktop Scope**: As a Java Swing desktop application, each client must run on a desktop JVM with network access to the MySQL server.
2. **Synchronous JDBC Calls**: Database queries execute on the Swing Event Dispatch Thread (EDT) for simplicity; on high-latency networks, a `SwingWorker` background thread pool would provide smoother UI responsiveness.
3. **Rule-Based Recommendation Scope**: The recommendation engine uses deterministic heuristic scoring rather than collaborative filtering or machine learning embeddings.

---

### Q17. What are potential future enhancements?
**Answer**:
1. **Background Asynchronous Workers**: Wrap JDBC calls inside `SwingWorker` tasks with loading spinners.
2. **PDF Certificate Generation**: Export event participation certificates using iText/PDFBox.
3. **Email / SMS Notifications**: Send event registration confirmations and announcement alerts via JavaMail API.
4. **Attendance QR Code Scanner**: Allow club coordinators to scan QR codes for automated event check-ins.
5. **REST API Gateway**: Transition the Service layer to a headless REST backend to support mobile apps alongside the Swing desktop client.
