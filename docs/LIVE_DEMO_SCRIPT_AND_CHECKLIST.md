# LIVE DEMO SCRIPT & EXAMINER DEMONSTRATION CHECKLIST
## Campus Club Management System (CCMS)
**Step-by-Step Live Demonstration Guide for College Project Evaluation**

---

## 📋 PRE-DEMO SETUP & CREDENTIALS REFERENCE

### Verified Demo Accounts (Tested against `campus_club_db`):
| Role | Username | Password | Full Name & Assigned Scope |
|:---|:---|:---|:---|
| **Student** | `student1` | `pass123` | Alex Turner (CSE Department, 3rd Year) |
| **Student** | `student2` | `pass123` | Sophia Chen (ECE Department, 2nd Year) |
| **Coordinator** | `coord_coding` | `pass123` | David Miller (Coordinator for **Coding Club**) |
| **Coordinator** | `coord_robotics` | `pass123` | Elena Rostova (Coordinator for **Robotics Club**) |
| **Administrator** | `admin` | `admin123` | System Administrator (Full Campus Control) |

---

## 🚀 STEP 1: Start MySQL and Verify Database Connectivity

1. **Verify MySQL Windows Service**:
   Open PowerShell as Administrator and run:
   ```powershell
   Get-Service -Name MySQL*
   ```
   *Expected output: Status should display `Running` (e.g. `MySQL84`).*

2. **Configure Database Credentials**:
   Open `src/main/resources/db.properties` in your editor:
   ```properties
   db.url=jdbc:mysql://localhost:3306/campus_club_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   db.username=root
   db.password=YOUR_MYSQL_ROOT_PASSWORD
   ```
   *(Or set the environment variable in PowerShell: `$env:CAMPUS_DB_PASSWORD="YOUR_PASSWORD"`)*

3. **Verify Database Initialized**:
   If you need to re-seed or verify tables, run in MySQL CLI or Workbench:
   ```sql
   USE campus_club_db;
   SHOW TABLES;
   ```
   *Expected: 7 tables (`announcements`, `clubs`, `event_registrations`, `events`, `memberships`, `students`, `users`).*

---

## 🖥️ STEP 2: Launch the Application

### Option A: Using Maven (Standard Development Command)
Open a terminal in the project root directory and execute:
```bash
mvn exec:java
```

### Option B: Running the Standalone Executable JAR
```bash
java -jar target/campus-club-management-system-1.0.0.jar
```

*The **`LoginFrame`** window ($520 \times 520$) will appear centered on your desktop.*

---

## 🎓 STEP 3: Student Workflow Demo (Target Time: 3 Minutes)

1. **Log in as Student**:
   - In `LoginFrame`, enter:
     - Username: `student1`
     - Password: `pass123`
   - Click **"Sign In"**.
   - Notice: `LoginFrame` smoothly disposes and **`MainFrame`** loads with the badge:
     `👤 Alex Turner [Student]`.

2. **Explore KPI Metric Cards**:
   - Highlight the top 4 metric cards:
     - *My Clubs*
     - *My Events*
     - *Campus Events*
     - *Smart Matches*

3. **Demonstrate Tab 1: Discover Clubs**:
   - In the Search field, type `"Robotics"`. Click **"Filter"**.
   - The table filters to show only the Robotics Club.
   - Click **"Reset"** to restore all 6 campus clubs.
   - Select an unjoined club (e.g. *Cultural Club* or *Sports Club*) and click **"Join Selected Club"**.
   - Confirmation dialog appears confirming the membership application is submitted as `PENDING`.

4. **Demonstrate Tab 2: My Memberships**:
   - Switch to the **"My Memberships"** tab.
   - Show that active and pending memberships appear in the table with role badges.

5. **Demonstrate Tab 3: Upcoming Events & Capacity Validation**:
   - Switch to **"Upcoming Events"**.
   - Show event details: Date, Venue, Capacity, and Remaining Seats.
   - Select an event (e.g. *HackCampus 2026 Hackathon*) and click **"Register for Event"**.
   - Show the success dialog confirming registration.

6. **Demonstrate Tab 4: My Event Passes**:
   - Switch to **"My Event Passes"**.
   - The newly registered event appears in the passes table.
   - Demonstrate the cancellation action: Click **"Cancel Pass"**, confirm the dialog, and observe the pass update.

7. **Demonstrate Tab 6: Smart Club Recommendations**:
   - Switch to **"Club Recommendations"**.
   - Show the Max-Heap scored list.
   - Point out the match percentage and human-readable explanation:
     *(e.g., "Recommended because you selected Coding and AI, and your department is CSE")*.
   - Click **"Apply to Top Recommended Club"** to demonstrate one-click application.

