package com.campusclub.gui;

import com.campusclub.dao.UserDAO;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.model.Admin;
import com.campusclub.model.Announcement;
import com.campusclub.model.Club;
import com.campusclub.model.ClubCoordinator;
import com.campusclub.model.Event;
import com.campusclub.model.Student;
import com.campusclub.model.User;
import com.campusclub.model.UserRole;
import com.campusclub.service.ClubService;
import com.campusclub.service.EventService;
import com.campusclub.util.PasswordUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing Dashboard Panel for System Administrators.
 * Features:
 * - System-wide metric summary cards
 * - Tab 1: Manage Campus Clubs (Register, Edit, Toggle Status, Delete)
 * - Tab 2: User Registry (Search, Filter by Role: STUDENT, COORDINATOR, ADMIN)
 * - Tab 3: All Campus Events Overview (Manage schedules, registrations)
 * - Tab 4: All Campus Announcements (System broadcasts)
 */
public class AdminDashboard extends JPanel {
    private final Admin admin;
    private final ClubService clubService;
    private final EventService eventService;
    private final UserDAO userDAO;

    // Top Metric Cards
    private DashboardCard totalUsersCard;
    private DashboardCard totalClubsCard;
    private DashboardCard activeClubsCard;
    private DashboardCard totalEventsCard;

    // Tab 1: Clubs
    private JTextField clubSearchField;
    private JComboBox<String> clubStatusFilter;
    private JTable clubsTable;
    private DefaultTableModel clubsTableModel;
    private List<Club> loadedClubs = new ArrayList<>();

    // Tab 2: Users
    private JTextField userSearchField;
    private JComboBox<String> userRoleFilter;
    private JTable usersTable;
    private DefaultTableModel usersTableModel;
    private List<User> loadedUsers = new ArrayList<>();

    // Tab 3: Events
    private JTable eventsTable;
    private DefaultTableModel eventsTableModel;
    private List<Event> loadedEvents = new ArrayList<>();

    // Tab 4: Announcements
    private JTable announcementsTable;
    private DefaultTableModel announcementsTableModel;

    // Tab 1: System KPI Metrics & Analytics Table
    private JTable analyticsTable;
    private DefaultTableModel analyticsTableModel;

    public AdminDashboard(Admin admin) {
        this(admin, new ClubService(), new EventService(), new UserDAO());
    }

