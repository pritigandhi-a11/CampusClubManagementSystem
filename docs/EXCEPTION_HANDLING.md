# Exception Handling & User-Defined Exceptions Specification

This document details the exception handling architecture, custom exception design, validation framework, and error boundaries implemented across the **Campus Club Management System**.

---

## 1. Architectural Philosophy & Rationale

In a layered desktop application (GUI $\rightarrow$ Service $\rightarrow$ DAO $\rightarrow$ JDBC $\rightarrow$ MySQL), exception handling serves three critical purposes:
1. **Fault Isolation**: Technical infrastructure errors (such as network disconnections or SQL constraint failures) are insulated within the DAO layer and translated into domain-level exceptions before reaching business or GUI layers.
2. **Predictable Recovery**: Domain business rule violations (such as capacity overflows, duplicate memberships, or expired deadlines) are explicitly signaled via **checked exceptions**, ensuring that caller layers handle each scenario and provide clear feedback to the user.
3. **Security & Information Masking**: Sensitive database internal schemas, SQL queries, and authentication details (e.g. distinguishing non-existent usernames from incorrect passwords) are never leaked to the client or console logs.

---

## 2. Checked vs. Unchecked Exception Strategy

The project adopts a clear, principled division between **checked** and **unchecked** exceptions:

```
                  java.lang.Throwable
                           │
                  java.lang.Exception
                ┌──────────┴──────────┐
         (Checked Exceptions)  (Unchecked Exceptions)
                │                     │
      ┌─────────┴─────────┐   java.lang.RuntimeException
      │                   │           │
Domain Business Exceptions       DatabaseOperationException
- InvalidLoginException          - Unrecoverable SQL/JDBC faults
- DuplicateMembershipException
- ClubNotFoundException
- EventRegistrationException
```

### Checked Exceptions (`extends Exception`)
* **When to use**: For recoverable business scenarios where the client/caller is expected to take corrective action or display a specific notice to the user.
* **Compiler Enforcement**: The Java compiler mandates that callers handle these with `try-catch` blocks or declare them in their `throws` signature.
* **Examples**:
  * `InvalidLoginException`: User supplied invalid credentials $\rightarrow$ GUI alerts user to re-enter.
  * `DuplicateMembershipException`: Student already enrolled $\rightarrow$ GUI warns that application is active.
  * `ClubNotFoundException`: Requested club does not exist $\rightarrow$ Service aborts dependent operations.
  * `EventRegistrationException`: Event full or deadline passed $\rightarrow$ GUI notifies student of seat unavailability.

### Unchecked Exceptions (`extends RuntimeException`)
* **When to use**: For fatal, infrastructure-level failures or program logic contract violations that cannot be recovered from at runtime.
* **Examples**:
  * `DatabaseOperationException`: Wraps low-level `java.sql.SQLException` errors (database unreachable, query syntax errors).
  * `IllegalArgumentException`: Thrown by `ValidationUtil` when invalid or null parameters are supplied to service methods.

---

## 3. Custom Exception Catalog

All user-defined exceptions reside in `com.campusclub.exception`:

| Exception Class | Base Class | Primary Purpose | Thrown By |
|---|---|---|---|
| `InvalidLoginException` | `Exception` (Checked) | Credential mismatch, deactivated account, or empty login fields | `AuthenticationService` |
| `DuplicateMembershipException` | `Exception` (Checked) | Prevent student from joining same club twice | `MembershipService` |
| `ClubNotFoundException` | `Exception` (Checked) | Lookup or operation on non-existent club ID | `ClubService`, `MembershipService`, `EventService` |
| `EventRegistrationException` | `Exception` (Checked) | Seat capacity exhaustion, expired deadlines, duplicate signups | `EventService` |
| `DatabaseOperationException` | `RuntimeException` (Unchecked) | Encapsulates underlying JDBC `SQLException` with root cause | `UserDAO`, `StudentDAO`, `ClubDAO`, `MembershipDAO`, `EventDAO`, `EventRegistrationDAO`, `AnnouncementDAO` |

---

## 4. Layer-by-Layer Exception Flow

