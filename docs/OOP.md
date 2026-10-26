# Object-Oriented Programming (OOP) Specification

**Campus Club Management System** — *Core OOP Design, Principles & Implementation*

---

## 1. Overview of OOP Architecture

The Campus Club Management System is designed strictly following Object-Oriented Programming (OOP) principles. The domain model forms the backbone of the application, representing campus entities, roles, activities, and operational rules.

```
                            ┌────────────────┐
                            │   «abstract»   │
                            │      User      │
                            └───────┬────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            │                       │                       │
    ┌───────▼────────┐      ┌───────▼────────┐      ┌───────▼────────┐
    │    Student     │      │ ClubCoordinator│      │     Admin      │
    └───────┬────────┘      └───────┬────────┘      └────────────────┘
            │ 1                     │ 1
            │ has                   │ manages
            ▼ *                     ▼ 1
    ┌────────────────┐      ┌────────────────┐
    │   Membership   │◄────►│      Club      │
    └────────────────┘  *   └───────┬────────┘
                                    │ 1
                                    │ hosts
                                    ▼ *
    ┌────────────────┐      ┌────────────────┐
    │EventRegistration│◄────►│     Event      │
    └────────────────┘  *   └────────────────┘
```

---

## 2. The Five Core OOP Principles in the Project

### 2.1 Encapsulation
Encapsulation bundles data with the methods that operate on that data while restricting direct access to the internal state.

- **Data Hiding**: All fields in model entities (`User`, `Student`, `ClubCoordinator`, `Admin`, `Club`, `Membership`, `Event`, `EventRegistration`, `Announcement`) are declared `private`.
- **Validation in Mutators**: Setters enforce invariant rules. For example, `Student.setYear(int)` validates that year is between 1 and 5; `Event.setCapacity(int)` ensures positive capacity; `ValidationUtil` validates non-null strings and email formats.
- **Defensive Copying**: Entity collections expose unmodifiable views (via `Collections.unmodifiableList()`) or copies so external callers cannot mutate internal collections directly.
- **Example in Code (`Student.java` & `Event.java`)**:
  ```java
  public class Student extends User {
      private String registerNumber;
      private String department;
      private int year;
      private int semester;
      private final List<Membership> memberships = new ArrayList<>();

      public void setYear(int year) {
          if (year < 1 || year > 5) {
              throw new IllegalArgumentException("Academic year must be between 1 and 5");
          }
          this.year = year;
      }

      public List<Membership> getMemberships() {
          return Collections.unmodifiableList(memberships);
      }
  }
  ```

---

### 2.2 Inheritance
Inheritance promotes code reuse and models "is-a" taxonomic hierarchies.

- **Base Class**: `com.campusclub.model.User` serves as the abstract root containing shared credentials, identity attributes (`userId`, `username`, `fullName`, `email`, `role`, `phoneNumber`), and timestamps.
- **Subclasses**:
  - `Student extends User`: Adds register number, department, semester, year, CGPA, interests, and membership collections.
  - `ClubCoordinator extends User`: Adds academic title/designation, department, and associated managed club reference.
  - `Admin extends User`: Adds administrative authority level (`SUPER_ADMIN`).
- **Hierarchy Diagram**:
  ```
  User (Abstract Base)
    ├── Student (Specialized academic user)
    ├── ClubCoordinator (Specialized club manager)
    └── Admin (Specialized system administrator)
  ```

---

### 2.3 Abstraction
Abstraction hides low-level implementation details and presents only relevant interfaces to the user and caller components.

- **Abstract Base Classes**: `User` cannot be instantiated directly and defines abstract contracts:
  - `public abstract String getRoleDisplayName();`
  - `public abstract String getDashboardDescription();`
  - `public abstract String getPermissionsDescription();`
- **Service Interfaces**: Service-layer contracts decouple business logic from the GUI:
  - `ClubOperations` (implemented by `ClubService`)
  - `EventOperations` (implemented by `EventService`)
  - `MembershipOperations` (implemented by `MembershipService`)
- **Example Interface Contract (`ClubOperations.java`)**:
  ```java
  public interface ClubOperations {
      Club createClub(Club club);
      Club updateClub(Club club);
      boolean deleteClub(int clubId);
      Optional<Club> getClubById(int clubId);
      List<Club> getAllClubs();
      List<Club> searchClubs(String keyword, String category);
  }
  ```

---

### 2.4 Polymorphism
Polymorphism allows objects of different subclasses to be treated uniformly through their shared superclass or interface type.

- **Dynamic Method Dispatch (Runtime Polymorphism)**: Subclasses override methods defined in `User`. When invoked through a `User` reference, the JVM dispatches to the concrete subclass implementation at runtime:
  ```java
  User user = authService.login(username, password); // Returns concrete Student, ClubCoordinator, or Admin
  String badge = user.getRoleDisplayName();           // Dispatches polymorphically
  ```
- **Polymorphic Dashboard Routing in GUI (`MainFrame.java`)**:
  ```java
  public static JPanel createRoleDashboard(User user) {
      switch (user.getRole()) {
          case STUDENT -> new StudentDashboard((Student) user);
          case COORDINATOR -> new CoordinatorDashboard((ClubCoordinator) user);
          case ADMIN -> new AdminDashboard((Admin) user);
      }
  }
  ```
- **Method Overriding**: Every entity overrides `toString()`, `equals()`, and `hashCode()` for identity and state evaluation.
- **Method Overloading (Compile-Time Polymorphism)**: Multiple constructor signatures across `Club`, `Event`, `Student`, and `Membership` support diverse initialization needs.

---

### 2.5 Composition ("Has-A" Relationships)
Composition combines simpler objects to model rich composite structures with clear lifecycle relationships.

- **`Club`** composes:
  - `List<Event>`: Events hosted by this club.
  - `List<Membership>`: Members enrolled in this club.
  - `List<Announcement>`: Broadcasts published by this club.
- **`Student`** composes:
  - `List<Membership>`: Active and pending club memberships.
  - `List<EventRegistration>`: Event signups and passes.
- **`Event`** composes:
  - `List<EventRegistration>`: Registered attendees, with helper methods like `isFull()` and `getRemainingSeats()`.
- **`ClubCoordinator`** composes:
  - A reference to the managed `Club`.

---

## 3. Interfaces & Collections Summary

| Interface / Collection | Usage in Project |
|:---|:---|
| `ClubOperations` | Service contract for club lifecycle, search, and filtering |
| `EventOperations` | Service contract for event scheduling, capacity checks, and roster retrieval |
| `MembershipOperations` | Service contract for membership applications, approvals, and resignations |
| `List<T>` / `ArrayList<T>` | Ordered storage for search results, tables, and event rosters |
| `Set<T>` / `HashSet<T>` | Fast $O(1)$ lookup for joined club IDs and tokenized interest keywords |
| `Map<K, V>` / `HashMap<K, V>` | Key-value mapping for department stats, category counts, and synonym dictionaries |
| `PriorityQueue<T>` | Max-Heap ordering for top-K club recommendations based on score |

---

## 4. Verification in Test Suite
The OOP design is verified by `com.campusclub.model.OOPConceptsTest` with 11 dedicated test cases covering:
- Encapsulation validation & bounds checks
- Inheritance hierarchy & property propagation
- Abstraction contract enforcement
- Polymorphic method resolution
- Compositional integrity and collection safety
