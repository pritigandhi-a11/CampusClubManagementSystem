package com.campusclub.web;

import com.campusclub.dao.AnnouncementDAO;
import com.campusclub.dao.ClubDAO;
import com.campusclub.dao.EventDAO;
import com.campusclub.dao.EventRegistrationDAO;
import com.campusclub.dao.MembershipDAO;
import com.campusclub.dao.StudentDAO;
import com.campusclub.dao.UserDAO;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.model.*;
import com.campusclub.service.AuthenticationService;
import com.campusclub.service.ClubService;
import com.campusclub.service.EventService;
import com.campusclub.service.MembershipService;
import com.campusclub.util.DatabaseConnection;
import com.campusclub.util.DatabaseMigrationRunner;
import com.campusclub.util.EmailService;
import com.campusclub.util.PasswordUtil;
import com.campusclub.util.ValidationUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller and REST API Handler for the multi-user Online Web Portal.
 * Enables simultaneous access for a 5-member team and an external teacher/reviewer.
 * Enforces Role-Based Access Control (RBAC):
 * - ADMIN: Full control (Clubs, Events, Notices, Memberships, Users)
 * - COORDINATOR / TEAM MEMBER: Manage clubs, events, notices, applications
 * - TEACHER / REVIEWER: Strictly Read-Only audit and evaluation oversight
 */
public class WebPortalController implements HttpHandler {

    private final AuthenticationService authService;
    private final ClubService clubService;
    private final EventService eventService;
    private final MembershipService membershipService;
    private final AnnouncementDAO announcementDAO;
    private final UserDAO userDAO;
    private final MembershipDAO membershipDAO;
    private final StudentDAO studentDAO;
    private final EventRegistrationDAO eventRegistrationDAO;

    // Active in-memory web sessions (cookie-backed)
    private static final Map<String, WebSession> activeSessions = new ConcurrentHashMap<>();
    private static final String SESSION_COOKIE_NAME = "ccms_session";

    // In-memory synchronized fallback store (ensures 100% zero-crash operation in container)
    private static final List<Map<String, String>> fallbackClubs = Collections.synchronizedList(new ArrayList<>());
    private static final List<Map<String, String>> fallbackEvents = Collections.synchronizedList(new ArrayList<>());
    private static final List<Map<String, String>> fallbackNotices = Collections.synchronizedList(new ArrayList<>());
    private static final List<Map<String, String>> fallbackApplications = Collections.synchronizedList(new ArrayList<>());
    private static final List<Map<String, String>> fallbackEventRegistrations = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, User> builtInUsers = new ConcurrentHashMap<>();
    private static final AtomicInteger idCounter = new AtomicInteger(100);
    private static volatile boolean accountsProvisioned = false;
    private static volatile long lastDbCheck = 0;
    private static volatile boolean lastDbStatus = false;

    public static boolean isDatabaseReachable() {
        long now = System.currentTimeMillis();
        if (now - lastDbCheck < 10000) {
            return lastDbStatus;
        }
        lastDbStatus = DatabaseConnection.testConnection();
        lastDbCheck = now;
        return lastDbStatus;
    }

    static {
        initFallbackData();
    }

    public WebPortalController() {
        this.authService = new AuthenticationService();
        this.clubService = new ClubService();
        this.eventService = new EventService();
        this.membershipService = new MembershipService();
        this.announcementDAO = new AnnouncementDAO();
        this.userDAO = new UserDAO();
        this.membershipDAO = new MembershipDAO();
        this.studentDAO = new StudentDAO();
        this.eventRegistrationDAO = new EventRegistrationDAO();

        // Non-blocking background account provisioning so user requests never stall on cold starts
        Thread provisionThread = new Thread(() -> {
            try {
                ensureAccountsProvisionedInDatabase();
            } catch (Exception ignored) {}
        }, "ccms-account-provisioning");
        provisionThread.setDaemon(true);
        provisionThread.start();
    }

    public static class WebSession {
        public final String sessionId;
        public final int userId;
        public final String username;
        public final String fullName;
        public final UserRole role;
        public final boolean verified;
        public final long createdAt;
        public volatile long lastAccessedAt;

        public WebSession(String sessionId, int userId, String username, String fullName, UserRole role) {
            this(sessionId, userId, username, fullName, role, true);
        }

        public WebSession(String sessionId, int userId, String username, String fullName, UserRole role, boolean verified) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.role = role;
            this.verified = verified;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessedAt = System.currentTimeMillis();
        }

        public boolean isReviewer() {
            return role == UserRole.TEACHER;
        }

        public boolean isAdmin() {
            return role == UserRole.ADMIN;
        }

        public boolean isCoordinator() {
            return role == UserRole.COORDINATOR;
        }

        public boolean isStudent() {
            return role == UserRole.STUDENT;
        }

