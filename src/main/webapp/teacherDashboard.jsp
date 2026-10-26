<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.sql.*, java.util.*, com.club.util.DatabaseConnection" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Teacher / Faculty Coordinator Dashboard - Campus Club Management System</title>
    <style>
        :root {
            --navy-primary: #192A56;
            --blue-accent: #2563EB;
            --emerald-green: #10B981;
            --amber-warning: #F59E0B;
            --danger-red: #EF4444;
            --bg-slate: #F8FAFC;
        }
        * { box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            background-color: var(--bg-slate);
            color: #1E293B;
            margin: 0;
            padding: 24px;
        }
        .container { max-width: 1200px; margin: 0 auto; }
        .header {
            background: var(--navy-primary);
            color: #FFFFFF;
            padding: 24px 30px;
            border-radius: 12px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
            margin-bottom: 24px;
        }
        .header h1 { margin: 0; font-size: 22px; font-weight: 700; }
        .header p { margin: 4px 0 0 0; font-size: 13px; color: #94A3B8; }
        .badge {
            background-color: var(--blue-accent);
            color: #FFFFFF;
            padding: 6px 14px;
            border-radius: 9999px;
            font-size: 12px;
            font-weight: 600;
            text-transform: uppercase;
        }
        .kpi-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
            gap: 16px;
            margin-bottom: 24px;
        }
        .kpi-card {
            background: #FFFFFF;
            border: 1px solid #E2E8F0;
            border-radius: 10px;
            padding: 20px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        }
        .kpi-title { font-size: 12px; font-weight: 600; text-transform: uppercase; color: #64748B; margin-bottom: 6px; }
        .kpi-value { font-size: 28px; font-weight: 700; color: #0F172A; }
        .kpi-sub { font-size: 12px; color: #94A3B8; margin-top: 4px; }
        .section-card {
            background: #FFFFFF;
            border: 1px solid #E2E8F0;
            border-radius: 10px;
            padding: 24px;
            margin-bottom: 24px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        }
        .section-card h2 {
            margin: 0 0 16px 0;
            font-size: 17px;
            font-weight: 700;
            color: var(--navy-primary);
            border-bottom: 2px solid #F1F5F9;
            padding-bottom: 10px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            font-size: 14px;
        }
        th {
            background-color: #F8FAFC;
            color: #475569;
            font-weight: 600;
            text-align: left;
            padding: 12px;
            border-bottom: 2px solid #E2E8F0;
        }
        td {
            padding: 12px;
            border-bottom: 1px solid #F1F5F9;
            vertical-align: middle;
        }
        tr:hover { background-color: #F8FAFC; }
        .pill {
            display: inline-block;
            padding: 4px 10px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 600;
        }
        .pill-blue { background-color: #DBEAFE; color: #1E40AF; }
        .pill-green { background-color: #DCFCE7; color: #166534; }
        .pill-amber { background-color: #FEF3C7; color: #92400E; }
        .pill-red { background-color: #FEE2E2; color: #991B1B; }
        .btn {
            display: inline-block;
            background-color: var(--blue-accent);
            color: white;
            padding: 6px 14px;
            border-radius: 6px;
            text-decoration: none;
            font-size: 13px;
            font-weight: 600;
            border: none;
            cursor: pointer;
        }
    </style>
</head>
<body>
<%
    // Data variables queried via DatabaseConnection.getConnection()
    String teacherName = "Prof. David Miller";
    String teacherEmail = "david.miller@campus.edu";
    String teacherDept = "Computer Science & Engineering";
    String teacherRole = "TEACHER / COORDINATOR";
    int managedClubsCount = 0;
    int pendingRequestsCount = 0;
    int noticesCount = 0;
    int eventsCount = 0;

    List<Map<String, String>> clubsList = new ArrayList<>();
    List<Map<String, String>> noticesList = new ArrayList<>();
    List<Map<String, String>> requestsList = new ArrayList<>();
    List<Map<String, String>> eventsList = new ArrayList<>();
    boolean dbSuccess = false;

    try (Connection conn = DatabaseConnection.getConnection()) {
        dbSuccess = true;

        // 1. Profile Query
        String userSql = "SELECT user_id, full_name, email, phone_number, role FROM users WHERE role IN ('COORDINATOR', 'TEACHER') LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(userSql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                teacherName = rs.getString("full_name");
                teacherEmail = rs.getString("email");
                teacherRole = rs.getString("role");
            }
        }

        // 2. Managed Clubs Query
        String clubSql = "SELECT club_id, club_name, category, description, status FROM clubs";
        try (PreparedStatement ps = conn.prepareStatement(clubSql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> c = new HashMap<>();
                c.put("id", String.valueOf(rs.getInt("club_id")));
                c.put("name", rs.getString("club_name"));
                c.put("category", rs.getString("category"));
                c.put("description", rs.getString("description"));
                c.put("status", rs.getString("status"));
                clubsList.add(c);
            }
        }
        managedClubsCount = clubsList.size();

        // 3. Notices Query
        String noticeSql = "SELECT a.announcement_id, a.title, a.content, a.priority, a.created_at, c.club_name " +
                           "FROM announcements a JOIN clubs c ON a.club_id = c.club_id ORDER BY a.created_at DESC LIMIT 5";
        try (PreparedStatement ps = conn.prepareStatement(noticeSql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> n = new HashMap<>();
                n.put("title", rs.getString("title"));
                n.put("content", rs.getString("content"));
                n.put("priority", rs.getString("priority"));
                n.put("date", String.valueOf(rs.getTimestamp("created_at")));
                n.put("club", rs.getString("club_name"));
                noticesList.add(n);
            }
        }
        noticesCount = noticesList.size();

        // 4. Pending Requests Query
        String reqSql = "SELECT m.membership_id, s.register_number, u.full_name, s.department, m.joined_at, c.club_name " +
                        "FROM memberships m JOIN students s ON m.student_id = s.student_id " +
                        "JOIN users u ON s.user_id = u.user_id JOIN clubs c ON m.club_id = c.club_id " +
                        "WHERE m.membership_status = 'PENDING' LIMIT 5";
        try (PreparedStatement ps = conn.prepareStatement(reqSql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> r = new HashMap<>();
                r.put("regNo", rs.getString("register_number"));
                r.put("studentName", rs.getString("full_name"));
                r.put("dept", rs.getString("department"));
                r.put("date", String.valueOf(rs.getTimestamp("joined_at")));
                r.put("club", rs.getString("club_name"));
                requestsList.add(r);
            }
        }
        pendingRequestsCount = requestsList.size();

        // 5. Events Query
        String evSql = "SELECT e.event_id, e.event_name, e.event_date, e.venue, e.capacity, c.club_name " +
                       "FROM events e JOIN clubs c ON e.club_id = c.club_id ORDER BY e.event_date ASC LIMIT 5";
        try (PreparedStatement ps = conn.prepareStatement(evSql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> e = new HashMap<>();
                e.put("title", rs.getString("event_name"));
                e.put("date", String.valueOf(rs.getTimestamp("event_date")));
                e.put("venue", rs.getString("venue"));
                e.put("capacity", String.valueOf(rs.getInt("capacity")));
                e.put("club", rs.getString("club_name"));
                eventsList.add(e);
            }
        }
        eventsCount = eventsList.size();

    } catch (Exception e) {
        // Fallback demo data for offline viewing
        managedClubsCount = 2;
        pendingRequestsCount = 3;
        noticesCount = 4;
        eventsCount = 2;

        Map<String, String> demoClub = new HashMap<>();
        demoClub.put("id", "1");
        demoClub.put("name", "Coding & Algorithms Club");
        demoClub.put("category", "TECHNICAL");
        demoClub.put("description", "Dedicated to competitive programming and software engineering.");
        demoClub.put("status", "ACTIVE");
        clubsList.add(demoClub);

        Map<String, String> demoNotice = new HashMap<>();
        demoNotice.put("title", "Hackathon Preparation Sprint");
        demoNotice.put("content", "Orientation session this Friday in Lab 402.");
        demoNotice.put("priority", "HIGH");
        demoNotice.put("date", "2026-10-21 10:00:00");
        demoNotice.put("club", "Coding & Algorithms Club");
        noticesList.add(demoNotice);
    }
%>

<div class="container">
    <!-- Header -->
    <div class="header">
        <div>
            <h1>🎓 Teacher / Faculty Coordinator Dashboard</h1>
            <p>Campus Club Management System &bull; Java JDBC & MySQL 8</p>
        </div>
        <div style="text-align: right;">
            <span class="badge"><%= teacherRole %></span>
            <div style="margin-top: 6px; font-size: 13px;">👤 <%= teacherName %> (<%= teacherEmail %>)</div>
        </div>
    </div>

    <!-- KPI Metrics -->
    <div class="kpi-grid">
        <div class="kpi-card">
            <div class="kpi-title">Faculty Profile</div>
            <div class="kpi-value" style="font-size: 18px;"><%= teacherName %></div>
            <div class="kpi-sub"><%= teacherDept %></div>
        </div>
        <div class="kpi-card">
            <div class="kpi-title">Managed Clubs</div>
            <div class="kpi-value" style="color: var(--blue-accent);"><%= managedClubsCount %></div>
            <div class="kpi-sub">Active faculty supervision</div>
        </div>
        <div class="kpi-card">
            <div class="kpi-title">Pending Requests</div>
            <div class="kpi-value" style="color: var(--amber-warning);"><%= pendingRequestsCount %></div>
            <div class="kpi-sub">Awaiting member verification</div>
        </div>
        <div class="kpi-card">
            <div class="kpi-title">Club Notices</div>
            <div class="kpi-value" style="color: var(--emerald-green);"><%= noticesCount %></div>
            <div class="kpi-sub">Published to students</div>
        </div>
    </div>

    <!-- Managed Clubs Section -->
    <div class="section-card">
        <h2>🏫 Managed Clubs Overview</h2>
        <table>
            <thead>
                <tr>
                    <th>Club ID</th>
                    <th>Club Name</th>
                    <th>Category</th>
                    <th>Description</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
                <% for (Map<String, String> c : clubsList) { %>
                <tr>
                    <td>#<%= c.get("id") %></td>
                    <td><strong><%= c.get("name") %></strong></td>
                    <td><span class="pill pill-blue"><%= c.get("category") %></span></td>
                    <td><%= c.get("description") %></td>
                    <td><span class="pill pill-green"><%= c.get("status") %></span></td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </div>

    <!-- Notices & Announcements Section -->
    <div class="section-card">
        <h2>📢 Club Notices & Announcements</h2>
        <table>
            <thead>
                <tr>
                    <th>Club</th>
                    <th>Notice Title</th>
                    <th>Details</th>
                    <th>Priority</th>
                    <th>Broadcast Date</th>
                </tr>
            </thead>
            <tbody>
                <% for (Map<String, String> n : noticesList) { %>
                <tr>
                    <td><strong><%= n.get("club") %></strong></td>
                    <td><%= n.get("title") %></td>
                    <td><%= n.get("content") %></td>
                    <td><span class="pill pill-amber"><%= n.get("priority") %></span></td>
                    <td><%= n.get("date") %></td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </div>

    <!-- Pending Applications Section -->
    <div class="section-card">
        <h2>📋 Pending Student Applications</h2>
        <table>
            <thead>
                <tr>
                    <th>Register Number</th>
                    <th>Student Name</th>
                    <th>Department</th>
                    <th>Target Club</th>
                    <th>Application Date</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
                <% for (Map<String, String> r : requestsList) { %>
                <tr>
                    <td><code><%= r.get("regNo") %></code></td>
                    <td><%= r.get("studentName") %></td>
                    <td><%= r.get("dept") %></td>
                    <td><%= r.get("club") %></td>
                    <td><%= r.get("date") %></td>
                    <td><span class="pill pill-amber">PENDING</span></td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
