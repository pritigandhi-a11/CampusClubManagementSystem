# Final System Architecture & Flow Specification

**Campus Club Management System** — *End-to-End System Architecture*

---

## 1. Multi-Tier End-to-End System Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            AUTHENTICATED ACTORS                             │
│       [🎓 Student]            [👨‍🏫 Club Coordinator]          [🛡️ Administrator]      │
└─────────────┬───────────────────────────┬───────────────────────────┬───────┘
              │                           │                           │
              ▼                           ▼                           ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER (SWING GUI)                     │
│  ┌────────────────────┐   ┌────────────────────────┐   ┌──────────────────┐  │
│  │  StudentDashboard  │   │  CoordinatorDashboard  │   │  AdminDashboard  │  │
│  │   • Browse Clubs   │   │   • Managed Club Info  │   │   • Live Metrics │  │
│  │   • My Memberships │   │   • Member Roster      │   │   • User Registry│  │
│  │   • Campus Events  │   │   • Schedule Events    │   │   • Club Control │  │
│  │   • Event Passes   │   │   • Post Announcements │   │   • All Events   │  │
│  │   • Profile Setup  │   │   • Approve Requests   │   │   • All Broadcast│  │
│  └─────────┬──────────┘   └───────────┬────────────┘   └─────────┬────────┘  │
│            │                          │                          │           │
└────────────┼──────────────────────────┼──────────────────────────┼───────────┘
             │                          │                          │
             ▼                          ▼                          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       BUSINESS LOGIC & SERVICE LAYER                        │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌────────────────────┐ │
│  │     ClubService      │  │     EventService     │  │ MembershipService  │ │
│  │  (ClubOperations)    │  │  (EventOperations)   │  │(MembershipOperat.) │ │
│  └──────────┬───────────┘  └──────────┬───────────┘  └─────────┬──────────┘ │
│             │                         │                        │            │
│  ┌──────────┴───────────┐  ┌──────────┴───────────┐            │            │
│  │    StudentService    │  │AuthenticationService │            │            │
│  └──────────────────────┘  └──────────────────────┘            │            │
└───────────────────────────────────────┬────────────────────────┼────────────┘
                                        │                        │
                                        ▼                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       DATA ACCESS OBJECT (DAO) LAYER                        │
│  ┌────────────┐ ┌───────────────┐ ┌───────────┐ ┌───────────────┐ ┌───────┐ │
│  │  UserDAO   │ │  StudentDAO   │ │  ClubDAO  │ │ MembershipDAO │ │...DAO │ │
│  └─────┬──────┘ └───────┬───────┘ └─────┬─────┘ └───────┬───────┘ └───┬───┘ │
└────────┼────────────────┼───────────────┼───────────────┼─────────────┼─────┘
         │                │               │               │             │
         ▼                ▼               ▼               ▼             ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       JAVA DATABASE CONNECTIVITY (JDBC)                     │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ DatabaseConnection (Singleton Connection Manager via db.properties)   │  │
│  │ java.sql.PreparedStatement (Parameterized Queries & SQL Anti-Injection)│  │
│  │ java.sql.ResultSet (Object-Relational Entity Hydration)                │  │
│  └───────────────────────────────────┬───────────────────────────────────┘  │
└──────────────────────────────────────┼──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      PERSISTENCE TIER (MySQL 8 DATABASE)                    │
│   Database: campus_club_db                                                  │
│   Tables: users | students | clubs | memberships | events |                 │
│           event_registrations | announcements                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Rule-Based Smart Recommendation Flow

```
┌──────────────────────────────────────────────┐
│             Student Domain Object            │
│  • Department (e.g. "CSE")                   │
│  • Interests (e.g. "Coding, AI, Web Dev")    │
│  • Already-Joined Club IDs: {1, 2}           │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────┐
│           ClubRecommendationEngine           │
│                                              │
│  1. Fetch all active clubs from ClubService  │
│  2. Exclude joined clubs via HashSet (O(1))  │
│  3. Score remaining candidate clubs:         │
│     • Category Match:       +50 pts          │
│     • Title Keyword:        +25 pts          │
│     • Description Keyword:  +15 pts          │
│     • Synonym Match:        +10 pts          │
│     • Department Alignment: +15 pts          │
│  4. Enqueue into PriorityQueue (Max-Heap)    │
│  5. Poll Top-K recommendations               │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────┐
│         Top-K Recommendation Results         │
│  1. Coding Club [Score: 90 | Match: 90%]     │
│  2. Robotics Club [Score: 65 | Match: 65%]   │
│  Rendered with human-readable rationale      │
└──────────────────────────────────────────────┘
```

---

## 3. Exception Handling & GUI Boundary Flow

```
   ┌─────────────────────────────────────────────────────────┐
   │                    User Interaction                     │
   │   (e.g., Student clicks "Register" on a full event)     │
   └────────────────────────────┬────────────────────────────┘
                                │
                                ▼
   ┌─────────────────────────────────────────────────────────┐
   │                      Service Layer                      │
   │  EventService.registerForEvent(eventId, studentId)      │
   │  Capacity Check: 50 >= 50 (Exceeded)                    │
   │  ──► Throws EventRegistrationException("Event is full") │
   └────────────────────────────┬────────────────────────────┘
                                │ (Checked Exception Thrown)
                                ▼
   ┌─────────────────────────────────────────────────────────┐
   │                  Swing GUI Catch Block                  │
   │  try {                                                  │
   │      eventService.registerForEvent(eventId, studentId); │
   │  } catch (EventRegistrationException ex) {              │
   │      JOptionPane.showMessageDialog(this,                │
   │          ex.getMessage(),                               │
   │          "Registration Error",                          │
   │          JOptionPane.WARNING_MESSAGE);                  │
   │  }                                                      │
   └────────────────────────────┬────────────────────────────┘
                                │
                                ▼
   ┌─────────────────────────────────────────────────────────┐
   │                   Friendly Dialog Box                   │
   │   ⚠️ "Event has reached maximum seat capacity."         │
   │   (No raw SQL or stack traces exposed to user)          │
   └─────────────────────────────────────────────────────────┘
```
