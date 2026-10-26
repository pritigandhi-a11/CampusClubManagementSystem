# Campus Club Management System - Architecture & Design

## 1. Overview
The **Campus Club Management System** is a modular Java desktop application designed for colleges and universities. It enables Students to explore clubs, apply for memberships, view and register for events, read club announcements, and receive rule-based club recommendations. Club Coordinators manage club operations, approve memberships, post announcements, and create events. Administrators oversee system users, clubs, and platform-wide analytics.

## 2. Layered Architecture (MVC / Clean Layering)

```
+-------------------------------------------------------------+
|                     Presentation Layer                      |
|                  Java Swing GUI Framework                   |
|  (LoginFrame, MainFrame, StudentDashboard, Coordinators...) |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                       Service Layer                         |
|     (AuthenticationService, StudentService, ClubService,    |
|      MembershipService, EventService, RecommendationEngine) |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                          DAO Layer                          |
|  (UserDAO, StudentDAO, ClubDAO, MembershipDAO, EventDAO...) |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                   Data Access & Utilities                   |
|      (JDBC Driver, DatabaseConnection, Validation, Crypto)  |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                     Persistence Layer                       |
|                       MySQL 8 Database                      |
+-------------------------------------------------------------+
```

## 3. Object-Oriented Design Highlights
- **Encapsulation**: All entity state fields are private with accessors, mutators, and domain validations.
- **Inheritance & Polymorphism**: `User` is an abstract base class extended by `Student`, `ClubCoordinator`, and `Admin`. Each subclass encapsulates role-specific attributes and custom behavior.
- **Abstraction**: DAO and Service contracts define strict functional boundaries.
- **Composition**: `Club` references its `ClubCoordinator`, `Membership` links `Student` and `Club`, and `EventRegistration` connects `Event` with `Student`.