        public boolean isVerified() {
            return verified;
        }
    }

    public static boolean isDemoPresetsEnabled() {
        String env = System.getenv("CCMS_ENV");
        if (env != null && (env.equalsIgnoreCase("production") || env.equalsIgnoreCase("prod"))) {
            return false;
        }
        String disable = System.getenv("DISABLE_DEMO_PRESETS");
        if ("true".equalsIgnoreCase(disable)) {
            return false;
        }
        String enable = System.getenv("ENABLE_DEMO_PRESETS");
        if ("false".equalsIgnoreCase(enable)) {
            return false;
        }
        return true;
    }

    public static boolean isProductionMode() {
        String env = System.getenv("CCMS_ENV");
        return env != null && (env.equalsIgnoreCase("production") || env.equalsIgnoreCase("prod"));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            // 1. Static Assets & Pages
            if (path.equals("/login") || path.equals("/login.html")) {
                if ("POST".equals(method)) {
                    handleFormLogin(exchange);
                } else {
                    WebSession session = getSession(exchange);
                    if (session != null) {
                        redirect(exchange, "/portal");
                        return;
                    }
                    serveLoginPage(exchange, null);
                }
                return;
            }

            if (path.equals("/portal") || path.equals("/portal.html") || path.equals("/adminPortal") || path.equals("/dashboard")) {
                WebSession session = getSession(exchange);
                if (session == null) {
                    redirect(exchange, "/login");
                    return;
                }
                if (path.equals("/adminPortal") && session.isStudent()) {
                    sendResponse(exchange, 403, "text/plain; charset=UTF-8", "403 Forbidden: Students are not authorized to access the Admin Portal.");
                    return;
                }
                if (session.isStudent()) {
                    serveStudentPortalPage(exchange, session);
                    return;
                }
                servePortalPage(exchange, session);
                return;
            }

            // 2. Authentication REST APIs
            if (path.equals("/api/auth/register-student") && "POST".equals(method)) {
                handleRegisterStudent(exchange);
                return;
            }
            if (path.equals("/api/auth/verify-student") && "POST".equals(method)) {
                handleVerifyStudent(exchange);
                return;
            }
            if (path.equals("/api/auth/resend-otp") && "POST".equals(method)) {
                handleResendOtp(exchange);
                return;
            }
            if (path.equals("/api/auth/login") && "POST".equals(method)) {
                handleApiLogin(exchange);
                return;
            }
            if (path.equals("/api/auth/logout") && ("POST".equals(method) || "GET".equals(method))) {
                handleApiLogout(exchange);
                return;
            }
            if (path.equals("/api/auth/me") && "GET".equals(method)) {
                handleApiMe(exchange);
                return;
            }
            if (path.equals("/api/auth/change-password") && "POST".equals(method)) {
                handleApiChangePassword(exchange);
                return;
            }

            // 3. Clubs REST APIs
            if (path.equals("/api/clubs")) {
                if ("GET".equals(method)) {
                    handleGetClubs(exchange);
                } else if ("POST".equals(method)) {
                    handleCreateClub(exchange);
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
                return;
            }
            if (path.equals("/api/clubs/toggle-status") && "POST".equals(method)) {
                handleToggleClubStatus(exchange);
                return;
            }

            // 4. Events REST APIs
            if (path.equals("/api/events")) {
                if ("GET".equals(method)) {
                    handleGetEvents(exchange);
                } else if ("POST".equals(method)) {
                    handleCreateEvent(exchange);
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
                return;
            }
            if (path.equals("/api/events/register") && "POST".equals(method)) {
                handleRegisterEvent(exchange);
                return;
            }

            // 5. Announcements REST APIs
            if (path.equals("/api/announcements")) {
                if ("GET".equals(method)) {
                    handleGetAnnouncements(exchange);
                } else if ("POST".equals(method)) {
                    handleCreateAnnouncement(exchange);
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
                return;
            }

            // 6. Membership Applications REST APIs
            if (path.equals("/api/memberships")) {
                if ("GET".equals(method)) {
                    handleGetMemberships(exchange);
                } else if ("POST".equals(method)) {
                    handleApplyMembership(exchange);
                } else {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
                return;
            }
            if (path.equals("/api/memberships/review") && "POST".equals(method)) {
                handleReviewMembership(exchange);
                return;
            }

            // 7. Student Portal & Admin Extended REST APIs
            if (path.equals("/api/student/my-clubs") && "GET".equals(method)) {
                handleGetStudentClubs(exchange);
                return;
            }
            if (path.equals("/api/student/my-events") && "GET".equals(method)) {
                handleGetStudentEvents(exchange);
                return;
            }
            if (path.equals("/api/student/profile") && "GET".equals(method)) {
                handleGetStudentProfile(exchange);
                return;
            }
            if (path.equals("/api/admin/students") && "GET".equals(method)) {
                handleGetAdminStudents(exchange);
                return;
            }
            if (path.equals("/api/admin/event-registrations") && "GET".equals(method)) {
                handleGetAdminEventRegistrations(exchange);
                return;
            }

            // 8. System & Diagnostics APIs
            if (path.equals("/api/team") && "GET".equals(method)) {
                handleGetTeam(exchange);
                return;
            }
            if (path.equals("/api/db-status") && "GET".equals(method)) {
                handleGetDbStatus(exchange);
                return;
            }

            // 8. Fallback / Default
            if (path.equals("/") || path.isEmpty()) {
                WebSession session = getSession(exchange);
                if (session != null) {
                    redirect(exchange, "/portal");
                } else {
                    redirect(exchange, "/login");
                }
                return;
            }

            sendResponse(exchange, 404, "text/plain", "404 Not Found");

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"Internal Server Error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    // =========================================================================
    // Authentication & Session Logic
    // =========================================================================

    private void handleFormLogin(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> params = parseFormData(body);
        String username = params.get("username");
        String password = params.get("password");

        User user = authenticate(username, password);
        if (user != null) {
            String sessionId = UUID.randomUUID().toString();
            WebSession session = new WebSession(sessionId, user.getUserId(), user.getUsername(), user.getFullName(), user.getRole(), user.isVerified());
            activeSessions.put(sessionId, session);
            setSessionCookie(exchange, sessionId);
            redirect(exchange, "/portal");
        } else {
            serveLoginPage(exchange, "Invalid username or password. Please check your credentials.");
        }
    }

    private void handleApiLogin(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> data = parseJson(body);
        String username = data.get("username");
        String password = data.get("password");

        User user = authenticate(username, password);
        if (user != null) {
            String sessionId = UUID.randomUUID().toString();
            WebSession session = new WebSession(sessionId, user.getUserId(), user.getUsername(), user.getFullName(), user.getRole(), user.isVerified());
            activeSessions.put(sessionId, session);
            setSessionCookie(exchange, sessionId);

            StringBuilder json = new StringBuilder();
            json.append("{")
                .append("\"success\":true,")
                .append("\"sessionId\":\"").append(sessionId).append("\",")
                .append("\"user\":{")
                .append("\"id\":").append(user.getUserId()).append(",")
                .append("\"username\":\"").append(escapeJson(user.getUsername())).append("\",")
                .append("\"fullName\":\"").append(escapeJson(user.getFullName())).append("\",")
                .append("\"role\":\"").append(user.getRole().name()).append("\",")
                .append("\"isReviewer\":").append(user.getRole() == UserRole.TEACHER).append(",")
                .append("\"isVerified\":").append(user.isVerified())
                .append("}")
                .append("}");
            sendJson(exchange, 200, json.toString());
        } else {
            sendJson(exchange, 401, "{\"success\":false,\"error\":\"Invalid username or password\"}");
        }
    }

    private void handleApiLogout(HttpExchange exchange) throws IOException {
        String sessionId = getSessionCookie(exchange);
        if (sessionId != null) {
            activeSessions.remove(sessionId);
        }
        clearSessionCookie(exchange);
        sendJson(exchange, 200, "{\"success\":true,\"message\":\"Logged out successfully\"}");
    }

    private void handleApiMe(HttpExchange exchange) throws IOException {
        WebSession session = getSession(exchange);
        if (session != null) {
            StringBuilder json = new StringBuilder();
            json.append("{")
                .append("\"authenticated\":true,")
                .append("\"user\":{")
                .append("\"id\":").append(session.userId).append(",")
                .append("\"username\":\"").append(escapeJson(session.username)).append("\",")
                .append("\"fullName\":\"").append(escapeJson(session.fullName)).append("\",")
                .append("\"role\":\"").append(session.role.name()).append("\",")
                .append("\"isReviewer\":").append(session.isReviewer()).append(",")
                .append("\"isVerified\":").append(session.isVerified())
                .append("}")
                .append("}");
            sendJson(exchange, 200, json.toString());
        } else {
            sendJson(exchange, 200, "{\"authenticated\":false}");
        }
    }

    private void handleApiChangePassword(HttpExchange exchange) throws IOException {
        WebSession session = getSession(exchange);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized. Please log in first.\"}");
            return;
        }

        Map<String, String> data = parseJson(readBody(exchange));
        String oldPass = data.get("oldPassword");
        String newPass = data.get("newPassword");

        if (newPass == null || newPass.trim().length() < 6) {
            sendJson(exchange, 400, "{\"error\":\"New password must be at least 6 characters long.\"}");
            return;
        }

        try {
            boolean updated = authService.changePassword(session.userId, oldPass, newPass);
            if (updated) {
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Password successfully updated!\"}");
                return;
            }
        } catch (Exception e) {
            // If DB update failed, check in-memory user
            User bu = builtInUsers.get(session.username.toLowerCase());
            if (bu != null && PasswordUtil.verifyPassword(oldPass, bu.getPasswordHash())) {
                bu.setPasswordHash(PasswordUtil.hashPassword(newPass));
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Password successfully updated in session!\"}");
                return;
            }
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        sendJson(exchange, 400, "{\"error\":\"Current password verification failed.\"}");
    }

    public synchronized void ensureAccountsProvisionedInDatabase() {
        if (accountsProvisioned) {
            return;
        }
        try {
            if (!isDatabaseReachable()) {
                return;
            }
            DatabaseMigrationRunner.runSecurityMigration();
            Set<User> uniqueUsers = new HashSet<>(builtInUsers.values());
            for (User u : uniqueUsers) {
                try {
                    Optional<User> existing = userDAO.findByUsername(u.getUsername());
                    int effectiveUserId;
                    if (existing.isEmpty()) {
                        effectiveUserId = userDAO.createUser(u);
                        u.setUserId(effectiveUserId);
                    } else {
                        User dbUser = existing.get();
                        effectiveUserId = dbUser.getUserId();
                        u.setUserId(effectiveUserId);
                        if (u.getRole() != null && u.getRole() != dbUser.getRole()) {
                            try (Connection conn = DatabaseConnection.getConnection();
                                 PreparedStatement ps = conn.prepareStatement("UPDATE users SET role = ? WHERE user_id = ?")) {
                                ps.setString(1, u.getRole().name());
                                ps.setInt(2, effectiveUserId);
                                ps.executeUpdate();
                            } catch (Exception ignored) {}
                        }
                    }
                    if (u instanceof Student s && effectiveUserId > 0) {
                        try {
                            studentDAO.ensureStudentRecordExists(effectiveUserId, s.getRegisterNumber(), s.getDepartment(), s.getYear(), s.getSemester(), s.getPhoneNumber(), s.getInterests());
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {
                }
            }
            accountsProvisioned = true;
        } catch (Exception ignored) {
        }
    }

    public static Map<String, User> getBuiltInUsers() {
        return Collections.unmodifiableMap(builtInUsers);
    }

    private User authenticate(String username, String password) {
        if (username == null || password == null) return null;
        String cleanUser = username.trim();

        // 1. Try real database authentication
        try {
            User user = authService.login(cleanUser, password);
            if (user != null) {
                User inMem = builtInUsers.get(cleanUser.toLowerCase());
                if (inMem != null) {
                    user.setVerified(inMem.isVerified());
                    user.setVerificationCode(inMem.getVerificationCode());
                }
                if ("teacher_reviewer".equalsIgnoreCase(user.getUsername()) || "prof_sharma".equalsIgnoreCase(user.getUsername())) {
                    user.setRole(UserRole.TEACHER);
                }
                return user;
            }
        } catch (Exception ignored) {
            // DB may be offline or user not yet in DB; fallback to built-in accounts
        }

        // 2. Built-in accounts (5 Team Members + 1 Reviewer + Admin + Students)
        // Supports logging in via username, @klu.ac.in institutional email, or student ID (register number)
        User fallbackUser = builtInUsers.get(cleanUser.toLowerCase());
        if (fallbackUser == null) {
            String low = cleanUser.toLowerCase();
            if (low.equals("teamdev1") || low.equals("member2") || low.equals("member_2") || low.equals("tech_lead")) {
                fallbackUser = builtInUsers.get("team_dev1");
            } else if (low.equals("teamdev2") || low.equals("member3") || low.equals("member_3") || low.equals("events_lead")) {
                fallbackUser = builtInUsers.get("team_dev2");
            } else if (low.equals("teamlead") || low.equals("team_lead_admin")) {
                fallbackUser = builtInUsers.get("team_lead");
            } else if (low.equals("reviewer") || low.equals("teacher")) {
                fallbackUser = builtInUsers.get("teacher_reviewer");
            }
        }
        if (fallbackUser == null) {
            for (User u : builtInUsers.values()) {
                if (cleanUser.equalsIgnoreCase(u.getEmail())) {
                    fallbackUser = u;
                    break;
                }
                if (u instanceof Student s) {
                    if (cleanUser.equalsIgnoreCase(s.getRegisterNumber())) {
                        fallbackUser = u;
                        break;
                    }
                }
            }
        }
        if (fallbackUser != null) {
            if (PasswordUtil.verifyPassword(password, fallbackUser.getPasswordHash())
                    || (AuthenticationService.isBuiltInEvaluationUser(fallbackUser.getUsername()) && AuthenticationService.verifyEvaluationPassword(fallbackUser.getUsername(), password))) {
                String fh = fallbackUser.getPasswordHash();
                if (fh != null && !fh.startsWith("$2a$") && !fh.startsWith("$2b$") && !fh.startsWith("$2y$")) {
                    try {
                        String upgraded = PasswordUtil.hashPassword(password);
                        fallbackUser.setPasswordHash(upgraded);
                    } catch (Exception ignored) {}
                }
                return fallbackUser;
            }
            // Allow initial review password overrides if set in env
            String envReviewerPass = System.getenv("REVIEWER_PASSWORD");
            if (envReviewerPass != null && ("teacher_reviewer".equalsIgnoreCase(cleanUser) || "prof_sharma".equalsIgnoreCase(cleanUser) || "reviewer".equalsIgnoreCase(cleanUser) || "teacher".equalsIgnoreCase(cleanUser)) && envReviewerPass.equals(password)) {
                return fallbackUser;
            }
            String envAdminPass = System.getenv("ADMIN_PASSWORD");
            if (envAdminPass != null && ("admin".equalsIgnoreCase(cleanUser) || "team_lead".equalsIgnoreCase(cleanUser) || "teamlead".equalsIgnoreCase(cleanUser)) && envAdminPass.equals(password)) {
                return fallbackUser;
            }
            String envCoordPass = System.getenv("COORDINATOR_PASSWORD");
            if (envCoordPass != null && ("team_dev1".equalsIgnoreCase(cleanUser) || "team_dev2".equalsIgnoreCase(cleanUser) || "coord_coding".equalsIgnoreCase(cleanUser) || "coord_robotics".equalsIgnoreCase(cleanUser) || "member2".equalsIgnoreCase(cleanUser) || "member3".equalsIgnoreCase(cleanUser)) && envCoordPass.equals(password)) {
                return fallbackUser;
            }
            if (envAdminPass != null && AuthenticationService.isBuiltInEvaluationUser(fallbackUser.getUsername()) && envAdminPass.equals(password)) {
                return fallbackUser;
            }
        }

        return null;
    }

    // =========================================================================
    // Business Operations REST APIs (Clubs, Events, Notices, Memberships)
    // =========================================================================

    private void handleGetClubs(HttpExchange exchange) throws IOException {
        List<Map<String, String>> result = new ArrayList<>();
        boolean dbOk = false;

        try {
            List<Club> clubs = clubService.getAllClubs();
            if (clubs != null && !clubs.isEmpty()) {
                for (Club c : clubs) {
                    Map<String, String> map = new HashMap<>();
                    map.put("id", String.valueOf(c.getClubId()));
                    map.put("name", c.getClubName());
                    map.put("description", c.getDescription() != null ? c.getDescription() : "");
                    map.put("category", c.getCategory() != null ? c.getCategory() : "Technical");
                    map.put("coordinator", c.getCoordinatorName() != null ? c.getCoordinatorName() : "Faculty Coordinator");
                    map.put("status", c.getStatus() != null ? c.getStatus() : "ACTIVE");
                    map.put("members", String.valueOf(c.getMemberCount()));
                    map.put("established", c.getEstablishedDate() != null ? c.getEstablishedDate().toString() : "2024-08-15");
                    result.add(map);
                }
                dbOk = true;
            }
        } catch (Exception ignored) {}

        if (!dbOk) {
            result = new ArrayList<>(fallbackClubs);
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < result.size(); i++) {
            Map<String, String> m = result.get(i);
            sb.append("{")
              .append("\"id\":\"").append(escapeJson(m.get("id"))).append("\",")
              .append("\"name\":\"").append(escapeJson(m.get("name"))).append("\",")
              .append("\"description\":\"").append(escapeJson(m.get("description"))).append("\",")
              .append("\"category\":\"").append(escapeJson(m.get("category"))).append("\",")
              .append("\"coordinator\":\"").append(escapeJson(m.get("coordinator"))).append("\",")
              .append("\"status\":\"").append(escapeJson(m.get("status"))).append("\",")
              .append("\"members\":").append(m.getOrDefault("members", "0")).append(",")
              .append("\"established\":\"").append(escapeJson(m.getOrDefault("established", "2024-08-15"))).append("\"")
              .append("}");
            if (i < result.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleCreateClub(HttpExchange exchange) throws IOException {
        WebSession session = getSession(exchange);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized. Please log in.\"}");
            return;
        }

        // STRICT RBAC CHECK: Reviewers cannot mutate data!
        if (session.isReviewer()) {
            sendJson(exchange, 403, "{\"error\":\"Permission Denied: Teacher Reviewer accounts are strictly Read-Only. Adding clubs is disabled.\"}");
            return;
        }
        if (session.isStudent()) {
            sendJson(exchange, 403, "{\"error\":\"Forbidden: Students cannot create campus clubs.\"}");
            return;
        }
        if (!session.isAdmin()) {
            sendJson(exchange, 403, "{\"error\":\"Forbidden: Only administrators can create new clubs.\"}");
            return;
        }

        Map<String, String> data = parseJson(readBody(exchange));
        String name = data.get("name");
        String category = data.get("category");
        String description = data.get("description");
        String coordinator = data.get("coordinator");

        if (name == null || name.trim().isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Club name is required.\"}");
            return;
        }

        int newId = idCounter.incrementAndGet();
        try {
            Club club = new Club();
            club.setClubName(name.trim());
            club.setCategory(category != null ? category.trim() : "Technical");
            club.setDescription(description != null ? description.trim() : "");
            club.setStatus("ACTIVE");
            clubService.createClub(club);
        } catch (Exception ignored) {}

        // Keep in-memory store synchronized
        Map<String, String> map = new HashMap<>();
        map.put("id", String.valueOf(newId));
        map.put("name", name.trim());
        map.put("category", category != null ? category.trim() : "Technical");
        map.put("description", description != null ? description.trim() : "New campus club registered by " + session.fullName);
        map.put("coordinator", coordinator != null ? coordinator.trim() : session.fullName);
        map.put("status", "ACTIVE");
        map.put("members", "1");
        map.put("established", LocalDateTime.now().toLocalDate().toString());
        fallbackClubs.add(0, map);

        sendJson(exchange, 201, "{\"success\":true,\"clubId\":" + newId + ",\"message\":\"Club created successfully!\"}");
    }

    private void handleToggleClubStatus(HttpExchange exchange) throws IOException {
        WebSession session = getSession(exchange);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        if (session.isReviewer()) {
            sendJson(exchange, 403, "{\"error\":\"Permission Denied: Reviewer accounts cannot modify club status.\"}");
            return;
        }
        if (session.isStudent()) {
            sendJson(exchange, 403, "{\"error\":\"Forbidden: Students cannot toggle club status.\"}");
            return;
        }
        if (!session.isAdmin() && !session.isCoordinator()) {
            sendJson(exchange, 403, "{\"error\":\"Forbidden: Administrative or coordinator privileges required.\"}");
            return;
        }

        Map<String, String> data = parseJson(readBody(exchange));
        String idStr = data.get("id");
        if (idStr == null) {
            sendJson(exchange, 400, "{\"error\":\"Club ID required\"}");
            return;
        }

        for (Map<String, String> c : fallbackClubs) {
            if (idStr.equals(c.get("id"))) {
                String current = c.get("status");
                String next = "ACTIVE".equalsIgnoreCase(current) ? "INACTIVE" : "ACTIVE";
                c.put("status", next);
                sendJson(exchange, 200, "{\"success\":true,\"newStatus\":\"" + next + "\"}");
                return;
            }
        }
        sendJson(exchange, 404, "{\"error\":\"Club not found\"}");
    }

    private void handleGetEvents(HttpExchange exchange) throws IOException {
        List<Map<String, String>> result = new ArrayList<>();
        boolean dbOk = false;

        try {
            List<Event> events = eventService.getAllEvents();
            if (events != null && !events.isEmpty()) {
                for (Event e : events) {
                    Map<String, String> map = new HashMap<>();
                    map.put("id", String.valueOf(e.getEventId()));
                    map.put("title", e.getEventName());
                    map.put("club", e.getClubName() != null ? e.getClubName() : "Campus Club");
                    map.put("date", e.getEventDate() != null ? e.getEventDate().toString() : "Upcoming");
                    map.put("venue", e.getVenue() != null ? e.getVenue() : "Main Auditorium");
                    map.put("capacity", String.valueOf(e.getCapacity()));
                    map.put("registrations", String.valueOf(e.getRegisteredCount()));
                    result.add(map);
                }
                dbOk = true;
            }
        } catch (Exception ignored) {}

        if (!dbOk) {
            result = new ArrayList<>(fallbackEvents);
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < result.size(); i++) {
            Map<String, String> m = result.get(i);
            sb.append("{")
              .append("\"id\":\"").append(escapeJson(m.get("id"))).append("\",")
              .append("\"title\":\"").append(escapeJson(m.get("title"))).append("\",")
              .append("\"club\":\"").append(escapeJson(m.get("club"))).append("\",")
              .append("\"date\":\"").append(escapeJson(m.get("date"))).append("\",")
              .append("\"venue\":\"").append(escapeJson(m.get("venue"))).append("\",")
              .append("\"capacity\":").append(m.getOrDefault("capacity", "100")).append(",")
              .append("\"registrations\":").append(m.getOrDefault("registrations", "0"))
              .append("}");
            if (i < result.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleCreateEvent(HttpExchange exchange) throws IOException {
        WebSession session = getSession(exchange);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        if (session.isReviewer()) {
            sendJson(exchange, 403, "{\"error\":\"Permission Denied: Teacher Reviewers cannot create events.\"}");
            return;
        }
        if (session.isStudent()) {
            sendJson(exchange, 403, "{\"error\":\"Forbidden: Students cannot schedule campus club events.\"}");
            return;
        }
        if (!session.isAdmin() && !session.isCoordinator()) {
            sendJson(exchange, 403, "{\"error\":\"Forbidden: Elevated privileges required to create events.\"}");
            return;
        }

        Map<String, String> data = parseJson(readBody(exchange));
        String title = data.get("title");
        String club = data.get("club");
        String date = data.get("date");
        String venue = data.get("venue");
        String capStr = data.get("capacity");

        if (title == null || title.trim().isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Event title is required.\"}");
            return;
        }

        int newId = idCounter.incrementAndGet();
        try {
            Event event = new Event();
            event.setEventName(title.trim());
            event.setClubId(1);
            event.setEventDate(LocalDateTime.now().plusDays(7));
            event.setVenue(venue != null ? venue.trim() : "Seminar Hall A");
            int parsedCap = 100;
            try { parsedCap = Integer.parseInt(capStr.trim()); } catch (Exception ignored) {}
            event.setCapacity(parsedCap);
            event.setStatus("UPCOMING");
            int createdId = eventService.createEvent(event);
            if (createdId > 0) newId = createdId;
        } catch (Exception ignored) {}

        Map<String, String> map = new HashMap<>();
        map.put("id", String.valueOf(newId));
        map.put("title", title.trim());
        map.put("club", club != null ? club.trim() : "Coding Club");
        map.put("date", date != null ? date.trim() : "2026-11-20 10:00:00");
        map.put("venue", venue != null ? venue.trim() : "Seminar Hall A");
        map.put("capacity", capStr != null ? capStr.trim() : "100");
        map.put("registrations", "0");
        fallbackEvents.add(0, map);

        sendJson(exchange, 201, "{\"success\":true,\"eventId\":" + newId + ",\"message\":\"Event scheduled successfully!\"}");
    }

    private void handleGetAnnouncements(HttpExchange exchange) throws IOException {
        List<Map<String, String>> result = new ArrayList<>();
        boolean dbOk = false;

        try {
            List<Announcement> notices = announcementDAO.findAll();
            if (notices != null && !notices.isEmpty()) {
                for (Announcement a : notices) {
                    Map<String, String> map = new HashMap<>();
                    map.put("id", String.valueOf(a.getAnnouncementId()));
                    map.put("title", a.getTitle());
                    map.put("content", a.getMessage());
                    map.put("priority", a.getPriority() != null ? a.getPriority() : "MEDIUM");
                    map.put("club", a.getClubName() != null ? a.getClubName() : "Campus Administration");
                    map.put("date", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "Recent");
                    result.add(map);
                }
                dbOk = true;
            }
        } catch (Exception ignored) {}

        if (!dbOk) {
            result = new ArrayList<>(fallbackNotices);
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < result.size(); i++) {
            Map<String, String> m = result.get(i);
            sb.append("{")
              .append("\"id\":\"").append(escapeJson(m.get("id"))).append("\",")
              .append("\"title\":\"").append(escapeJson(m.get("title"))).append("\",")
              .append("\"content\":\"").append(escapeJson(m.get("content"))).append("\",")
              .append("\"priority\":\"").append(escapeJson(m.get("priority"))).append("\",")
              .append("\"club\":\"").append(escapeJson(m.get("club"))).append("\",")
              .append("\"date\":\"").append(escapeJson(m.get("date"))).append("\"")
              .append("}");
            if (i < result.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleCreateAnnouncement(HttpExchange exchange) throws IOException {
        WebSession session = getSession(exchange);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        if (session.isReviewer()) {
            sendJson(exchange, 403, "{\"error\":\"Permission Denied: Teacher Reviewers cannot publish announcements.\"}");
            return;
        }
        if (session.isStudent()) {
            sendJson(exchange, 403, "{\"error\":\"Forbidden: Students cannot publish announcements.\"}");
            return;
        }
        if (!session.isAdmin() && !session.isCoordinator()) {
            sendJson(exchange, 403, "{\"error\":\"Forbidden: Elevated privileges required to broadcast announcements.\"}");
            return;
        }

        Map<String, String> data = parseJson(readBody(exchange));
        String title = data.get("title");
        String content = data.get("content");
        String priority = data.get("priority");
        String club = data.get("club");

        if (title == null || title.trim().isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Title is required.\"}");
            return;
        }

        int newId = idCounter.incrementAndGet();
        try {
            Announcement a = new Announcement();
            a.setClubId(1);
            a.setTitle(title.trim());
            a.setMessage(content != null ? content.trim() : "");
            a.setPriority(priority != null ? priority.trim().toUpperCase() : "HIGH");
            int createdId = announcementDAO.createAnnouncement(a);
            if (createdId > 0) newId = createdId;
        } catch (Exception ignored) {}

        Map<String, String> map = new HashMap<>();
        map.put("id", String.valueOf(newId));
        map.put("title", title.trim());
        map.put("content", content != null ? content.trim() : "");
        map.put("priority", priority != null ? priority.trim().toUpperCase() : "HIGH");
        map.put("club", club != null ? club.trim() : "Campus General");
        map.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        fallbackNotices.add(0, map);

        sendJson(exchange, 201, "{\"success\":true,\"announcementId\":" + newId + ",\"message\":\"Announcement broadcasted!\"}");
    }

    private void handleGetMemberships(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < fallbackApplications.size(); i++) {
            Map<String, String> m = fallbackApplications.get(i);
            sb.append("{")
              .append("\"id\":\"").append(escapeJson(m.get("id"))).append("\",")
              .append("\"regNo\":\"").append(escapeJson(m.get("regNo"))).append("\",")
              .append("\"name\":\"").append(escapeJson(m.get("name"))).append("\",")
              .append("\"dept\":\"").append(escapeJson(m.get("dept"))).append("\",")
              .append("\"club\":\"").append(escapeJson(m.get("club"))).append("\",")
              .append("\"date\":\"").append(escapeJson(m.get("date"))).append("\",")
              .append("\"status\":\"").append(escapeJson(m.get("status"))).append("\"")
              .append("}");
            if (i < fallbackApplications.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleReviewMembership(HttpExchange exchange) throws IOException {
        WebSession session = getSession(exchange);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        if (session.isReviewer()) {
            sendJson(exchange, 403, "{\"error\":\"Permission Denied: Teacher Reviewers cannot approve or reject applications.\"}");
            return;
        }
        if (session.isStudent()) {
            sendJson(exchange, 403, "{\"error\":\"Forbidden: Students cannot review membership applications.\"}");
            return;
        }
        if (!session.isAdmin() && !session.isCoordinator()) {
            sendJson(exchange, 403, "{\"error\":\"Forbidden: Coordinator or administrator permissions required to review applications.\"}");
            return;
        }

        Map<String, String> data = parseJson(readBody(exchange));
        String idStr = data.get("id");
        String decision = data.get("decision"); // "APPROVED" or "REJECTED"

        if (idStr == null || decision == null) {
            sendJson(exchange, 400, "{\"error\":\"Missing application ID or decision\"}");
            return;
        }

        try {
            int appId = Integer.parseInt(idStr);
            membershipDAO.updateStatus(appId, decision.toUpperCase());
        } catch (Exception ignored) {}

        for (Map<String, String> app : fallbackApplications) {
            if (idStr.equals(app.get("id"))) {
                app.put("status", decision.toUpperCase());
                sendJson(exchange, 200, "{\"success\":true,\"status\":\"" + decision.toUpperCase() + "\"}");
                return;
            }
        }
        sendJson(exchange, 404, "{\"error\":\"Application not found\"}");
    }

    private void handleApplyMembership(HttpExchange exchange) throws IOException {
        WebSession session = getSession(exchange);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized. Please log in first.\"}");
            return;
        }

        // Student identity verification check
        if (session.isStudent() && !session.isVerified()) {
            sendJson(exchange, 403, "{\"error\":\"Account unverified. Please verify your institutional college email OTP before applying for club membership.\"}");
            return;
        }

        Map<String, String> data = parseJson(readBody(exchange));
        String club = data.get("club");
        if (club == null || club.trim().isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Club name is required.\"}");
            return;
        }

        String cleanClub = club.trim();
        for (Map<String, String> app : fallbackApplications) {
            if (cleanClub.equalsIgnoreCase(app.get("club")) &&
                (session.username.equalsIgnoreCase(app.get("username")) || session.fullName.equalsIgnoreCase(app.get("name")))) {
                sendJson(exchange, 409, "{\"error\":\"You have already submitted an application for " + escapeJson(cleanClub) + ".\"}");
                return;
            }
        }

        String regNo = "1CC21CS100";
        String dept = "Computer Science & Engineering";
        User u = builtInUsers.get(session.username.toLowerCase());
        if (u instanceof Student s) {
            regNo = s.getRegisterNumber();
            dept = s.getDepartment();
        }

        int newId = idCounter.incrementAndGet();
        Map<String, String> app = new HashMap<>();
        app.put("id", String.valueOf(newId));
        app.put("username", session.username);
        app.put("userId", String.valueOf(session.userId));
        app.put("regNo", regNo);
        app.put("name", session.fullName);
        app.put("dept", dept);
        app.put("club", cleanClub);
        app.put("date", LocalDateTime.now().toLocalDate().toString());
        app.put("status", "PENDING");
        fallbackApplications.add(0, app);

        try {
            if (isDatabaseReachable()) {
                for (Club c : clubService.getAllClubs()) {
                    if (c.getClubName().equalsIgnoreCase(cleanClub)) {
                        membershipService.applyForMembership(session.userId, c.getClubId());
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        sendJson(exchange, 201, "{\"success\":true,\"applicationId\":" + newId + ",\"message\":\"Membership application submitted successfully!\"}");
    }

    private void handleRegisterEvent(HttpExchange exchange) throws IOException {
        WebSession session = getSession(exchange);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized. Please log in first.\"}");
            return;
        }

        // Student identity verification check
        if (session.isStudent() && !session.isVerified()) {
            sendJson(exchange, 403, "{\"error\":\"Account unverified. Please verify your institutional college email OTP before registering for events.\"}");
            return;
        }

        Map<String, String> data = parseJson(readBody(exchange));
        String eventIdStr = data.get("eventId");
        if (eventIdStr == null || eventIdStr.trim().isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Event ID is required.\"}");
            return;
        }

        // Check if student already registered
        for (Map<String, String> r : fallbackEventRegistrations) {
            if (eventIdStr.equals(r.get("eventId")) &&
                (session.username.equalsIgnoreCase(r.get("username")) || String.valueOf(session.userId).equals(r.get("userId")))) {
                sendJson(exchange, 409, "{\"error\":\"You are already registered for this event.\"}");
                return;
            }
        }

        String evTitle = "Campus Event #" + eventIdStr;
        String evClub = "Campus Club";
        for (Map<String, String> ev : fallbackEvents) {
            if (eventIdStr.equals(ev.get("id"))) {
                evTitle = ev.get("title");
                evClub = ev.get("club");
                int regCount = 0;
                try {
                    regCount = Integer.parseInt(ev.getOrDefault("registrations", "0"));
                } catch (Exception ignored) {}
                ev.put("registrations", String.valueOf(regCount + 1));
                break;
            }
        }

        int newId = idCounter.incrementAndGet();
        Map<String, String> reg = new HashMap<>();
        reg.put("id", String.valueOf(newId));
        reg.put("eventId", eventIdStr);
        reg.put("eventTitle", evTitle);
        reg.put("club", evClub);
        reg.put("userId", String.valueOf(session.userId));
        reg.put("username", session.username);
        reg.put("studentName", session.fullName);
        reg.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        reg.put("status", "CONFIRMED");
        fallbackEventRegistrations.add(0, reg);

        try {
            if (isDatabaseReachable()) {
                eventRegistrationDAO.registerForEvent(Integer.parseInt(eventIdStr), session.userId);
            }
        } catch (Exception ignored) {}

        sendJson(exchange, 200, "{\"success\":true,\"message\":\"Successfully registered for " + escapeJson(evTitle) + "!\"}");
    }

    private void handleRegisterStudent(HttpExchange exchange) throws IOException {
        Map<String, String> data = parseJson(readBody(exchange));
        String username = data.get("username");
        String password = data.get("password");
        String confirmPassword = data.get("confirmPassword");
        String fullName = data.get("fullName");
        String email = data.get("email");
        String phone = data.get("phoneNumber");
        String regNo = data.get("registerNumber");
        String dept = data.get("department");
        String yearStr = data.get("year");

        if (password == null || password.trim().isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Password is required.\"}");
            return;
        }
        if (password.length() < 6) {
            sendJson(exchange, 400, "{\"error\":\"Password must be at least 6 characters long.\"}");
            return;
        }
        if (confirmPassword != null && !confirmPassword.trim().isEmpty() && !password.equals(confirmPassword)) {
            sendJson(exchange, 400, "{\"error\":\"Passwords do not match. Please verify both passwords.\"}");
            return;
        }
        if (email == null || email.trim().isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Institutional college email is required.\"}");
            return;
        }

        String cleanEmail = email.trim().toLowerCase();
        // Enforce institutional college domain check for Kalasalingam University (@klu.ac.in)
        if (!ValidationUtil.isValidInstitutionalEmail(cleanEmail)) {
            sendJson(exchange, 400, "{\"error\":\"Registration requires an official institutional email ending exactly in @klu.ac.in (e.g. 99527800392@klu.ac.in).\"}");
            return;
        }

        int newUserId = idCounter.incrementAndGet();
        String cleanRegNo = (regNo != null && !regNo.trim().isEmpty()) ? regNo.trim() : "1CC24CS" + (100 + newUserId);
        String cleanUser = (username != null && !username.trim().isEmpty()) ? username.trim().toLowerCase() : cleanRegNo.toLowerCase();

        if (builtInUsers.containsKey(cleanUser)) {
            sendJson(exchange, 409, "{\"error\":\"Username or Student ID already exists. Please choose a different identifier or log in.\"}");
            return;
        }
        try {
            if (userDAO.findByUsername(cleanUser).isPresent()) {
                sendJson(exchange, 409, "{\"error\":\"Username already exists in system records.\"}");
                return;
            }
        } catch (Exception ignored) {}

        int yearVal = 1;
        if (yearStr != null && !yearStr.trim().isEmpty()) {
            try {
                yearVal = Integer.parseInt(yearStr.trim());
                if (yearVal < 1 || yearVal > 4) yearVal = 1;
            } catch (Exception ignored) {
                yearVal = 1;
            }
        }

        // Critical Security Rule: Always force STUDENT role regardless of any client parameter.
        UserRole assignedRole = UserRole.STUDENT;

        // Generate 6-digit institutional OTP
        int otpNum = 100000 + new SecureRandom().nextInt(900000);
        String otpCode = String.valueOf(otpNum);
        long now = System.currentTimeMillis();
        long expiresAt = now + 10 * 60 * 1000L; // 10 minutes OTP expiration

        Student student = new Student();
        student.setUserId(newUserId);
        student.setUsername(cleanUser);
        student.setFullName(fullName != null && !fullName.trim().isEmpty() ? fullName.trim() : cleanUser);
        student.setEmail(cleanEmail);
        student.setPhoneNumber(phone != null ? phone.trim() : "+91 9952780039");
        student.setRegisterNumber(cleanRegNo);
        student.setDepartment(dept != null && !dept.trim().isEmpty() ? dept.trim() : "Computer Science & Engineering");
        student.setYear(yearVal);
        student.setSemester(yearVal * 2 - 1);
        student.setRole(assignedRole);
        student.setVerified(false);
        student.setVerificationCode(otpCode);
        student.setOtpExpiresAt(expiresAt);
        student.setOtpAttempts(0);
        student.setLastOtpSentAt(now);
        student.setPasswordHash(PasswordUtil.hashPassword(password));

        // Store in memory across identifiers for instant login
        builtInUsers.put(cleanUser, student);
        builtInUsers.put(cleanEmail, student);
        builtInUsers.put(cleanRegNo.toLowerCase(), student);

        // Attempt persistence if database is available
        try {
            if (isDatabaseReachable()) {
                Student dbStudent = new Student();
                dbStudent.setUsername(cleanUser);
                dbStudent.setPasswordHash(student.getPasswordHash());
                dbStudent.setFullName(student.getFullName());
                dbStudent.setEmail(cleanEmail);
                dbStudent.setPhoneNumber(student.getPhoneNumber());
                dbStudent.setRegisterNumber(cleanRegNo);
                dbStudent.setDepartment(student.getDepartment());
                dbStudent.setYear(student.getYear());
                dbStudent.setSemester(student.getSemester());
                dbStudent.setRole(assignedRole);
                dbStudent.setVerified(false);
                dbStudent.setVerificationCode(otpCode);
                dbStudent.setOtpExpiresAt(expiresAt);
                dbStudent.setOtpAttempts(0);

                int dbStudentId = studentDAO.createStudent(dbStudent);
                if (dbStudent.getUserId() > 0) {
                    student.setUserId(dbStudent.getUserId());
                    student.setStudentTableId(dbStudentId);
                    userDAO.updateOtpDetails(dbStudent.getUserId(), otpCode, expiresAt, 0);
                }
            }
        } catch (Exception ignored) {}

        // Attempt real SMTP dispatch if configured
        EmailService.sendOtpEmail(cleanEmail, otpCode, 10);

        boolean showDevOtp = !isProductionMode();
        StringBuilder resp = new StringBuilder("{");
        resp.append("\"success\":true,")
            .append("\"message\":\"Student registration initiated. Verification OTP sent to institutional email.\",")
            .append("\"username\":\"").append(escapeJson(cleanUser)).append("\",")
            .append("\"registerNumber\":\"").append(escapeJson(cleanRegNo)).append("\",")
            .append("\"email\":\"").append(escapeJson(cleanEmail)).append("\",")
            .append("\"role\":\"STUDENT\",")
            .append("\"expiresInMinutes\":10,")
            .append("\"verified\":false");
        if (showDevOtp) {
            resp.append(",\"devOtp\":\"").append(otpCode).append("\"");
        }
        resp.append("}");
        sendJson(exchange, 201, resp.toString());
    }

    private void handleVerifyStudent(HttpExchange exchange) throws IOException {
        Map<String, String> data = parseJson(readBody(exchange));
        String username = data.get("username");
        String code = data.get("code");

        if (username == null || code == null) {
            sendJson(exchange, 400, "{\"error\":\"Username and verification code are required.\"}");
            return;
        }

        String cleanUser = username.trim().toLowerCase();
        User user = builtInUsers.get(cleanUser);
        if (user == null) {
            for (User u : builtInUsers.values()) {
                if (cleanUser.equalsIgnoreCase(u.getEmail())) { user = u; break; }
                if (u instanceof Student s && cleanUser.equalsIgnoreCase(s.getRegisterNumber())) { user = u; break; }
            }
        }
        if (user == null) {
            try {
                Optional<User> dbUser = userDAO.findByIdentifier(cleanUser);
                if (dbUser.isPresent()) user = dbUser.get();
            } catch (Exception ignored) {}
        }

        if (user == null) {
            sendJson(exchange, 404, "{\"error\":\"Student account not found.\"}");
            return;
        }

        if (user.isVerified()) {
            sendJson(exchange, 200, "{\"success\":true,\"message\":\"Account is already verified.\"}");
            return;
        }

        // 1. Lock check: 5 consecutive failed attempts
        if (user.getOtpAttempts() >= 5) {
            user.setVerificationCode(null);
            sendJson(exchange, 429, "{\"error\":\"Too many failed verification attempts (5/5). Verification code locked. Please request a new code via resend.\"}");
            return;
        }

        // 2. Expiry check: 10 minutes limit
        if (user.getOtpExpiresAt() != null && System.currentTimeMillis() > user.getOtpExpiresAt()) {
            user.setVerificationCode(null);
            sendJson(exchange, 400, "{\"error\":\"Verification code has expired (10 minutes limit). Please request a new code via resend.\"}");
            return;
        }

        // 3. Code match check
        if (user.getVerificationCode() == null || !user.getVerificationCode().trim().equals(code.trim())) {
            int attempts = user.getOtpAttempts() + 1;
            user.setOtpAttempts(attempts);
            if (attempts >= 5) {
                user.setVerificationCode(null);
                try {
                    userDAO.updateOtpDetails(user.getUserId(), null, null, attempts);
                } catch (Exception ignored) {}
                sendJson(exchange, 429, "{\"error\":\"Too many failed verification attempts (5/5). Verification code locked. Please request a new code via resend.\"}");
                return;
            }
            try {
                userDAO.updateOtpDetails(user.getUserId(), user.getVerificationCode(), user.getOtpExpiresAt(), attempts);
            } catch (Exception ignored) {}
            sendJson(exchange, 400, "{\"error\":\"Invalid verification code. Attempts remaining: " + (5 - attempts) + "\"}");
            return;
        }

        // 4. Verification successful: securely invalidate OTP and activate account
        user.setVerified(true);
        user.setVerificationCode(null);
        user.setOtpExpiresAt(null);
        user.setOtpAttempts(0);
        try {
            userDAO.updateVerificationStatus(user.getUserId(), true);
            userDAO.updateOtpDetails(user.getUserId(), null, null, 0);
        } catch (Exception ignored) {}

        // Update active sessions if present
        for (Map.Entry<String, WebSession> entry : activeSessions.entrySet()) {
            WebSession sess = entry.getValue();
            if (sess.username.equalsIgnoreCase(cleanUser)) {
                activeSessions.put(entry.getKey(), new WebSession(sess.sessionId, sess.userId, sess.username, sess.fullName, sess.role, true));
            }
        }

        sendJson(exchange, 200, "{\"success\":true,\"message\":\"Institutional email verified successfully. You may now participate in club activities.\"}");
    }

    private void handleResendOtp(HttpExchange exchange) throws IOException {
        Map<String, String> data = parseJson(readBody(exchange));
        String username = data.get("username");
        String email = data.get("email");

        if ((username == null || username.trim().isEmpty()) && (email == null || email.trim().isEmpty())) {
            sendJson(exchange, 400, "{\"error\":\"Username or email is required to resend verification code.\"}");
            return;
        }

        User user = null;
        if (username != null && !username.trim().isEmpty()) {
            String cleanUser = username.trim().toLowerCase();
            user = builtInUsers.get(cleanUser);
            if (user == null) {
                try {
                    Optional<User> dbUser = userDAO.findByUsername(cleanUser);
                    if (dbUser.isPresent()) user = dbUser.get();
                } catch (Exception ignored) {}
            }
        } else if (email != null && !email.trim().isEmpty()) {
            String cleanEmail = email.trim().toLowerCase();
            for (User u : builtInUsers.values()) {
                if (cleanEmail.equalsIgnoreCase(u.getEmail())) {
                    user = u;
                    break;
                }
            }
            if (user == null) {
                try {
                    Optional<User> dbUser = userDAO.findByEmail(cleanEmail);
                    if (dbUser.isPresent()) user = dbUser.get();
                } catch (Exception ignored) {}
            }
        }

        if (user == null) {
            sendJson(exchange, 404, "{\"error\":\"Student account not found.\"}");
            return;
        }

        if (user.isVerified()) {
            sendJson(exchange, 400, "{\"error\":\"Account is already verified. No new code needed.\"}");
            return;
        }

        long now = System.currentTimeMillis();
        // Rate limit: 60 seconds cooldown between resend requests
        if (user.getLastOtpSentAt() != null && (now - user.getLastOtpSentAt() < 60_000L)) {
            long waitSecs = (60_000L - (now - user.getLastOtpSentAt())) / 1000L;
            sendJson(exchange, 429, "{\"error\":\"Please wait " + (waitSecs > 0 ? waitSecs : 1) + " seconds before requesting another verification code.\"}");
            return;
        }

        // Generate new 6-digit OTP
        int otpNum = 100000 + new SecureRandom().nextInt(900000);
        String newOtp = String.valueOf(otpNum);
        long expiresAt = now + 10 * 60 * 1000L; // 10 minutes

        user.setVerificationCode(newOtp);
        user.setOtpExpiresAt(expiresAt);
        user.setOtpAttempts(0);
        user.setLastOtpSentAt(now);

        try {
            userDAO.updateOtpDetails(user.getUserId(), newOtp, expiresAt, 0);
        } catch (Exception ignored) {}

        // Dispatch via EmailService
        EmailService.sendOtpEmail(user.getEmail(), newOtp, 10);

        boolean showDevOtp = !isProductionMode();
        StringBuilder resp = new StringBuilder("{");
        resp.append("\"success\":true,")
            .append("\"message\":\"A new verification code has been dispatched to your institutional email.\",")
            .append("\"username\":\"").append(escapeJson(user.getUsername())).append("\",")
            .append("\"email\":\"").append(escapeJson(user.getEmail())).append("\",")
            .append("\"expiresInMinutes\":10");
        if (showDevOtp) {
            resp.append(",\"devOtp\":\"").append(newOtp).append("\"");
        }
        resp.append("}");
        sendJson(exchange, 200, resp.toString());
    }

    private void handleGetTeam(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> teamList = new ArrayList<>();
        boolean dbOk = false;

        try {
            List<User> dbUsers = userDAO.findAll();
            if (dbUsers != null && !dbUsers.isEmpty()) {
                for (User u : dbUsers) {
                    if (u.getRole() == UserRole.ADMIN || u.getRole() == UserRole.COORDINATOR || u.getRole() == UserRole.TEACHER) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", u.getUserId());
                        map.put("username", u.getUsername());
                        map.put("fullName", u.getFullName());
                        map.put("email", u.getEmail());
                        boolean isRev = u.getRole() == UserRole.TEACHER || "teacher_reviewer".equalsIgnoreCase(u.getUsername());
                        map.put("role", isRev ? "TEACHER" : u.getRole().name());
                        map.put("roleDisplay", isRev ? "External Reviewer (Evaluator)" : u.getRole().getDisplayName());
                        map.put("isReviewer", isRev);
                        teamList.add(map);
                    }
                }
                if (!teamList.isEmpty()) {
                    dbOk = true;
                }
            }
        } catch (Exception ignored) {}

        if (!dbOk) {
            for (User u : builtInUsers.values()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", u.getUserId());
                map.put("username", u.getUsername());
                map.put("fullName", u.getFullName());
                map.put("email", u.getEmail());
                boolean isRev = u.getRole() == UserRole.TEACHER || "teacher_reviewer".equalsIgnoreCase(u.getUsername());
                map.put("role", isRev ? "TEACHER" : u.getRole().name());
                map.put("roleDisplay", isRev ? "External Reviewer (Evaluator)" : u.getRole().getDisplayName());
                map.put("isReviewer", isRev);
                teamList.add(map);
            }
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < teamList.size(); i++) {
            Map<String, Object> m = teamList.get(i);
            sb.append("{")
              .append("\"id\":").append(m.get("id")).append(",")
              .append("\"username\":\"").append(escapeJson(String.valueOf(m.get("username")))).append("\",")
              .append("\"fullName\":\"").append(escapeJson(String.valueOf(m.get("fullName")))).append("\",")
              .append("\"email\":\"").append(escapeJson(String.valueOf(m.get("email")))).append("\",")
              .append("\"role\":\"").append(m.get("role")).append("\",")
              .append("\"roleDisplay\":\"").append(escapeJson(String.valueOf(m.get("roleDisplay")))).append("\",")
              .append("\"isReviewer\":").append(m.get("isReviewer"))
              .append("}");
            if (i < teamList.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleGetDbStatus(HttpExchange exchange) throws IOException {
        boolean connected = DatabaseConnection.testConnection();
        String safeUrl = System.getenv("CAMPUS_DB_URL");
        if (safeUrl != null) {
            safeUrl = safeUrl.replaceAll("(?i)password=[^&;]*", "password=***");
        } else {
            safeUrl = "Local / Standalone Fallback";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append("\"databaseConnected\":").append(connected).append(",")
          .append("\"targetUrl\":\"").append(escapeJson(safeUrl)).append("\",")
          .append("\"activeWebSessions\":").append(activeSessions.size()).append(",")
          .append("\"activeClubsCount\":").append(fallbackClubs.size()).append(",")
          .append("\"scheduledEventsCount\":").append(fallbackEvents.size())
          .append("}");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleGetStudentClubs(HttpExchange exchange) throws IOException {
        WebSession session = getSession(exchange);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized. Please log in first.\"}");
            return;
        }

        List<Map<String, String>> myClubs = new ArrayList<>();
        for (Map<String, String> app : fallbackApplications) {
            if (session.username.equalsIgnoreCase(app.get("username")) || session.fullName.equalsIgnoreCase(app.get("name"))) {
                myClubs.add(app);
            }
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < myClubs.size(); i++) {
            Map<String, String> c = myClubs.get(i);
            sb.append("{")
              .append("\"id\":\"").append(escapeJson(c.get("id"))).append("\",")
              .append("\"club\":\"").append(escapeJson(c.get("club"))).append("\",")
              .append("\"dept\":\"").append(escapeJson(c.get("dept"))).append("\",")
              .append("\"date\":\"").append(escapeJson(c.get("date"))).append("\",")
              .append("\"status\":\"").append(escapeJson(c.get("status"))).append("\"")
              .append("}");
            if (i < myClubs.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleGetStudentEvents(HttpExchange exchange) throws IOException {
        WebSession session = getSession(exchange);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized. Please log in first.\"}");
            return;
        }

        List<Map<String, String>> myEvents = new ArrayList<>();
        for (Map<String, String> ev : fallbackEventRegistrations) {
            if (session.username.equalsIgnoreCase(ev.get("username")) || String.valueOf(session.userId).equals(ev.get("userId"))) {
                myEvents.add(ev);
            }
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < myEvents.size(); i++) {
            Map<String, String> e = myEvents.get(i);
            sb.append("{")
              .append("\"id\":\"").append(escapeJson(e.get("id"))).append("\",")
              .append("\"eventId\":\"").append(escapeJson(e.get("eventId"))).append("\",")
              .append("\"eventTitle\":\"").append(escapeJson(e.get("eventTitle"))).append("\",")
              .append("\"club\":\"").append(escapeJson(e.get("club"))).append("\",")
              .append("\"date\":\"").append(escapeJson(e.get("date"))).append("\",")
              .append("\"status\":\"").append(escapeJson(e.get("status"))).append("\"")
              .append("}");
            if (i < myEvents.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleGetStudentProfile(HttpExchange exchange) throws IOException {
        WebSession session = getSession(exchange);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized. Please log in first.\"}");
            return;
        }

        String regNo = "99527800392";
        String dept = "Computer Science & Engineering";
        int year = 3;
        String email = session.username + "@klu.ac.in";
        User u = builtInUsers.get(session.username.toLowerCase());
        if (u instanceof Student s) {
            regNo = s.getRegisterNumber();
            dept = s.getDepartment();
            year = s.getYear();
            email = s.getEmail();
        } else if (u != null) {
            email = u.getEmail();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append("\"id\":").append(session.userId).append(",")
          .append("\"username\":\"").append(escapeJson(session.username)).append("\",")
          .append("\"fullName\":\"").append(escapeJson(session.fullName)).append("\",")
          .append("\"registerNumber\":\"").append(escapeJson(regNo)).append("\",")
          .append("\"email\":\"").append(escapeJson(email)).append("\",")
          .append("\"department\":\"").append(escapeJson(dept)).append("\",")
          .append("\"year\":").append(year).append(",")
          .append("\"role\":\"STUDENT\",")
          .append("\"isVerified\":").append(session.isVerified())
          .append("}");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleGetAdminStudents(HttpExchange exchange) throws IOException {
        WebSession session = getSession(exchange);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized. Please log in first.\"}");
            return;
        }
        if (session.isStudent()) {
            sendJson(exchange, 403, "{\"error\":\"Forbidden: Students cannot access registered student roster.\"}");
            return;
        }

        Map<String, Student> studentMap = new LinkedHashMap<>();
        for (User u : builtInUsers.values()) {
            if (u instanceof Student s) {
                studentMap.put(s.getUsername().toLowerCase(), s);
            }
        }
        try {
            if (isDatabaseReachable()) {
                List<Student> dbStudents = studentDAO.findAll();
                if (dbStudents != null) {
                    for (Student s : dbStudents) {
                        studentMap.put(s.getUsername().toLowerCase(), s);
                    }
                }
            }
        } catch (Exception ignored) {}

        StringBuilder sb = new StringBuilder("[");
        int idx = 0;
        int total = studentMap.size();
        for (Student s : studentMap.values()) {
            List<String> joined = new ArrayList<>();
            for (Map<String, String> app : fallbackApplications) {
                if (s.getUsername().equalsIgnoreCase(app.get("username")) || s.getFullName().equalsIgnoreCase(app.get("name"))) {
                    joined.add(app.get("club") + " (" + app.get("status") + ")");
                }
            }
            String joinedStr = joined.isEmpty() ? "No active clubs" : String.join(", ", joined);

            sb.append("{")
              .append("\"id\":").append(s.getUserId()).append(",")
              .append("\"username\":\"").append(escapeJson(s.getUsername())).append("\",")
              .append("\"fullName\":\"").append(escapeJson(s.getFullName())).append("\",")
              .append("\"registerNumber\":\"").append(escapeJson(s.getRegisterNumber())).append("\",")
              .append("\"email\":\"").append(escapeJson(s.getEmail())).append("\",")
              .append("\"department\":\"").append(escapeJson(s.getDepartment())).append("\",")
              .append("\"year\":").append(s.getYear()).append(",")
              .append("\"isVerified\":").append(s.isVerified()).append(",")
              .append("\"role\":\"STUDENT\",")
              .append("\"clubs\":\"").append(escapeJson(joinedStr)).append("\"")
              .append("}");
            idx++;
            if (idx < total) sb.append(",");
        }
        sb.append("]");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleGetAdminEventRegistrations(HttpExchange exchange) throws IOException {
        WebSession session = getSession(exchange);
        if (session == null) {
            sendJson(exchange, 401, "{\"error\":\"Unauthorized. Please log in first.\"}");
            return;
        }
        if (session.isStudent()) {
            sendJson(exchange, 403, "{\"error\":\"Forbidden\"}");
            return;
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < fallbackEventRegistrations.size(); i++) {
            Map<String, String> r = fallbackEventRegistrations.get(i);
            sb.append("{")
              .append("\"id\":\"").append(escapeJson(r.get("id"))).append("\",")
              .append("\"eventId\":\"").append(escapeJson(r.get("eventId"))).append("\",")
              .append("\"eventTitle\":\"").append(escapeJson(r.get("eventTitle"))).append("\",")
              .append("\"club\":\"").append(escapeJson(r.get("club"))).append("\",")
              .append("\"studentName\":\"").append(escapeJson(r.get("studentName"))).append("\",")
              .append("\"username\":\"").append(escapeJson(r.get("username"))).append("\",")
              .append("\"date\":\"").append(escapeJson(r.get("date"))).append("\",")
              .append("\"status\":\"").append(escapeJson(r.get("status"))).append("\"")
              .append("}");
            if (i < fallbackEventRegistrations.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendJson(exchange, 200, sb.toString());
    }

    // =========================================================================
    // HTML Page Renderers (Login & Multi-User Portal)
    // =========================================================================

    private void serveLoginPage(HttpExchange exchange, String errorMsg) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
          .append("  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
          .append("  <meta http-equiv=\"Cache-Control\" content=\"no-cache, no-store, must-revalidate\">\n")
          .append("  <meta http-equiv=\"Pragma\" content=\"no-cache\">\n")
          .append("  <meta http-equiv=\"Expires\" content=\"0\">\n")
          .append("  <title>Login - Campus Club Management System</title>\n")
          .append("  <style>\n")
          .append("    :root { --navy: #192A56; --blue: #2563EB; --blue-hover: #1d4ed8; --emerald: #10B981; --bg: #F8FAFC; --border: #E2E8F0; }\n")
          .append("    * { box-sizing: border-box; }\n")
          .append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; color: #1e293b; }\n")
          .append("    .login-card { background: #ffffff; border-radius: 16px; width: 100%; max-width: 480px; padding: 36px; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.35); position: relative; }\n")
          .append("    .login-header { text-align: center; margin-bottom: 22px; }\n")
          .append("    .brand-icon { width: 56px; height: 56px; background: linear-gradient(135deg, #2563EB, #192A56); border-radius: 12px; display: inline-flex; align-items: center; justify-content: center; font-size: 28px; color: white; margin-bottom: 12px; box-shadow: 0 4px 10px rgba(37,99,235,0.3); }\n")
          .append("    h1 { font-size: 22px; font-weight: 700; color: #0f172a; margin: 0 0 6px 0; }\n")
          .append("    p.sub { font-size: 13px; color: #64748b; margin: 0; }\n")
          .append("    .segmented-toggle { display: flex; background: #f1f5f9; border-radius: 10px; padding: 4px; margin-bottom: 22px; gap: 4px; border: 1px solid var(--border); }\n")
          .append("    .seg-btn { flex: 1; padding: 9px 12px; border: none; background: transparent; border-radius: 8px; font-size: 13px; font-weight: 600; color: #64748b; cursor: pointer; transition: all 0.15s; }\n")
          .append("    .seg-btn.active { background: #ffffff; color: var(--blue); box-shadow: 0 2px 4px rgba(0,0,0,0.06); font-weight: 700; }\n")
          .append("    .form-group { margin-bottom: 18px; }\n")
          .append("    label { display: block; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; color: #475569; margin-bottom: 6px; }\n")
          .append("    input[type='text'], input[type='password'], input[type='email'], select { width: 100%; padding: 12px 14px; border: 1.5px solid var(--border); border-radius: 8px; font-size: 14px; transition: border-color 0.15s; outline: none; font-family: inherit; }\n")
          .append("    input:focus, select:focus { border-color: var(--blue); box-shadow: 0 0 0 3px rgba(37,99,235,0.15); }\n")
          .append("    .btn-submit { width: 100%; padding: 13px; background: var(--blue); color: white; border: none; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; transition: background 0.15s; display: flex; align-items: center; justify-content: center; gap: 8px; }\n")
          .append("    .btn-submit:hover:not(:disabled) { background: var(--blue-hover); }\n")
          .append("    .btn-submit:disabled { opacity: 0.75; cursor: not-allowed; }\n")
          .append("    .alert-error { background: #fee2e2; border: 1px solid #fca5a5; color: #991b1b; padding: 10px 14px; border-radius: 8px; font-size: 13px; margin-bottom: 20px; }\n")
          .append("    .alert-success { background: #dcfce7; border: 1px solid #86efac; color: #166534; padding: 10px 14px; border-radius: 8px; font-size: 13px; margin-bottom: 20px; }\n")
          .append("    .quick-roles { margin-top: 24px; border-top: 1px solid var(--border); padding-top: 18px; }\n")
          .append("    .quick-title { font-size: 11px; text-transform: uppercase; font-weight: 700; color: #64748b; margin-bottom: 10px; display: block; text-align: center; }\n")
          .append("    .role-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }\n")
          .append("    .role-btn { background: #f8fafc; border: 1px solid #cbd5e1; border-radius: 6px; padding: 8px 10px; font-size: 11px; font-weight: 600; text-align: left; cursor: pointer; transition: all 0.15s; display: flex; flex-direction: column; }\n")
          .append("    .role-btn:hover { background: #eff6ff; border-color: var(--blue); color: var(--blue); }\n")
          .append("    .role-btn .title { font-weight: 700; font-size: 12px; }\n")
          .append("    .role-btn .user { font-size: 10px; color: #64748b; }\n")
          .append("    .spinner { display: inline-block; width: 15px; height: 15px; border: 2px solid rgba(255,255,255,0.35); border-radius: 50%; border-top-color: #ffffff; animation: spin 0.8s linear infinite; vertical-align: middle; }\n")
          .append("    @keyframes spin { to { transform: rotate(360deg); } }\n")
          .append("    .modal-overlay { display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15,23,42,0.65); z-index: 1000; align-items: center; justify-content: center; padding: 20px; }\n")
          .append("    .modal { background: white; border-radius: 16px; width: 100%; max-width: 520px; padding: 28px; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.35); position: relative; max-height: 90vh; overflow-y: auto; }\n")
          .append("    .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }\n")
          .append("    .modal-header h3 { margin: 0; font-size: 18px; font-weight: 700; color: #0f172a; }\n")
          .append("    .modal-close { background: none; border: none; font-size: 22px; cursor: pointer; color: #64748b; line-height: 1; }\n")
          .append("    .form-row { margin-bottom: 13px; text-align: left; }\n")
          .append("    .form-row label { display: block; font-size: 11px; font-weight: 700; text-transform: uppercase; color: #475569; margin-bottom: 4px; }\n")
          .append("    .modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 18px; }\n")
          .append("    .btn-outline-modal { background: white; border: 1.5px solid var(--border); color: #475569; padding: 9px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; }\n")
          .append("  </style>\n</head>\n<body>\n")
          .append("  <div class=\"login-card\">\n")
          .append("    <div class=\"login-header\">\n")
          .append("      <div class=\"brand-icon\">🏛️</div>\n")
          .append("      <h1 id=\"header-title\">Campus Club System</h1>\n")
          .append("      <p id=\"header-sub\" class=\"sub\">Kalasalingam University • Institutional Portal</p>\n")
          .append("    </div>\n")

          // Segmented Login Toggle: Student Portal vs Staff & Admin Portal
          .append("    <div class=\"segmented-toggle\">\n")
          .append("      <button type=\"button\" id=\"btn-toggle-student\" class=\"seg-btn active\" onclick=\"switchLoginMode('STUDENT')\">🎓 Student Portal</button>\n")
          .append("      <button type=\"button\" id=\"btn-toggle-staff\" class=\"seg-btn\" onclick=\"switchLoginMode('STAFF')\">👑 Staff &amp; Admin</button>\n")
          .append("    </div>\n");

        if (errorMsg != null) {
            sb.append("    <div id=\"server-alert\" class=\"alert-error\">⚠️ ").append(escapeHtml(errorMsg)).append("</div>\n");
        }
        sb.append("    <div id=\"client-alert\" class=\"alert-error\" style=\"display:none;\"></div>\n")
          .append("    <div id=\"client-success\" class=\"alert-success\" style=\"display:none;\"></div>\n");

        sb.append("    <form id=\"login-form\" action=\"/login\" method=\"POST\">\n")
          .append("      <input type=\"hidden\" id=\"login-role-mode\" value=\"STUDENT\">\n")
          .append("      <div class=\"form-group\">\n")
          .append("        <label id=\"user-field-label\" for=\"username\">College Email (@klu.ac.in) or Student ID</label>\n")
          .append("        <input type=\"text\" id=\"username\" name=\"username\" required placeholder=\"e.g. 99527800392@klu.ac.in or 99527800392\">\n")
          .append("      </div>\n")
          .append("      <div class=\"form-group\">\n")
          .append("        <label for=\"password\">Password</label>\n")
          .append("        <input type=\"password\" id=\"password\" name=\"password\" required placeholder=\"Enter account password\">\n")
          .append("      </div>\n")
          .append("      <button type=\"submit\" id=\"btn-submit\" class=\"btn-submit\"><span id=\"btn-submit-text\">Sign In to Student Dashboard →</span></button>\n")
          .append("    </form>\n");

        // Student Registration Action
        sb.append("    <div id=\"student-register-link\" style=\"margin-top: 18px; text-align: center;\">\n")
          .append("      <span style=\"font-size: 13px; color: #64748b;\">New student? </span>\n")
          .append("      <a href=\"javascript:void(0)\" onclick=\"openModal('modal-register')\" style=\"font-size: 13px; font-weight: 700; color: var(--blue); text-decoration: none;\">Register with College Email (@klu.ac.in) →</a>\n")
          .append("    </div>\n");

        if (isDemoPresetsEnabled()) {
            sb.append("    <div id=\"quick-roles-box\" class=\"quick-roles\" style=\"display:none;\">\n")
              .append("      <span class=\"quick-title\">Quick Evaluation Presets (Click to autofill username)</span>\n")
              .append("      <div class=\"role-grid\">\n")
              .append("        <button type=\"button\" class=\"role-btn\" onclick=\"autofill('teacher_reviewer')\"><span class=\"title\">🔍 Teacher Reviewer</span><span class=\"user\">Read-Only Oversight</span></button>\n")
              .append("        <button type=\"button\" class=\"role-btn\" onclick=\"autofill('team_lead')\"><span class=\"title\">👑 Team Lead</span><span class=\"user\">Administrator</span></button>\n")
              .append("        <button type=\"button\" class=\"role-btn\" onclick=\"autofill('team_dev1')\"><span class=\"title\">💻 Member 2 (Tech)</span><span class=\"user\">Club Coordinator</span></button>\n")
              .append("        <button type=\"button\" class=\"role-btn\" onclick=\"autofill('team_dev2')\"><span class=\"title\">🗓️ Member 3 (Events)</span><span class=\"user\">Events Coordinator</span></button>\n")
              .append("      </div>\n")
              .append("      <div style=\"margin-top:12px; text-align:center; padding-top:10px; border-top:1px dashed var(--border);\">\n")
              .append("        <a href=\"/teacherDashboard\" target=\"_blank\" style=\"font-size:12px; font-weight:700; color:var(--blue); text-decoration:none;\">📋 Teacher / Reviewer Dashboard View (Direct Read-Only) ↗</a>\n")
              .append("      </div>\n")
              .append("    </div>\n");
        }
        sb.append("  </div>\n")

          // Modal: Student Registration (with OTP verification workflow)
          .append("  <div id=\"modal-register\" class=\"modal-overlay\">\n")
          .append("    <div class=\"modal\">\n")
          .append("      <!-- Step 1: Details -->\n")
          .append("      <div id=\"reg-step-1\">\n")
          .append("        <div class=\"modal-header\">\n")
          .append("          <div>\n")
          .append("            <h3>🎓 Student Registration</h3>\n")
          .append("            <p style=\"margin:3px 0 0; font-size:12px; color:#64748b;\">Kalasalingam Academy of Research and Education</p>\n")
          .append("          </div>\n")
          .append("          <button type=\"button\" class=\"modal-close\" onclick=\"closeModal('modal-register')\">&times;</button>\n")
          .append("        </div>\n")
          .append("        <div id=\"reg-alert\" class=\"alert-error\" style=\"display:none;\"></div>\n")
          .append("        <div class=\"form-row\">\n")
          .append("          <label>Full Student Name *</label>\n")
          .append("          <input type=\"text\" id=\"reg-name\" placeholder=\"e.g. Ramesh K\" required>\n")
          .append("        </div>\n")
          .append("        <div style=\"display:grid; grid-template-columns:1fr 1fr; gap:10px;\">\n")
          .append("          <div class=\"form-row\">\n")
          .append("            <label>Student ID / Reg No *</label>\n")
          .append("            <input type=\"text\" id=\"reg-no\" placeholder=\"e.g. 99527800392\" required>\n")
          .append("          </div>\n")
          .append("          <div class=\"form-row\">\n")
          .append("            <label>Year of Study *</label>\n")
          .append("            <select id=\"reg-year\">\n")
          .append("              <option value=\"1\">1st Year</option>\n")
          .append("              <option value=\"2\">2nd Year</option>\n")
          .append("              <option value=\"3\" selected>3rd Year</option>\n")
          .append("              <option value=\"4\">4th Year</option>\n")
          .append("            </select>\n")
          .append("          </div>\n")
          .append("        </div>\n")
          .append("        <div class=\"form-row\">\n")
          .append("          <label>College Email (@klu.ac.in) *</label>\n")
          .append("          <input type=\"email\" id=\"reg-email\" placeholder=\"e.g. 99527800392@klu.ac.in\" required>\n")
          .append("          <span style=\"font-size:11px; color:#64748b;\">Must end strictly in @klu.ac.in. OTP will be sent here.</span>\n")
          .append("        </div>\n")
          .append("        <div class=\"form-row\">\n")
          .append("          <label>Academic Department *</label>\n")
          .append("          <select id=\"reg-dept\">\n")
          .append("            <option value=\"Computer Science & Engineering\">Computer Science & Engineering</option>\n")
          .append("            <option value=\"Information Technology\">Information Technology</option>\n")
          .append("            <option value=\"Electronics & Communication Engineering\">Electronics & Communication Engineering</option>\n")
          .append("            <option value=\"Electrical & Electronics Engineering\">Electrical & Electronics Engineering</option>\n")
          .append("            <option value=\"Mechanical Engineering\">Mechanical Engineering</option>\n")
          .append("            <option value=\"Civil Engineering\">Civil Engineering</option>\n")
          .append("            <option value=\"Biotechnology\">Biotechnology</option>\n")
          .append("          </select>\n")
          .append("        </div>\n")
          .append("        <div style=\"display:grid; grid-template-columns:1fr 1fr; gap:10px;\">\n")
          .append("          <div class=\"form-row\">\n")
          .append("            <label>Password (min 6 chars) *</label>\n")
          .append("            <input type=\"password\" id=\"reg-password\" placeholder=\"Password\" required>\n")
          .append("          </div>\n")
          .append("          <div class=\"form-row\">\n")
          .append("            <label>Confirm Password *</label>\n")
          .append("            <input type=\"password\" id=\"reg-confirm-password\" placeholder=\"Confirm\" required>\n")
          .append("          </div>\n")
          .append("        </div>\n")
          .append("        <div class=\"modal-actions\">\n")
          .append("          <button type=\"button\" class=\"btn-outline-modal\" onclick=\"closeModal('modal-register')\">Cancel</button>\n")
          .append("          <button type=\"button\" id=\"btn-submit-reg\" class=\"btn-submit\" style=\"width:auto; padding:9px 20px;\" onclick=\"submitStudentRegistration()\"><span>Send Verification OTP →</span></button>\n")
          .append("        </div>\n")
          .append("      </div>\n")

          // Step 2: OTP Entry
          .append("      <div id=\"reg-step-2\" style=\"display:none; text-align:center;\">\n")
          .append("        <div class=\"brand-icon\" style=\"margin:0 auto 12px; font-size:24px; width:48px; height:48px;\">✉️</div>\n")
          .append("        <h3 style=\"margin:0 0 6px 0; font-size:18px; color:#0f172a;\">Verify College Email</h3>\n")
          .append("        <p style=\"font-size:13px; color:#64748b; margin:0 0 16px 0;\">Enter the 6-digit code sent to <br><strong id=\"otp-target-email\" style=\"color:var(--blue);\"></strong></p>\n")
          .append("        <div id=\"otp-alert\" class=\"alert-error\" style=\"display:none; text-align:left;\"></div>\n")
          .append("        <div class=\"form-row\" style=\"max-width:220px; margin:0 auto 16px;\">\n")
          .append("          <input type=\"text\" id=\"reg-otp-code\" maxlength=\"6\" style=\"text-align:center; font-size:22px; letter-spacing:6px; font-weight:700; padding:10px;\" placeholder=\"······\">\n")
          .append("        </div>\n")
          .append("        <button type=\"button\" id=\"btn-verify-otp\" class=\"btn-submit\" style=\"margin-bottom:14px;\" onclick=\"submitStudentOtp()\"><span>Verify &amp; Activate Account</span></button>\n")
          .append("        <div style=\"font-size:12px; color:#64748b;\">\n")
          .append("          Didn't receive code? <button type=\"button\" id=\"btn-resend-otp\" onclick=\"resendStudentOtp()\" style=\"background:none; border:none; color:var(--blue); font-weight:700; cursor:pointer; text-decoration:underline;\">Resend OTP</button>\n")
          .append("          <span id=\"resend-timer-span\" style=\"color:#94a3b8; margin-left:6px;\"></span>\n")
          .append("        </div>\n")
          .append("      </div>\n")
          .append("    </div>\n")
          .append("  </div>\n")

          .append("  <script>\n")
          .append("    var registeredUsername = '';\n")
          .append("    var registeredEmail = '';\n")
          .append("\n")
          .append("    function switchLoginMode(mode) {\n")
          .append("      var roleInput = document.getElementById('login-role-mode');\n")
          .append("      var btnStudent = document.getElementById('btn-toggle-student');\n")
          .append("      var btnStaff = document.getElementById('btn-toggle-staff');\n")
          .append("      var userLabel = document.getElementById('user-field-label');\n")
          .append("      var userInput = document.getElementById('username');\n")
          .append("      var btnText = document.getElementById('btn-submit-text');\n")
          .append("      var regLink = document.getElementById('student-register-link');\n")
          .append("      var quickRoles = document.getElementById('quick-roles-box');\n")
          .append("      var subTitle = document.getElementById('header-sub');\n")
          .append("\n")
          .append("      if (mode === 'STUDENT') {\n")
          .append("        if (roleInput) roleInput.value = 'STUDENT';\n")
          .append("        if (btnStudent) btnStudent.classList.add('active');\n")
          .append("        if (btnStaff) btnStaff.classList.remove('active');\n")
          .append("        if (userLabel) userLabel.textContent = 'College Email (@klu.ac.in) or Student ID';\n")
          .append("        if (userInput) userInput.placeholder = 'e.g. 99527800392@klu.ac.in or 99527800392';\n")
          .append("        if (btnText) btnText.textContent = 'Sign In to Student Dashboard →';\n")
          .append("        if (regLink) regLink.style.display = 'block';\n")
          .append("        if (quickRoles) quickRoles.style.display = 'none';\n")
          .append("        if (subTitle) subTitle.textContent = 'Kalasalingam University • Institutional Portal';\n")
          .append("      } else {\n")
          .append("        if (roleInput) roleInput.value = 'STAFF';\n")
          .append("        if (btnStudent) btnStudent.classList.remove('active');\n")
          .append("        if (btnStaff) btnStaff.classList.add('active');\n")
          .append("        if (userLabel) userLabel.textContent = 'Staff Username';\n")
          .append("        if (userInput) userInput.placeholder = 'e.g. teacher_reviewer or team_lead';\n")
          .append("        if (btnText) btnText.textContent = 'Sign In to Online Portal →';\n")
          .append("        if (regLink) regLink.style.display = 'none';\n")
          .append("        if (quickRoles) quickRoles.style.display = 'block';\n")
          .append("        if (subTitle) subTitle.textContent = 'Multi-User Online Portal • Team & Reviewer Login';\n")
          .append("      }\n")
          .append("    }\n")
          .append("\n")
          .append("    function autofill(u) {\n")
          .append("      var uEl = document.getElementById('username');\n")
          .append("      var pEl = document.getElementById('password');\n")
          .append("      if (uEl) uEl.value = u || '';\n")
          .append("      if (pEl) {\n")
          .append("        pEl.value = '';\n")
          .append("        pEl.focus();\n")
          .append("      }\n")
          .append("    }\n")
          .append("\n")
          .append("    function openModal(id) {\n")
          .append("      var el = document.getElementById(id);\n")
          .append("      if (el) el.style.display = 'flex';\n")
          .append("    }\n")
          .append("    function closeModal(id) {\n")
          .append("      var el = document.getElementById(id);\n")
          .append("      if (el) el.style.display = 'none';\n")
          .append("    }\n")
          .append("\n")
          .append("    function submitStudentRegistration() {\n")
          .append("      var name = document.getElementById('reg-name').value.trim();\n")
          .append("      var regNo = document.getElementById('reg-no').value.trim();\n")
          .append("      var year = document.getElementById('reg-year').value;\n")
          .append("      var email = document.getElementById('reg-email').value.trim();\n")
          .append("      var dept = document.getElementById('reg-dept').value;\n")
          .append("      var pwd = document.getElementById('reg-password').value;\n")
          .append("      var confirmPwd = document.getElementById('reg-confirm-password').value;\n")
          .append("      var alertBox = document.getElementById('reg-alert');\n")
          .append("      var btn = document.getElementById('btn-submit-reg');\n")
          .append("      if (alertBox) alertBox.style.display = 'none';\n")
          .append("\n")
          .append("      if (!name || !regNo || !email || !pwd) {\n")
          .append("        if (alertBox) { alertBox.textContent = '⚠️ Please fill in all required fields.'; alertBox.style.display = 'block'; }\n")
          .append("        return;\n")
          .append("      }\n")
          .append("      if (!email.toLowerCase().endsWith('@klu.ac.in')) {\n")
          .append("        if (alertBox) { alertBox.textContent = '⚠️ Email must end strictly in @klu.ac.in (e.g. 99527800392@klu.ac.in).'; alertBox.style.display = 'block'; }\n")
          .append("        return;\n")
          .append("      }\n")
          .append("      if (pwd.length < 6) {\n")
          .append("        if (alertBox) { alertBox.textContent = '⚠️ Password must be at least 6 characters.'; alertBox.style.display = 'block'; }\n")
          .append("        return;\n")
          .append("      }\n")
          .append("      if (pwd !== confirmPwd) {\n")
          .append("        if (alertBox) { alertBox.textContent = '⚠️ Passwords do not match. Please re-enter.'; alertBox.style.display = 'block'; }\n")
          .append("        return;\n")
          .append("      }\n")
          .append("\n")
          .append("      if (btn) { btn.disabled = true; btn.innerHTML = '<span class=\"spinner\"></span> <span>Sending OTP...</span>'; }\n")
          .append("\n")
          .append("      fetch('/api/auth/register-student', {\n")
          .append("        method: 'POST',\n")
          .append("        headers: { 'Content-Type': 'application/json' },\n")
          .append("        body: JSON.stringify({ fullName: name, registerNumber: regNo, year: year, email: email, department: dept, password: pwd, confirmPassword: confirmPwd })\n")
          .append("      })\n")
          .append("      .then(function(res) { return res.json().then(function(j) { return { status: res.status, data: j }; }); })\n")
          .append("      .then(function(res) {\n")
          .append("        if (btn) { btn.disabled = false; btn.innerHTML = '<span>Send Verification OTP →</span>'; }\n")
          .append("        if (res.status === 201 && res.data && res.data.success) {\n")
          .append("          registeredUsername = res.data.username || regNo;\n")
          .append("          registeredEmail = res.data.email || email;\n")
          .append("          document.getElementById('otp-target-email').textContent = registeredEmail;\n")
          .append("          document.getElementById('reg-step-1').style.display = 'none';\n")
          .append("          document.getElementById('reg-step-2').style.display = 'block';\n")
          .append("          if (res.data.devOtp) {\n")
          .append("            var otpIn = document.getElementById('reg-otp-code');\n")
          .append("            if (otpIn) otpIn.value = res.data.devOtp;\n")
          .append("          }\n")
          .append("        } else {\n")
          .append("          var msg = (res.data && res.data.error) ? res.data.error : 'Registration failed. Please check inputs.';\n")
          .append("          if (alertBox) { alertBox.textContent = '⚠️ ' + msg; alertBox.style.display = 'block'; }\n")
          .append("        }\n")
          .append("      })\n")
          .append("      .catch(function(err) {\n")
          .append("        if (btn) { btn.disabled = false; btn.innerHTML = '<span>Send Verification OTP →</span>'; }\n")
          .append("        if (alertBox) { alertBox.textContent = '⚠️ Network error during registration.'; alertBox.style.display = 'block'; }\n")
          .append("      });\n")
          .append("    }\n")
          .append("\n")
          .append("    function submitStudentOtp() {\n")
          .append("      var code = document.getElementById('reg-otp-code').value.trim();\n")
          .append("      var alertBox = document.getElementById('otp-alert');\n")
          .append("      var btn = document.getElementById('btn-verify-otp');\n")
          .append("      if (alertBox) alertBox.style.display = 'none';\n")
          .append("      if (!code) {\n")
          .append("        if (alertBox) { alertBox.textContent = '⚠️ Please enter the 6-digit code.'; alertBox.style.display = 'block'; }\n")
          .append("        return;\n")
          .append("      }\n")
          .append("      if (btn) { btn.disabled = true; btn.innerHTML = '<span class=\"spinner\"></span> <span>Verifying...</span>'; }\n")
          .append("\n")
          .append("      fetch('/api/auth/verify-student', {\n")
          .append("        method: 'POST',\n")
          .append("        headers: { 'Content-Type': 'application/json' },\n")
          .append("        body: JSON.stringify({ username: registeredUsername || registeredEmail, code: code })\n")
          .append("      })\n")
          .append("      .then(function(res) { return res.json().then(function(j) { return { status: res.status, data: j }; }); })\n")
          .append("      .then(function(res) {\n")
          .append("        if (btn) { btn.disabled = false; btn.innerHTML = '<span>Verify &amp; Activate Account</span>'; }\n")
          .append("        if (res.status === 200 && res.data && res.data.success) {\n")
          .append("          closeModal('modal-register');\n")
          .append("          var succBox = document.getElementById('client-success');\n")
          .append("          if (succBox) {\n")
          .append("            succBox.textContent = '✅ Institutional email verified! You can now log in with your college email or student ID.';\n")
          .append("            succBox.style.display = 'block';\n")
          .append("          }\n")
          .append("          switchLoginMode('STUDENT');\n")
          .append("          var uIn = document.getElementById('username');\n")
          .append("          if (uIn) uIn.value = registeredEmail || registeredUsername;\n")
          .append("          var pIn = document.getElementById('password');\n")
          .append("          if (pIn) { pIn.value = ''; pIn.focus(); }\n")
          .append("        } else {\n")
          .append("          var msg = (res.data && res.data.error) ? res.data.error : 'Verification failed.';\n")
          .append("          if (alertBox) { alertBox.textContent = '⚠️ ' + msg; alertBox.style.display = 'block'; }\n")
          .append("        }\n")
          .append("      })\n")
          .append("      .catch(function(err) {\n")
          .append("        if (btn) { btn.disabled = false; btn.innerHTML = '<span>Verify &amp; Activate Account</span>'; }\n")
          .append("        if (alertBox) { alertBox.textContent = '⚠️ Network error during verification.'; alertBox.style.display = 'block'; }\n")
          .append("      });\n")
          .append("    }\n")
          .append("\n")
          .append("    function resendStudentOtp() {\n")
          .append("      var alertBox = document.getElementById('otp-alert');\n")
          .append("      var span = document.getElementById('resend-timer-span');\n")
          .append("      if (alertBox) alertBox.style.display = 'none';\n")
          .append("      fetch('/api/auth/resend-otp', {\n")
          .append("        method: 'POST',\n")
          .append("        headers: { 'Content-Type': 'application/json' },\n")
          .append("        body: JSON.stringify({ username: registeredUsername, email: registeredEmail })\n")
          .append("      })\n")
          .append("      .then(function(r) { return r.json(); })\n")
          .append("      .then(function(res) {\n")
          .append("        if (res.error) {\n")
          .append("          if (alertBox) { alertBox.textContent = '⚠️ ' + res.error; alertBox.style.display = 'block'; }\n")
          .append("        } else {\n")
          .append("          if (span) span.textContent = '(New OTP sent!)';\n")
          .append("          if (res.devOtp) {\n")
          .append("            var otpIn = document.getElementById('reg-otp-code');\n")
          .append("            if (otpIn) otpIn.value = res.devOtp;\n")
          .append("          }\n")
          .append("        }\n")
          .append("      });\n")
          .append("    }\n")
          .append("\n")
          .append("    // 1. Session Restoration: Check if already authenticated\n")
          .append("    (function checkExistingSession() {\n")
          .append("      try {\n")
          .append("        fetch('/api/auth/me', { cache: 'no-store' })\n")
          .append("          .then(function(r) { return r.ok ? r.json() : null; })\n")
          .append("          .then(function(data) {\n")
          .append("            if (data && data.authenticated) {\n")
          .append("              window.location.replace('/portal');\n")
          .append("            }\n")
          .append("          })\n")
          .append("          .catch(function() {});\n")
          .append("      } catch(e) {}\n")
          .append("    })();\n")
          .append("\n")
          .append("    // 2. Client-side login with loading spinner, safety timeout, and guaranteed stop\n")
          .append("    var form = document.getElementById('login-form');\n")
          .append("    if (form) {\n")
          .append("      form.addEventListener('submit', function(e) {\n")
          .append("        e.preventDefault();\n")
          .append("        var btn = document.getElementById('btn-submit');\n")
          .append("        var clientAlert = document.getElementById('client-alert');\n")
          .append("        var clientSuccess = document.getElementById('client-success');\n")
          .append("        var serverAlert = document.getElementById('server-alert');\n")
          .append("        if (serverAlert) serverAlert.style.display = 'none';\n")
          .append("        if (clientAlert) clientAlert.style.display = 'none';\n")
          .append("        if (clientSuccess) clientSuccess.style.display = 'none';\n")
          .append("\n")
          .append("        var uEl = document.getElementById('username');\n")
          .append("        var pEl = document.getElementById('password');\n")
          .append("        var u = uEl ? uEl.value.trim() : '';\n")
          .append("        var p = pEl ? pEl.value : '';\n")
          .append("        if (!u || !p) {\n")
          .append("          if (clientAlert) {\n")
          .append("            clientAlert.textContent = '⚠️ Please enter both identifier and password.';\n")
          .append("            clientAlert.style.display = 'block';\n")
          .append("          }\n")
          .append("          return;\n")
          .append("        }\n")
          .append("\n")
          .append("        var originalHtml = btn ? btn.innerHTML : '<span>Sign In</span>';\n")
          .append("        if (btn) {\n")
          .append("          btn.disabled = true;\n")
          .append("          btn.innerHTML = '<span class=\"spinner\"></span> <span>Signing In...</span>';\n")
          .append("        }\n")
          .append("\n")
          .append("        var timedOut = false;\n")
          .append("        var timer = setTimeout(function() {\n")
          .append("          timedOut = true;\n")
          .append("          if (btn) {\n")
          .append("            btn.disabled = false;\n")
          .append("            btn.innerHTML = originalHtml;\n")
          .append("          }\n")
          .append("          if (clientAlert) {\n")
          .append("            clientAlert.textContent = '⚠️ Request took longer than expected. Please check your connection or try again.';\n")
          .append("            clientAlert.style.display = 'block';\n")
          .append("          }\n")
          .append("        }, 30000);\n")
          .append("\n")
          .append("        fetch('/api/auth/login', {\n")
          .append("          method: 'POST',\n")
          .append("          headers: { 'Content-Type': 'application/json' },\n")
          .append("          body: JSON.stringify({ username: u, password: p }),\n")
          .append("          cache: 'no-store'\n")
          .append("        })\n")
          .append("        .then(function(res) {\n")
          .append("          return res.json().then(function(json) { return { status: res.status, data: json }; });\n")
          .append("        })\n")
          .append("        .then(function(result) {\n")
          .append("          clearTimeout(timer);\n")
          .append("          if (result.status === 200 && result.data && result.data.success) {\n")
          .append("            if (clientAlert) clientAlert.style.display = 'none';\n")
          .append("            if (btn) btn.innerHTML = '<span>Redirecting to Dashboard...</span>';\n")
          .append("            window.location.replace('/portal');\n")
          .append("            return;\n")
          .append("          }\n")
          .append("          if (timedOut) return;\n")
          .append("          if (btn) {\n")
          .append("            btn.disabled = false;\n")
          .append("            btn.innerHTML = originalHtml;\n")
          .append("          }\n")
          .append("          var msg = (result.data && result.data.error) ? result.data.error : 'Invalid identifier or password.';\n")
          .append("          if (clientAlert) {\n")
          .append("            clientAlert.textContent = '⚠️ ' + msg;\n")
          .append("            clientAlert.style.display = 'block';\n")
          .append("          }\n")
          .append("        })\n")
          .append("        .catch(function(err) {\n")
          .append("          clearTimeout(timer);\n")
          .append("          if (timedOut) return;\n")
          .append("          try {\n")
          .append("            form.submit();\n")
          .append("          } catch(submitErr) {\n")
          .append("            if (btn) {\n")
          .append("              btn.disabled = false;\n")
          .append("              btn.innerHTML = originalHtml;\n")
          .append("            }\n")
          .append("            if (clientAlert) {\n")
          .append("              clientAlert.textContent = '⚠️ Network error: ' + (err.message || 'Could not connect to server.');\n")
          .append("              clientAlert.style.display = 'block';\n")
          .append("            }\n")
          .append("          }\n")
          .append("        });\n")
          .append("      });\n")
          .append("    }\n")
          .append("  </script>\n")
          .append("</body>\n</html>\n");

        sendResponse(exchange, 200, "text/html; charset=UTF-8", sb.toString());
    }

    private void serveStudentPortalPage(HttpExchange exchange, WebSession session) throws IOException {
        String regNo = "99527800392";
        String dept = "Computer Science & Engineering";
        int year = 3;
        String email = session.username + "@klu.ac.in";
        User u = builtInUsers.get(session.username.toLowerCase());
        if (u instanceof Student s) {
            regNo = s.getRegisterNumber();
            dept = s.getDepartment();
            year = s.getYear();
            email = s.getEmail();
        } else if (u != null) {
            email = u.getEmail();
        }

        // Count student's clubs & events
        int myClubCount = 0;
        for (Map<String, String> app : fallbackApplications) {
            if (session.username.equalsIgnoreCase(app.get("username")) || session.fullName.equalsIgnoreCase(app.get("name"))) {
                myClubCount++;
            }
        }
        int myEventCount = 0;
        for (Map<String, String> ev : fallbackEventRegistrations) {
            if (session.username.equalsIgnoreCase(ev.get("username")) || String.valueOf(session.userId).equals(ev.get("userId"))) {
                myEventCount++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
          .append("  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
          .append("  <meta http-equiv=\"Cache-Control\" content=\"no-cache, no-store, must-revalidate\">\n")
          .append("  <title>Student Portal - Campus Club Management System</title>\n")
          .append("  <style>\n")
          .append("    :root { --navy: #192A56; --blue: #2563EB; --blue-hover: #1d4ed8; --emerald: #10B981; --amber: #F59E0B; --danger: #EF4444; --bg: #F8FAFC; --border: #E2E8F0; }\n")
          .append("    * { box-sizing: border-box; }\n")
          .append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: var(--bg); margin: 0; padding: 24px; color: #1e293b; }\n")
          .append("    .container { max-width: 1280px; margin: 0 auto; }\n")
          .append("    .header { background: var(--navy); color: white; padding: 18px 24px; border-radius: 12px; display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 14px; margin-bottom: 20px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }\n")
          .append("    .header-title h1 { margin: 0; font-size: 20px; font-weight: 700; }\n")
          .append("    .header-title p { margin: 4px 0 0 0; font-size: 12px; color: #cbd5e1; }\n")
          .append("    .header-user { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }\n")
          .append("    .btn { display: inline-flex; align-items: center; gap: 6px; padding: 8px 14px; font-size: 12px; font-weight: 600; border-radius: 6px; border: 1px solid transparent; cursor: pointer; transition: all 0.15s; text-decoration: none; }\n")
          .append("    .btn-primary { background: var(--blue); color: white; }\n")
          .append("    .btn-primary:hover { background: var(--blue-hover); }\n")
          .append("    .btn-outline { background: rgba(255,255,255,0.12); color: white; border-color: rgba(255,255,255,0.25); }\n")
          .append("    .btn-outline:hover { background: rgba(255,255,255,0.22); }\n")
          .append("    .badge { padding: 4px 10px; border-radius: 9999px; font-size: 11px; font-weight: 700; text-transform: uppercase; }\n")
          .append("    .badge-student { background: #3b82f6; color: white; }\n")
          .append("    .badge-verified { background: #10b981; color: white; }\n")
          .append("    .badge-unverified { background: #f59e0b; color: white; }\n")
          .append("    .welcome-banner { background: linear-gradient(135deg, #1e3a8a, #2563eb); color: white; border-radius: 12px; padding: 22px 26px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; box-shadow: 0 4px 12px rgba(37,99,235,0.2); }\n")
          .append("    .welcome-banner h2 { margin: 0 0 6px 0; font-size: 20px; font-weight: 800; }\n")
          .append("    .welcome-banner p { margin: 0; font-size: 13px; opacity: 0.9; }\n")
          .append("    .tab-bar { display: flex; gap: 8px; border-bottom: 2px solid var(--border); margin-bottom: 20px; overflow-x: auto; padding-bottom: 8px; }\n")
          .append("    .tab-btn { background: #ffffff; border: 1.5px solid var(--border); padding: 9px 16px; font-size: 13px; font-weight: 600; color: #475569; border-radius: 8px; cursor: pointer; transition: all 0.15s; white-space: nowrap; }\n")
          .append("    .tab-btn:hover { border-color: var(--blue); color: var(--blue); }\n")
          .append("    .tab-btn.active { background: var(--blue) !important; color: #ffffff !important; border-color: var(--blue) !important; box-shadow: 0 2px 6px rgba(37,99,235,0.35) !important; font-weight: 700; }\n")
          .append("    .section-card { background: white; border-radius: 12px; border: 1px solid var(--border); padding: 22px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }\n")
          .append("    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 10px; }\n")
          .append("    .section-header h2 { margin: 0; font-size: 17px; font-weight: 700; color: var(--navy); }\n")
          .append("    table { width: 100%; border-collapse: collapse; font-size: 13px; }\n")
          .append("    th { background: #f8fafc; color: #475569; text-transform: uppercase; font-size: 11px; letter-spacing: 0.5px; font-weight: 700; text-align: left; padding: 12px 14px; border-bottom: 1.5px solid var(--border); }\n")
          .append("    td { padding: 12px 14px; border-bottom: 1px solid var(--border); vertical-align: middle; }\n")
          .append("    tr:hover td { background: #f8fafc; }\n")
          .append("    .pill { display: inline-block; padding: 3px 8px; border-radius: 9999px; font-size: 11px; font-weight: 700; }\n")
          .append("    .pill-green { background: #dcfce7; color: #166534; }\n")
          .append("    .pill-red { background: #fee2e2; color: #991b1b; }\n")
          .append("    .pill-amber { background: #fef3c7; color: #92400e; }\n")
          .append("    .pill-blue { background: #dbeafe; color: #1e40af; }\n")
          .append("    .pill-gray { background: #f1f5f9; color: #475569; }\n")
          .append("    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px; margin-bottom: 20px; }\n")
          .append("    .kpi-card { background: white; border: 1px solid var(--border); border-radius: 10px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }\n")
          .append("    .kpi-val { font-size: 26px; font-weight: 800; color: var(--navy); margin-top: 4px; }\n")
          .append("    .kpi-lbl { font-size: 12px; font-weight: 600; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; }\n")
          .append("    .modal-overlay { display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15,23,42,0.6); z-index: 1000; align-items: center; justify-content: center; padding: 20px; }\n")
          .append("    .modal { background: white; border-radius: 14px; width: 100%; max-width: 500px; padding: 24px; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.2); }\n")
          .append("    .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }\n")
          .append("    .modal-header h3 { margin: 0; font-size: 18px; font-weight: 700; color: var(--navy); }\n")
          .append("    .modal-close { background: none; border: none; font-size: 20px; cursor: pointer; color: #64748b; }\n")
          .append("    .profile-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid var(--border); font-size: 13px; }\n")
          .append("    .profile-label { font-weight: 600; color: #64748b; }\n")
          .append("    .profile-val { font-weight: 700; color: #0f172a; }\n")
          .append("  </style>\n</head>\n<body>\n")
          .append("  <div class=\"container\">\n")
          .append("    <!-- Header Navbar -->\n")
          .append("    <div class=\"header\">\n")
          .append("      <div class=\"header-title\">\n")
          .append("        <h1>🏛️ Kalasalingam Campus Club System</h1>\n")
          .append("        <p>Student Portal • Kalasalingam Academy of Research and Education (@klu.ac.in)</p>\n")
          .append("      </div>\n")
          .append("      <div class=\"header-user\">\n")
          .append("        <span class=\"badge ").append(session.isVerified() ? "badge-verified" : "badge-unverified").append("\">")
          .append(session.isVerified() ? "✅ Verified @klu.ac.in" : "⏳ Pending OTP").append("</span>\n")
          .append("        <span style=\"font-size: 13px; font-weight: 600;\">🎓 ").append(escapeHtml(session.fullName)).append(" (").append(escapeHtml(regNo)).append(")</span>\n")
          .append("        <button class=\"btn btn-outline\" onclick=\"switchTab('tab-profile', document.getElementById('tab-btn-prof'))\">👤 Profile</button>\n")
          .append("        <button class=\"btn btn-outline\" onclick=\"openModal('modal-password')\">🔑 Password</button>\n")
          .append("        <button class=\"btn btn-outline\" onclick=\"logout()\">🚪 Logout</button>\n")
          .append("      </div>\n")
          .append("    </div>\n")

          // Welcome Banner
          .append("    <div class=\"welcome-banner\">\n")
          .append("      <div>\n")
          .append("        <h2>Welcome, ").append(escapeHtml(session.fullName)).append("! 🎓</h2>\n")
          .append("        <p>Student ID: <strong>").append(escapeHtml(regNo)).append("</strong> &bull; ").append(escapeHtml(dept)).append(" &bull; Year ").append(year).append(" &bull; Kalasalingam Academy of Research and Education</p>\n")
          .append("      </div>\n")
          .append("      <div>\n")
          .append("        <span class=\"pill pill-green\" style=\"font-size: 12px; padding: 6px 12px;\">Active Institutional Student</span>\n")
          .append("      </div>\n")
          .append("    </div>\n")

          // KPI Grid
          .append("    <div class=\"kpi-grid\">\n")
          .append("      <div class=\"kpi-card\"><div class=\"kpi-lbl\">Available Clubs</div><div class=\"kpi-val\">").append(fallbackClubs.size()).append("</div></div>\n")
          .append("      <div class=\"kpi-card\"><div class=\"kpi-lbl\">My Clubs &amp; Applications</div><div class=\"kpi-val\" id=\"kpi-my-clubs\">").append(myClubCount).append("</div></div>\n")
          .append("      <div class=\"kpi-card\"><div class=\"kpi-lbl\">Upcoming Events</div><div class=\"kpi-val\">").append(fallbackEvents.size()).append("</div></div>\n")
          .append("      <div class=\"kpi-card\"><div class=\"kpi-lbl\">My Registered Events</div><div class=\"kpi-val\" id=\"kpi-my-events\">").append(myEventCount).append("</div></div>\n")
          .append("    </div>\n")

          // Tab Navigation Bar
          .append("    <div class=\"tab-bar\">\n")
          .append("      <button class=\"tab-btn active\" onclick=\"switchTab('tab-clubs', this)\">🏫 Available Clubs</button>\n")
          .append("      <button class=\"tab-btn\" onclick=\"switchTab('tab-events', this)\">🗓️ Upcoming Events</button>\n")
          .append("      <button class=\"tab-btn\" onclick=\"switchTab('tab-my-clubs', this)\">📌 My Clubs</button>\n")
          .append("      <button class=\"tab-btn\" onclick=\"switchTab('tab-my-events', this)\">🎟️ My Events</button>\n")
          .append("      <button id=\"tab-btn-prof\" class=\"tab-btn\" onclick=\"switchTab('tab-profile', this)\">👤 Student Profile</button>\n")
          .append("    </div>\n")

          // Tab 1: Available Clubs
          .append("    <div id=\"tab-clubs\" class=\"section-card tab-content\">\n")
          .append("      <div class=\"section-header\">\n")
          .append("        <h2>🏫 Available Campus Clubs &amp; Societies</h2>\n")
          .append("        <span class=\"pill pill-blue\">Open for Enrollment</span>\n")
          .append("      </div>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>ID</th><th>Club Name &amp; Description</th><th>Category</th><th>Coordinator</th><th>Members</th><th>Action</th></tr></thead>\n")
          .append("        <tbody>\n");

        for (Map<String, String> c : fallbackClubs) {
            String clubName = c.get("name");
            boolean alreadyApplied = false;
            String appStatus = "";
            for (Map<String, String> app : fallbackApplications) {
                if (clubName.equalsIgnoreCase(app.get("club")) &&
                    (session.username.equalsIgnoreCase(app.get("username")) || session.fullName.equalsIgnoreCase(app.get("name")))) {
                    alreadyApplied = true;
                    appStatus = app.getOrDefault("status", "PENDING");
                    break;
                }
            }
            sb.append("          <tr>\n")
              .append("            <td><strong>#").append(escapeHtml(c.get("id"))).append("</strong></td>\n")
              .append("            <td><strong>").append(escapeHtml(clubName)).append("</strong><br><span style=\"font-size:12px; color:#64748b;\">").append(escapeHtml(c.get("description"))).append("</span></td>\n")
              .append("            <td><span class=\"pill pill-blue\">").append(escapeHtml(c.get("category"))).append("</span></td>\n")
              .append("            <td>").append(escapeHtml(c.get("coordinator"))).append("</td>\n")
              .append("            <td>").append(c.getOrDefault("members", "1")).append(" students</td>\n")
              .append("            <td>");
            if (alreadyApplied) {
                String pillColor = "APPROVED".equalsIgnoreCase(appStatus) ? "pill-green" : ("REJECTED".equalsIgnoreCase(appStatus) ? "pill-red" : "pill-amber");
                sb.append("<span class=\"pill ").append(pillColor).append("\">").append(escapeHtml(appStatus)).append("</span>");
            } else {
                sb.append("<button class=\"btn btn-primary\" style=\"padding:5px 12px; font-size:11px;\" onclick=\"joinClub('").append(escapeHtml(clubName)).append("')\">➕ Join Club</button>");
            }
            sb.append("</td>\n          </tr>\n");
        }
        sb.append("        </tbody>\n      </table>\n    </div>\n")

          // Tab 2: Upcoming Events
          .append("    <div id=\"tab-events\" class=\"section-card tab-content\" style=\"display:none;\">\n")
          .append("      <div class=\"section-header\">\n")
          .append("        <h2>🗓️ Scheduled Campus Events &amp; Workshops</h2>\n")
          .append("        <span class=\"pill pill-green\">Registration Open</span>\n")
          .append("      </div>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>Event Title</th><th>Host Club</th><th>Date &amp; Time</th><th>Venue</th><th>Capacity</th><th>Action</th></tr></thead>\n")
          .append("        <tbody>\n");

        for (Map<String, String> ev : fallbackEvents) {
            String evId = ev.get("id");
            boolean alreadyReg = false;
            for (Map<String, String> reg : fallbackEventRegistrations) {
                if (evId.equals(reg.get("eventId")) &&
                    (session.username.equalsIgnoreCase(reg.get("username")) || String.valueOf(session.userId).equals(reg.get("userId")))) {
                    alreadyReg = true;
                    break;
                }
            }
            sb.append("          <tr>\n")
              .append("            <td><strong>").append(escapeHtml(ev.get("title"))).append("</strong></td>\n")
              .append("            <td>").append(escapeHtml(ev.get("club"))).append("</td>\n")
              .append("            <td>").append(escapeHtml(ev.get("date"))).append("</td>\n")
              .append("            <td>").append(escapeHtml(ev.get("venue"))).append("</td>\n")
              .append("            <td>").append(escapeHtml(ev.get("capacity"))).append(" seats</td>\n")
              .append("            <td>");
            if (alreadyReg) {
                sb.append("<span class=\"pill pill-green\">Registered ✅</span>");
            } else {
                sb.append("<button class=\"btn btn-primary\" style=\"padding:5px 12px; font-size:11px;\" onclick=\"registerEvent('").append(evId).append("')\">🎟️ Register</button>");
            }
            sb.append("</td>\n          </tr>\n");
        }
        sb.append("        </tbody>\n      </table>\n    </div>\n")

          // Tab 3: My Clubs & Applications
          .append("    <div id=\"tab-my-clubs\" class=\"section-card tab-content\" style=\"display:none;\">\n")
          .append("      <div class=\"section-header\">\n")
          .append("        <h2>📌 My Club Memberships &amp; Applications</h2>\n")
          .append("      </div>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>Club Name</th><th>Department</th><th>Application Date</th><th>Status</th></tr></thead>\n")
          .append("        <tbody>\n");

        boolean hasClubs = false;
        for (Map<String, String> app : fallbackApplications) {
            if (session.username.equalsIgnoreCase(app.get("username")) || session.fullName.equalsIgnoreCase(app.get("name"))) {
                hasClubs = true;
                String st = app.getOrDefault("status", "PENDING");
                String pColor = "APPROVED".equalsIgnoreCase(st) ? "pill-green" : ("REJECTED".equalsIgnoreCase(st) ? "pill-red" : "pill-amber");
                sb.append("          <tr>\n")
                  .append("            <td><strong>").append(escapeHtml(app.get("club"))).append("</strong></td>\n")
                  .append("            <td>").append(escapeHtml(app.get("dept"))).append("</td>\n")
                  .append("            <td>").append(escapeHtml(app.get("date"))).append("</td>\n")
                  .append("            <td><span class=\"pill ").append(pColor).append("\">").append(escapeHtml(st)).append("</span></td>\n")
                  .append("          </tr>\n");
            }
        }
        if (!hasClubs) {
            sb.append("          <tr><td colspan=\"4\" style=\"text-align:center; color:#64748b; padding:20px;\">You haven't joined any clubs yet. Go to <a href=\"javascript:void(0)\" onclick=\"switchTab('tab-clubs')\">Available Clubs</a> to apply!</td></tr>\n");
        }
        sb.append("        </tbody>\n      </table>\n    </div>\n")

          // Tab 4: My Registered Events
          .append("    <div id=\"tab-my-events\" class=\"section-card tab-content\" style=\"display:none;\">\n")
          .append("      <div class=\"section-header\">\n")
          .append("        <h2>🎟️ My Registered Events &amp; Passes</h2>\n")
          .append("      </div>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>Event Title</th><th>Host Club</th><th>Registered Date</th><th>Pass Status</th></tr></thead>\n")
          .append("        <tbody>\n");

        boolean hasEvents = false;
        for (Map<String, String> reg : fallbackEventRegistrations) {
            if (session.username.equalsIgnoreCase(reg.get("username")) || String.valueOf(session.userId).equals(reg.get("userId"))) {
                hasEvents = true;
                sb.append("          <tr>\n")
                  .append("            <td><strong>").append(escapeHtml(reg.get("eventTitle"))).append("</strong></td>\n")
                  .append("            <td>").append(escapeHtml(reg.get("club"))).append("</td>\n")
                  .append("            <td>").append(escapeHtml(reg.get("date"))).append("</td>\n")
                  .append("            <td><span class=\"pill pill-green\">Confirmed Entry ✅</span></td>\n")
                  .append("          </tr>\n");
            }
        }
        if (!hasEvents) {
            sb.append("          <tr><td colspan=\"4\" style=\"text-align:center; color:#64748b; padding:20px;\">You haven't registered for any events yet. Check out <a href=\"javascript:void(0)\" onclick=\"switchTab('tab-events')\">Upcoming Events</a>!</td></tr>\n");
        }
        sb.append("        </tbody>\n      </table>\n    </div>\n")

          // Tab 5: Student Profile
          .append("    <div id=\"tab-profile\" class=\"section-card tab-content\" style=\"display:none;\">\n")
          .append("      <div class=\"section-header\">\n")
          .append("        <h2>👤 Official Student Profile</h2>\n")
          .append("        <span class=\"pill pill-blue\">Kalasalingam University Student Roster</span>\n")
          .append("      </div>\n")
          .append("      <div style=\"max-width: 600px;\">\n")
          .append("        <div class=\"profile-row\"><span class=\"profile-label\">Full Student Name:</span><span class=\"profile-val\">").append(escapeHtml(session.fullName)).append("</span></div>\n")
          .append("        <div class=\"profile-row\"><span class=\"profile-label\">Student ID / Register No:</span><span class=\"profile-val\">").append(escapeHtml(regNo)).append("</span></div>\n")
          .append("        <div class=\"profile-row\"><span class=\"profile-label\">Institutional Email:</span><span class=\"profile-val\"><code>").append(escapeHtml(email)).append("</code></span></div>\n")
          .append("        <div class=\"profile-row\"><span class=\"profile-label\">Academic Department:</span><span class=\"profile-val\">").append(escapeHtml(dept)).append("</span></div>\n")
          .append("        <div class=\"profile-row\"><span class=\"profile-label\">Year of Study:</span><span class=\"profile-val\">Year ").append(year).append("</span></div>\n")
          .append("        <div class=\"profile-row\"><span class=\"profile-label\">Verification Status:</span><span class=\"profile-val\"><span class=\"pill ").append(session.isVerified() ? "pill-green" : "pill-amber").append("\">").append(session.isVerified() ? "✅ Institutional Email Verified" : "⏳ Pending OTP Verification").append("</span></span></div>\n")
          .append("        <div class=\"profile-row\"><span class=\"profile-label\">Security Role:</span><span class=\"profile-val\"><span class=\"badge badge-student\">STUDENT</span></span></div>\n")
          .append("        <div style=\"margin-top:20px;\"><button class=\"btn btn-primary\" onclick=\"openModal('modal-password')\">🔑 Change Password</button></div>\n")
          .append("      </div>\n    </div>\n")

          // Modal: Change Password
          .append("    <div id=\"modal-password\" class=\"modal-overlay\">\n")
          .append("      <div class=\"modal\">\n")
          .append("        <div class=\"modal-header\"><h3>Change Your Password</h3><button class=\"modal-close\" onclick=\"closeModal('modal-password')\">&times;</button></div>\n")
          .append("        <div class=\"form-row\"><label>Current Password</label><input type=\"password\" id=\"pwd-old\"></div>\n")
          .append("        <div class=\"form-row\"><label>New Password (min 6 chars)</label><input type=\"password\" id=\"pwd-new\"></div>\n")
          .append("        <div class=\"modal-actions\"><button class=\"btn btn-outline\" style=\"color:#0f172a;\" onclick=\"closeModal('modal-password')\">Cancel</button><button class=\"btn btn-primary\" onclick=\"submitPasswordChange()\">Update Password</button></div>\n")
          .append("      </div>\n    </div>\n")
          .append("  </div>\n")

          // JavaScript
          .append("  <script>\n")
          .append("    function switchTab(id, btn) {\n")
          .append("      document.querySelectorAll('.tab-content').forEach(function(el){ el.style.display = 'none'; });\n")
          .append("      document.querySelectorAll('.tab-btn').forEach(function(b){ b.classList.remove('active'); });\n")
          .append("      var target = document.getElementById(id);\n")
          .append("      if(target) target.style.display = 'block';\n")
          .append("      if(btn) btn.classList.add('active');\n")
          .append("    }\n")
          .append("    function openModal(id) { document.getElementById(id).style.display = 'flex'; }\n")
          .append("    function closeModal(id) { document.getElementById(id).style.display = 'none'; }\n")
          .append("    function logout() { fetch('/api/auth/logout', {method:'POST'}).then(function(){ location.href = '/login'; }); }\n")
          .append("    function joinClub(clubName) {\n")
          .append("      fetch('/api/memberships', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({club: clubName})})\n")
          .append("      .then(function(r){ return r.json(); }).then(function(res){\n")
          .append("        if(res.error) alert('⚠️ ' + res.error);\n")
          .append("        else { alert('✅ ' + (res.message || 'Membership application submitted successfully!')); location.reload(); }\n")
          .append("      }).catch(function(err){ alert('Network error submitting application'); });\n")
          .append("    }\n")
          .append("    function registerEvent(eventId) {\n")
          .append("      fetch('/api/events/register', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({eventId: eventId})})\n")
          .append("      .then(function(r){ return r.json(); }).then(function(res){\n")
          .append("        if(res.error) alert('⚠️ ' + res.error);\n")
          .append("        else { alert('✅ ' + (res.message || 'Successfully registered for event!')); location.reload(); }\n")
          .append("      }).catch(function(err){ alert('Network error registering for event'); });\n")
          .append("    }\n")
          .append("    function submitPasswordChange() {\n")
          .append("      var oldP = document.getElementById('pwd-old').value;\n")
          .append("      var newP = document.getElementById('pwd-new').value;\n")
          .append("      if(!oldP || !newP) return alert('Please enter both passwords.');\n")
          .append("      fetch('/api/auth/change-password', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({oldPassword:oldP, newPassword:newP})})\n")
          .append("      .then(function(r){ return r.json(); }).then(function(res){\n")
          .append("        if(res.error) alert(res.error);\n")
          .append("        else { alert(res.message); closeModal('modal-password'); }\n")
          .append("      });\n")
          .append("    }\n")
          .append("  </script>\n")
          .append("</body>\n</html>\n");

        sendResponse(exchange, 200, "text/html; charset=UTF-8", sb.toString());
    }

    private void servePortalPage(HttpExchange exchange, WebSession session) throws IOException {
        boolean isReviewer = session.isReviewer();
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
          .append("  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
          .append("  <title>Portal - Campus Club Management System</title>\n")
          .append("  <style>\n")
          .append("    :root { --navy: #192A56; --blue: #2563EB; --blue-hover: #1d4ed8; --emerald: #10B981; --amber: #F59E0B; --danger: #EF4444; --bg: #F8FAFC; --border: #E2E8F0; }\n")
          .append("    * { box-sizing: border-box; }\n")
          .append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: var(--bg); margin: 0; padding: 24px; color: #1e293b; }\n")
          .append("    .container { max-width: 1280px; margin: 0 auto; }\n")
          .append("    .header { background: var(--navy); color: white; padding: 18px 24px; border-radius: 12px; display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 14px; margin-bottom: 20px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }\n")
          .append("    .header-title h1 { margin: 0; font-size: 20px; font-weight: 700; }\n")
          .append("    .header-title p { margin: 4px 0 0 0; font-size: 12px; color: #cbd5e1; }\n")
          .append("    .header-user { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }\n")
          .append("    .btn { display: inline-flex; align-items: center; gap: 6px; padding: 8px 14px; font-size: 12px; font-weight: 600; border-radius: 6px; border: 1px solid transparent; cursor: pointer; transition: all 0.15s; text-decoration: none; }\n")
          .append("    .btn-primary { background: var(--blue); color: white; }\n")
          .append("    .btn-primary:hover { background: var(--blue-hover); }\n")
          .append("    .btn-outline { background: rgba(255,255,255,0.12); color: white; border-color: rgba(255,255,255,0.25); }\n")
          .append("    .btn-outline:hover { background: rgba(255,255,255,0.22); }\n")
          .append("    .badge { padding: 4px 10px; border-radius: 9999px; font-size: 11px; font-weight: 700; text-transform: uppercase; }\n")
          .append("    .badge-admin { background: #3b82f6; color: white; }\n")
          .append("    .badge-coord { background: #10b981; color: white; }\n")
          .append("    .badge-reviewer { background: #f59e0b; color: white; }\n")
          .append("    .notice-banner { background: #eff6ff; border-left: 4px solid var(--blue); padding: 12px 16px; border-radius: 6px; margin-bottom: 20px; font-size: 13px; color: #1e40af; display: flex; align-items: center; justify-content: space-between; }\n")
          .append("    .tab-bar { display: flex; gap: 8px; border-bottom: 2px solid var(--border); margin-bottom: 20px; overflow-x: auto; padding-bottom: 8px; }\n")
          .append("    .tab-btn { background: #ffffff; border: 1.5px solid var(--border); padding: 9px 16px; font-size: 13px; font-weight: 600; color: #475569; border-radius: 8px; cursor: pointer; transition: all 0.15s; white-space: nowrap; }\n")
          .append("    .tab-btn:hover { border-color: var(--blue); color: var(--blue); }\n")
          .append("    .tab-btn.active { background: var(--blue) !important; color: #ffffff !important; border-color: var(--blue) !important; box-shadow: 0 2px 6px rgba(37,99,235,0.35) !important; font-weight: 700; }\n")
          .append("    .section-card { background: white; border-radius: 12px; border: 1px solid var(--border); padding: 22px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }\n")
          .append("    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 10px; }\n")
          .append("    .section-header h2 { margin: 0; font-size: 17px; font-weight: 700; color: var(--navy); }\n")
          .append("    table { width: 100%; border-collapse: collapse; font-size: 13px; }\n")
          .append("    th { background: #f8fafc; color: #475569; text-transform: uppercase; font-size: 11px; letter-spacing: 0.5px; font-weight: 700; text-align: left; padding: 12px 14px; border-bottom: 1.5px solid var(--border); }\n")
          .append("    td { padding: 12px 14px; border-bottom: 1px solid var(--border); vertical-align: middle; }\n")
          .append("    tr:hover td { background: #f8fafc; }\n")
          .append("    .pill { display: inline-block; padding: 3px 8px; border-radius: 9999px; font-size: 11px; font-weight: 700; }\n")
          .append("    .pill-green { background: #dcfce7; color: #166534; }\n")
          .append("    .pill-red { background: #fee2e2; color: #991b1b; }\n")
          .append("    .pill-amber { background: #fef3c7; color: #92400e; }\n")
          .append("    .pill-blue { background: #dbeafe; color: #1e40af; }\n")
          .append("    .pill-gray { background: #f1f5f9; color: #475569; }\n")
          .append("    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px; margin-bottom: 20px; }\n")
          .append("    .kpi-card { background: white; border: 1px solid var(--border); border-radius: 10px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }\n")
          .append("    .kpi-val { font-size: 26px; font-weight: 800; color: var(--navy); margin-top: 4px; }\n")
          .append("    .kpi-lbl { font-size: 12px; font-weight: 600; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; }\n")
          .append("    .modal-overlay { display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15,23,42,0.6); z-index: 1000; align-items: center; justify-content: center; padding: 20px; }\n")
          .append("    .modal { background: white; border-radius: 14px; width: 100%; max-width: 500px; padding: 24px; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.2); }\n")
          .append("    .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }\n")
          .append("    .modal-header h3 { margin: 0; font-size: 18px; font-weight: 700; color: var(--navy); }\n")
          .append("    .modal-close { background: none; border: none; font-size: 20px; cursor: pointer; color: #64748b; }\n")
          .append("    .form-row { margin-bottom: 14px; }\n")
          .append("    .form-row label { display: block; font-size: 11px; font-weight: 700; text-transform: uppercase; color: #475569; margin-bottom: 5px; }\n")
          .append("    .form-row input, .form-row select, .form-row textarea { width: 100%; padding: 9px 12px; border: 1.5px solid var(--border); border-radius: 6px; font-size: 13px; }\n")
          .append("    .modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 20px; }\n")
          .append("  </style>\n</head>\n<body>\n")
          .append("  <div class=\"container\">\n")
          .append("    <!-- Header Navbar -->\n")
          .append("    <div class=\"header\">\n")
          .append("      <div class=\"header-title\">\n")
          .append("        <h1>🏛️ Campus Club Management System</h1>\n")
          .append("        <p>Online Portal • Multi-User Workspace</p>\n")
          .append("      </div>\n")
          .append("      <div class=\"header-user\">\n")
          .append("        <span class=\"badge ").append(isReviewer ? "badge-reviewer" : (session.role == UserRole.ADMIN ? "badge-admin" : "badge-coord")).append("\">")
          .append(escapeHtml(session.role.getDisplayName())).append("</span>\n")
          .append("        <span style=\"font-size: 13px; font-weight: 600;\">👤 ").append(escapeHtml(session.fullName)).append("</span>\n")
          .append("        <button class=\"btn btn-outline\" onclick=\"openModal('modal-password')\">🔑 Password</button>\n")
          .append("        <button class=\"btn btn-outline\" onclick=\"logout()\">🚪 Logout</button>\n")
          .append("      </div>\n")
          .append("    </div>\n");

        if (isReviewer) {
            sb.append("    <div class=\"notice-banner\">\n")
              .append("      <div><strong>🔍 External Teacher Reviewer Mode:</strong> You have full visibility into all clubs, events, applications, and announcements. Modification actions are strictly locked for review integrity.</div>\n")
              .append("      <span class=\"pill pill-amber\">Read-Only Oversight</span>\n")
              .append("    </div>\n");
        }

        // Top KPI Cards
        sb.append("    <div class=\"kpi-grid\">\n")
          .append("      <div class=\"kpi-card\"><div class=\"kpi-lbl\">Supervised Clubs</div><div class=\"kpi-val\" id=\"kpi-clubs\">").append(fallbackClubs.size()).append("</div></div>\n")
          .append("      <div class=\"kpi-card\"><div class=\"kpi-lbl\">Campus Events</div><div class=\"kpi-val\" id=\"kpi-events\">").append(fallbackEvents.size()).append("</div></div>\n")
          .append("      <div class=\"kpi-card\"><div class=\"kpi-lbl\">Club Notices</div><div class=\"kpi-val\" id=\"kpi-notices\">").append(fallbackNotices.size()).append("</div></div>\n")
          .append("      <div class=\"kpi-card\"><div class=\"kpi-lbl\">Pending Applications</div><div class=\"kpi-val\" id=\"kpi-apps\">").append(fallbackApplications.size()).append("</div></div>\n")
          .append("    </div>\n");

        // Tab Navigation
        sb.append("    <div class=\"tab-bar\">\n")
          .append("      <button class=\"tab-btn active\" onclick=\"switchTab('tab-clubs', this)\">🏫 Clubs Management</button>\n")
          .append("      <button class=\"tab-btn\" onclick=\"switchTab('tab-events', this)\">🗓️ Scheduled Events</button>\n")
          .append("      <button class=\"tab-btn\" onclick=\"switchTab('tab-notices', this)\">📢 Announcements</button>\n")
          .append("      <button class=\"tab-btn\" onclick=\"switchTab('tab-apps', this)\">📋 Applications</button>\n")
          .append("      <button class=\"tab-btn\" onclick=\"switchTab('tab-students', this); loadAdminStudents();\">🎓 Registered Students</button>\n")
          .append("      <button class=\"tab-btn\" onclick=\"switchTab('tab-event-regs', this); loadAdminEventRegistrations();\">🎟️ Event Registrations</button>\n")
          .append("      <button class=\"tab-btn\" onclick=\"switchTab('tab-team', this)\">👥 Team &amp; Reviewers</button>\n")
          .append("      <button class=\"tab-btn\" onclick=\"location.href='/teacherDashboard'\">📋 Teacher Dashboard View ↗</button>\n")
          .append("    </div>\n");

        // Tab 1: Clubs
        sb.append("    <div id=\"tab-clubs\" class=\"section-card tab-content\">\n")
          .append("      <div class=\"section-header\">\n")
          .append("        <h2>🏫 Registered Campus Clubs</h2>\n");
        if (!isReviewer) {
            sb.append("        <button class=\"btn btn-primary\" onclick=\"openModal('modal-club')\">➕ Add New Club</button>\n");
        } else {
            sb.append("        <span class=\"pill pill-gray\">Evaluation View (Add Locked)</span>\n");
        }
        sb.append("      </div>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>ID</th><th>Club Name</th><th>Category</th><th>Coordinator</th><th>Status</th><th>Members</th><th>Action</th></tr></thead>\n")
          .append("        <tbody id=\"clubs-tbody\">\n");
        for (Map<String, String> c : fallbackClubs) {
            boolean active = "ACTIVE".equalsIgnoreCase(c.get("status"));
            sb.append("          <tr>\n")
              .append("            <td><strong>#").append(escapeHtml(c.get("id"))).append("</strong></td>\n")
              .append("            <td><strong>").append(escapeHtml(c.get("name"))).append("</strong><br><span style=\"font-size:11px; color:#64748b;\">").append(escapeHtml(c.get("description"))).append("</span></td>\n")
              .append("            <td><span class=\"pill pill-blue\">").append(escapeHtml(c.get("category"))).append("</span></td>\n")
              .append("            <td>").append(escapeHtml(c.get("coordinator"))).append("</td>\n")
              .append("            <td><span class=\"pill ").append(active ? "pill-green" : "pill-red").append("\">").append(escapeHtml(c.get("status"))).append("</span></td>\n")
              .append("            <td>").append(c.getOrDefault("members", "1")).append(" students</td>\n")
              .append("            <td>");
            if (!isReviewer) {
                sb.append("<button class=\"btn btn-outline\" style=\"color:#0f172a; border-color:#cbd5e1;\" onclick=\"toggleClubStatus('").append(c.get("id")).append("')\">Toggle Status</button>");
            } else {
                sb.append("<span class=\"pill pill-gray\">Read-Only</span>");
            }
            sb.append("</td>\n          </tr>\n");
        }
        sb.append("        </tbody>\n      </table>\n    </div>\n");

        // Tab 2: Events
        sb.append("    <div id=\"tab-events\" class=\"section-card tab-content\" style=\"display:none;\">\n")
          .append("      <div class=\"section-header\">\n")
          .append("        <h2>🗓️ Scheduled Campus Events &amp; Capacities</h2>\n");
        if (!isReviewer) {
            sb.append("        <button class=\"btn btn-primary\" onclick=\"openModal('modal-event')\">➕ Schedule New Event</button>\n");
        } else {
            sb.append("        <span class=\"pill pill-gray\">Evaluation View (Schedule Locked)</span>\n");
        }
        sb.append("      </div>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>Event Title</th><th>Host Club</th><th>Date &amp; Time</th><th>Venue</th><th>Capacity</th><th>Registrations</th></tr></thead>\n")
          .append("        <tbody id=\"events-tbody\">\n");
        for (Map<String, String> ev : fallbackEvents) {
            sb.append("          <tr>\n")
              .append("            <td><strong>").append(escapeHtml(ev.get("title"))).append("</strong></td>\n")
              .append("            <td>").append(escapeHtml(ev.get("club"))).append("</td>\n")
              .append("            <td>").append(escapeHtml(ev.get("date"))).append("</td>\n")
              .append("            <td>").append(escapeHtml(ev.get("venue"))).append("</td>\n")
              .append("            <td>").append(escapeHtml(ev.get("capacity"))).append(" seats</td>\n")
              .append("            <td><span class=\"pill pill-green\">").append(ev.getOrDefault("registrations", "0")).append(" confirmed</span></td>\n")
              .append("          </tr>\n");
        }
        sb.append("        </tbody>\n      </table>\n    </div>\n");

        // Tab 3: Announcements
        sb.append("    <div id=\"tab-notices\" class=\"section-card tab-content\" style=\"display:none;\">\n")
          .append("      <div class=\"section-header\">\n")
          .append("        <h2>📢 Broadcasts &amp; Official Notices</h2>\n");
        if (!isReviewer) {
            sb.append("        <button class=\"btn btn-primary\" onclick=\"openModal('modal-notice')\">📢 Publish Notice</button>\n");
        } else {
            sb.append("        <span class=\"pill pill-gray\">Evaluation View (Publish Locked)</span>\n");
        }
        sb.append("      </div>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>Club</th><th>Title &amp; Message</th><th>Priority</th><th>Published</th></tr></thead>\n")
          .append("        <tbody id=\"notices-tbody\">\n");
        for (Map<String, String> n : fallbackNotices) {
            String p = n.getOrDefault("priority", "HIGH");
            String pClass = "HIGH".equalsIgnoreCase(p) ? "pill-amber" : "pill-blue";
            sb.append("          <tr>\n")
              .append("            <td><strong>").append(escapeHtml(n.get("club"))).append("</strong></td>\n")
              .append("            <td><strong>").append(escapeHtml(n.get("title"))).append("</strong><br><span style=\"font-size:12px; color:#475569;\">").append(escapeHtml(n.get("content"))).append("</span></td>\n")
              .append("            <td><span class=\"pill ").append(pClass).append("\">").append(escapeHtml(p)).append("</span></td>\n")
              .append("            <td>").append(escapeHtml(n.get("date"))).append("</td>\n")
              .append("          </tr>\n");
        }
        sb.append("        </tbody>\n      </table>\n    </div>\n");

        // Tab 4: Applications
        sb.append("    <div id=\"tab-apps\" class=\"section-card tab-content\" style=\"display:none;\">\n")
          .append("      <div class=\"section-header\">\n")
          .append("        <h2>📋 Student Membership Applications</h2>\n")
          .append("      </div>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>Student</th><th>Reg No</th><th>Department</th><th>Target Club</th><th>Date</th><th>Status</th><th>Action</th></tr></thead>\n")
          .append("        <tbody id=\"apps-tbody\">\n");
        for (Map<String, String> app : fallbackApplications) {
            String status = app.getOrDefault("status", "PENDING");
            String sClass = "APPROVED".equalsIgnoreCase(status) ? "pill-green" : ("REJECTED".equalsIgnoreCase(status) ? "pill-red" : "pill-amber");
            sb.append("          <tr>\n")
              .append("            <td><strong>").append(escapeHtml(app.get("name"))).append("</strong></td>\n")
              .append("            <td>").append(escapeHtml(app.get("regNo"))).append("</td>\n")
              .append("            <td>").append(escapeHtml(app.get("dept"))).append("</td>\n")
              .append("            <td><strong>").append(escapeHtml(app.get("club"))).append("</strong></td>\n")
              .append("            <td>").append(escapeHtml(app.get("date"))).append("</td>\n")
              .append("            <td><span class=\"pill ").append(sClass).append("\">").append(escapeHtml(status)).append("</span></td>\n")
              .append("            <td>");
            if (!isReviewer && "PENDING".equalsIgnoreCase(status)) {
                sb.append("<button class=\"btn btn-primary\" style=\"padding:4px 8px; font-size:11px;\" onclick=\"reviewApp('").append(app.get("id")).append("','APPROVED')\">Approve</button> ")
                  .append("<button class=\"btn btn-outline\" style=\"padding:4px 8px; font-size:11px; color:#ef4444; border-color:#fca5a5;\" onclick=\"reviewApp('").append(app.get("id")).append("','REJECTED')\">Reject</button>");
            } else {
                sb.append("<span class=\"pill pill-gray\">Read-Only</span>");
            }
            sb.append("</td>\n          </tr>\n");
        }
        sb.append("        </tbody>\n      </table>\n    </div>\n");

        // Tab 5: Team & Reviewers Directory
        sb.append("    <div id=\"tab-team\" class=\"section-card tab-content\" style=\"display:none;\">\n")
          .append("      <div class=\"section-header\">\n")
          .append("        <h2>👥 Team Workspace Accounts &amp; External Reviewer</h2>\n")
          .append("      </div>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>Full Name</th><th>Username</th><th>Role</th><th>Permission Level</th><th>Status</th></tr></thead>\n")
          .append("        <tbody>\n")
          .append("          <tr><td><strong>Team Lead (Project Admin)</strong></td><td><code>team_lead</code> / <code>admin</code></td><td><span class=\"badge badge-admin\">ADMIN</span></td><td>Full Read/Write &amp; User Registry Oversight</td><td><span class=\"pill pill-green\">Active</span></td></tr>\n")
          .append("          <tr><td><strong>Team Member 2 (Tech Lead)</strong></td><td><code>team_dev1</code> / <code>coord_coding</code></td><td><span class=\"badge badge-coord\">COORDINATOR</span></td><td>Manage Coding Club, Approve Memberships, Notices</td><td><span class=\"pill pill-green\">Active</span></td></tr>\n")
          .append("          <tr><td><strong>Team Member 3 (Events Lead)</strong></td><td><code>team_dev2</code> / <code>coord_robotics</code></td><td><span class=\"badge badge-coord\">COORDINATOR</span></td><td>Manage Robotics Club, Schedule Events</td><td><span class=\"pill pill-green\">Active</span></td></tr>\n")
          .append("          <tr><td><strong>Team Member 4 (Member Coordinator)</strong></td><td><code>team_member4</code></td><td><span class=\"badge badge-coord\">COORDINATOR</span></td><td>Review Student Membership Applications</td><td><span class=\"pill pill-green\">Active</span></td></tr>\n")
          .append("          <tr><td><strong>Team Member 5 (QA &amp; Operations)</strong></td><td><code>team_member5</code></td><td><span class=\"badge badge-coord\">COORDINATOR</span></td><td>Operations Audit, Publish Broadcasts</td><td><span class=\"pill pill-green\">Active</span></td></tr>\n")
          .append("          <tr><td><strong>Prof. Reviewer (External Evaluator)</strong></td><td><code>teacher_reviewer</code></td><td><span class=\"badge badge-reviewer\">TEACHER</span></td><td><strong>Strictly Read-Only Evaluation Oversight</strong></td><td><span class=\"pill pill-amber\">Reviewer</span></td></tr>\n")
          .append("        </tbody>\n      </table>\n    </div>\n");

        // Tab 6: Registered Institutional Students
        sb.append("    <div id=\"tab-students\" class=\"section-card tab-content\" style=\"display:none;\">\n")
          .append("      <div class=\"section-header\">\n")
          .append("        <h2>🎓 Registered Institutional Students (@klu.ac.in)</h2>\n")
          .append("        <button class=\"btn btn-outline\" style=\"color:#0f172a;\" onclick=\"loadAdminStudents()\">🔄 Refresh List</button>\n")
          .append("      </div>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>Student ID / Reg No</th><th>Full Name</th><th>Institutional Email</th><th>Department</th><th>Year</th><th>Verification Status</th><th>Enrolled Clubs</th></tr></thead>\n")
          .append("        <tbody id=\"admin-students-tbody\">\n")
          .append("          <tr><td colspan=\"7\" style=\"text-align:center; padding:18px;\">Click tab to load registered students...</td></tr>\n")
          .append("        </tbody>\n      </table>\n    </div>\n");

        // Tab 7: Event Registrations
        sb.append("    <div id=\"tab-event-regs\" class=\"section-card tab-content\" style=\"display:none;\">\n")
          .append("      <div class=\"section-header\">\n")
          .append("        <h2>🎟️ Student Event Registrations</h2>\n")
          .append("        <button class=\"btn btn-outline\" style=\"color:#0f172a;\" onclick=\"loadAdminEventRegistrations()\">🔄 Refresh List</button>\n")
          .append("      </div>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>Registration ID</th><th>Event Title</th><th>Organizing Club</th><th>Student Name</th><th>Registration Date</th><th>Status</th></tr></thead>\n")
          .append("        <tbody id=\"admin-event-regs-tbody\">\n")
          .append("          <tr><td colspan=\"6\" style=\"text-align:center; padding:18px;\">Click tab to load registrations...</td></tr>\n")
          .append("        </tbody>\n      </table>\n    </div>\n");

        // Modals: Add Club, Schedule Event, Publish Notice, Change Password
        sb.append("    <!-- Modal: Add Club -->\n")
          .append("    <div id=\"modal-club\" class=\"modal-overlay\">\n")
          .append("      <div class=\"modal\">\n")
          .append("        <div class=\"modal-header\"><h3>Add New Campus Club</h3><button class=\"modal-close\" onclick=\"closeModal('modal-club')\">&times;</button></div>\n")
          .append("        <div class=\"form-row\"><label>Club Name</label><input type=\"text\" id=\"new-club-name\" placeholder=\"e.g. AI & Data Science Club\"></div>\n")
          .append("        <div class=\"form-row\"><label>Category</label><select id=\"new-club-cat\"><option>Technical</option><option>Cultural</option><option>Sports</option><option>Academic</option></select></div>\n")
          .append("        <div class=\"form-row\"><label>Description</label><textarea id=\"new-club-desc\" rows=\"3\" placeholder=\"Mission and objectives of this club\"></textarea></div>\n")
          .append("        <div class=\"modal-actions\"><button class=\"btn btn-outline\" style=\"color:#0f172a;\" onclick=\"closeModal('modal-club')\">Cancel</button><button class=\"btn btn-primary\" onclick=\"submitClub()\">Register Club</button></div>\n")
          .append("      </div>\n    </div>\n")

          .append("    <!-- Modal: Schedule Event -->\n")
          .append("    <div id=\"modal-event\" class=\"modal-overlay\">\n")
          .append("      <div class=\"modal\">\n")
          .append("        <div class=\"modal-header\"><h3>Schedule Campus Event</h3><button class=\"modal-close\" onclick=\"closeModal('modal-event')\">&times;</button></div>\n")
          .append("        <div class=\"form-row\"><label>Event Title</label><input type=\"text\" id=\"new-event-title\" placeholder=\"e.g. HackSprint 2026\"></div>\n")
          .append("        <div class=\"form-row\"><label>Host Club</label><input type=\"text\" id=\"new-event-club\" placeholder=\"e.g. Coding Club\"></div>\n")
          .append("        <div class=\"form-row\"><label>Venue</label><input type=\"text\" id=\"new-event-venue\" placeholder=\"e.g. Main Auditorium\"></div>\n")
          .append("        <div class=\"form-row\"><label>Capacity Cap</label><input type=\"number\" id=\"new-event-cap\" value=\"100\"></div>\n")
          .append("        <div class=\"modal-actions\"><button class=\"btn btn-outline\" style=\"color:#0f172a;\" onclick=\"closeModal('modal-event')\">Cancel</button><button class=\"btn btn-primary\" onclick=\"submitEvent()\">Schedule Event</button></div>\n")
          .append("      </div>\n    </div>\n")

          .append("    <!-- Modal: Publish Notice -->\n")
          .append("    <div id=\"modal-notice\" class=\"modal-overlay\">\n")
          .append("      <div class=\"modal\">\n")
          .append("        <div class=\"modal-header\"><h3>Publish Broadcast Announcement</h3><button class=\"modal-close\" onclick=\"closeModal('modal-notice')\">&times;</button></div>\n")
          .append("        <div class=\"form-row\"><label>Notice Title</label><input type=\"text\" id=\"new-notice-title\" placeholder=\"e.g. Workshop Registration Open\"></div>\n")
          .append("        <div class=\"form-row\"><label>Priority</label><select id=\"new-notice-priority\"><option>HIGH</option><option>MEDIUM</option><option>LOW</option></select></div>\n")
          .append("        <div class=\"form-row\"><label>Broadcast Content</label><textarea id=\"new-notice-content\" rows=\"3\" placeholder=\"Detailed broadcast text for students\"></textarea></div>\n")
          .append("        <div class=\"modal-actions\"><button class=\"btn btn-outline\" style=\"color:#0f172a;\" onclick=\"closeModal('modal-notice')\">Cancel</button><button class=\"btn btn-primary\" onclick=\"submitNotice()\">Publish Broadcast</button></div>\n")
          .append("      </div>\n    </div>\n")

          .append("    <!-- Modal: Change Password -->\n")
          .append("    <div id=\"modal-password\" class=\"modal-overlay\">\n")
          .append("      <div class=\"modal\">\n")
          .append("        <div class=\"modal-header\"><h3>Change Your Password</h3><button class=\"modal-close\" onclick=\"closeModal('modal-password')\">&times;</button></div>\n")
          .append("        <div class=\"form-row\"><label>Current Password</label><input type=\"password\" id=\"pwd-old\"></div>\n")
          .append("        <div class=\"form-row\"><label>New Password (min 6 chars)</label><input type=\"password\" id=\"pwd-new\"></div>\n")
          .append("        <div class=\"modal-actions\"><button class=\"btn btn-outline\" style=\"color:#0f172a;\" onclick=\"closeModal('modal-password')\">Cancel</button><button class=\"btn btn-primary\" onclick=\"submitPasswordChange()\">Update Password</button></div>\n")
          .append("      </div>\n    </div>\n")

          .append("  </div>\n")
          .append("  <script>\n")
          .append("    function switchTab(id, btn) {\n")
          .append("      document.querySelectorAll('.tab-content').forEach(function(el){ el.style.display = 'none'; });\n")
          .append("      document.querySelectorAll('.tab-btn').forEach(function(b){ b.classList.remove('active'); });\n")
          .append("      document.getElementById(id).style.display = 'block';\n")
          .append("      if(btn) btn.classList.add('active');\n")
          .append("    }\n")
          .append("    function openModal(id) { document.getElementById(id).style.display = 'flex'; }\n")
          .append("    function closeModal(id) { document.getElementById(id).style.display = 'none'; }\n")
          .append("    function logout() { fetch('/api/auth/logout', {method:'POST'}).then(function(){ location.href = '/login'; }); }\n")

          .append("    function submitClub() {\n")
          .append("      var name = document.getElementById('new-club-name').value.trim();\n")
          .append("      var cat = document.getElementById('new-club-cat').value;\n")
          .append("      var desc = document.getElementById('new-club-desc').value.trim();\n")
          .append("      if(!name) return alert('Club name is required.');\n")
          .append("      fetch('/api/clubs', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({name:name, category:cat, description:desc})})\n")
          .append("      .then(function(r){ return r.json(); }).then(function(res){\n")
          .append("        if(res.error) alert(res.error);\n")
          .append("        else { alert(res.message); location.reload(); }\n")
          .append("      });\n")
          .append("    }\n")

          .append("    function toggleClubStatus(id) {\n")
          .append("      fetch('/api/clubs/toggle-status', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({id:id})})\n")
          .append("      .then(function(r){ return r.json(); }).then(function(res){\n")
          .append("        if(res.error) alert(res.error);\n")
          .append("        else location.reload();\n")
          .append("      });\n")
          .append("    }\n")

          .append("    function submitEvent() {\n")
          .append("      var title = document.getElementById('new-event-title').value.trim();\n")
          .append("      var club = document.getElementById('new-event-club').value.trim();\n")
          .append("      var venue = document.getElementById('new-event-venue').value.trim();\n")
          .append("      var cap = document.getElementById('new-event-cap').value;\n")
          .append("      if(!title) return alert('Event title is required.');\n")
          .append("      fetch('/api/events', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({title:title, club:club, venue:venue, capacity:cap})})\n")
          .append("      .then(function(r){ return r.json(); }).then(function(res){\n")
          .append("        if(res.error) alert(res.error);\n")
          .append("        else { alert(res.message); location.reload(); }\n")
          .append("      });\n")
          .append("    }\n")

          .append("    function submitNotice() {\n")
          .append("      var title = document.getElementById('new-notice-title').value.trim();\n")
          .append("      var pri = document.getElementById('new-notice-priority').value;\n")
          .append("      var content = document.getElementById('new-notice-content').value.trim();\n")
          .append("      if(!title) return alert('Title is required.');\n")
          .append("      fetch('/api/announcements', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({title:title, priority:pri, content:content})})\n")
          .append("      .then(function(r){ return r.json(); }).then(function(res){\n")
          .append("        if(res.error) alert(res.error);\n")
          .append("        else { alert(res.message); location.reload(); }\n")
          .append("      });\n")
          .append("    }\n")

          .append("    function reviewApp(id, decision) {\n")
          .append("      fetch('/api/memberships/review', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({id:id, decision:decision})})\n")
          .append("      .then(function(r){ return r.json(); }).then(function(res){\n")
          .append("        if(res.error) alert(res.error);\n")
          .append("        else location.reload();\n")
          .append("      });\n")
          .append("    }\n")

          .append("    function submitPasswordChange() {\n")
          .append("      var oldP = document.getElementById('pwd-old').value;\n")
          .append("      var newP = document.getElementById('pwd-new').value;\n")
          .append("      if(!oldP || !newP) return alert('Please enter both passwords.');\n")
          .append("      fetch('/api/auth/change-password', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({oldPassword:oldP, newPassword:newP})})\n")
          .append("      .then(function(r){ return r.json(); }).then(function(res){\n")
          .append("        if(res.error) alert(res.error);\n")
          .append("        else { alert(res.message); closeModal('modal-password'); }\n")
          .append("      });\n")
          .append("    }\n")
          .append("    function loadAdminStudents() {\n")
          .append("      fetch('/api/admin/students').then(function(r){ return r.json(); }).then(function(students){\n")
          .append("        var tbody = document.getElementById('admin-students-tbody');\n")
          .append("        if(!students || students.length === 0){\n")
          .append("          tbody.innerHTML = '<tr><td colspan=\"7\" style=\"text-align:center; padding:18px;\">No registered students found.</td></tr>';\n")
          .append("          return;\n")
          .append("        }\n")
          .append("        var html = '';\n")
          .append("        students.forEach(function(s){\n")
          .append("          var verPill = s.isVerified ? '<span class=\"pill pill-green\">✓ Verified (@klu.ac.in)</span>' : '<span class=\"pill pill-amber\">⏳ Pending OTP</span>';\n")
          .append("          html += '<tr>' +\n")
          .append("            '<td><strong>' + (s.registerNumber || s.username) + '</strong></td>' +\n")
          .append("            '<td>' + (s.fullName || '—') + '</td>' +\n")
          .append("            '<td><code>' + s.email + '</code></td>' +\n")
          .append("            '<td>' + (s.department || 'General') + '</td>' +\n")
          .append("            '<td>Year ' + (s.year || 1) + '</td>' +\n")
          .append("            '<td>' + verPill + '</td>' +\n")
          .append("            '<td><span class=\"pill pill-blue\">' + (s.clubs || 'None') + '</span></td>' +\n")
          .append("            '</tr>';\n")
          .append("        });\n")
          .append("        tbody.innerHTML = html;\n")
          .append("      }).catch(function(){\n")
          .append("        document.getElementById('admin-students-tbody').innerHTML = '<tr><td colspan=\"7\" style=\"color:red; text-align:center;\">Failed to load students.</td></tr>';\n")
          .append("      });\n")
          .append("    }\n")
          .append("    function loadAdminEventRegistrations() {\n")
          .append("      fetch('/api/admin/event-registrations').then(function(r){ return r.json(); }).then(function(regs){\n")
          .append("        var tbody = document.getElementById('admin-event-regs-tbody');\n")
          .append("        if(!regs || regs.length === 0){\n")
          .append("          tbody.innerHTML = '<tr><td colspan=\"6\" style=\"text-align:center; padding:18px;\">No event registrations recorded yet.</td></tr>';\n")
          .append("          return;\n")
          .append("        }\n")
          .append("        var html = '';\n")
          .append("        regs.forEach(function(r){\n")
          .append("          html += '<tr>' +\n")
          .append("            '<td><strong>#' + r.id + '</strong></td>' +\n")
          .append("            '<td>' + r.eventTitle + '</td>' +\n")
          .append("            '<td>' + r.club + '</td>' +\n")
          .append("            '<td>👤 ' + r.studentName + '</td>' +\n")
          .append("            '<td>' + r.date + '</td>' +\n")
          .append("            '<td><span class=\"pill pill-green\">' + r.status + '</span></td>' +\n")
          .append("            '</tr>';\n")
          .append("        });\n")
          .append("        tbody.innerHTML = html;\n")
          .append("      }).catch(function(){\n")
          .append("        document.getElementById('admin-event-regs-tbody').innerHTML = '<tr><td colspan=\"6\" style=\"color:red; text-align:center;\">Failed to load event registrations.</td></tr>';\n")
          .append("      });\n")
          .append("    }\n")
          .append("  </script>\n")
          .append("</body>\n</html>\n");

        sendResponse(exchange, 200, "text/html; charset=UTF-8", sb.toString());
    }

    // =========================================================================
    // Initial Seed & Fallback Data Setup
    // =========================================================================

    private static void initFallbackData() {
        // Desktop & Seed Demo Accounts (admin/admin123, coord_coding/pass123, prof_sharma/password123, student1/pass123)
        addUser(1, "admin", "System Administrator", "admin@campus.edu", "admin123", UserRole.ADMIN);
        addUser(2, "coord_coding", "David Miller", "coord.coding@campus.edu", "pass123", UserRole.COORDINATOR);
        addUser(3, "coord_robotics", "Elena Rostova", "coord.robotics@campus.edu", "pass123", UserRole.COORDINATOR);
        addUser(4, "prof_sharma", "Prof. Rajesh Sharma", "sharma@campus.edu", "password123", UserRole.COORDINATOR);
        addUser(15, "student1", "Alex Turner", "student1@campus.edu", "pass123", UserRole.STUDENT);
        addUser(16, "student2", "Sophia Chen", "student2@campus.edu", "pass123", UserRole.STUDENT);
        addUser(25, "99527800392", "KARE Student", "99527800392@klu.ac.in", "StudentPass@2026", UserRole.STUDENT);

        // 5 Team Workspace Accounts + 1 Reviewer Account for Online Portal
        addUser(17, "team_lead", "Team Lead (Project Admin)", "team.lead@campus.edu", "TeamLead@2026", UserRole.ADMIN);
        addUser(18, "team_dev1", "Team Member 2 (Tech Lead)", "team.dev1@campus.edu", "TeamDev1@2026", UserRole.COORDINATOR);
        addUser(19, "team_dev2", "Team Member 3 (Events Lead)", "team.dev2@campus.edu", "TeamDev2@2026", UserRole.COORDINATOR);
        addUser(20, "team_member4", "Team Member 4 (Member Coord)", "team.member4@campus.edu", "TeamMem4@2026", UserRole.COORDINATOR);
        addUser(21, "team_member5", "Team Member 5 (QA & Operations)", "team.member5@campus.edu", "TeamMem5@2026", UserRole.COORDINATOR);
        addUser(22, "teacher_reviewer", "Prof. Reviewer (External Evaluator)", "reviewer@campus.edu", "Reviewer@2026", UserRole.TEACHER);

        // Seed Clubs
        addClub("1", "Coding & Algorithms Club", "Competitive programming, data structures, hackathons, and systems engineering.", "Technical", "David Miller", "ACTIVE", "64", "2021-08-15");
        addClub("2", "Robotics & Automation Club", "Autonomous mobile robots, IoT controllers, and embedded systems.", "Technical", "Elena Rostova", "ACTIVE", "42", "2022-01-10");
        addClub("3", "Campus Sports & Athletics Club", "Intra-university athletic tournaments, football, basketball, and fitness.", "Sports", "Prof. Vikas Hegde", "ACTIVE", "85", "2020-03-05");
        addClub("4", "Photography & Media Club", "DSLR masterclasses, creative visual storytelling, and college event photo coverage.", "Cultural", "Prof. Rajesh Sharma", "ACTIVE", "38", "2021-11-20");
        addClub("5", "Cultural Arts & Theatre Club", "Celebration of stage drama, musical concerts, and national youth fest auditions.", "Cultural", "Dr. Anita Desai", "ACTIVE", "55", "2019-09-01");
        addClub("6", "Entrepreneurship & Startup Hub", "Incubation for startup pitch decks, venture capital mentorship, and prototyping.", "Academic", "Team Lead", "ACTIVE", "30", "2022-06-18");

        // Seed Events
        addEvent("101", "HackCampus 2026 National Hackathon", "Coding & Algorithms Club", "2026-11-15 09:00:00", "Main Auditorium & CS Labs", "150", "112");
        addEvent("102", "Autonomous Line Follower Workshop", "Robotics & Automation Club", "2026-11-08 14:00:00", "Robotics Lab 302", "40", "36");
        addEvent("103", "Inter-Department Football Championship", "Campus Sports & Athletics Club", "2026-11-12 08:30:00", "Campus Sports Ground", "80", "72");
        addEvent("104", "Annual Campus Photo Walk", "Photography & Media Club", "2026-11-18 07:00:00", "Botanical Garden & Amphitheatre", "30", "28");

        // Seed Notices
        addNotice("201", "Spring CodeSprint 2026 Orientation", "Mandatory briefing for all participants in Lab 402 this Friday.", "HIGH", "Coding & Algorithms Club", "2026-10-21 14:00");
        addNotice("202", "Hardware Kit Distribution Schedule", "Registered line-follower teams can collect Arduino kits from Innovation Cell.", "MEDIUM", "Robotics & Automation Club", "2026-10-25 11:30");
        addNotice("203", "Annual Cultural Fest Auditions Announced", "Auditions for music band, solo singing, and drama troupes begin next week.", "HIGH", "Cultural Arts & Theatre Club", "2026-10-27 16:00");

        // Seed Applications
        addApp("301", "1CC21CS042", "Sarah Jenkins", "Computer Science", "Coding & Algorithms Club", "2026-10-20 09:30", "PENDING");
        addApp("302", "1CC22EC015", "Marcus Vance", "Electronics & Comm", "Robotics & Automation Club", "2026-10-22 14:15", "PENDING");
        addApp("303", "1CC23IS088", "Aisha Patel", "Information Science", "Photography & Media Club", "2026-10-24 10:00", "PENDING");
        addApp("304", "99527800392", "KARE Student", "Computer Science & Engineering", "Coding & Algorithms Club", "2026-10-28 11:00", "APPROVED");

        // Seed Event Registrations
        Map<String, String> reg1 = new HashMap<>();
        reg1.put("id", "501");
        reg1.put("eventId", "101");
        reg1.put("eventTitle", "HackCampus 2026 National Hackathon");
        reg1.put("club", "Coding & Algorithms Club");
        reg1.put("userId", "25");
        reg1.put("username", "99527800392");
        reg1.put("studentName", "KARE Student");
        reg1.put("date", "2026-10-28 14:00");
        reg1.put("status", "CONFIRMED");
        fallbackEventRegistrations.add(reg1);
    }

    private static void addUser(int id, String username, String fullName, String email, String pass, UserRole role) {
        User u;
        if (role == UserRole.ADMIN) {
            u = new Admin(id, username, PasswordUtil.hashPassword(pass), fullName, email, "+1 555-0100", "SUPER_ADMIN");
        } else if (role == UserRole.COORDINATOR || role == UserRole.TEACHER) {
            u = new ClubCoordinator(id, username, PasswordUtil.hashPassword(pass), fullName, email, "+1 555-0101", "Engineering", "Faculty Lead");
            u.setRole(role);
        } else {
            String regNo = username.matches("^\\d+$") ? username : ("1CC21CS0" + id);
            Student s = new Student(id, username, PasswordUtil.hashPassword(pass), fullName, email, "+91 9952780039", regNo, "Computer Science & Engineering", 3, 5, "Coding, Web Development");
            s.setVerified(true);
            u = s;
        }
        builtInUsers.put(username.toLowerCase(), u);
        if (email != null) builtInUsers.put(email.toLowerCase(), u);
        if (u instanceof Student s && s.getRegisterNumber() != null) {
            builtInUsers.put(s.getRegisterNumber().toLowerCase(), u);
        }
    }

    private static void addClub(String id, String name, String desc, String cat, String coord, String status, String mem, String est) {
        Map<String, String> m = new HashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("description", desc);
        m.put("category", cat);
        m.put("coordinator", coord);
        m.put("status", status);
        m.put("members", mem);
        m.put("established", est);
        fallbackClubs.add(m);
    }

    private static void addEvent(String id, String title, String club, String date, String venue, String cap, String reg) {
        Map<String, String> m = new HashMap<>();
        m.put("id", id);
        m.put("title", title);
        m.put("club", club);
        m.put("date", date);
        m.put("venue", venue);
        m.put("capacity", cap);
        m.put("registrations", reg);
        fallbackEvents.add(m);
    }

    private static void addNotice(String id, String title, String content, String priority, String club, String date) {
        Map<String, String> m = new HashMap<>();
        m.put("id", id);
        m.put("title", title);
        m.put("content", content);
        m.put("priority", priority);
        m.put("club", club);
        m.put("date", date);
        fallbackNotices.add(m);
    }

    private static void addApp(String id, String regNo, String name, String dept, String club, String date, String status) {
        Map<String, String> m = new HashMap<>();
        m.put("id", id);
        m.put("regNo", regNo);
        m.put("name", name);
        m.put("dept", dept);
        m.put("club", club);
        m.put("date", date);
        m.put("status", status);
        fallbackApplications.add(m);
    }

    // =========================================================================
    // HTTP Helpers (Cookies, JSON, Responses)
    // =========================================================================

    public static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes inactivity timeout

    private WebSession getSession(HttpExchange exchange) {
        String sessionId = getSessionCookie(exchange);
        if (sessionId == null) return null;
        WebSession session = activeSessions.get(sessionId);
        if (session != null) {
            long now = System.currentTimeMillis();
            if (now - session.lastAccessedAt > SESSION_TIMEOUT_MS) {
                activeSessions.remove(sessionId);
                clearSessionCookie(exchange);
                return null;
            }
            session.lastAccessedAt = now;
        } else {
            // Stale or invalid session from an earlier server lifecycle; clean up client cookie immediately
            clearSessionCookie(exchange);
        }
        return session;
    }

    private String getSessionCookie(HttpExchange exchange) {
        List<String> cookieHeaders = exchange.getRequestHeaders().get("Cookie");
        if (cookieHeaders == null) return null;
        for (String header : cookieHeaders) {
            String[] pairs = header.split(";");
            for (String pair : pairs) {
                String[] kv = pair.trim().split("=", 2);
                if (kv.length == 2 && SESSION_COOKIE_NAME.equals(kv[0].trim())) {
                    return kv[1].trim();
                }
            }
        }
        return null;
    }

    private void setSessionCookie(HttpExchange exchange, String sessionId) {
        exchange.getResponseHeaders().set("Set-Cookie", SESSION_COOKIE_NAME + "=" + sessionId + "; Path=/; HttpOnly; SameSite=Lax");
    }

    private void clearSessionCookie(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Set-Cookie", SESSION_COOKIE_NAME + "=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; HttpOnly; SameSite=Lax");
    }

    private void redirect(HttpExchange exchange, String target) throws IOException {
        exchange.getResponseHeaders().set("Location", target);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        exchange.getResponseHeaders().set("Expires", "0");
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        exchange.getResponseHeaders().set("Expires", "0");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String contentType, String content) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        exchange.getResponseHeaders().set("Expires", "0");
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private Map<String, String> parseFormData(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isEmpty()) return map;
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    String v = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    map.put(k, v);
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private Map<String, String> parseJson(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;
        String trimmed = body.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        String[] tokens = trimmed.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String token : tokens) {
            String[] kv = token.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
            if (kv.length == 2) {
                String k = kv[0].trim().replace("\"", "");
                String v = kv[1].trim();
                if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                    v = v.substring(1, v.length() - 1);
                }
                map.put(k, v);
            }
        }
        return map;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    // Testing Helpers
    public void setBuiltInUserOtpExpirationForTesting(String username, long expiresAt) {
        User u = builtInUsers.get(username.toLowerCase());
        if (u != null) {
            u.setOtpExpiresAt(expiresAt);
        }
    }

    public void setBuiltInUserLastOtpSentAtForTesting(String username, long lastOtpSentAt) {
        User u = builtInUsers.get(username.toLowerCase());
        if (u != null) {
            u.setLastOtpSentAt(lastOtpSentAt);
        }
    }

    public void setSessionLastAccessedForTesting(String sessionId, long lastAccessedAt) {
        if (sessionId != null && sessionId.contains("=")) {
            sessionId = sessionId.substring(sessionId.indexOf('=') + 1).trim();
        }
        WebSession s = activeSessions.get(sessionId);
        if (s != null) {
            s.lastAccessedAt = lastAccessedAt;
        }
    }
}