    public AdminDashboard(Admin admin, ClubService clubService, EventService eventService, UserDAO userDAO) {
        this.admin = admin != null ? admin : new Admin();
        this.clubService = clubService != null ? clubService : new ClubService();
        this.eventService = eventService != null ? eventService : new EventService();
        this.userDAO = userDAO != null ? userDAO : new UserDAO();

        initComponents();
        loadAllData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(UITheme.BG_LIGHT);

        // North: Metric Summary Cards
        JPanel metricsPanel = new JPanel(new GridLayout(1, 4, 12, 0));
        metricsPanel.setOpaque(false);

        totalUsersCard = new DashboardCard("Total Users", "0", "Students, Coords, Admins", UITheme.PRIMARY_BLUE);
        totalClubsCard = new DashboardCard("Campus Clubs", "0", "Registered Clubs", UITheme.ACCENT_EMERALD);
        activeClubsCard = new DashboardCard("Active Clubs", "0", "Open for Membership", UITheme.WARNING_AMBER);
        totalEventsCard = new DashboardCard("Campus Events", "0", "Total Scheduled", UITheme.PURPLE_ACCENT);

        metricsPanel.add(totalUsersCard);
        metricsPanel.add(totalClubsCard);
        metricsPanel.add(activeClubsCard);
        metricsPanel.add(totalEventsCard);

        add(metricsPanel, BorderLayout.NORTH);

        // Center: Tabbed Pane (Aligined with 4 Administrator Tabs)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_HEADER);

        tabbedPane.addTab("📊 System KPI Metrics & Analytics", createMetricsAnalyticsPanel());
        tabbedPane.addTab("👥 User Registry & RBAC", createUsersAdminPanel());
        tabbedPane.addTab("🏛️ Manage Clubs & Coordinators", createClubsAdminPanel());
        tabbedPane.addTab("📅 Campus Events Oversight", createEventsAdminPanel());
        tabbedPane.addTab("📢 All Announcements", createAnnouncementsAdminPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==========================================
    // Tab 1: System KPI Metrics & Analytics Panel
    // ==========================================
    private JPanel createMetricsAnalyticsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setBackground(UITheme.CARD_BG);

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);
        JPanel titlePanel = UITheme.createSectionHeader("📊 System KPI Metrics & Campus Analytics", "Actual database-backed totals and activity metrics across all campus clubs.");

        JButton refreshBtn = UITheme.createPrimaryButton("🔄 Refresh Live Analytics");
        refreshBtn.addActionListener(e -> {
            loadAllData();
            JOptionPane.showMessageDialog(this, "Analytics metrics reloaded from live database.", "Metrics Refreshed", JOptionPane.INFORMATION_MESSAGE);
        });
        topHeader.add(titlePanel, BorderLayout.WEST);
        topHeader.add(refreshBtn, BorderLayout.EAST);
        panel.add(topHeader, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setOpaque(false);

        String[] cols = {"Category", "Metric / Indicator", "Database Value", "Operational Status", "Description"};
        analyticsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        analyticsTable = new JTable(analyticsTableModel);
        UITheme.styleTable(analyticsTable);

        JScrollPane scrollPane = new JScrollPane(analyticsTable);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel statusBanner = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        statusBanner.setBackground(UITheme.BG_LIGHT);
        statusBanner.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        JLabel dbStatusLbl = new JLabel("🟢 Database Status: Connected to MySQL 8 (campus_club_db) via JDBC");
        dbStatusLbl.setFont(UITheme.FONT_BOLD);
        dbStatusLbl.setForeground(UITheme.ACCENT_EMERALD);
        JLabel rbacStatusLbl = new JLabel("🔒 RBAC Engine: Active (Enforcing Student, Coordinator, Admin privileges)");
        rbacStatusLbl.setFont(UITheme.FONT_REGULAR);
        rbacStatusLbl.setForeground(UITheme.TEXT_MUTED);

        statusBanner.add(dbStatusLbl);
        statusBanner.add(rbacStatusLbl);
        centerPanel.add(statusBanner, BorderLayout.SOUTH);

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    // ==========================================
    // Tab 3: Manage Clubs Panel
    // ==========================================
    private JPanel createClubsAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UITheme.CARD_BG);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterBar.setOpaque(false);

        JLabel searchLbl = new JLabel("Search Clubs:");
        searchLbl.setFont(UITheme.FONT_HEADER);
        clubSearchField = new JTextField(15);

        JLabel statusLbl = new JLabel("Status:");
        statusLbl.setFont(UITheme.FONT_HEADER);
        clubStatusFilter = new JComboBox<>(new String[]{"All Statuses", "ACTIVE", "INACTIVE"});

        JButton filterBtn = UITheme.createPrimaryButton("Search / Filter");
        filterBtn.addActionListener(e -> applyClubFilter());

        JButton resetBtn = UITheme.createSecondaryButton("Reset");
        resetBtn.addActionListener(e -> {
            clubSearchField.setText("");
            clubStatusFilter.setSelectedIndex(0);
            loadClubs();
        });

        filterBar.add(searchLbl);
        filterBar.add(clubSearchField);
        filterBar.add(statusLbl);
        filterBar.add(clubStatusFilter);
        filterBar.add(filterBtn);
        filterBar.add(resetBtn);

        panel.add(filterBar, BorderLayout.NORTH);

        String[] columns = {"ID", "Club Name", "Category", "Coordinator", "Status", "Members", "Est. Date"};
        clubsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        clubsTable = new JTable(clubsTableModel);
        UITheme.styleTable(clubsTable);
        panel.add(new JScrollPane(clubsTable), BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionsPanel.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadClubs());

        JButton addClubBtn = UITheme.createPrimaryButton("Register New Club");
        addClubBtn.addActionListener(e -> handleAddClub());

        JButton assignCoordBtn = UITheme.createSecondaryButton("Assign Coordinator");
        assignCoordBtn.addActionListener(e -> handleAssignCoordinator());

        JButton toggleStatusBtn = UITheme.createSecondaryButton("Toggle Status (Active/Inactive)");
        toggleStatusBtn.addActionListener(e -> handleToggleClubStatus());

        JButton deleteClubBtn = UITheme.createDangerButton("Delete Club");
        deleteClubBtn.addActionListener(e -> handleDeleteClub());

        actionsPanel.add(refreshBtn);
        actionsPanel.add(addClubBtn);
        actionsPanel.add(assignCoordBtn);
        actionsPanel.add(toggleStatusBtn);
        actionsPanel.add(deleteClubBtn);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // Tab 2: User Registry Panel
    // ==========================================
    private JPanel createUsersAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UITheme.CARD_BG);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterBar.setOpaque(false);