```
┌─────────────────────────────────────────────────────────────┐
│                      Swing GUI Layer                        │
│ - Catches checked exceptions & displays JOptionPane dialogs │
│ - Never exposes raw stack traces to normal end-users        │
└──────────────────────────────┬──────────────────────────────┘
                               │ Calls service methods
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                       Service Layer                         │
│ - Validates inputs via ValidationUtil                       │
│ - Enforces domain business rules                            │
│ - Throws domain checked exceptions on rule violations       │
└──────────────────────────────┬──────────────────────────────┘
                               │ Invokes DAO queries
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                         DAO Layer                           │
│ - Executes JDBC PreparedStatements with try-with-resources  │
│ - Catches java.sql.SQLException                             │
│ - Wraps in DatabaseOperationException(message, sqlException)│
└──────────────────────────────┬──────────────────────────────┘
                               │ JDBC Connectivity
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      MySQL 8.4 Database                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. Concrete Code Examples

### 1. Authentication Exception Handling & User Masking
```java
// AuthenticationService.java
public User authenticateUser(String username, String password) 
        throws InvalidLoginException, DatabaseOperationException {
    if (ValidationUtil.isNullOrEmpty(username)) {
        throw new InvalidLoginException("Username is required.");
    }
    if (ValidationUtil.isNullOrEmpty(password)) {
        throw new InvalidLoginException("Password is required.");
    }

    Optional<User> optionalUser = userDAO.findByUsername(username.trim());
    if (optionalUser.isEmpty()) {
        // Prevent username enumeration attacks
        throw new InvalidLoginException("Invalid username or password.");
    }

    User user = optionalUser.get();
    if (!user.isActive()) {
        throw new InvalidLoginException("Account is deactivated. Please contact the administrator.");
    }

    if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
        throw new InvalidLoginException("Invalid username or password.");
    }

    return user;
}
```

### 2. Event Registration Business Constraints
```java
// EventService.java
public int registerForEvent(int eventId, int studentId)
        throws EventRegistrationException, DatabaseOperationException {
    if (eventId <= 0 || studentId <= 0) {
        throw new EventRegistrationException("Invalid event or student ID.");
    }

    Optional<Event> optionalEvent = eventDAO.findById(eventId);
    if (optionalEvent.isEmpty()) {
        throw new EventRegistrationException("Event does not exist (ID: " + eventId + ").");
    }
    Event event = optionalEvent.get();

    if (event.getRegistrationDeadline() != null && LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
        throw new EventRegistrationException("Registration closed on " + event.getRegistrationDeadline());
    }

    if (registrationDAO.isAlreadyRegistered(eventId, studentId)) {
        throw new EventRegistrationException("You are already registered for this event.");
    }

    int currentRegistrations = registrationDAO.getRegistrationCount(eventId);
    if (currentRegistrations >= event.getCapacity()) {
        throw new EventRegistrationException("Event has reached maximum capacity (" + event.getCapacity() + " seats).");
    }

    return registrationDAO.registerForEvent(registration);
}
```

### 3. DAO JDBC Wrapping with Try-With-Resources
```java
// ClubDAO.java
public Optional<Club> findById(int clubId) throws DatabaseOperationException {
    String sql = "SELECT * FROM clubs WHERE club_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, clubId);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(mapResultSetToClub(rs));
            }
        }
    } catch (SQLException e) {
        throw new DatabaseOperationException("Error finding club with id: " + clubId, e);
    }
    return Optional.empty();
}
```

### 4. GUI Error Boundary & User Notification
```java
// StudentDashboard.java
try {
    membershipService.applyForMembership(student.getUserId(), clubId);
    JOptionPane.showMessageDialog(this, "Membership request submitted!", "Application Sent", JOptionPane.INFORMATION_MESSAGE);
    loadMemberships();
} catch (DuplicateMembershipException ex) {
    JOptionPane.showMessageDialog(this, "You are already a member of this club or have a pending application.", "Membership Exists", JOptionPane.WARNING_MESSAGE);
} catch (ClubNotFoundException ex) {
    JOptionPane.showMessageDialog(this, "Club not found.", "Error", JOptionPane.ERROR_MESSAGE);
} catch (DatabaseOperationException ex) {
    JOptionPane.showMessageDialog(this, "Unable to complete the operation. Please try again.", "Database Error", JOptionPane.ERROR_MESSAGE);
} catch (IllegalArgumentException ex) {
    JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
}
```

---

## 6. Verification and Test Coverage

The [`ExceptionHandlingTest.java`](file:///c:/Users/Preeti%20gandhi/Desktop/CampusClubManagementSystem/src/test/java/com/campusclub/ExceptionHandlingTest.java) suite validates all 10 required exception scenarios with JUnit 5:
1. `InvalidLoginException` on bad credentials.
2. Rejection of blank/empty username or password.
3. `DuplicateMembershipException` on repeat join attempts.
4. `ClubNotFoundException` on invalid/missing club IDs.
5. `EventRegistrationException` on full event capacity.
6. `EventRegistrationException` on duplicate event registration.
7. `EventRegistrationException` on expired deadlines.
8. `ValidationUtil` rejecting invalid email syntax.
9. `ValidationUtil` rejecting invalid phone numbers.
10. `DatabaseOperationException` encapsulation of root SQL causes.
