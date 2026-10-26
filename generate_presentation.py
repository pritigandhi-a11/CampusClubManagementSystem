import sys
import pptx
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
from pptx.enum.shapes import MSO_SHAPE

def create_deck(output_path):
    prs = pptx.Presentation()
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)
    blank_layout = prs.slide_layouts[6]

    # Colors matching UITheme.java
    COLOR_NAVY = RGBColor(25, 42, 86)       # #192A56
    COLOR_BLUE = RGBColor(37, 99, 235)      # #2563EB
    COLOR_EMERALD = RGBColor(16, 185, 129)  # #10B981
    COLOR_DARK = RGBColor(15, 23, 42)       # #0F172A
    COLOR_MUTED = RGBColor(100, 116, 139)   # #64748B
    COLOR_WHITE = RGBColor(255, 255, 255)
    COLOR_BG_CARD = RGBColor(248, 250, 252)
    COLOR_BORDER = RGBColor(226, 232, 240)

    def add_header(slide, title_text, category_text="CAMPUS CLUB MANAGEMENT SYSTEM"):
        # Header background bar
        hdr_rect = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(1.15))
        hdr_rect.fill.solid()
        hdr_rect.fill.fore_color.rgb = COLOR_NAVY
        hdr_rect.line.color.rgb = COLOR_NAVY

        # Subtitle category
        sub_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.12), Inches(11.7), Inches(0.3))
        tf_sub = sub_box.text_frame
        tf_sub.word_wrap = True
        p_sub = tf_sub.paragraphs[0]
        p_sub.text = category_text.upper()
        p_sub.font.size = Pt(11)
        p_sub.font.bold = True
        p_sub.font.color.rgb = COLOR_EMERALD

        # Title
        t_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.35), Inches(11.7), Inches(0.65))
        tf_t = t_box.text_frame
        tf_t.word_wrap = True
        p_t = tf_t.paragraphs[0]
        p_t.text = title_text
        p_t.font.size = Pt(22)
        p_t.font.bold = True
        p_t.font.color.rgb = COLOR_WHITE

        # Bottom accent stripe
        stripe = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(1.15), Inches(13.333), Inches(0.06))
        stripe.fill.solid()
        stripe.fill.fore_color.rgb = COLOR_BLUE
        stripe.line.color.rgb = COLOR_BLUE

    def add_card(slide, left, top, width, height, title="", fill_color=COLOR_WHITE, border_color=COLOR_BORDER):
        card = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(left), Inches(top), Inches(width), Inches(height))
        card.fill.solid()
        card.fill.fore_color.rgb = fill_color
        card.line.color.rgb = border_color
        card.line.width = Pt(1.5)

        if title:
            t_box = slide.shapes.add_textbox(Inches(left + 0.2), Inches(top + 0.15), Inches(width - 0.4), Inches(0.45))
            tf = t_box.text_frame
            tf.word_wrap = True
            p = tf.paragraphs[0]
            p.text = title
            p.font.size = Pt(15)
            p.font.bold = True
            p.font.color.rgb = COLOR_BLUE
        return card

    def set_notes(slide, notes_text):
        notes_slide = slide.notes_slide
        tf = notes_slide.notes_text_frame
        tf.text = notes_text

    # ==========================================
    # SLIDE 1: Title Slide
    # ==========================================
    s1 = prs.slides.add_slide(blank_layout)
    bg1 = s1.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(7.5))
    bg1.fill.solid()
    bg1.fill.fore_color.rgb = COLOR_NAVY
    bg1.line.color.rgb = COLOR_NAVY

    # Decorative banner
    deco = s1.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(0.8), Inches(11.733), Inches(5.9))
    deco.fill.solid()
    deco.fill.fore_color.rgb = RGBColor(15, 23, 42)
    deco.line.color.rgb = COLOR_BLUE
    deco.line.width = Pt(2)

    # Title text
    t_box = s1.shapes.add_textbox(Inches(1.2), Inches(1.1), Inches(11.0), Inches(1.8))
    tf1 = t_box.text_frame
    tf1.word_wrap = True
    p1 = tf1.paragraphs[0]
    p1.text = "Campus Club Management System"
    p1.font.size = Pt(36)
    p1.font.bold = True
    p1.font.color.rgb = COLOR_WHITE

    p2 = tf1.add_paragraph()
    p2.text = "A Desktop Extracurricular Activity & Club Administration Platform"
    p2.font.size = Pt(18)
    p2.font.color.rgb = COLOR_EMERALD

    # Tech badge
    p3 = tf1.add_paragraph()
    p3.text = "Java 17 LTS  |  Java Swing  |  JDBC  |  MySQL 8.4  |  Apache Maven 3.10"
    p3.font.size = Pt(13)
    p3.font.color.rgb = RGBColor(203, 213, 225)

    # Left Card: Student details placeholder
    c_stud = s1.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(1.3), Inches(3.4), Inches(5.0), Inches(2.7))
    c_stud.fill.solid()
    c_stud.fill.fore_color.rgb = RGBColor(30, 41, 59)
    c_stud.line.color.rgb = COLOR_BLUE
    tf_c1 = c_stud.text_frame
    tf_c1.word_wrap = True
    p_c1_t = tf_c1.paragraphs[0]
    p_c1_t.text = "STUDENT / TEAM DETAILS"
    p_c1_t.font.bold = True
    p_c1_t.font.size = Pt(14)
    p_c1_t.font.color.rgb = COLOR_EMERALD

    p_c1_1 = tf_c1.add_paragraph()
    p_c1_1.text = "\nStudent Name: [Your Full Name]"
    p_c1_1.font.size = Pt(13)
    p_c1_1.font.color.rgb = COLOR_WHITE

    p_c1_2 = tf_c1.add_paragraph()
    p_c1_2.text = "Register / USN No: [Your USN / Roll No]"
    p_c1_2.font.size = Pt(13)
    p_c1_2.font.color.rgb = COLOR_WHITE

    p_c1_3 = tf_c1.add_paragraph()
    p_c1_3.text = "Degree: B.E. / B.Tech Computer Science & Engg"
    p_c1_3.font.size = Pt(12)
    p_c1_3.font.color.rgb = RGBColor(203, 213, 225)

    # Right Card: Institutional details placeholder
    c_inst = s1.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(3.4), Inches(5.2), Inches(2.7))
    c_inst.fill.solid()
    c_inst.fill.fore_color.rgb = RGBColor(30, 41, 59)
    c_inst.line.color.rgb = COLOR_BLUE
    tf_c2 = c_inst.text_frame
    tf_c2.word_wrap = True
    p_c2_t = tf_c2.paragraphs[0]
    p_c2_t.text = "ACADEMIC & GUIDE DETAILS"
    p_c2_t.font.bold = True
    p_c2_t.font.size = Pt(14)
    p_c2_t.font.color.rgb = COLOR_EMERALD

    p_c2_1 = tf_c2.add_paragraph()
    p_c2_1.text = "\nProject Supervisor: [Guide Name & Designation]"
    p_c2_1.font.size = Pt(13)
    p_c2_1.font.color.rgb = COLOR_WHITE

    p_c2_2 = tf_c2.add_paragraph()
    p_c2_2.text = "Department: Computer Science and Engineering"
    p_c2_2.font.size = Pt(13)
    p_c2_2.font.color.rgb = COLOR_WHITE

    p_c2_3 = tf_c2.add_paragraph()
    p_c2_3.text = "Institution: [Your College / University Name]"
    p_c2_3.font.size = Pt(12)
    p_c2_3.font.color.rgb = RGBColor(203, 213, 225)

    set_notes(s1, "Good morning respected evaluators and committee members. Today I am presenting our college mini-project: The Campus Club Management System. This is a desktop application developed using Java 17, Java Swing, and MySQL 8 via JDBC, designed to centralize and automate student club activities across our campus.")

    # ==========================================
    # SLIDE 2: Introduction & Problem Statement
    # ==========================================
    s2 = prs.slides.add_slide(blank_layout)
    add_header(s2, "Introduction & Problem Statement", "BACKGROUND & CONTEXT")

    # Card 1: Background
    add_card(s2, 0.8, 1.5, 5.6, 5.4, "Background & Importance")
    tb = s2.shapes.add_textbox(Inches(1.0), Inches(2.1), Inches(5.2), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.paragraphs[0].text = "• Student clubs form the center of experiential learning, technical coding, and campus leadership."
    tf.paragraphs[0].font.size = Pt(13)
    tf.paragraphs[0].font.color.rgb = COLOR_DARK

    p = tf.add_paragraph()
    p.text = "\n• Active clubs on campus include technical coding clubs, robotics labs, sports teams, cultural societies, and incubation hubs."
    p.font.size = Pt(13)
    p.font.color.rgb = COLOR_DARK

    p = tf.add_paragraph()
    p.text = "\n• Currently, these organizations operate independently without centralized digital tooling, leading to significant administrative overhead."
    p.font.size = Pt(13)
    p.font.color.rgb = COLOR_DARK

    # Card 2: Problem Statement
    add_card(s2, 6.8, 1.5, 5.7, 5.4, "Key Problems in College Campus Today")
    tb = s2.shapes.add_textbox(Inches(7.0), Inches(2.1), Inches(5.3), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.paragraphs[0].text = "1. Information Fragmentation:\nEvent posters and club announcements are lost in unorganized chat groups, causing missed deadlines."
    tf.paragraphs[0].font.size = Pt(13)
    tf.paragraphs[0].font.color.rgb = COLOR_DARK

    p = tf.add_paragraph()
    p.text = "\n2. Spreadsheet Inaccuracy:\nManual Excel/Google Sheets lead to duplicate memberships, accidental record loss, and conflicting schedules."
    p.font.size = Pt(13)
    p.font.color.rgb = COLOR_DARK

    p = tf.add_paragraph()
    p.text = "\n3. Uncontrolled Venue Overcrowding:\nManual forms fail to enforce seat capacity limits, resulting in seminar halls overflowing on event day."
    p.font.size = Pt(13)
    p.font.color.rgb = COLOR_DARK

    p = tf.add_paragraph()
    p.text = "\n4. No Interest-Driven Discovery:\nNew students lack guidance on which clubs align with their academic branch and skill interests."
    p.font.size = Pt(13)
    p.font.color.rgb = COLOR_DARK

    set_notes(s2, "In colleges today, club operations are scattered across informal chat groups and unlinked spreadsheets. This creates major problems: event halls get overcrowded because forms lack automated capacity limits, students miss deadlines, and coordinators spend countless hours manually tracking records. Our project addresses these exact challenges.")

    # ==========================================
    # SLIDE 3: Existing System vs. Proposed System
    # ==========================================
    s3 = prs.slides.add_slide(blank_layout)
    add_header(s3, "Existing System vs. Proposed Proposed System", "SYSTEM COMPARISON")

    # Table comparing the two
    rows, cols = 6, 3
    left, top, width, height = Inches(0.8), Inches(1.5), Inches(11.733), Inches(5.2)
    table_shape = s3.shapes.add_table(rows, cols, left, top, width, height)
    table = table_shape.table
    table.columns[0].width = Inches(2.8)
    table.columns[1].width = Inches(4.4)
    table.columns[2].width = Inches(4.533)

    headers = ["Evaluation Metric", "Existing System (Manual / Sheets)", "Proposed CCMS (Java Swing & MySQL)"]
    for c_idx, h in enumerate(headers):
        cell = table.cell(0, c_idx)
        cell.text = h
        cell.fill.solid()
        cell.fill.fore_color.rgb = COLOR_NAVY
        for p in cell.text_frame.paragraphs:
            p.font.bold = True
            p.font.size = Pt(14)
            p.font.color.rgb = COLOR_WHITE
            p.alignment = PP_ALIGN.CENTER

    data = [
        ("Architecture", "Disjointed Google Forms, Sheets, WhatsApp", "Unified 4-Tier Desktop MVC Architecture"),
        ("Data Consistency", "High risk of duplicate rows and data loss", "ACID Compliant MySQL with Foreign Keys & Constraints"),
        ("Access Control", "Shared public links with no security boundaries", "Strict Role-Based Security (Student / Coord / Admin)"),
        ("Capacity Management", "Manual tallying; frequent hall overfills", "Real-time automated seat limit checks & waitlists"),
        ("Club Discovery", "Word of mouth and physical bulletin boards", "Rule-Based Smart Recommendation Engine (Max-Heap)")
    ]

    for r_idx, row in enumerate(data):
        for c_idx, val in enumerate(row):
            cell = table.cell(r_idx + 1, c_idx)
            cell.text = val
            cell.fill.solid()
            cell.fill.fore_color.rgb = COLOR_WHITE if r_idx % 2 == 0 else COLOR_BG_CARD
            for p in cell.text_frame.paragraphs:
                p.font.size = Pt(12)
                p.font.color.rgb = COLOR_DARK
                if c_idx == 0:
                    p.font.bold = True

    set_notes(s3, "Here is a direct comparison between the traditional spreadsheet approach and our Campus Club Management System. Unlike spreadsheets that allow duplicate entries and lack access controls, our system enforces relational integrity, role-based boundaries, and automated seat limits at both the application and database tiers.")

    # ==========================================
    # SLIDE 4: Objectives & System Requirements
    # ==========================================
    s4 = prs.slides.add_slide(blank_layout)
    add_header(s4, "Project Objectives & Requirements", "GOALS & ENVIRONMENT")

    add_card(s4, 0.8, 1.5, 5.6, 5.4, "Core Project Objectives")
    tb = s4.shapes.add_textbox(Inches(1.0), Inches(2.1), Inches(5.2), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.paragraphs[0].text = "1. Centralized College Hub:\nProvide dedicated desktop portals for Students, Club Coordinators, and System Administrators."
    tf.paragraphs[0].font.size = Pt(12)
    tf.paragraphs[0].font.color.rgb = COLOR_DARK

    p = tf.add_paragraph()
    p.text = "\n2. Automated Operational Workflows:\nEliminate paper applications; handle member approvals, event registrations, and announcements digitally."
    p.font.size = Pt(12)
    p.font.color.rgb = COLOR_DARK

    p = tf.add_paragraph()
    p.text = "\n3. Business Rule Integrity:\nPrevent duplicate club memberships and enforce event seat caps through dual Java and database constraints."
    p.font.size = Pt(12)
    p.font.color.rgb = COLOR_DARK

    p = tf.add_paragraph()
    p.text = "\n4. Intelligent Client-Side Matching:\nRecommend clubs matching a student's department and personal interests using a local Max-Heap algorithm."
    p.font.size = Pt(12)
    p.font.color.rgb = COLOR_DARK

    add_card(s4, 6.8, 1.5, 5.7, 5.4, "Hardware & Software Requirements")
    tb = s4.shapes.add_textbox(Inches(7.0), Inches(2.1), Inches(5.3), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.paragraphs[0].text = "Hardware Requirements:"
    tf.paragraphs[0].font.bold = True
    tf.paragraphs[0].font.size = Pt(13)
    tf.paragraphs[0].font.color.rgb = COLOR_BLUE

    p = tf.add_paragraph()
    p.text = "• CPU: Dual-Core 2.0 GHz or higher (Intel Core i3 / AMD Ryzen 3)\n• RAM: 4 GB minimum (8 GB recommended)\n• Disk: 500 MB free space\n• Screen: 1280x720 minimum (1920x1080 recommended)"
    p.font.size = Pt(12)
    p.font.color.rgb = COLOR_DARK

    p = tf.add_paragraph()
    p.text = "\nSoftware Environment:"
    p.font.bold = True
    p.font.size = Pt(13)
    p.font.color.rgb = COLOR_BLUE

    p = tf.add_paragraph()
    p.text = "• OS: Windows 10/11 or Linux Ubuntu 20.04+\n• JDK: Java SE 17 LTS (Compiled with release target 17)\n• Database: MySQL 8.4 Community Server\n• JDBC: mysql-connector-j 8.3.0 / 8.4.0\n• Build Tool: Apache Maven 3.10.0\n• Testing: JUnit 5 (Jupiter 5.10.2)"
    p.font.size = Pt(12)
    p.font.color.rgb = COLOR_DARK

    set_notes(s4, "Our objectives center around providing three role-based portals, automated seat capacity enforcement, and rule-based club recommendations. The hardware and software requirements are lightweight: any standard college PC with Java 17 and MySQL 8 can run the system smoothly without requiring internet connectivity.")

    # ==========================================
    # SLIDE 5: Technology Stack & Choice Rationale
    # ==========================================
    s5 = prs.slides.add_slide(blank_layout)
    add_header(s5, "Technology Stack & Engineering Rationale", "TOOLS & DECISIONS")

    tech_cards = [
        ("Java 17 LTS", "Platform Core", "Modern LTS version providing sealed classes, pattern matching, records, and strong performance without experimental features.", 0.8, 1.5, 3.6, 2.5),
        ("Java Swing", "Desktop GUI", "Pure desktop UI with zero web dependencies. Uses Event Dispatch Thread (EDT) and UITheme design system for high responsiveness.", 4.8, 1.5, 3.6, 2.5),
        ("MySQL 8.4", "Relational Storage", "ACID-compliant relational database. Enforces Foreign Key cascades and composite UNIQUE constraints for bulletproof integrity.", 8.8, 1.5, 3.7, 2.5),
        ("Native JDBC", "Persistence Layer", "Direct control over PreparedStatement execution, transaction commits/rollbacks, and eliminates heavy ORM startup overhead.", 0.8, 4.3, 3.6, 2.6),
        ("Apache Maven", "Build & Lifecycle", "Standardized dependency management and reproducible multi-phase build (clean, compile, test, package) generating a clean JAR.", 4.8, 4.3, 3.6, 2.6),
        ("JUnit 5", "Testing Framework", "Complete test suite of 91 automated tests verifying OOP models, exception boundaries, recommendations, and GUI integration.", 8.8, 4.3, 3.7, 2.6)
    ]

    for title, role, desc, l, t, w, h in tech_cards:
        add_card(s5, l, t, w, h, title)
        tb = s5.shapes.add_textbox(Inches(l + 0.2), Inches(t + 0.65), Inches(w - 0.4), Inches(h - 0.8))
        tf = tb.text_frame
        tf.word_wrap = True
        p_sub = tf.paragraphs[0]
        p_sub.text = role.upper()
        p_sub.font.bold = True
        p_sub.font.size = Pt(10)
        p_sub.font.color.rgb = COLOR_EMERALD

        p_desc = tf.add_paragraph()
        p_desc.text = desc
        p_desc.font.size = Pt(11)
        p_desc.font.color.rgb = COLOR_DARK

    set_notes(s5, "Every technology in our stack was chosen deliberately. We selected Java 17 for long-term stability, Swing for an isolated desktop experience that doesn't depend on web servers or internet access, native JDBC for explicit control over SQL execution, and MySQL 8 for robust relational constraints.")

    # ==========================================
    # SLIDE 6: System Architecture & Layered Flow
    # ==========================================
    s6 = prs.slides.add_slide(blank_layout)
    add_header(s6, "4-Tier Layered System Architecture", "SOFTWARE ARCHITECTURE")

    tiers = [
        ("1. Presentation Tier (Swing GUI)", "LoginFrame, MainFrame (Polymorphic Router), StudentDashboard (7 tabs), CoordinatorDashboard (4 tabs), AdminDashboard (4 tabs), DashboardCard, UITheme", 0.8, 1.5, 11.733, 1.15, COLOR_NAVY, COLOR_WHITE),
        ("2. Business Logic & Service Tier", "AuthenticationService, ClubService (ClubOperations), EventService (EventOperations), MembershipService (MembershipOperations), StudentService, ClubRecommendationEngine, ValidationUtil", 0.8, 2.85, 11.733, 1.15, COLOR_BLUE, COLOR_WHITE),
        ("3. Data Access Object (DAO) Tier", "UserDAO, StudentDAO, ClubDAO, EventDAO, MembershipDAO, EventRegistrationDAO, AnnouncementDAO (Parameterized PreparedStatements with try-with-resources)", 0.8, 4.2, 11.733, 1.15, RGBColor(30, 41, 59), COLOR_WHITE),
        ("4. Persistence Tier (MySQL 8 Database)", "campus_club_db Schema: users (16), students (10), clubs (6), memberships (9), events (8), event_registrations (8), announcements (4)", 0.8, 5.55, 11.733, 1.15, RGBColor(15, 23, 42), COLOR_WHITE)
    ]

    for title, desc, l, t, w, h, bg_c, fg_c in tiers:
        card = s6.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(l), Inches(t), Inches(w), Inches(h))
        card.fill.solid()
        card.fill.fore_color.rgb = bg_c
        card.line.color.rgb = COLOR_BORDER
        tb = s6.shapes.add_textbox(Inches(l + 0.3), Inches(t + 0.1), Inches(w - 0.6), Inches(h - 0.2))
        tf = tb.text_frame
        tf.word_wrap = True
        p1 = tf.paragraphs[0]
        p1.text = title
        p1.font.bold = True
        p1.font.size = Pt(13)
        p1.font.color.rgb = COLOR_EMERALD

        p2 = tf.add_paragraph()
        p2.text = desc
        p2.font.size = Pt(11)
        p2.font.color.rgb = fg_c

    set_notes(s6, "This slide illustrates our 4-tier layered architecture. Requests flow downwards from Swing GUI to Service, then to DAO, and finally through JDBC to MySQL. Crucially, presentation classes contain zero SQL code. The Service layer isolates all business validations, and the DAO layer abstracts away all database queries.")

    # ==========================================
    # SLIDE 7: Database Design & Relational Schema
    # ==========================================
    s7 = prs.slides.add_slide(blank_layout)
    add_header(s7, "Database Design & Relational Schema", "PERSISTENCE TIER")

    add_card(s7, 0.8, 1.5, 5.6, 5.4, "The 7 Core Relational Tables")
    tb = s7.shapes.add_textbox(Inches(1.0), Inches(2.1), Inches(5.2), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.paragraphs[0].text = "1. users: Base credentials, full name, email, password_hash, role (STUDENT, COORDINATOR, ADMIN)."
    tf.paragraphs[0].font.size = Pt(11)
    tf.paragraphs[0].font.color.rgb = COLOR_DARK

    tables_info = [
        "2. students: USN, branch, year, semester, CGPA, interest keywords (FK -> users.user_id).",
        "3. clubs: Name, category, description, status, coordinator (FK -> users.user_id).",
        "4. memberships: Student enrollments with composite UNIQUE(student_id, club_id).",
        "5. events: Club events, venue, schedule, seat capacity, deadline (FK -> clubs.club_id).",
        "6. event_registrations: Attendee passes with composite UNIQUE(event_id, student_id).",
        "7. announcements: Club broadcasts with priority ENUM (LOW, MEDIUM, HIGH)."
    ]
    for item in tables_info:
        p = tf.add_paragraph()
        p.text = item
        p.font.size = Pt(11)
        p.font.color.rgb = COLOR_DARK

    add_card(s7, 6.8, 1.5, 5.7, 5.4, "Relational Constraints & Integrity")
    tb = s7.shapes.add_textbox(Inches(7.0), Inches(2.1), Inches(5.3), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.paragraphs[0].text = "Relational Integrity Rules:"
    tf.paragraphs[0].font.bold = True
    tf.paragraphs[0].font.size = Pt(13)
    tf.paragraphs[0].font.color.rgb = COLOR_BLUE

    rules = [
        "• Foreign Key Cascades: Deleting a user safely cascades to delete linked student records; deleting a club cascades to its events and memberships.",
        "• Anti-Duplicate Membership Constraint:\nCONSTRAINT uq_student_club UNIQUE (student_id, club_id)\nGuarantees at database level that no student can join the same club twice.",
        "• Anti-Duplicate Event Registration:\nCONSTRAINT uq_event_student UNIQUE (event_id, student_id)\nGuarantees that no student can double-book an event pass.",
        "• Parameterized PreparedStatements: 100% of SQL statements bind values via placeholders (?), preventing SQL injection."
    ]
    for r in rules:
        p = tf.add_paragraph()
        p.text = r
        p.font.size = Pt(11)
        p.font.color.rgb = COLOR_DARK

    set_notes(s7, "Our database schema consists of seven tables in 3rd Normal Form. In addition to foreign key constraints with ON DELETE CASCADE, we enforce business rules like anti-duplicate membership and anti-duplicate event booking directly at the schema level using composite UNIQUE constraints.")

    # ==========================================
    # SLIDE 8: Object-Oriented Programming (OOP)
    # ==========================================
    s8 = prs.slides.add_slide(blank_layout)
    add_header(s8, "Object-Oriented Programming Implementation", "OOP PRINCIPLES")

    oop_items = [
        ("Encapsulation", "Data Hiding & Protection", "All entity fields (User, Student, Club, Event) are private. Mutators validate boundaries (year: 1-5, positive capacity). Defensive copying with Collections.unmodifiableList() prevents external state corruption.", 0.8, 1.5, 5.6, 2.5),
        ("Inheritance", "Code Reuse & Specialization", "Abstract base class User defines shared fields (userId, username, email, role). Subclasses Student, ClubCoordinator, and Admin extend User with role-specific properties. Swing frames extend JFrame/JPanel.", 6.8, 1.5, 5.7, 2.5),
        ("Abstraction", "Contract Decoupling", "User declares abstract methods (getRoleDisplayName(), getDashboardDescription()). High-level interfaces (ClubOperations, EventOperations, MembershipOperations) decouple GUI from DAO implementations.", 0.8, 4.3, 5.6, 2.6),
        ("Polymorphism & Composition", "Dynamic Dispatch & Has-A", "Runtime Polymorphism: MainFrame dynamically mounts role dashboards based on User subtype. Composition: Club composes List<Event> and List<Membership>; Event composes attendee registrations.", 6.8, 4.3, 5.7, 2.6)
    ]

    for title, sub, desc, l, t, w, h in oop_items:
        add_card(s8, l, t, w, h, title)
        tb = s8.shapes.add_textbox(Inches(l + 0.2), Inches(t + 0.65), Inches(w - 0.4), Inches(h - 0.8))
        tf = tb.text_frame
        tf.word_wrap = True
        p_sub = tf.paragraphs[0]
        p_sub.text = sub.upper()
        p_sub.font.bold = True
        p_sub.font.size = Pt(11)
        p_sub.font.color.rgb = COLOR_EMERALD

        p_desc = tf.add_paragraph()
        p_desc.text = desc
        p_desc.font.size = Pt(11)
        p_desc.font.color.rgb = COLOR_DARK

    set_notes(s8, "Our codebase serves as a thorough demonstration of OOP principles. Encapsulation protects private fields with bounds checking; Inheritance establishes an abstract User hierarchy; Abstraction defines clean service contracts; Polymorphism dynamically dispatches dashboard loading; and Composition models rich 'Has-A' relationships.")

    # ==========================================
    # SLIDE 9: Exception Handling Architecture
    # ==========================================
    s9 = prs.slides.add_slide(blank_layout)
    add_header(s9, "Exception Handling & Boundary Architecture", "RELIABILITY & RECOVERY")

    add_card(s9, 0.8, 1.5, 5.6, 5.4, "Custom User-Defined Exceptions")
    tb = s9.shapes.add_textbox(Inches(1.0), Inches(2.1), Inches(5.2), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.paragraphs[0].text = "Checked Exceptions (Domain Business Rules):"
    tf.paragraphs[0].font.bold = True
    tf.paragraphs[0].font.size = Pt(12)
    tf.paragraphs[0].font.color.rgb = COLOR_BLUE

    p = tf.add_paragraph()
    p.text = "• InvalidLoginException:\nThrown on credential mismatch, deactivated account, or empty inputs. Masks failure details to prevent username enumeration.\n• DuplicateMembershipException:\nEnforces that a student cannot join or apply for the same club twice.\n• ClubNotFoundException:\nThrown when an operation targets a non-existent club identifier.\n• EventRegistrationException:\nEnforces capacity exhaustion, expired registration deadlines, and duplicate signups."
    p.font.size = Pt(11)
    p.font.color.rgb = COLOR_DARK

    p = tf.add_paragraph()
    p.text = "\nUnchecked Exceptions (Infrastructure):"
    p.font.bold = True
    p.font.size = Pt(12)
    p.font.color.rgb = COLOR_BLUE

    p = tf.add_paragraph()
    p.text = "• DatabaseOperationException (RuntimeException):\nWraps JDBC SQLException, providing safe error diagnostics (Error 1045, 1049, 2003) without leaking raw passwords or schema details."
    p.font.size = Pt(11)
    p.font.color.rgb = COLOR_DARK

    add_card(s9, 6.8, 1.5, 5.7, 5.4, "Multi-Tier Error Flow")
    tb = s9.shapes.add_textbox(Inches(7.0), Inches(2.1), Inches(5.3), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.paragraphs[0].text = "Layer-by-Layer Exception Propagation:"
    tf.paragraphs[0].font.bold = True
    tf.paragraphs[0].font.size = Pt(13)
    tf.paragraphs[0].font.color.rgb = COLOR_BLUE

    layers = [
        "1. Presentation Tier (Swing GUI):\nCatches domain exceptions and renders clear JOptionPane dialogs. Never prints raw stack traces to normal end-users.",
        "2. Service Tier:\nValidates inputs with ValidationUtil and enforces domain constraints. Throws checked exceptions on business rule violations.",
        "3. DAO Tier:\nExecutes PreparedStatements inside try-with-resources. Catches SQLException and wraps it in DatabaseOperationException.",
        "4. Fault Isolation Guarantee:\nA database disconnection or constraint violation is cleanly contained without crashing the desktop application."
    ]
    for l_text in layers:
        p = tf.add_paragraph()
        p.text = l_text
        p.font.size = Pt(11)
        p.font.color.rgb = COLOR_DARK

    set_notes(s9, "We separated checked exceptions for expected business rule violations from unchecked exceptions for infrastructure faults. When a student tries to register for a full event, an EventRegistrationException is thrown by the service layer, caught by the GUI, and displayed as a clean warning dialog rather than crashing the JVM.")

    # ==========================================
    # SLIDE 10: Collections & Recommendation Engine
    # ==========================================
    s10 = prs.slides.add_slide(blank_layout)
    add_header(s10, "Collections & Smart Recommendation Algorithm", "ALGORITHMS & DATA STRUCTURES")

    add_card(s10, 0.8, 1.5, 5.6, 5.4, "Java Collections Framework Used")
    tb = s10.shapes.add_textbox(Inches(1.0), Inches(2.1), Inches(5.2), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.paragraphs[0].text = "• ArrayList<T>:\nDynamic indexed sequences for table models, search results, and attendee rosters."
    tf.paragraphs[0].font.size = Pt(11)
    tf.paragraphs[0].font.color.rgb = COLOR_DARK

    collections = [
        "• HashSet<T> & LinkedHashSet<T>:\nO(1) fast lookup to exclude clubs the student has already joined, and deduplicate interest keyword tokens.",
        "• HashMap<K, V> & LinkedHashMap<K, V>:\nCategory taxonomy maps, synonym dictionaries, and aggregated dashboard statistics (e.g. member count per club).",
        "• PriorityQueue<T> (Max-Heap):\nExtracts top-K recommendation results ranked descending by recommendation score.",
        "• Comparator<T>:\nMulti-attribute sorting for clubs (by name/category) and events (by date)."
    ]
    for c in collections:
        p = tf.add_paragraph()
        p.text = c
        p.font.size = Pt(11)
        p.font.color.rgb = COLOR_DARK

    add_card(s10, 6.8, 1.5, 5.7, 5.4, "Rule-Based Recommendation Scoring")
    tb = s10.shapes.add_textbox(Inches(7.0), Inches(2.1), Inches(5.3), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.paragraphs[0].text = "Scoring Weight Hierarchy:"
    tf.paragraphs[0].font.bold = True
    tf.paragraphs[0].font.size = Pt(13)
    tf.paragraphs[0].font.color.rgb = COLOR_BLUE

    rules = [
        "1. Category Exact Match: +50 Points\n(e.g., student selected 'Coding' and club category is 'Technical')",
        "2. Title Keyword Match: +25 Points\n(e.g., club title contains 'Robotics' or 'Coding')",
        "3. Description Keyword Match: +15 Points\n(e.g., description mentions 'AI' or 'IoT')",
        "4. Domain Synonym Match: +10 Points\n(e.g., 'Programming' <-> 'Web Development')",
        "5. Department Alignment Bonus: +15 Points\n(e.g., CSE student <-> Technical club)",
        "Explainable Output:\nEvery recommendation provides a match percentage and human-readable explanation (e.g., 'Recommended because you selected AI and Coding')."
    ]
    for r in rules:
        p = tf.add_paragraph()
        p.text = r
        p.font.size = Pt(11)
        p.font.color.rgb = COLOR_DARK

    set_notes(s10, "Our Smart Recommendation Engine runs locally on the JVM without external AI APIs, preserving student privacy. It filters out clubs the student has already joined using an O(1) HashSet, scores candidate clubs using five transparent rules, and extracts the top recommendations using a Max-Heap PriorityQueue.")

    # ==========================================
    # SLIDE 11: Main User Interface Modules
    # ==========================================
    s11 = prs.slides.add_slide(blank_layout)
    add_header(s11, "Core Graphical User Interface Modules", "SWING PRESENTATION LAYER")

    dashboards = [
        ("Student Portal (7 Tabs)", "Discover Clubs (Keyword search + category filter + 1-click join)\nMy Memberships (Status tracking: APPROVED / PENDING + Leave club)\nUpcoming Events (Schedules, venue, capacity, remaining seats)\nMy Event Passes (Active passes + 1-click cancellation)\nAnnouncements (Broadcast table with HIGH/MED/LOW priority tags)\nSmart Recommendations (Max-Heap suggestions with match %)\nMy Profile (Academic info view + editable phone & interest tags)", 0.8, 1.5, 3.6, 5.4),
        ("Coordinator Portal (4 Tabs)", "Club Overview (Manage profile, category, and editable description)\nMembership Requests (Review student applicants with status filter; Approve / Reject actions with instant database update)\nManage Events & Rosters (Create new event modal with seat limits and deadlines; View registered attendee roster with USNs; Delete event)\nBroadcast Announcements (Create and broadcast announcements with priority levels)", 4.8, 1.5, 3.6, 5.4),
        ("Administrator Portal (4 Tabs)", "Live KPI Metric Cards (Real-time counts for Total Users, Campus Clubs, Active Clubs, and Scheduled Events)\nUser Registry (Search users by name/email; filter by STUDENT, COORDINATOR, ADMIN; zero password hash leakage)\nManage Clubs (Register new club modal, assign coordinator, toggle status ACTIVE/INACTIVE, delete club)\nCampus Events (Supervise all events and broadcasts)", 8.8, 1.5, 3.7, 5.4)
    ]

    for title, desc, l, t, w, h in dashboards:
        add_card(s11, l, t, w, h, title)
        tb = s11.shapes.add_textbox(Inches(l + 0.2), Inches(t + 0.65), Inches(w - 0.4), Inches(h - 0.8))
        tf = tb.text_frame
        tf.word_wrap = True
        for line in desc.split("\n"):
            p = tf.add_paragraph()
            p.text = "• " + line
            p.font.size = Pt(10)
            p.font.color.rgb = COLOR_DARK

    set_notes(s11, "The application features 15 functional tabs divided across three role-specific dashboards. Students discover clubs, manage passes, and view recommendations. Coordinators are scoped strictly to their assigned club to approve members and manage rosters. Administrators have macro governance over users and clubs.")

    # ==========================================
    # SLIDE 12: Security Implementation
    # ==========================================
    s12 = prs.slides.add_slide(blank_layout)
    add_header(s12, "Security & Data Integrity Implementation", "SYSTEM SECURITY")

    sec_cards = [
        ("SHA-256 Password Hashing", "Cryptographic Protection", "Passwords are never stored, logged, or compared in plaintext. During registration and authentication, PasswordUtil digests credentials using SHA-256 before database comparison.", 0.8, 1.5, 5.6, 2.5),
        ("SQL Injection Immunity", "PreparedStatement Usage", "100% of DAO database interactions use parameterized PreparedStatement queries. User input is bound as data literals, rendering SQL injection attacks impossible.", 6.8, 1.5, 5.7, 2.5),
        ("Role-Based Authorization", "Boundary Isolation", "MainFrame's polymorphic router enforces strict UI boundaries. Coordinators cannot access other clubs or admin tools; students cannot access administrative panels.", 0.8, 4.3, 5.6, 2.6),
        ("Zero Information Leakage", "Privacy Preservation", "User registry JTable displays User ID, Username, Name, Email, and Role, but never retrieves or exposes password hashes. Login error messages prevent username enumeration.", 6.8, 4.3, 5.7, 2.6)
    ]

    for title, sub, desc, l, t, w, h in sec_cards:
        add_card(s12, l, t, w, h, title)
        tb = s12.shapes.add_textbox(Inches(l + 0.2), Inches(t + 0.65), Inches(w - 0.4), Inches(h - 0.8))
        tf = tb.text_frame
        tf.word_wrap = True
        p_sub = tf.paragraphs[0]
        p_sub.text = sub.upper()
        p_sub.font.bold = True
        p_sub.font.size = Pt(11)
        p_sub.font.color.rgb = COLOR_EMERALD

        p_desc = tf.add_paragraph()
        p_desc.text = desc
        p_desc.font.size = Pt(11)
        p_desc.font.color.rgb = COLOR_DARK

    set_notes(s12, "Security was built into the architecture from day one. All passwords are encrypted with SHA-256, all database queries use parameterized PreparedStatements to guarantee SQL injection immunity, and the User Registry table never exposes password hashes.")

    # ==========================================
    # SLIDE 13: Testing Results & Screenshots
    # ==========================================
    s13 = prs.slides.add_slide(blank_layout)
    add_header(s13, "Automated Testing & Visual Verification", "VERIFICATION EVIDENCE")

    add_card(s13, 0.8, 1.5, 5.6, 5.4, "JUnit 5 Automated Test Metrics")
    tb = s13.shapes.add_textbox(Inches(1.0), Inches(2.1), Inches(5.2), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.paragraphs[0].text = "Test Suite Breakdown (91 Total Tests):"
    tf.paragraphs[0].font.bold = True
    tf.paragraphs[0].font.size = Pt(12)
    tf.paragraphs[0].font.color.rgb = COLOR_BLUE

    suite_stats = [
        "• OOPConceptsTest: 11 / 11 Passed (OOP Rigor)",
        "• ExceptionHandlingTest: 11 / 11 Passed (Error Boundaries)",
        "• CollectionsAndRecommendationTest: 31 / 31 Passed (Heap/Rules)",
        "• CredentialsConsistencyTest: 5 / 5 Passed (Auth Mappings)",
        "• GUIIntegrationTest: 11 / 11 Passed (Headless Swing Views)",
        "• IntegrationTest: 7 / 7 Passed (Multi-Role Workflows)",
        "• CampusClubModelTest: 4 / 4 Passed (Domain POJOs)",
        "• LiveDatabaseTest: 11 Conditionally Skipped (Runs against live MySQL)"
    ]
    for s_stat in suite_stats:
        p = tf.add_paragraph()
        p.text = s_stat
        p.font.size = Pt(11)
        p.font.color.rgb = COLOR_DARK

    p_sum = tf.add_paragraph()
    p_sum.text = "\nSummary: 80 Passed, 0 Failures, 0 Errors, 11 Conditional Skips.\nBuild Status: Maven BUILD SUCCESS."
    p_sum.font.bold = True
    p_sum.font.size = Pt(11)
    p_sum.font.color.rgb = COLOR_EMERALD

    add_card(s13, 6.8, 1.5, 5.7, 5.4, "Visual Verification & Screenshots")
    tb = s13.shapes.add_textbox(Inches(7.0), Inches(2.1), Inches(5.3), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.paragraphs[0].text = "Verified GUI Screenshot Artifacts:"
    tf.paragraphs[0].font.bold = True
    tf.paragraphs[0].font.size = Pt(13)
    tf.paragraphs[0].font.color.rgb = COLOR_BLUE

    screens = [
        "1. Login Screen: Card design with demo credentials hint.",
        "2. Student Dashboard: 4 summary cards & 7 interactive tabs.",
        "3. Event Registration: Real-time seat tallying & capacity validation.",
        "4. Recommendations: Max-Heap ranked matches with rationale.",
        "5. Coordinator Dashboard: Club roster & applicant approval.",
        "6. Event Scheduler: Creation modal with deadlines & capacity caps.",
        "7. Admin Dashboard: Live metric cards & user registry filtering.",
        "8. MySQL Database: 7 normalized tables verified via CLI."
    ]
    for sc in screens:
        p = tf.add_paragraph()
        p.text = "• " + sc
        p.font.size = Pt(11)
        p.font.color.rgb = COLOR_DARK

    set_notes(s13, "We verified the entire system using 91 automated JUnit 5 tests. 80 unit, model, and GUI integration tests pass with 0 failures and 0 errors. The 11 live database tests use JUnit assumptions to run when live MySQL is available and skip gracefully when offline. All 15 GUI tabs have been verified.")

    # ==========================================
    # SLIDE 14: Conclusion & Future Enhancements
    # ==========================================
    s14 = prs.slides.add_slide(blank_layout)
    add_header(s14, "Conclusion & Future Enhancements", "SUMMARY & OUTLOOK")

    add_card(s14, 0.8, 1.5, 5.6, 5.4, "Project Conclusion & Key Takeaways")
    tb = s14.shapes.add_textbox(Inches(1.0), Inches(2.1), Inches(5.2), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.paragraphs[0].text = "Key Project Achievements:"
    tf.paragraphs[0].font.bold = True
    tf.paragraphs[0].font.size = Pt(13)
    tf.paragraphs[0].font.color.rgb = COLOR_BLUE

    concl = [
        "• Successfully engineered a robust Java Swing desktop platform eliminating manual spreadsheet errors and hall overcrowding.",
        "• Enforced dual-layer business rules using Java service validations and MySQL relational unique constraints.",
        "• Implemented a private, client-side Smart Recommendation Engine using a Max-Heap PriorityQueue.",
        "• Maintained 100% adherence to core OOP principles, custom exception handling, and parameterized SQL security.",
        "• Completed comprehensive documentation and 91 automated unit and integration tests."
    ]
    for c_item in concl:
        p = tf.add_paragraph()
        p.text = c_item
        p.font.size = Pt(11)
        p.font.color.rgb = COLOR_DARK

    add_card(s14, 6.8, 1.5, 5.7, 5.4, "Future Roadmap")
    tb = s14.shapes.add_textbox(Inches(7.0), Inches(2.1), Inches(5.3), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.paragraphs[0].text = "Future Technical Enhancements:"
    tf.paragraphs[0].font.bold = True
    tf.paragraphs[0].font.size = Pt(13)
    tf.paragraphs[0].font.color.rgb = COLOR_BLUE

    enhancements = [
        "1. Asynchronous SwingWorker:\nWrap JDBC queries inside background threads with loading progress bars to eliminate UI latency on slow remote networks.",
        "2. Automated PDF Certificate Export:\nIntegrate Apache PDFBox to generate downloadable event participation certificates for students.",
        "3. Automated Email Broadcasts:\nIntegrate JavaMail API to send registration confirmations and broadcast notices directly to student inboxes.",
        "4. QR Code Attendance Scanner:\nProvide webcam/mobile QR code check-in scanning for coordinators at hall entrance doors.",
        "5. REST API Architecture:\nDecouple Service and DAO layers into a headless backend to support mobile apps alongside the Swing desktop client."
    ]
    for e_item in enhancements:
        p = tf.add_paragraph()
        p.text = e_item
        p.font.size = Pt(11)
        p.font.color.rgb = COLOR_DARK

    set_notes(s14, "In conclusion, the Campus Club Management System delivers a robust, secure, and user-friendly desktop solution that solves genuine college extracurricular challenges while demonstrating complete software engineering rigor. Thank you for your time, guidance, and evaluation. We are now open for your questions.")

    prs.save(output_path)
    print(f"Successfully generated PowerPoint presentation at: {output_path}")

if __name__ == "__main__":
    out_file = sys.argv[1] if len(sys.argv) > 1 else "Campus_Club_Management_System_Presentation.pptx"
    create_deck(out_file)