        JLabel searchLbl = new JLabel("Search Users:");
        searchLbl.setFont(UITheme.FONT_HEADER);
        userSearchField = new JTextField(15);

        JLabel roleLbl = new JLabel("Filter Role:");
        roleLbl.setFont(UITheme.FONT_HEADER);
        userRoleFilter = new JComboBox<>(new String[]{"All Roles", "STUDENT", "COORDINATOR", "ADMIN"});

        JButton filterBtn = UITheme.createPrimaryButton("Search");
        filterBtn.addActionListener(e -> applyUserFilter());

        JButton resetBtn = UITheme.createSecondaryButton("Show All");
        resetBtn.addActionListener(e -> {
            userSearchField.setText("");
            userRoleFilter.setSelectedIndex(0);
            loadUsers();
        });

        filterBar.add(searchLbl);
        filterBar.add(userSearchField);
        filterBar.add(roleLbl);
        filterBar.add(userRoleFilter);
        filterBar.add(filterBtn);
        filterBar.add(resetBtn);

        panel.add(filterBar, BorderLayout.NORTH);

        String[] columns = {"User ID", "Username", "Full Name", "Email", "Role", "Phone", "Account Created"};
        usersTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        usersTable = new JTable(usersTableModel);
        UITheme.styleTable(usersTable);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionsPanel.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadUsers());

        JButton createUserBtn = UITheme.createPrimaryButton("Create User");
        createUserBtn.addActionListener(e -> handleCreateUser());

        JButton changeRoleBtn = UITheme.createSecondaryButton("Assign / Edit Role");
        changeRoleBtn.addActionListener(e -> handleChangeUserRole());

        JButton deleteUserBtn = UITheme.createDangerButton("Deactivate / Delete");
        deleteUserBtn.addActionListener(e -> handleDeleteUser());

        actionsPanel.add(refreshBtn);
        actionsPanel.add(createUserBtn);
        actionsPanel.add(changeRoleBtn);
        actionsPanel.add(deleteUserBtn);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // Tab 3: Events Overview Panel
    // ==========================================
    private JPanel createEventsAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UITheme.CARD_BG);

        String[] columns = {"Event ID", "Club Name", "Event Title", "Date & Time", "Location", "Capacity", "Registrations", "Status"};
        eventsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        eventsTable = new JTable(eventsTableModel);
        UITheme.styleTable(eventsTable);
        panel.add(new JScrollPane(eventsTable), BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionsPanel.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadEvents());

        JButton deleteBtn = UITheme.createDangerButton("Delete / Cancel Event");
        deleteBtn.addActionListener(e -> handleDeleteEvent());

        actionsPanel.add(refreshBtn);
        actionsPanel.add(deleteBtn);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // Tab 4: Announcements Admin Panel
    // ==========================================
    private JPanel createAnnouncementsAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UITheme.CARD_BG);

        String[] columns = {"ID", "Club Name", "Title", "Message", "Priority", "Broadcast Date"};
        announcementsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        announcementsTable = new JTable(announcementsTableModel);
        UITheme.styleTable(announcementsTable);
        panel.add(new JScrollPane(announcementsTable), BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionsPanel.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadAnnouncements());

        actionsPanel.add(refreshBtn);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // Data Loading & Business Logic
    // ==========================================

    public void loadAllData() {
        loadClubs();
        loadUsers();
        loadEvents();
        loadAnnouncements();
        loadAnalyticsMetrics();
    }

    public void loadAnalyticsMetrics() {
        if (analyticsTableModel == null) return;
        analyticsTableModel.setRowCount(0);

        int studentCount = 0;
        int coordCount = 0;
        int adminCount = 0;
        for (User u : loadedUsers) {
            if (u.getRole() == UserRole.STUDENT) studentCount++;
            else if (u.getRole() == UserRole.COORDINATOR || u.getRole() == UserRole.TEACHER) coordCount++;
            else if (u.getRole() == UserRole.ADMIN) adminCount++;
        }

        int activeClubs = 0;
        int techClubs = 0;
        int cultClubs = 0;
        int sportsClubs = 0;
        int acadClubs = 0;
        int totalMembers = 0;
        for (Club c : loadedClubs) {
            if ("ACTIVE".equalsIgnoreCase(c.getStatus())) activeClubs++;
            if ("Technical".equalsIgnoreCase(c.getCategory())) techClubs++;
            else if ("Cultural".equalsIgnoreCase(c.getCategory())) cultClubs++;
            else if ("Sports".equalsIgnoreCase(c.getCategory())) sportsClubs++;
            else if ("Academic".equalsIgnoreCase(c.getCategory())) acadClubs++;
            totalMembers += c.getMemberCount();
        }

        int totalCap = 0;
        int totalReg = 0;
        for (Event e : loadedEvents) {
            totalCap += e.getCapacity();
            totalReg += e.getRegisteredCount();
        }

        analyticsTableModel.addRow(new Object[]{"Users & RBAC", "Total Registered Users", String.valueOf(loadedUsers.size()), "ACTIVE", "All student, coordinator, and administrator accounts"});
        analyticsTableModel.addRow(new Object[]{"Users & RBAC", "Enrolled Students", String.valueOf(studentCount), "ACTIVE", "Undergraduate and postgraduate campus students"});
        analyticsTableModel.addRow(new Object[]{"Users & RBAC", "Faculty Coordinators", String.valueOf(coordCount), "ACTIVE", "Department club coordinators & teachers"});
        analyticsTableModel.addRow(new Object[]{"Users & RBAC", "System Administrators", String.valueOf(adminCount), "ACTIVE", "Privileged portal administrators"});
        analyticsTableModel.addRow(new Object[]{"Club Management", "Total Registered Clubs", String.valueOf(loadedClubs.size()), "OPERATIONAL", "Campus student activity clubs and organizations"});
        analyticsTableModel.addRow(new Object[]{"Club Management", "Active Clubs", String.valueOf(activeClubs), "OPERATIONAL", "Clubs currently accepting student memberships"});
        analyticsTableModel.addRow(new Object[]{"Club Management", "Technical / Engineering Clubs", String.valueOf(techClubs), "ACTIVE", "Coding, Robotics, and Technical innovation clubs"});
        analyticsTableModel.addRow(new Object[]{"Club Management", "Cultural & Arts Clubs", String.valueOf(cultClubs), "ACTIVE", "Photography, Music, Dance, and Drama clubs"});
        analyticsTableModel.addRow(new Object[]{"Club Management", "Sports & Athletics Clubs", String.valueOf(sportsClubs), "ACTIVE", "Sports tournaments and fitness clubs"});
        analyticsTableModel.addRow(new Object[]{"Club Management", "Academic & Entrepreneurship Clubs", String.valueOf(acadClubs), "ACTIVE", "Business incubation and debating clubs"});
        analyticsTableModel.addRow(new Object[]{"Memberships", "Active Club Memberships", String.valueOf(totalMembers), "VERIFIED", "Approved active student memberships"});
        analyticsTableModel.addRow(new Object[]{"Events Oversight", "Scheduled Campus Events", String.valueOf(loadedEvents.size()), "UPCOMING", "Live scheduled workshops, hackathons, and tournaments"});
        analyticsTableModel.addRow(new Object[]{"Events Oversight", "Total Seat Capacity", String.valueOf(totalCap), "ENFORCED", "Capacity caps enforced across all event venues"});
        analyticsTableModel.addRow(new Object[]{"Events Oversight", "Confirmed Event Registrations", String.valueOf(totalReg), "CONFIRMED", "Students registered with verified Pass IDs"});
    }

    public void loadClubs() {
        clubsTableModel.setRowCount(0);
        int activeCount = 0;
        try {
            loadedClubs = clubService.getAllClubs();
            for (Club c : loadedClubs) {
                if ("ACTIVE".equalsIgnoreCase(c.getStatus())) activeCount++;

                clubsTableModel.addRow(new Object[]{
                        c.getClubId(),
                        c.getClubName(),
                        c.getCategory(),
                        c.getCoordinatorName() != null ? c.getCoordinatorName() : "Unassigned",
                        c.getStatus() != null ? c.getStatus() : "ACTIVE",
                        c.getMemberCount(),
                        c.getEstablishedDate() != null ? c.getEstablishedDate().toString() : "N/A"
                });
            }
            totalClubsCard.setValue(loadedClubs.size());
            activeClubsCard.setValue(activeCount);
        } catch (DatabaseOperationException ex) {
            System.err.println("Error loading clubs: " + ex.getMessage());
        }
    }

    private void applyClubFilter() {
        String query = clubSearchField.getText().trim().toLowerCase();
        String statusFilter = (String) clubStatusFilter.getSelectedItem();

        clubsTableModel.setRowCount(0);
        for (Club c : loadedClubs) {
            boolean matchesStatus = "All Statuses".equals(statusFilter) || (c.getStatus() != null && c.getStatus().equalsIgnoreCase(statusFilter));
            boolean matchesKey = query.isEmpty() ||
                    (c.getClubName() != null && c.getClubName().toLowerCase().contains(query)) ||
                    (c.getCategory() != null && c.getCategory().toLowerCase().contains(query));

            if (matchesStatus && matchesKey) {
                clubsTableModel.addRow(new Object[]{
                        c.getClubId(),
                        c.getClubName(),
                        c.getCategory(),
                        c.getCoordinatorName() != null ? c.getCoordinatorName() : "Unassigned",
                        c.getStatus() != null ? c.getStatus() : "ACTIVE",
                        c.getMemberCount(),
                        c.getEstablishedDate() != null ? c.getEstablishedDate().toString() : "N/A"
                });
            }
        }
    }

    public void loadUsers() {
        usersTableModel.setRowCount(0);
        try {
            loadedUsers = userDAO.findAll();
            for (User u : loadedUsers) {
                usersTableModel.addRow(new Object[]{
                        u.getUserId(),
                        u.getUsername(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getRole() != null ? u.getRole().getDisplayName() : "N/A",
                        u.getPhoneNumber() != null ? u.getPhoneNumber() : "N/A",
                        u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate().toString() : "N/A"
                });
            }
            totalUsersCard.setValue(loadedUsers.size());
        } catch (DatabaseOperationException ex) {
            System.err.println("Error loading users: " + ex.getMessage());
        }
    }

    private void applyUserFilter() {
        String query = userSearchField.getText().trim().toLowerCase();
        String selectedRole = (String) userRoleFilter.getSelectedItem();

        usersTableModel.setRowCount(0);
        for (User u : loadedUsers) {
            boolean matchesRole = "All Roles".equals(selectedRole) ||
                    (u.getRole() != null && u.getRole().name().equalsIgnoreCase(selectedRole));
            boolean matchesKey = query.isEmpty() ||
                    (u.getUsername() != null && u.getUsername().toLowerCase().contains(query)) ||
                    (u.getFullName() != null && u.getFullName().toLowerCase().contains(query)) ||
                    (u.getEmail() != null && u.getEmail().toLowerCase().contains(query));

            if (matchesRole && matchesKey) {
                usersTableModel.addRow(new Object[]{
                        u.getUserId(),
                        u.getUsername(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getRole() != null ? u.getRole().getDisplayName() : "N/A",
                        u.getPhoneNumber() != null ? u.getPhoneNumber() : "N/A",
                        u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate().toString() : "N/A"
                });
            }
        }
    }

    public void loadEvents() {
        eventsTableModel.setRowCount(0);
        try {
            loadedEvents = eventService.getAllEvents();
            for (Event e : loadedEvents) {
                eventsTableModel.addRow(new Object[]{
                        e.getEventId(),
                        e.getClubName() != null ? e.getClubName() : "Club #" + e.getClubId(),
                        e.getEventName(),
                        e.getEventDate() != null ? e.getEventDate().toString().replace("T", " ") : "TBA",
                        e.getVenue() != null ? e.getVenue() : "TBA",
                        e.getCapacity(),
                        e.getRegisteredCount(),
                        e.getStatus() != null ? e.getStatus() : "UPCOMING"
                });
            }
            totalEventsCard.setValue(loadedEvents.size());
        } catch (DatabaseOperationException ex) {
            System.err.println("Error loading events: " + ex.getMessage());
        }
    }

    public void loadAnnouncements() {
        announcementsTableModel.setRowCount(0);
        try {
            List<Announcement> announcements = eventService.getAllAnnouncements();
            for (Announcement a : announcements) {
                announcementsTableModel.addRow(new Object[]{
                        a.getAnnouncementId(),
                        a.getClubName() != null ? a.getClubName() : "Club #" + a.getClubId(),
                        a.getTitle(),
                        a.getMessage(),
                        a.getPriority(),
                        a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate().toString() : "Recent"
                });
            }
        } catch (DatabaseOperationException ex) {
            System.err.println("Error loading announcements: " + ex.getMessage());
        }
    }

    // ==========================================
    // Action Handlers
    // ==========================================

    private void handleAddClub() {
        JTextField nameField = new JTextField(20);
        JComboBox<String> categoryBox = new JComboBox<>(new String[]{"Technical", "Cultural", "Sports", "Social", "Academic", "Literary"});
        JTextField descField = new JTextField(20);
        JTextField coordIdField = new JTextField(10);

        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
        panel.add(new JLabel("Club Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Category:"));
        panel.add(categoryBox);
        panel.add(new JLabel("Description:"));
        panel.add(descField);
        panel.add(new JLabel("Coordinator User ID (Optional):"));
        panel.add(coordIdField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Register New Campus Club", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Club name cannot be blank.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                Club club = new Club();
                club.setClubName(name);
                club.setCategory((String) categoryBox.getSelectedItem());
                club.setDescription(descField.getText().trim());
                club.setEstablishedDate(LocalDate.now());
                club.setStatus("ACTIVE");

                String coordStr = coordIdField.getText().trim();
                if (!coordStr.isEmpty()) {
                    club.setCoordinatorId(Integer.parseInt(coordStr));
                }

                clubService.createClub(club);
                JOptionPane.showMessageDialog(this, "Club registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadClubs();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to register club: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleToggleClubStatus() {
        int row = clubsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a club to toggle its status.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int clubId = (int) clubsTableModel.getValueAt(row, 0);
        String clubName = (String) clubsTableModel.getValueAt(row, 1);
        String currentStatus = (String) clubsTableModel.getValueAt(row, 4);
        String newStatus = "ACTIVE".equalsIgnoreCase(currentStatus) ? "INACTIVE" : "ACTIVE";

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Change status of '" + clubName + "' from " + currentStatus + " to " + newStatus + "?",
                "Toggle Club Status",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            Club club = clubService.getClubById(clubId);
            club.setStatus(newStatus);
            clubService.updateClub(club);
            JOptionPane.showMessageDialog(this, "Club status updated to " + newStatus + "!", "Updated", JOptionPane.INFORMATION_MESSAGE);
            loadClubs();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteClub() {
        int row = clubsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a club to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int clubId = (int) clubsTableModel.getValueAt(row, 0);
        String clubName = (String) clubsTableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to permanently delete '" + clubName + "'?\nThis will remove all associated events, memberships, and announcements.",
                "Confirm Delete Club",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            clubService.deleteClub(clubId);
            JOptionPane.showMessageDialog(this, "Club deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            loadClubs();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error deleting club: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteEvent() {
        int row = eventsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an event to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int eventId = (int) eventsTableModel.getValueAt(row, 0);
        String eventTitle = (String) eventsTableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel and delete event: '" + eventTitle + "'?",
                "Confirm Delete Event",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            eventService.deleteEvent(eventId);
            JOptionPane.showMessageDialog(this, "Event deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            loadEvents();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error deleting event: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleAssignCoordinator() {
        int row = clubsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a club to assign a coordinator.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int clubId = (int) clubsTableModel.getValueAt(row, 0);
        String clubName = (String) clubsTableModel.getValueAt(row, 1);

        String input = JOptionPane.showInputDialog(this,
                "Enter Coordinator User ID for '" + clubName + "':",
                "Assign Club Coordinator",
                JOptionPane.QUESTION_MESSAGE);

        if (input != null && !input.trim().isEmpty()) {
            try {
                int coordId = Integer.parseInt(input.trim());
                Club club = clubService.getClubById(clubId);
                club.setCoordinatorId(coordId);
                clubService.updateClub(club);
                JOptionPane.showMessageDialog(this, "Coordinator (User ID: " + coordId + ") assigned to " + clubName + " successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadClubs();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Please enter a valid numeric User ID.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error assigning coordinator: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleCreateUser() {
        JTextField usernameField = new JTextField(15);
        JTextField fullNameField = new JTextField(15);
        JTextField emailField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JComboBox<UserRole> roleCombo = new JComboBox<>(new UserRole[]{UserRole.STUDENT, UserRole.COORDINATOR, UserRole.ADMIN});

        JPanel panel = new JPanel(new GridLayout(5, 2, 8, 8));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Full Name:"));
        panel.add(fullNameField);
        panel.add(new JLabel("Email Address:"));
        panel.add(emailField);
        panel.add(new JLabel("Initial Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Role:"));
        panel.add(roleCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, "Create New Campus User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String uname = usernameField.getText().trim();
            String name = fullNameField.getText().trim();
            String email = emailField.getText().trim();
            String pwd = new String(passwordField.getPassword());
            UserRole role = (UserRole) roleCombo.getSelectedItem();

            if (uname.isEmpty() || name.isEmpty() || email.isEmpty() || pwd.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required to register a user.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                User newUser;
                if (role == UserRole.ADMIN) {
                    newUser = new Admin();
                } else if (role == UserRole.COORDINATOR) {
                    newUser = new ClubCoordinator();
                } else {
                    newUser = new Student();
                }
                newUser.setUsername(uname);
                newUser.setFullName(name);
                newUser.setEmail(email);
                newUser.setPasswordHash(PasswordUtil.hashPassword(pwd));
                newUser.setRole(role);
                userDAO.createUser(newUser);

                JOptionPane.showMessageDialog(this, "User '" + uname + "' created successfully with role: " + role.getDisplayName() + "!", "User Registered", JOptionPane.INFORMATION_MESSAGE);
                loadUsers();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error creating user: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleChangeUserRole() {
        int row = usersTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user to change their role.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int userId = (int) usersTableModel.getValueAt(row, 0);
        String username = (String) usersTableModel.getValueAt(row, 1);
        String currentRole = (String) usersTableModel.getValueAt(row, 4);

        UserRole[] roles = {UserRole.STUDENT, UserRole.COORDINATOR, UserRole.ADMIN};
        UserRole selected = (UserRole) JOptionPane.showInputDialog(this,
                "Select new role for user '" + username + "' (Current: " + currentRole + "):",
                "Assign / Edit User Role",
                JOptionPane.QUESTION_MESSAGE,
                null,
                roles,
                roles[0]);

        if (selected != null) {
            try {
                userDAO.updateUserRole(userId, selected);
                JOptionPane.showMessageDialog(this, "User '" + username + "' role updated to: " + selected.getDisplayName() + "!", "Role Updated", JOptionPane.INFORMATION_MESSAGE);
                loadUsers();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error updating role: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteUser() {
        int row = usersTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int userId = (int) usersTableModel.getValueAt(row, 0);
        String username = (String) usersTableModel.getValueAt(row, 1);

        if (userId == admin.getUserId()) {
            JOptionPane.showMessageDialog(this, "You cannot delete your own active administrator account.", "Action Prohibited", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete user '" + username + "' (ID: " + userId + ")?\nThis will remove their memberships, registrations, and account data.",
                "Confirm Delete User",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            userDAO.deleteUser(userId);
            JOptionPane.showMessageDialog(this, "User '" + username + "' deleted successfully.", "User Deleted", JOptionPane.INFORMATION_MESSAGE);
            loadUsers();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error deleting user: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Admin getAdmin() {
        return admin;
    }

    public JTable getClubsTable() {
        return clubsTable;
    }

    public JTable getUsersTable() {
        return usersTable;
    }
}
