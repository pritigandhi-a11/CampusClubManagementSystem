package com.campusclub.web;

import com.club.util.DatabaseConnection;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller and Route Handler for /teacherDashboard.
 * Queries the database using DatabaseConnection.getConnection() to fetch
 * teacher-specific data: profile details, managed clubs, notices, pending requests, and events.
 * Renders the clean frontend view or JSON payload.
 * Kept strictly read-only for public access.
 */
public class TeacherDashboardController implements HttpHandler {

    public static final String ROUTE_PATH = "/teacherDashboard";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            boolean isJson = query != null && query.contains("format=json");

            // Fetch teacher data using DatabaseConnection.getConnection()
            TeacherDashboardData data = fetchTeacherDashboardData();

            if (isJson) {
                String jsonResponse = data.toJson();
                sendResponse(exchange, 200, "application/json", jsonResponse);
            } else {
                String htmlResponse = renderHtmlView(data);
                sendResponse(exchange, 200, "text/html; charset=UTF-8", htmlResponse);
            }
        } catch (Exception e) {
            String err = "<html><body><h1>500 Internal Server Error</h1><p>" + escapeHtml(e.getMessage()) + "</p></body></html>";
            sendResponse(exchange, 500, "text/html; charset=UTF-8", err);
        }
    }

    /**
     * Queries MySQL using DatabaseConnection.getConnection() for teacher-specific data.
     */
    public TeacherDashboardData fetchTeacherDashboardData() {
        TeacherDashboardData data = new TeacherDashboardData();

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Fetch Teacher / Coordinator Profile
            String userSql = "SELECT user_id, username, full_name, email, phone_number, role " +
                             "FROM users WHERE role IN ('COORDINATOR', 'TEACHER') ORDER BY user_id ASC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(userSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data.teacherId = rs.getInt("user_id");
                    data.teacherUsername = rs.getString("username");
                    data.teacherName = rs.getString("full_name");
                    data.teacherEmail = rs.getString("email");
                    data.teacherPhone = rs.getString("phone_number");
                    data.teacherRole = rs.getString("role");
                }
            }

            // Fallback profile if none returned
            if (data.teacherName == null) {
                data.teacherId = 2;
                data.teacherUsername = "coord_coding";
                data.teacherName = "Prof. David Miller";
                data.teacherEmail = "david.miller@campus.edu";
                data.teacherPhone = "+1 555-0102";
                data.teacherRole = "COORDINATOR";
            }
            data.department = "Computer Science & Engineering";
            data.designation = "Faculty Club Coordinator";

            // 2. Fetch Managed Clubs for this teacher
            String clubSql = "SELECT club_id, club_name, description, category, status, established_date " +
                             "FROM clubs WHERE coordinator_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(clubSql)) {
                ps.setInt(1, data.teacherId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> club = new HashMap<>();
                        club.put("id", String.valueOf(rs.getInt("club_id")));
                        club.put("name", rs.getString("club_name"));
                        club.put("description", rs.getString("description"));
                        club.put("category", rs.getString("category"));
                        club.put("status", rs.getString("status"));
                        club.put("established", String.valueOf(rs.getDate("established_date")));
                        data.managedClubs.add(club);
                    }
                }
            }

            // If no club managed specifically, fetch active clubs
            if (data.managedClubs.isEmpty()) {
                String fallbackClubSql = "SELECT club_id, club_name, description, category, status, established_date " +
                                         "FROM clubs LIMIT 2";
                try (PreparedStatement ps = conn.prepareStatement(fallbackClubSql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> club = new HashMap<>();
                        club.put("id", String.valueOf(rs.getInt("club_id")));
                        club.put("name", rs.getString("club_name"));
                        club.put("description", rs.getString("description"));
                        club.put("category", rs.getString("category"));
                        club.put("status", rs.getString("status"));
                        club.put("established", String.valueOf(rs.getDate("established_date")));
                        data.managedClubs.add(club);
                    }
                }
            }

            // 3. Fetch Notices and Announcements
            String noticeSql = "SELECT a.announcement_id, a.title, a.message AS content, a.priority, a.created_at, c.club_name " +
                               "FROM announcements a JOIN clubs c ON a.club_id = c.club_id " +
                               "ORDER BY a.created_at DESC LIMIT 5";
            try (PreparedStatement ps = conn.prepareStatement(noticeSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> notice = new HashMap<>();
                    notice.put("id", String.valueOf(rs.getInt("announcement_id")));
                    notice.put("title", rs.getString("title"));
                    notice.put("content", rs.getString("content"));
                    notice.put("priority", rs.getString("priority"));
                    notice.put("date", String.valueOf(rs.getTimestamp("created_at")));
                    notice.put("club", rs.getString("club_name"));
                    data.notices.add(notice);
                }
            }

            // 4. Fetch Pending Membership Requests
            String reqSql = "SELECT m.membership_id, s.register_number, u.full_name, s.department, m.joined_at, m.membership_status, c.club_name " +
                            "FROM memberships m " +
                            "JOIN students s ON m.student_id = s.student_id " +
                            "JOIN users u ON s.user_id = u.user_id " +
                            "JOIN clubs c ON m.club_id = c.club_id " +
                            "WHERE m.membership_status = 'PENDING' LIMIT 5";
            try (PreparedStatement ps = conn.prepareStatement(reqSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> req = new HashMap<>();
                    req.put("id", String.valueOf(rs.getInt("membership_id")));
                    req.put("regNo", rs.getString("register_number"));
                    req.put("studentName", rs.getString("full_name"));
                    req.put("dept", rs.getString("department"));
                    req.put("date", String.valueOf(rs.getTimestamp("joined_at")));
                    req.put("club", rs.getString("club_name"));
                    req.put("status", rs.getString("membership_status"));
                    data.pendingRequests.add(req);
                }
            }

            // 5. Fetch Events
            String eventSql = "SELECT e.event_id, e.event_name, e.description, e.event_date, e.venue, e.capacity, c.club_name " +
                              "FROM events e JOIN clubs c ON e.club_id = c.club_id " +
                              "WHERE e.event_date >= NOW() ORDER BY e.event_date ASC LIMIT 5";
            try (PreparedStatement ps = conn.prepareStatement(eventSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> ev = new HashMap<>();
                    ev.put("id", String.valueOf(rs.getInt("event_id")));
                    ev.put("title", rs.getString("event_name"));
                    ev.put("desc", rs.getString("description"));
                    ev.put("date", String.valueOf(rs.getTimestamp("event_date")));
                    ev.put("venue", rs.getString("venue"));
                    ev.put("capacity", String.valueOf(rs.getInt("capacity")));
                    ev.put("club", rs.getString("club_name"));
                    data.upcomingEvents.add(ev);
                }
            }

            data.databaseConnected = true;

        } catch (Exception e) {
            data.databaseConnected = false;
            data.errorMessage = e.getMessage();
            populateOfflineDemoData(data);
        }

        return data;
    }

    private void populateOfflineDemoData(TeacherDashboardData data) {
        data.teacherId = 2;
        data.teacherUsername = "coord_coding";
        data.teacherName = "Prof. David Miller";
        data.teacherEmail = "david.miller@campus.edu";
        data.teacherPhone = "+1 555-0102";
        data.teacherRole = "COORDINATOR / TEACHER";
        data.department = "Computer Science & Engineering";
        data.designation = "Faculty Coordinator";

        Map<String, String> club = new HashMap<>();
        club.put("id", "1");
        club.put("name", "Coding & Algorithms Club");
        club.put("description", "Dedicated to competitive programming, system design, and hackathons.");
        club.put("category", "TECHNICAL");
        club.put("status", "ACTIVE");
        club.put("established", "2024-08-15");
        data.managedClubs.add(club);

        Map<String, String> notice = new HashMap<>();
        notice.put("id", "1");
        notice.put("title", "Spring CodeSprint 2026 Orientation");
        notice.put("content", "Mandatory briefing for all club participants in Lab 402 this Friday.");
        notice.put("priority", "HIGH");
        notice.put("date", "2026-10-21 14:00:00");
        notice.put("club", "Coding & Algorithms Club");
        data.notices.add(notice);

        Map<String, String> req = new HashMap<>();
        req.put("id", "101");
        req.put("regNo", "1CC21CS042");
        req.put("studentName", "Sarah Jenkins");
        req.put("dept", "Computer Science");
        req.put("date", "2026-10-20 09:30:00");
        req.put("club", "Coding & Algorithms Club");
        req.put("status", "PENDING");
        data.pendingRequests.add(req);

        Map<String, String> ev = new HashMap<>();
        ev.put("id", "201");
        ev.put("title", "Algorithmic Problem Solving Workshop");
        ev.put("desc", "Deep dive into dynamic programming and graph theory.");
        ev.put("date", "2026-11-05 10:00:00");
        ev.put("venue", "Seminar Hall A");
        ev.put("capacity", "80");
        ev.put("club", "Coding & Algorithms Club");
        data.upcomingEvents.add(ev);
    }

    private String renderHtmlView(TeacherDashboardData d) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n")
          .append("<html lang=\"en\">\n")
          .append("<head>\n")
          .append("  <meta charset=\"UTF-8\">\n")
          .append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
          .append("  <title>Teacher / Faculty Coordinator Dashboard - CCMS</title>\n")
          .append("  <style>\n")
          .append("    :root { --navy: #192A56; --blue: #2563EB; --blue-hover: #1d4ed8; --emerald: #10B981; --amber: #F59E0B; --bg: #F8FAFC; --border: #E2E8F0; }\n")
          .append("    * { box-sizing: border-box; }\n")
          .append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: var(--bg); margin: 0; padding: 24px; color: #1e293b; }\n")
          .append("    .container { max-width: 1200px; margin: 0 auto; }\n")
          .append("    .header { background: var(--navy); color: white; padding: 20px 24px; border-radius: 12px; display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }\n")
          .append("    .header h1 { margin: 0; font-size: 22px; font-weight: 700; }\n")
          .append("    .header-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }\n")
          .append("    .btn { display: inline-flex; align-items: center; gap: 6px; padding: 7px 14px; font-size: 13px; font-weight: 600; border-radius: 6px; border: 1px solid transparent; cursor: pointer; transition: all 0.15s ease-in-out; text-decoration: none; }\n")
          .append("    .btn-primary { background: var(--blue); color: white; }\n")
          .append("    .btn-primary:hover { background: var(--blue-hover); }\n")
          .append("    .btn-outline { background: rgba(255,255,255,0.12); color: white; border-color: rgba(255,255,255,0.3); }\n")
          .append("    .btn-outline:hover { background: rgba(255,255,255,0.22); }\n")
          .append("    .badge { background: #3b82f6; color: white; padding: 4px 12px; border-radius: 9999px; font-size: 12px; font-weight: 600; text-transform: uppercase; }\n")
          .append("    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px; }\n")
          .append("    .card { background: white; border-radius: 10px; padding: 18px; border: 1px solid var(--border); box-shadow: 0 1px 3px rgba(0,0,0,0.05); }\n")
          .append("    .card h3 { margin: 0 0 6px 0; font-size: 13px; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; }\n")
          .append("    .card .val { font-size: 26px; font-weight: 700; color: #0f172a; }\n")
          .append("    .tab-bar { display: flex; gap: 10px; border-bottom: 2px solid var(--border); margin-bottom: 24px; overflow-x: auto; padding-bottom: 8px; }\n")
          .append("    .tab-btn { background: #f1f5f9; color: #475569; border: 1px solid var(--border); border-radius: 8px; padding: 10px 18px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s ease; white-space: nowrap; display: inline-flex; align-items: center; gap: 6px; outline: none; }\n")
          .append("    .tab-btn:hover { background: #e2e8f0; color: #0f172a; border-color: #cbd5e1; }\n")
          .append("    .tab-btn.active { background: var(--blue) !important; color: #ffffff !important; border-color: var(--blue) !important; box-shadow: 0 2px 6px rgba(37, 99, 235, 0.35) !important; font-weight: 700; }\n")
          .append("    .section { background: white; border-radius: 10px; border: 1px solid var(--border); padding: 20px; margin-bottom: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); transition: opacity 0.2s ease; }\n")
          .append("    .section.hidden { display: none !important; }\n")
          .append("    .section.active { display: block !important; }\n")
          .append("    .section h2 { margin-top: 0; font-size: 16px; color: var(--navy); border-bottom: 2px solid #f1f5f9; padding-bottom: 10px; }\n")
          .append("    table { width: 100%; border-collapse: collapse; margin-top: 12px; font-size: 14px; }\n")
          .append("    th { text-align: left; padding: 10px 12px; background: #f8fafc; color: #475569; font-weight: 600; border-bottom: 1px solid #cbd5e1; }\n")
          .append("    td { padding: 10px 12px; border-bottom: 1px solid #f1f5f9; vertical-align: middle; }\n")
          .append("    tr:hover td { background-color: #f8fafc; }\n")
          .append("    .pill { display: inline-block; padding: 3px 8px; border-radius: 6px; font-size: 12px; font-weight: 600; }\n")
          .append("    .pill-green { background: #dcfce7; color: #166534; }\n")
          .append("    .pill-amber { background: #fef3c7; color: #92400e; }\n")
          .append("    .pill-blue { background: #dbeafe; color: #1e40af; }\n")
          .append("    .pill-gray { background: #f1f5f9; color: #475569; border: 1px solid #e2e8f0; }\n")
          .append("    .status-toast { font-size: 12px; color: #cbd5e1; margin-left: 4px; }\n")
          .append("    @media (max-width: 768px) {\n")
          .append("      body { padding: 12px; }\n")
          .append("      .header { flex-direction: column; align-items: flex-start; }\n")
          .append("      .header-actions { width: 100%; justify-content: flex-start; }\n")
          .append("      table { display: block; overflow-x: auto; white-space: nowrap; }\n")
          .append("    }\n")
          .append("    @media print {\n")
          .append("      body { background: white; padding: 0; }\n")
          .append("      .header-actions, .tab-bar { display: none !important; }\n")
          .append("      .section.hidden { display: block !important; }\n")
          .append("      .card, .section { box-shadow: none; border: 1px solid #ccc; break-inside: avoid; }\n")
          .append("    }\n")
          .append("  </style>\n")
          .append("</head>\n")
          .append("<body>\n")
          .append("  <div class=\"container\">\n")
          .append("    <div class=\"header\">\n")
          .append("      <div>\n")
          .append("        <h1>🎓 Teacher / Faculty Coordinator Dashboard</h1>\n")
          .append("        <p style=\"margin: 4px 0 0 0; font-size: 13px; color: #cbd5e1;\">Campus Club Management System &bull; Route: /teacherDashboard</p>\n")
          .append("      </div>\n")
          .append("      <div class=\"header-actions\">\n")
          .append("        <button id=\"btn-refresh\" class=\"btn btn-primary\" onclick=\"refreshDashboardData()\">🔄 Refresh Data</button>\n")
          .append("        <button id=\"btn-print\" class=\"btn btn-outline\" onclick=\"window.print()\">🖨️ Print Report</button>\n")
          .append("        <div style=\"text-align: right; margin-left: 8px;\">\n")
          .append("          <span class=\"badge\">").append(escapeHtml(d.teacherRole)).append("</span>\n")
          .append("          <div id=\"teacher-profile-header\" style=\"margin-top: 6px; font-size: 13px;\">👤 ").append(escapeHtml(d.teacherName)).append("</div>\n")
          .append("        </div>\n")
          .append("      </div>\n")
          .append("    </div>\n\n");

        // KPI Grid (with intuitive click-to-filter tabs)
        sb.append("    <div class=\"kpi-grid\">\n")
          .append("      <div class=\"card\"><h3>Teacher Profile</h3><div id=\"kpi-name\" class=\"val\" style=\"font-size: 18px;\">")
          .append(escapeHtml(d.teacherName)).append("</div><p id=\"kpi-dept\" style=\"font-size: 12px; color: #64748b; margin: 4px 0 0 0;\">")
          .append(escapeHtml(d.department)).append("</p></div>\n")
          .append("      <div class=\"card\" style=\"cursor: pointer;\" onclick=\"switchTab('clubs')\" title=\"Click to view Managed Clubs\"><h3>Managed Clubs</h3><div id=\"kpi-clubs\" class=\"val\" style=\"color: var(--blue);\">")
          .append(d.managedClubs.size()).append("</div><p style=\"font-size: 12px; color: #64748b; margin: 4px 0 0 0;\">Active Coordinator</p></div>\n")
          .append("      <div class=\"card\" style=\"cursor: pointer;\" onclick=\"switchTab('requests')\" title=\"Click to view Membership Applications\"><h3>Pending Requests</h3><div id=\"kpi-requests\" class=\"val\" style=\"color: var(--amber);\">")
          .append(d.pendingRequests.size()).append("</div><p style=\"font-size: 12px; color: #64748b; margin: 4px 0 0 0;\">Awaiting Review</p></div>\n")
          .append("      <div class=\"card\" style=\"cursor: pointer;\" onclick=\"switchTab('notices')\" title=\"Click to view Club Notices\"><h3>Club Notices</h3><div id=\"kpi-notices\" class=\"val\" style=\"color: var(--emerald);\">")
          .append(d.notices.size()).append("</div><p style=\"font-size: 12px; color: #64748b; margin: 4px 0 0 0;\">Published Broadcasts</p></div>\n")
          .append("    </div>\n\n");

        // Tab Bar
        sb.append("    <div class=\"tab-bar\">\n")
          .append("      <button class=\"tab-btn active\" id=\"tab-all\" data-tab=\"all\" onclick=\"switchTab('all', this)\">📋 All Overview</button>\n")
          .append("      <button class=\"tab-btn\" id=\"tab-clubs\" data-tab=\"clubs\" onclick=\"switchTab('clubs', this)\">🏫 Managed Clubs</button>\n")
          .append("      <button class=\"tab-btn\" id=\"tab-notices\" data-tab=\"notices\" onclick=\"switchTab('notices', this)\">📢 Notices &amp; Broadcasts</button>\n")
          .append("      <button class=\"tab-btn\" id=\"tab-requests\" data-tab=\"requests\" onclick=\"switchTab('requests', this)\">📋 Membership Applications</button>\n")
          .append("      <button class=\"tab-btn\" id=\"tab-events\" data-tab=\"events\" onclick=\"switchTab('events', this)\">🗓️ Club Events</button>\n")
          .append("      <span id=\"last-updated\" class=\"status-toast\" style=\"margin-left: auto; align-self: center;\">● Live</span>\n")
          .append("    </div>\n\n");

        // Section 1: Managed Clubs
        sb.append("    <div id=\"sec-clubs\" data-tab=\"clubs\" class=\"section tab-content\">\n")
          .append("      <h2>🏫 Managed Clubs Overview</h2>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>Club ID</th><th>Club Name</th><th>Category</th><th>Description</th><th>Status</th></tr></thead>\n")
          .append("        <tbody>\n");
        for (Map<String, String> club : d.managedClubs) {
            sb.append("          <tr><td>#").append(escapeHtml(club.get("id"))).append("</td>")
              .append("<td><strong>").append(escapeHtml(club.get("name"))).append("</strong></td>")
              .append("<td><span class=\"pill pill-blue\">").append(escapeHtml(club.get("category"))).append("</span></td>")
              .append("<td>").append(escapeHtml(club.get("description"))).append("</td>")
              .append("<td><span class=\"pill pill-green\">").append(escapeHtml(club.get("status"))).append("</span></td></tr>\n");
        }
        sb.append("        </tbody></table>\n    </div>\n\n");

        // Section 2: Club Notices & Announcements
        sb.append("    <div id=\"sec-notices\" data-tab=\"notices\" class=\"section tab-content\">\n")
          .append("      <h2>📢 Club Notices & Announcements</h2>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>Club</th><th>Notice Title</th><th>Content</th><th>Priority</th><th>Broadcast Date</th></tr></thead>\n")
          .append("        <tbody>\n");
        for (Map<String, String> notice : d.notices) {
            sb.append("          <tr><td><strong>").append(escapeHtml(notice.get("club"))).append("</strong></td>")
              .append("<td>").append(escapeHtml(notice.get("title"))).append("</td>")
              .append("<td>").append(escapeHtml(notice.get("content"))).append("</td>")
              .append("<td><span class=\"pill pill-amber\">").append(escapeHtml(notice.get("priority"))).append("</span></td>")
              .append("<td>").append(escapeHtml(notice.get("date"))).append("</td></tr>\n");
        }
        sb.append("        </tbody></table>\n    </div>\n\n");

        // Section 3: Pending Membership Requests (Read-Only)
        sb.append("    <div id=\"sec-requests\" data-tab=\"requests\" class=\"section tab-content\">\n")
          .append("      <h2>📋 Pending Student Membership Requests</h2>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>Student Reg No</th><th>Full Name</th><th>Department</th><th>Target Club</th><th>Application Date</th><th>Status / Action</th></tr></thead>\n")
          .append("        <tbody>\n");
        for (Map<String, String> req : d.pendingRequests) {
            sb.append("          <tr><td><code>").append(escapeHtml(req.get("regNo"))).append("</code></td>")
              .append("<td>").append(escapeHtml(req.get("studentName"))).append("</td>")
              .append("<td>").append(escapeHtml(req.get("dept"))).append("</td>")
              .append("<td>").append(escapeHtml(req.get("club"))).append("</td>")
              .append("<td>").append(escapeHtml(req.get("date"))).append("</td>")
              .append("<td><span class=\"pill pill-amber\">").append(escapeHtml(req.get("status"))).append("</span>")
              .append(" <span class=\"pill pill-gray\" style=\"font-size:11px; margin-left:4px;\" title=\"Decisions handled securely via Coordinator Swing App\">Read-Only</span></td></tr>\n");
        }
        sb.append("        </tbody></table>\n    </div>\n\n");

        // Section 4: Upcoming Events
        sb.append("    <div id=\"sec-events\" data-tab=\"events\" class=\"section tab-content\">\n")
          .append("      <h2>🗓️ Upcoming Club Events & Capacities</h2>\n")
          .append("      <table>\n")
          .append("        <thead><tr><th>Event Name</th><th>Club</th><th>Date & Time</th><th>Venue</th><th>Capacity</th></tr></thead>\n")
          .append("        <tbody>\n");
        for (Map<String, String> ev : d.upcomingEvents) {
            sb.append("          <tr><td><strong>").append(escapeHtml(ev.get("title"))).append("</strong></td>")
              .append("<td>").append(escapeHtml(ev.get("club"))).append("</td>")
              .append("<td>").append(escapeHtml(ev.get("date"))).append("</td>")
              .append("<td>").append(escapeHtml(ev.get("venue"))).append("</td>")
              .append("<td><span class=\"pill pill-blue\">").append(escapeHtml(ev.get("capacity"))).append(" seats</span></td></tr>\n");
        }
        sb.append("        </tbody></table>\n    </div>\n\n");

        // Client-side JavaScript for Tab Switching and Dynamic Refresh
        sb.append("    <script>\n")
          .append("      function switchTab(targetId, btn) {\n")
          .append("        var key = 'all';\n")
          .append("        if (targetId) {\n")
          .append("          var str = String(targetId).toLowerCase().replace(/^sec-/, '').replace(/^tab-/, '');\n")
          .append("          if (str === 'all' || str === '') key = 'all';\n")
          .append("          else if (str.indexOf('club') !== -1) key = 'clubs';\n")
          .append("          else if (str.indexOf('notice') !== -1 || str.indexOf('broadcast') !== -1 || str.indexOf('announce') !== -1) key = 'notices';\n")
          .append("          else if (str.indexOf('request') !== -1 || str.indexOf('member') !== -1 || str.indexOf('app') !== -1) key = 'requests';\n")
          .append("          else if (str.indexOf('event') !== -1) key = 'events';\n")
          .append("          else key = str;\n")
          .append("        }\n")
          .append("        var buttons = document.querySelectorAll('.tab-btn');\n")
          .append("        for (var i = 0; i < buttons.length; i++) { buttons[i].classList.remove('active'); }\n")
          .append("        if (btn && btn.classList) {\n")
          .append("          btn.classList.add('active');\n")
          .append("        } else {\n")
          .append("          for (var b = 0; b < buttons.length; b++) {\n")
          .append("            var dTab = (buttons[b].getAttribute('data-tab') || buttons[b].id || '').toLowerCase();\n")
          .append("            if ((key === 'all' && dTab.indexOf('all') !== -1) || (key !== 'all' && dTab.indexOf(key) !== -1)) {\n")
          .append("              buttons[b].classList.add('active');\n")
          .append("              break;\n")
          .append("            }\n")
          .append("          }\n")
          .append("        }\n")
          .append("        var sections = document.querySelectorAll('.section');\n")
          .append("        for (var j = 0; j < sections.length; j++) {\n")
          .append("          var sec = sections[j];\n")
          .append("          var sId = (sec.id || '').toLowerCase();\n")
          .append("          var sData = (sec.getAttribute('data-tab') || '').toLowerCase();\n")
          .append("          var isMatch = (key === 'all') || (sId.indexOf(key) !== -1) || (sData.indexOf(key) !== -1);\n")
          .append("          if (isMatch) {\n")
          .append("            sec.classList.remove('hidden');\n")
          .append("            sec.classList.add('active');\n")
          .append("            sec.style.setProperty('display', 'block', 'important');\n")
          .append("          } else {\n")
          .append("            sec.classList.add('hidden');\n")
          .append("            sec.classList.remove('active');\n")
          .append("            sec.style.setProperty('display', 'none', 'important');\n")
          .append("          }\n")
          .append("        }\n")
          .append("      }\n")
          .append("      function filterSections(targetId, btn) { switchTab(targetId, btn); }\n")
          .append("      window.switchTab = switchTab;\n")
          .append("      window.filterSections = switchTab;\n")
          .append("      window.switchDashboardTab = switchTab;\n")
          .append("      document.addEventListener('DOMContentLoaded', function() {\n")
          .append("        var tabBtns = document.querySelectorAll('.tab-btn');\n")
          .append("        for (var i = 0; i < tabBtns.length; i++) {\n")
          .append("          tabBtns[i].addEventListener('click', function(e) {\n")
          .append("            var tgt = this.getAttribute('data-tab') || this.id.replace('tab-', '');\n")
          .append("            switchTab(tgt, this);\n")
          .append("          });\n")
          .append("        }\n")
          .append("      });\n")
          .append("      function refreshDashboardData() {\n")
          .append("        var btn = document.getElementById('btn-refresh');\n")
          .append("        var originalText = btn ? btn.innerHTML : '🔄 Refresh Data';\n")
          .append("        if (btn) { btn.disabled = true; btn.innerHTML = '⏳ Refreshing...'; }\n")
          .append("        fetch('/teacherDashboard?format=json')\n")
          .append("          .then(function(res) { if (!res.ok) throw new Error('HTTP ' + res.status); return res.json(); })\n")
          .append("          .then(function(data) {\n")
          .append("            if (data) {\n")
          .append("              if (data.teacherName) {\n")
          .append("                var elName = document.getElementById('kpi-name');\n")
          .append("                if (elName) elName.textContent = data.teacherName;\n")
          .append("                var elHead = document.getElementById('teacher-profile-header');\n")
          .append("                if (elHead) elHead.textContent = '👤 ' + data.teacherName;\n")
          .append("              }\n")
          .append("              if (data.department) {\n")
          .append("                var elDept = document.getElementById('kpi-dept');\n")
          .append("                if (elDept) elDept.textContent = data.department;\n")
          .append("              }\n")
          .append("              if (data.managedClubsCount !== undefined) {\n")
          .append("                var elClubs = document.getElementById('kpi-clubs');\n")
          .append("                if (elClubs) elClubs.textContent = data.managedClubsCount;\n")
          .append("              }\n")
          .append("              if (data.pendingRequestsCount !== undefined) {\n")
          .append("                var elReq = document.getElementById('kpi-requests');\n")
          .append("                if (elReq) elReq.textContent = data.pendingRequestsCount;\n")
          .append("              }\n")
          .append("              if (data.noticesCount !== undefined) {\n")
          .append("                var elNotices = document.getElementById('kpi-notices');\n")
          .append("                if (elNotices) elNotices.textContent = data.noticesCount;\n")
          .append("              }\n")
          .append("              var stamp = document.getElementById('last-updated');\n")
          .append("              if (stamp) {\n")
          .append("                var d = new Date();\n")
          .append("                var timeStr = d.getHours().toString().padStart(2, '0') + ':' + d.getMinutes().toString().padStart(2, '0') + ':' + d.getSeconds().toString().padStart(2, '0');\n")
          .append("                stamp.textContent = 'Refreshed at ' + timeStr;\n")
          .append("              }\n")
          .append("            }\n")
          .append("          })\n")
          .append("          .catch(function(err) { console.warn('Refresh error:', err); })\n")
          .append("          .finally(function() {\n")
          .append("            if (btn) { btn.disabled = false; btn.innerHTML = originalText; }\n")
          .append("          });\n")
          .append("      }\n")
          .append("    </script>\n")
          .append("  </div>\n</body>\n</html>\n");
        return sb.toString();
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        exchange.getResponseHeaders().set("Expires", "0");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    public static class TeacherDashboardData {
        public int teacherId;
        public String teacherUsername;
        public String teacherName;
        public String teacherEmail;
        public String teacherPhone;
        public String teacherRole;
        public String department;
        public String designation;
        public boolean databaseConnected;
        public String errorMessage;

        public final List<Map<String, String>> managedClubs = new ArrayList<>();
        public final List<Map<String, String>> notices = new ArrayList<>();
        public final List<Map<String, String>> pendingRequests = new ArrayList<>();
        public final List<Map<String, String>> upcomingEvents = new ArrayList<>();

        public String toJson() {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"teacherId\":").append(teacherId).append(",");
            sb.append("\"teacherUsername\":\"").append(escapeJson(teacherUsername)).append("\",");
            sb.append("\"teacherName\":\"").append(escapeJson(teacherName)).append("\",");
            sb.append("\"teacherEmail\":\"").append(escapeJson(teacherEmail)).append("\",");
            sb.append("\"department\":\"").append(escapeJson(department)).append("\",");
            sb.append("\"designation\":\"").append(escapeJson(designation)).append("\",");
            sb.append("\"databaseConnected\":").append(databaseConnected).append(",");
            sb.append("\"managedClubsCount\":").append(managedClubs.size()).append(",");
            sb.append("\"noticesCount\":").append(notices.size()).append(",");
            sb.append("\"pendingRequestsCount\":").append(pendingRequests.size());
            sb.append("}");
            return sb.toString();
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
        }
    }
}
