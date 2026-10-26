-- ==========================================================
-- Campus Club Management System - MySQL 8 Database Schema
-- Database Name: campus_club_db
-- ==========================================================

CREATE DATABASE IF NOT EXISTS campus_club_db;
USE campus_club_db;

-- ----------------------------------------------------------
-- 1. Base USERS Table
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('STUDENT', 'COORDINATOR', 'ADMIN') NOT NULL,
    phone_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_role (role)
);

-- ----------------------------------------------------------
-- 2. STUDENTS Table (Linked to users)
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    register_number VARCHAR(50) NOT NULL UNIQUE,
    department VARCHAR(100) NOT NULL,
    year INT NOT NULL,
    semester INT DEFAULT 1,
    phone VARCHAR(20),
    interests TEXT,
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_students_dept (department)
);

-- ----------------------------------------------------------
-- 3. CLUBS Table
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS clubs (
    club_id INT AUTO_INCREMENT PRIMARY KEY,
    club_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    coordinator_id INT,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    established_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_club_coordinator FOREIGN KEY (coordinator_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_clubs_category (category),
    INDEX idx_clubs_status (status)
);

-- ----------------------------------------------------------
-- 4. MEMBERSHIPS Table (Business Constraint: 1 student cannot join same club twice)
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS memberships (
    membership_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    club_id INT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    membership_status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    role_in_club VARCHAR(50) DEFAULT 'MEMBER',
    CONSTRAINT fk_membership_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_club FOREIGN KEY (club_id) REFERENCES clubs(club_id) ON DELETE CASCADE,
    CONSTRAINT uq_student_club UNIQUE (student_id, club_id),
    INDEX idx_memberships_student (student_id),
    INDEX idx_memberships_club (club_id),
    INDEX idx_memberships_status (membership_status)
);

-- ----------------------------------------------------------
-- 5. EVENTS Table
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS events (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    club_id INT NOT NULL,
    event_name VARCHAR(150) NOT NULL,
    description TEXT,
    event_date DATETIME NOT NULL,
    venue VARCHAR(150) NOT NULL,
    capacity INT DEFAULT 100,
    registration_deadline DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_club FOREIGN KEY (club_id) REFERENCES clubs(club_id) ON DELETE CASCADE,
    INDEX idx_events_club (club_id),
    INDEX idx_events_date (event_date)
);

-- ----------------------------------------------------------
-- 6. EVENT REGISTRATIONS Table (Business Constraint: 1 student cannot register for same event twice)
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS event_registrations (
    registration_id INT AUTO_INCREMENT PRIMARY KEY,
    event_id INT NOT NULL,
    student_id INT NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    registration_status ENUM('REGISTERED', 'CANCELLED', 'WAITLISTED') DEFAULT 'REGISTERED',
    attended BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_reg_event FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE,
    CONSTRAINT fk_reg_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT uq_event_student UNIQUE (event_id, student_id),
    INDEX idx_reg_event (event_id),
    INDEX idx_reg_student (student_id),
    INDEX idx_reg_status (registration_status)
);

-- ----------------------------------------------------------
-- 7. ATTENDANCE Table
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS attendance (
    attendance_id INT AUTO_INCREMENT PRIMARY KEY,
    registration_id INT NOT NULL,
    event_id INT NOT NULL,
    student_id INT NOT NULL,
    status ENUM('PRESENT', 'ABSENT') DEFAULT 'ABSENT',
    marked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_reg FOREIGN KEY (registration_id) REFERENCES event_registrations(registration_id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_event FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    UNIQUE KEY uq_attendance_reg (registration_id),
    INDEX idx_attendance_event (event_id),
    INDEX idx_attendance_student (student_id)
);

-- ----------------------------------------------------------
-- 8. ANNOUNCEMENTS Table
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS announcements (
    announcement_id INT AUTO_INCREMENT PRIMARY KEY,
    club_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH') DEFAULT 'MEDIUM',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_announcement_club FOREIGN KEY (club_id) REFERENCES clubs(club_id) ON DELETE CASCADE,
    INDEX idx_announcements_club (club_id),
    INDEX idx_announcements_created (created_at)
);

-- ==========================================================
-- SEED DATA (Default password for all demo accounts: "password123")
-- SHA-256 Hash: ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f
-- ==========================================================

-- 1. USERS (1 Admin, 3 Coordinators, 8 Students + 4 Quick Demo Accounts)
INSERT IGNORE INTO users (user_id, username, full_name, email, password_hash, role, phone_number) VALUES
(1, 'admin', 'System Administrator', 'admin@campus.edu', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN', '9876543210'),
(2, 'prof_sharma', 'Prof. Rajesh Sharma', 'sharma@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'COORDINATOR', '9876543211'),
(3, 'prof_anita', 'Dr. Anita Desai', 'anita@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'COORDINATOR', '9876543212'),
(4, 'prof_vikas', 'Prof. Vikas Hegde', 'vikas@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'COORDINATOR', '9876543213'),
(5, 'rohit_k', 'Rohit Kumar', 'rohit.k@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'STUDENT', '9876543220'),
(6, 'priya_s', 'Priya Singh', 'priya.s@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'STUDENT', '9876543221'),
(7, 'aravind_m', 'Aravind Menon', 'aravind.m@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'STUDENT', '9876543222'),
(8, 'sneha_r', 'Sneha Rao', 'sneha.r@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'STUDENT', '9876543223'),
(9, 'vikram_t', 'Vikram Thakur', 'vikram.t@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'STUDENT', '9876543224'),
(10, 'ananya_g', 'Ananya Gupta', 'ananya.g@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'STUDENT', '9876543225'),
(11, 'rahul_v', 'Rahul Verma', 'rahul.v@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'STUDENT', '9876543226'),
(12, 'meera_n', 'Meera Nair', 'meera.n@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'STUDENT', '9876543227'),
(13, 'coord_coding', 'David Miller', 'coord.coding@campus.edu', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'COORDINATOR', '9876543230'),
(14, 'coord_robotics', 'Elena Rostova', 'coord.robotics@campus.edu', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'COORDINATOR', '9876543231'),
(15, 'student1', 'Alex Turner', 'student1@campus.edu', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'STUDENT', '9876543232'),
(16, 'student2', 'Sophia Chen', 'student2@campus.edu', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'STUDENT', '9876543233');

-- 2. STUDENTS (10 Students)
INSERT IGNORE INTO students (student_id, user_id, register_number, department, year, semester, phone, interests) VALUES
(1, 5, '1CC21CS045', 'Computer Science & Engineering', 3, 5, '9876543220', 'Coding, Artificial Intelligence, Web Development, Cloud'),
(2, 6, '1CC22EC012', 'Electronics & Communication', 2, 3, '9876543221', 'Robotics, IoT, Hardware, Embedded Systems'),
(3, 7, '1CC21ME033', 'Mechanical Engineering', 3, 5, '9876543222', 'Sports, Football, Athletics, Fitness'),
(4, 8, '1CC23IS078', 'Information Science', 1, 2, '9876543223', 'Photography, Cinematography, Digital Art, Design'),
(5, 9, '1CC22CS099', 'Computer Science & Engineering', 2, 4, '9876543224', 'Entrepreneurship, Startups, Product Management, Coding'),
(6, 10, '1CC21BT015', 'Biotechnology', 3, 5, '9876543225', 'Cultural, Music, Classical Dance, Drama'),
(7, 11, '1CC23EC050', 'Electronics & Communication', 1, 1, '9876543226', 'Robotics, Coding, Competitive Programming'),
(8, 12, '1CC22IS024', 'Information Science', 2, 3, '9876543227', 'Literature, Debating, MUN, Creative Writing'),
(9, 15, '1CC21CS101', 'Computer Science & Engineering', 3, 5, '9876543232', 'Coding, Artificial Intelligence, Web Development, Cloud'),
(10, 16, '1CC22EC102', 'Electronics & Communication', 2, 3, '9876543233', 'Robotics, IoT, Hardware, Embedded Systems');

-- 3. CLUBS (6 Requested Clubs)
INSERT IGNORE INTO clubs (club_id, club_name, description, category, coordinator_id, status, established_date) VALUES
(1, 'Coding Club', 'Campus hub for competitive programming, algorithmic puzzles, hackathons, and open source development.', 'Technical', 13, 'ACTIVE', '2021-08-15'),
(2, 'Robotics Club', 'Hands-on hardware development, autonomous robots, IoT systems, and embedded controllers.', 'Technical', 14, 'ACTIVE', '2022-01-10'),
(3, 'Sports Club', 'Promotes campus athletics, intra-college tournaments, cricket, football, basketball, and fitness.', 'Sports', 4, 'ACTIVE', '2020-03-05'),
(4, 'Photography Club', 'Visual storytelling, DSLR masterclasses, campus event coverage, and photo gallery exhibitions.', 'Cultural', 2, 'ACTIVE', '2021-11-20'),
(5, 'Cultural Club', 'Celebrating stage theatre, music, classical and western dance, and annual college festivals.', 'Cultural', 3, 'ACTIVE', '2019-09-01'),
(6, 'Entrepreneurship Club', 'Incubation hub fostering innovation, business plan pitch decks, venture capital talks, and leadership.', 'Academic', 4, 'ACTIVE', '2022-06-18');

-- 4. MEMBERSHIPS
INSERT IGNORE INTO memberships (membership_id, student_id, club_id, membership_status, role_in_club) VALUES
(1, 1, 1, 'APPROVED', 'LEAD'),
(2, 1, 2, 'APPROVED', 'MEMBER'),
(3, 2, 2, 'APPROVED', 'CO-LEAD'),
(4, 3, 3, 'APPROVED', 'CAPTAIN'),
(5, 4, 4, 'APPROVED', 'MEMBER'),
(6, 5, 6, 'APPROVED', 'LEAD'),
(7, 6, 5, 'APPROVED', 'SECRETARY'),
(8, 7, 1, 'PENDING', 'MEMBER'),
(9, 8, 5, 'APPROVED', 'MEMBER');

-- 5. EVENTS (At least 8 realistic events)
INSERT IGNORE INTO events (event_id, club_id, event_name, description, event_date, venue, capacity, registration_deadline) VALUES
(1, 1, 'HackCampus 2026 Hackathon', '24-hour national hackathon on AI, Web3, and Sustainable Tech.', '2026-11-15 09:00:00', 'Main Auditorium & CS Labs', 150, '2026-11-10 23:59:59'),
(2, 2, 'Autonomous Line Follower Workshop', 'Hands-on training building Arduino-based line tracer robots.', '2026-10-25 14:00:00', 'Robotics Lab 302', 40, '2026-10-23 18:00:00'),
(3, 3, 'Inter-Department Football Cup', 'Annual 7-a-side knock-out football tournament for all departments.', '2026-11-02 08:00:00', 'Campus Sports Ground', 80, '2026-10-30 20:00:00'),
(4, 4, 'Campus Photo Walk & Lens Masterclass', 'Outdoor morning photography session focusing on light and composition.', '2026-10-28 07:00:00', 'Botanical Garden & Amphitheatre', 30, '2026-10-26 23:59:59'),
(5, 5, 'Annual Cultural Fest Auditions', 'Auditions for music band, solo singing, and drama troupes.', '2026-11-05 16:00:00', 'Open Air Theatre (OAT)', 100, '2026-11-03 17:00:00'),
(6, 6, 'Startup Pitch Deck Battle', 'Pitch your startup ideas to angel investors and industry mentors with cash grants.', '2026-11-20 10:30:00', 'Seminar Hall 1', 60, '2026-11-16 23:59:59'),
(7, 1, 'Web & Cloud Dev Bootcamp', 'Interactive workshop deploying full-stack apps on modern cloud platforms.', '2026-10-30 15:00:00', 'Advanced Computing Lab', 50, '2026-10-28 18:00:00'),
(8, 2, 'Drone Building & Flight Lab', 'Learn aerodynamic principles and assemble miniature quadcopters.', '2026-11-12 11:00:00', 'Mechatronics Ground Lab', 35, '2026-11-09 23:59:59');

-- 6. EVENT REGISTRATIONS
INSERT IGNORE INTO event_registrations (registration_id, event_id, student_id, registration_status, attended) VALUES
(1, 1, 1, 'REGISTERED', FALSE),
(2, 1, 5, 'REGISTERED', FALSE),
(3, 2, 2, 'REGISTERED', FALSE),
(4, 2, 7, 'REGISTERED', FALSE),
(5, 3, 3, 'REGISTERED', FALSE),
(6, 4, 4, 'REGISTERED', FALSE),
(7, 5, 6, 'REGISTERED', FALSE),
(8, 6, 5, 'REGISTERED', FALSE);

-- 7. ANNOUNCEMENTS
INSERT IGNORE INTO announcements (announcement_id, club_id, title, message, priority) VALUES
(1, 1, 'HackCampus 2026 Registrations Open!', 'Registrations for HackCampus 2026 are now live. Form teams of 3-4 members and submit problem statements.', 'HIGH'),
(2, 2, 'Hardware Kit Distribution for Robotics Workshop', 'Registered participants can collect sensor kits from Lab 302 starting Wednesday 2 PM.', 'MEDIUM'),
(3, 3, 'Practice Schedule for Football Cup', 'Team trials will take place every weekday evening from 5:00 PM to 6:30 PM on the main ground.', 'LOW'),
(4, 6, 'Incubation Mentorship Slots Available', 'Top 5 pitch decks from the Startup Battle will receive 6 months of free college incubator workspace.', 'HIGH');