8. **Demonstrate Tab 7: My Profile**:
   - Switch to **"My Profile"**.
   - Show academic information: USN, Department, Semester.
   - Edit the Phone Number or add a new interest keyword (e.g., `IoT, Hardware`). Click **"Save Changes"**.
   - Show that recommendations immediately recalculate to reflect the updated interests.

9. **Log Out**:
   - Click the red **"Logout"** button in the top right header.
   - Confirm the logout dialog.
   - Show that `MainFrame` cleanly closes and `LoginFrame` reopens.

---

## 👨‍🏫 STEP 4: Coordinator Workflow Demo (Target Time: 2.5 Minutes)

1. **Log in as Coordinator**:
   - Enter:
     - Username: `coord_coding`
     - Password: `pass123`
   - Click **"Sign In"**.
   - Notice: The dashboard loads with the badge:
     `👤 David Miller [Club Coordinator]` and shows the managed club: **Coding Club**.

2. **Demonstrate Tab 1: Club Details**:
   - Show club category, establishment date, and editable description area.
   - Modify the description slightly and click **"Save Description"**.
   - Show the success dialog confirming database persistence.

3. **Demonstrate Tab 2: Membership Requests**:
   - Switch to **"Membership Requests"**.
   - Filter by `PENDING`.
   - Select a pending student applicant. Click **"Approve Application"**.
   - Show the applicant's status instantly updates to `APPROVED` in the table and database.

4. **Demonstrate Tab 3: Manage Events & Attendee Rosters**:
   - Switch to **"Manage Events & Rosters"**.
   - Click **"Schedule New Event"**.
   - A modal dialog appears. Enter:
     - Event Name: `Spring Code Sprint 2026`
     - Date & Time: `2026-11-25 10:00:00`
     - Venue: `Computer Lab 4`
     - Capacity: `50`
     - Deadline: `2026-11-24 18:00:00`
   - Click **"Create Event"**.
   - The new event appears in the table immediately.
   - Select an existing event and click **"View Attendee Roster"** to show registered students with their USNs and timestamps.

5. **Demonstrate Tab 4: Broadcast Announcements**:
   - Switch to **"Broadcast Announcements"**.
   - Enter Title: `Important Hackathon Prep Meeting`
   - Select Priority: `HIGH`.
   - Type message and click **"Publish Announcement"**.
   - The announcement appears instantly in the club's broadcast history table.

6. **Log Out**:
   - Click **"Logout"** in the top header bar and confirm.

---

## 🛡️ STEP 5: Administrator Workflow Demo (Target Time: 2 Minutes)

1. **Log in as Administrator**:
   - Enter:
     - Username: `admin`
     - Password: `admin123`
   - Click **"Sign In"**.
   - Notice: Loads `AdminDashboard` with `👤 System Administrator [Administrator]`.

2. **Demonstrate Macro Analytics**:
   - Show the live KPI cards:
     - *Total Users*
     - *Campus Clubs*
     - *Active Clubs*
     - *Campus Events*

3. **Demonstrate Tab 2: User Registry**:
   - Switch to **"User Registry"**.
   - Filter dropdown by `STUDENT`, `COORDINATOR`, and `ADMIN`.
   - Use the Search box to search for `"Alex"` or `"Sharma"`.
   - **Crucial Security Check**: Point out that **no passwords or password hashes** are displayed in the table.

4. **Demonstrate Tab 1: Manage Clubs**:
   - Switch to **"Manage Clubs"**.
   - Select a club and click **"Toggle Status (Active/Inactive)"**.
   - Notice the status toggle updates in real-time.
   - Click **"Register New Club"** to show the club creation modal.

5. **Demonstrate Tab 3 & 4: Cross-Campus Events & Broadcasts**:
   - Inspect all scheduled campus events across all clubs in one centralized view.
   - View campus-wide announcements.

6. **Clean Exit**:
   - Click **"Logout"** to return to `LoginFrame`, then click **"Exit"** to close the application cleanly.

---

## ⚠️ STEP 6: Common Demo Issues & Fast Recovery Steps

| Issue Encountered | Root Cause | Instant Recovery Action |
|:---|:---|:---|
| **"MySQL Authentication Failed (Error 1045)"** | Password in `db.properties` does not match local MySQL root password. | Open `src/main/resources/db.properties`, update `db.password=YOUR_PASSWORD`, and rerun `mvn exec:java`. |
| **"MySQL Server Unreachable (Error 2003 / Communications link failure)"** | Windows service `MySQL84` is stopped. | Open PowerShell as Admin and run: `Start-Service -Name MySQL*`. |
| **"Unknown Database campus_club_db (Error 1049)"** | Schema has not been imported. | Run: `mysql -u root -p < database/schema.sql`. |
| **"Invalid username or password"** | Typo in demo credentials. | Use the verified demo accounts shown in the table at the top of this guide. |
| **Port 3306 Conflict** | Another MySQL or MariaDB instance is running. | Run `Get-NetTCPConnection -LocalPort 3306` to verify the active process. |
