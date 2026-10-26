# ORDERED SCREENSHOT CAPTURE PLAN & VISUAL EVIDENCE GUIDE
## Campus Club Management System (CCMS)
**Step-by-Step Guide for Capturing the 20 Authentic Screenshots for Report & PPT**

---

> [!IMPORTANT]
> **Academic Integrity Notice**:
> All screenshots must be captured from the actual running Java desktop application on your machine. Do NOT use fake, fabricated, or web-mockup images. Evaluators look for genuine Java Swing title bars, Nimbus/System component styling, and data matching `campus_club_db`.

---

## 📸 Recommended Setup Before Capturing:
1. Ensure MySQL service (`MySQL84`) is running with database `campus_club_db`.
2. Configure your MySQL root password in `src/main/resources/db.properties`.
3. Launch the application:
   ```bash
   mvn exec:java
   ```
4. Set Windows display scaling to **100%** so table texts and buttons render with optimal clarity.
5. Create a folder named `docs/screenshots/` to store your captured images.
6. Use `Alt + PrintScreen` (or Windows Snipping Tool `Win + Shift + S`) to capture the active window cleanly.

---

## 📋 The 20 Required Screenshots: Step-by-Step Instructions

| # | Screenshot Title | Exact Steps to Open | Test Data & Prerequisites | Suggested Filename | Where to Place in Report & PPT |
|:---:|:---|:---|:---|:---|:---|
| **1** | **Portal Login Screen** | 1. Launch `mvn exec:java`<br>2. `LoginFrame` appears ($520 \times 520$). | Initial clean state (blank fields, quick credentials banner visible). | `01_login_screen.png` | Report: Section 9.1 & 12<br>PPT: Slide 10 & 13 |
| **2** | **Student Dashboard Overview** | 1. Enter `student1` / `pass123`<br>2. Click **"Sign In"**<br>3. Inspect top header and 4 KPI cards. | Logged in as Alex Turner (`STUDENT`). | `02_student_dashboard_overview.png` | Report: Section 9.3 & 12<br>PPT: Slide 10 & 13 |
| **3** | **Discover Clubs (Search & Filter)** | 1. On Student Dashboard, select **"Discover Clubs"** (Tab 1)<br>2. Type `"Robotics"` in search and click **"Filter"**. | Shows filtered results in `JTable`. | `03_discover_clubs_filter.png` | Report: Section 9.3<br>PPT: Slide 10 |
| **4** | **Club Join Application Confirmation** | 1. Click **"Reset"**<br>2. Select *Cultural Club*<br>3. Click **"Join Selected Club"** | Confirmation dialog appears: *"Application submitted as PENDING"*. | `04_join_club_dialog.png` | Report: Section 10.1 & 12<br>PPT: Slide 10 |
| **5** | **Student Memberships View** | 1. Click **"My Memberships"** (Tab 2)<br>2. Shows enrolled clubs with status badges. | Table populated with `APPROVED` and `PENDING` rows. | `05_student_memberships_table.png` | Report: Section 9.3<br>PPT: Slide 10 |
| **6** | **Upcoming Campus Events & Seats** | 1. Click **"Upcoming Events"** (Tab 3)<br>2. View dates, venues, capacity, and remaining seats. | Shows schedule for HackCampus, Robotics Workshop, etc. | `06_upcoming_events_table.png` | Report: Section 8.2 & 9.3<br>PPT: Slide 10 |
| **7** | **Event Registration Confirmation** | 1. Select *HackCampus 2026 Hackathon*<br>2. Click **"Register for Event"** | Modal dialog: *"Registration successful! Event pass generated."* | `07_event_registration_dialog.png` | Report: Section 6.2 & 12<br>PPT: Slide 10 |
| **8** | **My Event Passes (Pass Management)** | 1. Click **"My Event Passes"** (Tab 4)<br>2. Shows active passes with `REGISTERED` status and **"Cancel Pass"** button. | Pass generated for HackCampus event. | `08_my_event_passes_table.png` | Report: Section 9.3 & 12<br>PPT: Slide 10 |
| **9** | **Campus & Club Announcements** | 1. Click **"Announcements"** (Tab 5)<br>2. View table with priority tags (`HIGH`, `MEDIUM`, `LOW`). | Shows broadcast notices from club coordinators. | `09_announcements_table.png` | Report: Section 9.3<br>PPT: Slide 10 |
| **10** | **Smart Recommendations (Max-Heap)** | 1. Click **"Club Recommendations"** (Tab 6)<br>2. Highlight recommendation cards showing scores, match percentages, and explainable rationale. | Scored clubs from `ClubRecommendationEngine` (Max-Heap). | `10_smart_recommendations.png` | Report: Section 7.2 & 12<br>PPT: Slide 9 & 13 |
| **11** | **Student Profile & Interests Editor** | 1. Click **"My Profile"** (Tab 7)<br>2. Shows USN, Department, Semester, editable phone and interest tags. | Alex Turner profile with CSE department details. | `11_student_profile_tab.png` | Report: Section 9.3<br>PPT: Slide 10 |
| **12** | **Coordinator Dashboard Overview** | 1. Click **"Logout"**<br>2. Enter `coord_coding` / `pass123`<br>3. Inspect header with badge: `👤 David Miller [Club Coordinator]`. | Logged in as Coding Club Coordinator. | `12_coordinator_dashboard_overview.png` | Report: Section 9.3 & 12<br>PPT: Slide 11 & 13 |
| **13** | **Membership Requests Approval** | 1. Click **"Membership Requests"** (Tab 2)<br>2. Filter by `PENDING`<br>3. Select applicant and click **"Approve Application"**. | Shows instant status change from `PENDING` to `APPROVED`. | `13_membership_request_approval.png` | Report: Section 9.3 & 12<br>PPT: Slide 11 |
| **14** | **Event Creation Modal Form** | 1. Click **"Manage Events & Rosters"** (Tab 3)<br>2. Click **"Schedule New Event"**<br>3. Fill Event Name, Venue, Date, Capacity, Deadline. | Event creation modal dialog displayed. | `14_create_event_modal.png` | Report: Section 9.3 & 12<br>PPT: Slide 11 |
| **15** | **Event Attendee Roster Dialog** | 1. Select an existing event<br>2. Click **"View Attendee Roster"**<br>3. Dialog displays registered students, USNs, and signup timestamps. | Roster popup displaying registered student rows. | `15_event_attendee_roster.png` | Report: Section 9.3 & 12<br>PPT: Slide 11 |
| **16** | **Broadcast Announcement Dialog** | 1. Click **"Broadcast Announcements"** (Tab 4)<br>2. Compose title, select `HIGH` priority, enter notice message. | Form with Title, Priority dropdown, Message area. | `16_broadcast_announcement_form.png` | Report: Section 9.3 & 12<br>PPT: Slide 11 |
| **17** | **Admin Dashboard Overview (Live KPIs)** | 1. Click **"Logout"**<br>2. Enter `admin` / `admin123`<br>3. Shows 4 live system KPI cards: Total Users, Campus Clubs, Active Clubs, Campus Events. | Logged in as System Administrator. | `17_admin_dashboard_kpis.png` | Report: Section 9.3 & 12<br>PPT: Slide 11 & 13 |
| **18** | **Admin User Registry & Role Filtering** | 1. Click **"User Registry"** (Tab 2)<br>2. Filter by `STUDENT`, `COORDINATOR`, and `ADMIN`<br>3. Note that password hashes are NOT exposed. | Clean user directory without password leakage. | `18_admin_user_registry_table.png` | Report: Section 9.3 & 10.2<br>PPT: Slide 11 & 12 |
| **19** | **Admin Manage Clubs & Status Toggle** | 1. Click **"Manage Clubs"** (Tab 1)<br>2. Select club, click **"Toggle Status (Active/Inactive)"**<br>3. Notice real-time status update in table. | Club status toggle in action. | `19_admin_manage_clubs.png` | Report: Section 9.3 & 12<br>PPT: Slide 11 |
| **20** | **Build & Database Evidence** | 1. Terminal split view showing `mvn clean package` `BUILD SUCCESS`<br>2. MySQL CLI showing `SHOW TABLES;` with all 7 tables. | Build success output & MySQL 7 core tables. | `20_build_and_mysql_evidence.png` | Report: Section 8.1, 11 & 12<br>PPT: Slide 7 & 13 |

---

## 📂 Summary of Image Placements in Project Deliverables:
- **In `docs/FINAL_PROJECT_REPORT.md`**: Section 12 contains labeled markdown image tags `![Screenshot Title](screenshots/filename.png)`.
- **In `Campus_Club_Management_System_Presentation.pptx`**: Insert screenshots onto Slides 7, 9, 10, 11, and 13.
