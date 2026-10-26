# Screenshot Checklist for Final Project Report & PPT

**Campus Club Management System** — *Mini-Project Visual Evidence Checklist*

---

This checklist specifies the 20 screenshots required for the academic project report and presentation deck. Capture each screen running the live application.

| # | Screenshot Title | Component / View | What to Highlight in Screenshot |
|:---|:---|:---|:---|
| **1** | **Login Screen** | `LoginFrame` | Clean card design, username/password fields, demo credentials hint banner, "Sign In" button. |
| **2** | **Student Dashboard Overview** | `StudentDashboard` | Top 4 summary metric cards (*My Clubs*, *My Events*, *Campus Events*, *Smart Matches*), navigation tabs. |
| **3** | **Browse & Discover Clubs** | `StudentDashboard` (Tab 1) | Club table list, keyword search field, category dropdown filter, and "Join Club" action button. |
| **4** | **My Clubs & Memberships** | `StudentDashboard` (Tab 2) | Enrolled club memberships, status indicators (`APPROVED` / `PENDING`), and "Leave Club" button. |
| **5** | **Upcoming Campus Events** | `StudentDashboard` (Tab 3) | Schedule table showing event dates, venues, capacities, remaining seats, and registration deadlines. |
| **6** | **Event Registration Confirmation** | `StudentDashboard` (Tab 3) | Confirmation modal dialog verifying registration for a selected event. |
| **7** | **My Event Passes** | `StudentDashboard` (Tab 4) | Active event passes table with status (`REGISTERED`) and "Cancel Pass" option. |
| **8** | **Smart Club Recommendations** | `StudentDashboard` (Tab 6) | Max-Heap recommendation list showing club names, scores, match percentages, and explainable rule reasons. |
| **9** | **Campus & Club Announcements** | `StudentDashboard` (Tab 5) | Broadcast announcements table with high/medium/low priority indicators and publishing club. |
| **10** | **Student Profile & Interests** | `StudentDashboard` (Tab 7) | Profile view displaying USN, Department, Semester, editable Phone, and Interests fields with "Save" action. |
| **11** | **Coordinator Dashboard Overview** | `CoordinatorDashboard` | Top 4 coordinator KPI metric cards (*Managed Club*, *Total Members*, *Pending Requests*, *Club Events*). |
| **12** | **Membership Requests Management** | `CoordinatorDashboard` (Tab 2) | Applicant roster table, status filter (`PENDING`/`APPROVED`/`REJECTED`), and "Approve" / "Reject" buttons. |
| **13** | **Event Creation Dialog** | `CoordinatorDashboard` (Tab 3) | "Create New Event" modal form with Name, Date/Time, Venue, Capacity, and Deadline inputs. |
| **14** | **Event Attendee Roster** | `CoordinatorDashboard` (Tab 3) | "View Attendees" dialog displaying registered student names, USNs, and registration timestamps. |
| **15** | **Broadcast Announcement Dialog** | `CoordinatorDashboard` (Tab 4) | Announcement creation form with Title, Content, Priority dropdown (`HIGH`/`MEDIUM`/`LOW`), and post action. |
| **16** | **Admin Dashboard Overview** | `AdminDashboard` | Top system KPI metric cards (*Total Users*, *Campus Clubs*, *Active Clubs*, *Campus Events*). |
| **17** | **Admin User Registry & Filtering** | `AdminDashboard` (Tab 2) | User registry table with Role filter (`STUDENT`, `COORDINATOR`, `ADMIN`), search box, and zero password hash leakage. |
| **18** | **Admin Manage Clubs & Status Toggle** | `AdminDashboard` (Tab 1) | Club administration panel showing "Register Club", "Edit", "Toggle Status (`ACTIVE`/`INACTIVE`)", and "Delete Club". |
| **19** | **MySQL Database Tables & Schema** | MySQL Workbench / CLI | Table listing (`SHOW TABLES;`) verifying all 7 tables (`users`, `students`, `clubs`, `memberships`, `events`, `event_registrations`, `announcements`). |
| **20** | **Maven Build & JUnit Test Results** | Terminal / IDE Console | `mvn clean package` output showing `BUILD SUCCESS` with 91 tests executed (80 passed, 0 failures, 0 errors, 11 skipped). |

---

## Tips for Capturing Screenshots:
1. Use default 100% display scaling for crisp text rendering.
2. Ensure realistic demo data is visible (e.g. Alex Turner for student, David Miller for coordinator, System Administrator for admin).
3. Capture the full window including title bars and status banners.
