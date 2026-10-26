package com.campusclub.gui;

import com.campusclub.exception.ClubNotFoundException;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.exception.DuplicateMembershipException;
import com.campusclub.exception.EventRegistrationException;
import com.campusclub.model.Announcement;
import com.campusclub.model.Club;
import com.campusclub.model.Event;
import com.campusclub.model.EventRegistration;
import com.campusclub.model.Membership;
import com.campusclub.model.Student;
import com.campusclub.recommendation.ClubRecommendationEngine;
import com.campusclub.service.ClubService;
import com.campusclub.service.EventService;
import com.campusclub.service.MembershipService;
import com.campusclub.service.StudentService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Swing Dashboard Panel for Students.
 * Features:
 * - Summary metric cards
 * - Tab 1: Discover Clubs (Keyword search + Category filter + Join action)
 * - Tab 2: My Memberships (Status view + Leave club action)
 * - Tab 3: Upcoming Events (Search + Register action)
 * - Tab 4: My Registrations (View + Cancel action)
 * - Tab 5: Announcements (View club & campus broadcasts)
 * - Tab 6: Smart Recommendations (Rule-based Max-Heap scoring with direct apply)
 * - Tab 7: My Profile & Interests (Profile viewing & editing)
 */
public class StudentDashboard extends JPanel {
    private final Student student;
    private final ClubService clubService;
    private final MembershipService membershipService;
    private final EventService eventService;
    private final StudentService studentService;
    private final ClubRecommendationEngine recommendationEngine;

    // Top Metric Cards
    private DashboardCard joinedClubsCard;
    private DashboardCard registeredEventsCard;
    private DashboardCard availableEventsCard;
    private DashboardCard recommendationsCard;

    // Tab 1: Discover Clubs
    private JTextField clubSearchField;
    private JComboBox<String> clubCategoryFilter;
    private JTable clubsTable;
    private DefaultTableModel clubsTableModel;
    private List<Club> loadedClubs = new ArrayList<>();

    // Tab 2: My Memberships
    private JTable myMembershipsTable;
    private DefaultTableModel myMembershipsTableModel;
    private List<Membership> loadedMemberships = new ArrayList<>();

    // Tab 3: Upcoming Events
    private JTextField eventSearchField;
    private JTable eventsTable;
    private DefaultTableModel eventsTableModel;
    private List<Event> loadedEvents = new ArrayList<>();

    // Tab 4: My Registrations
    private JTable myRegistrationsTable;
    private DefaultTableModel myRegistrationsTableModel;
    private List<EventRegistration> loadedRegistrations = new ArrayList<>();

    // Tab 5: Announcements
    private JTable announcementsTable;
    private DefaultTableModel announcementsTableModel;

    // Tab 6: Recommendations
    private DefaultListModel<String> recommendationsListModel;
    private JList<String> recommendationsList;
    private List<ClubRecommendationEngine.ClubRecommendation> activeRecommendations = new ArrayList<>();

    // Tab 7: My Profile
    private JTextField profilePhoneField;
    private JTextField profileInterestsField;
    private JLabel profileUsnLabel;
    private JLabel profileDeptLabel;
    private JLabel profileSemLabel;
    private JLabel profileEmailLabel;

    public StudentDashboard(Student student) {
        this(student, new ClubService(), new MembershipService(), new EventService(), new StudentService(), new ClubRecommendationEngine());
    }

    public StudentDashboard(Student student, ClubService clubService, MembershipService membershipService,
                            EventService eventService, StudentService studentService, ClubRecommendationEngine recommendationEngine) {
        this.student = student != null ? student : new Student();
        this.clubService = clubService != null ? clubService : new ClubService();
        this.membershipService = membershipService != null ? membershipService : new MembershipService();
        this.eventService = eventService != null ? eventService : new EventService();
        this.studentService = studentService != null ? studentService : new StudentService();
        this.recommendationEngine = recommendationEngine != null ? recommendationEngine : new ClubRecommendationEngine();

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

        joinedClubsCard = new DashboardCard("My Clubs", "0", "Approved / Pending", UITheme.PRIMARY_BLUE);
        registeredEventsCard = new DashboardCard("My Events", "0", "Active Registrations", UITheme.ACCENT_EMERALD);
        availableEventsCard = new DashboardCard("Campus Events", "0", "Upcoming Schedule", UITheme.WARNING_AMBER);
        recommendationsCard = new DashboardCard("Smart Matches", "0", "Recommended For You", UITheme.PURPLE_ACCENT);

        metricsPanel.add(joinedClubsCard);
        metricsPanel.add(registeredEventsCard);
        metricsPanel.add(availableEventsCard);
        metricsPanel.add(recommendationsCard);

        add(metricsPanel, BorderLayout.NORTH);

        // Center: Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_HEADER);

        tabbedPane.addTab("🔍 Discover Clubs", createDiscoverClubsPanel());
        tabbedPane.addTab("🏷️ My Memberships", createMembershipsPanel());
        tabbedPane.addTab("📅 Upcoming Events", createUpcomingEventsPanel());
        tabbedPane.addTab("🎟️ My Event Passes", createRegistrationsPanel());
        tabbedPane.addTab("📢 Announcements", createAnnouncementsPanel());
        tabbedPane.addTab("⭐ Club Recommendations", createRecommendationsPanel());
        tabbedPane.addTab("👤 My Profile", createProfilePanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==========================================
    // Tab 1: Discover Clubs Panel
    // ==========================================
    private JPanel createDiscoverClubsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UITheme.CARD_BG);

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterBar.setOpaque(false);

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(UITheme.FONT_HEADER);
        clubSearchField = new JTextField(15);

        JLabel catLbl = new JLabel("Category:");
        catLbl.setFont(UITheme.FONT_HEADER);
        clubCategoryFilter = new JComboBox<>(new String[]{"All Categories", "Technical", "Cultural", "Sports", "Social", "Academic", "Literary"});

        JButton filterBtn = UITheme.createPrimaryButton("Filter");
        filterBtn.addActionListener(e -> applyClubFilter());

        JButton resetBtn = UITheme.createSecondaryButton("Reset");
        resetBtn.addActionListener(e -> {
            clubSearchField.setText("");
            clubCategoryFilter.setSelectedIndex(0);
            applyClubFilter();
        });

        filterBar.add(searchLbl);
        filterBar.add(clubSearchField);
        filterBar.add(catLbl);
        filterBar.add(clubCategoryFilter);
        filterBar.add(filterBtn);
        filterBar.add(resetBtn);

        panel.add(filterBar, BorderLayout.NORTH);

        // Table
        String[] columns = {"Club ID", "Club Name", "Category", "Description", "Coordinator", "Active Members"};
        clubsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        clubsTable = new JTable(clubsTableModel);
        UITheme.styleTable(clubsTable);
        panel.add(new JScrollPane(clubsTable), BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionsPanel.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadClubs());

        JButton joinBtn = UITheme.createPrimaryButton("Join Selected Club");
        joinBtn.addActionListener(e -> handleJoinClub());

        actionsPanel.add(refreshBtn);
        actionsPanel.add(joinBtn);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // Tab 2: My Memberships Panel
    // ==========================================
    private JPanel createMembershipsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UITheme.CARD_BG);

        String[] columns = {"Membership ID", "Club ID", "Club Name", "Category", "Joined Date", "Role", "Status"};
        myMembershipsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        myMembershipsTable = new JTable(myMembershipsTableModel);
        UITheme.styleTable(myMembershipsTable);
        panel.add(new JScrollPane(myMembershipsTable), BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionsPanel.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadMemberships());

        JButton leaveBtn = UITheme.createDangerButton("Leave / Withdraw Membership");
        leaveBtn.addActionListener(e -> handleLeaveClub());

        actionsPanel.add(refreshBtn);
        actionsPanel.add(leaveBtn);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // Tab 3: Upcoming Events Panel
    // ==========================================
    private JPanel createUpcomingEventsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UITheme.CARD_BG);

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchBar.setOpaque(false);

        JLabel searchLbl = new JLabel("Search Events:");
        searchLbl.setFont(UITheme.FONT_HEADER);
        eventSearchField = new JTextField(18);

        JButton searchBtn = UITheme.createPrimaryButton("Search");
        searchBtn.addActionListener(e -> applyEventSearch());

        JButton resetBtn = UITheme.createSecondaryButton("Show All");
        resetBtn.addActionListener(e -> {
            eventSearchField.setText("");
            loadEvents();
        });

        searchBar.add(searchLbl);
        searchBar.add(eventSearchField);
        searchBar.add(searchBtn);
        searchBar.add(resetBtn);

        panel.add(searchBar, BorderLayout.NORTH);

        String[] columns = {"Event ID", "Club Name", "Event Title", "Date & Time", "Location", "Capacity", "Registered"};
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

        JButton registerBtn = UITheme.createSuccessButton("Register for Selected Event");
        registerBtn.addActionListener(e -> handleEventRegistration());

        actionsPanel.add(refreshBtn);
        actionsPanel.add(registerBtn);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // Tab 4: My Registrations Panel
    // ==========================================
    private JPanel createRegistrationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UITheme.CARD_BG);

        String[] columns = {"Registration ID", "Event ID", "Event Title", "Club Name", "Event Date", "Venue", "Registered At", "Status"};
        myRegistrationsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        myRegistrationsTable = new JTable(myRegistrationsTableModel);
        UITheme.styleTable(myRegistrationsTable);
        panel.add(new JScrollPane(myRegistrationsTable), BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionsPanel.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadRegistrations());

        JButton cancelBtn = UITheme.createDangerButton("Cancel Registration");
        cancelBtn.addActionListener(e -> handleCancelRegistration());

        actionsPanel.add(refreshBtn);
        actionsPanel.add(cancelBtn);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // Tab 5: Announcements Panel
    // ==========================================
    private JPanel createAnnouncementsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UITheme.CARD_BG);

        String[] columns = {"ID", "Club", "Announcement Title", "Message", "Priority", "Posted Date"};
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
    // Tab 6: Recommendations Panel
    // ==========================================
    private JPanel createRecommendationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setBackground(UITheme.CARD_BG);

        JPanel header = UITheme.createSectionHeader(
                "⭐ Rule-Based Smart Club Recommendations",
                "Calculated based on your interests, academic department, and club activity."
        );
        panel.add(header, BorderLayout.NORTH);

        recommendationsListModel = new DefaultListModel<>();
        recommendationsList = new JList<>(recommendationsListModel);
        recommendationsList.setFont(UITheme.FONT_REGULAR);
        recommendationsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recommendationsList.setBorder(new EmptyBorder(8, 8, 8, 8));

        panel.add(new JScrollPane(recommendationsList), BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionsPanel.setOpaque(false);

        JButton applyRecBtn = UITheme.createPrimaryButton("Apply to Selected Recommendation");
        applyRecBtn.addActionListener(e -> handleApplyRecommendation());

        JButton recomputeBtn = UITheme.createSecondaryButton("Recompute Recommendations");
        recomputeBtn.addActionListener(e -> loadRecommendations());

        actionsPanel.add(recomputeBtn);
        actionsPanel.add(applyRecBtn);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // Tab 7: Profile & Interests Panel
    // ==========================================
    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(UITheme.CARD_BG);

        JPanel header = UITheme.createSectionHeader("👤 Student Profile & Interests", "View and update your academic details and club matching interests.");
        panel.add(header, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Full Name & USN
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        JLabel nameVal = new JLabel(student.getFullName());
        nameVal.setFont(UITheme.FONT_BOLD);
        formPanel.add(nameVal, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Register No. / USN:"), gbc);
        gbc.gridx = 1;
        profileUsnLabel = new JLabel(student.getRegisterNumber() != null ? student.getRegisterNumber() : "N/A");
        profileUsnLabel.setFont(UITheme.FONT_BOLD);
        formPanel.add(profileUsnLabel, gbc);

        // Row 2: Department & Semester
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Department:"), gbc);
        gbc.gridx = 1;
        profileDeptLabel = new JLabel(student.getDepartment() != null ? student.getDepartment() : "General");
        formPanel.add(profileDeptLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Semester / Year:"), gbc);
        gbc.gridx = 1;
        profileSemLabel = new JLabel("Sem " + student.getSemester() + " (Year " + student.getYear() + ")");
        formPanel.add(profileSemLabel, gbc);

        // Row 4: Email
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Email Address:"), gbc);
        gbc.gridx = 1;
        profileEmailLabel = new JLabel(student.getEmail());
        formPanel.add(profileEmailLabel, gbc);

        // Row 5: Editable Phone
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Phone Number:"), gbc);
        gbc.gridx = 1;
        profilePhoneField = new JTextField(student.getPhoneNumber() != null ? student.getPhoneNumber() : "", 20);
        formPanel.add(profilePhoneField, gbc);

        // Row 6: Editable Interests
        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(new JLabel("Interests (comma separated):"), gbc);
        gbc.gridx = 1;
        profileInterestsField = new JTextField(student.getInterests() != null ? student.getInterests() : "AI, Coding, Robotics", 25);
        formPanel.add(profileInterestsField, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionsPanel.setOpaque(false);

        JButton saveBtn = UITheme.createPrimaryButton("Save Profile Changes");
        saveBtn.addActionListener(e -> handleSaveProfile());

        actionsPanel.add(saveBtn);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // Data Loading & Business Logic
    // ==========================================

    public void loadAllData() {
        loadClubs();
        loadMemberships();
        loadEvents();
        loadRegistrations();
        loadAnnouncements();
        loadRecommendations();
        updateMetrics();
    }

    private void updateMetrics() {
        joinedClubsCard.setValue(loadedMemberships.size());
        registeredEventsCard.setValue(loadedRegistrations.size());
        availableEventsCard.setValue(loadedEvents.size());
        recommendationsCard.setValue(activeRecommendations.size());
    }

    public void loadClubs() {
        clubsTableModel.setRowCount(0);
        try {
            loadedClubs = clubService.getActiveClubs();
            for (Club c : loadedClubs) {
                clubsTableModel.addRow(new Object[]{
                        c.getClubId(),
                        c.getClubName(),
                        c.getCategory(),
                        c.getDescription(),
                        c.getCoordinatorName() != null ? c.getCoordinatorName() : "Unassigned",
                        c.getMemberCount()
                });
            }
        } catch (DatabaseOperationException ex) {
            System.err.println("Error loading clubs: " + ex.getMessage());
        }
        updateMetrics();
    }

    private void applyClubFilter() {
        String keyword = clubSearchField.getText().trim().toLowerCase();
        String selectedCat = (String) clubCategoryFilter.getSelectedItem();

        clubsTableModel.setRowCount(0);
        for (Club c : loadedClubs) {
            boolean matchesCat = "All Categories".equals(selectedCat) || (c.getCategory() != null && c.getCategory().equalsIgnoreCase(selectedCat));
            boolean matchesKey = keyword.isEmpty() ||
                    (c.getClubName() != null && c.getClubName().toLowerCase().contains(keyword)) ||
                    (c.getDescription() != null && c.getDescription().toLowerCase().contains(keyword));

            if (matchesCat && matchesKey) {
                clubsTableModel.addRow(new Object[]{
                        c.getClubId(),
                        c.getClubName(),
                        c.getCategory(),
                        c.getDescription(),
                        c.getCoordinatorName() != null ? c.getCoordinatorName() : "Unassigned",
                        c.getMemberCount()
                });
            }
        }
    }

    public void loadMemberships() {
        myMembershipsTableModel.setRowCount(0);
        try {
            loadedMemberships = membershipService.getStudentMemberships(student.getUserId());
            for (Membership m : loadedMemberships) {
                myMembershipsTableModel.addRow(new Object[]{
                        m.getMembershipId(),
                        m.getClubId(),
                        m.getClubName(),
                        m.getClubCategory() != null ? m.getClubCategory() : "General",
                        m.getJoinedAt() != null ? m.getJoinedAt().toLocalDate().toString() : "N/A",
                        m.getRoleInClub(),
                        m.getStatus()
                });
            }
        } catch (DatabaseOperationException ex) {
            System.err.println("Error loading memberships: " + ex.getMessage());
        }
        updateMetrics();
    }

    public void loadEvents() {
        eventsTableModel.setRowCount(0);
        try {
            loadedEvents = eventService.getUpcomingEvents();
            for (Event e : loadedEvents) {
                eventsTableModel.addRow(new Object[]{
                        e.getEventId(),
                        e.getClubName() != null ? e.getClubName() : "Club #" + e.getClubId(),
                        e.getEventName(),
                        e.getEventDate() != null ? e.getEventDate().toString().replace("T", " ") : "TBA",
                        e.getVenue() != null ? e.getVenue() : "TBA",
                        e.getCapacity(),
                        e.getRegisteredCount()
                });
            }
        } catch (DatabaseOperationException ex) {
            System.err.println("Error loading events: " + ex.getMessage());
        }
        updateMetrics();
    }

    private void applyEventSearch() {
        String query = eventSearchField.getText().trim().toLowerCase();
        eventsTableModel.setRowCount(0);
        for (Event e : loadedEvents) {
            boolean match = query.isEmpty() ||
                    (e.getEventName() != null && e.getEventName().toLowerCase().contains(query)) ||
                    (e.getClubName() != null && e.getClubName().toLowerCase().contains(query)) ||
                    (e.getVenue() != null && e.getVenue().toLowerCase().contains(query));
            if (match) {
                eventsTableModel.addRow(new Object[]{
                        e.getEventId(),
                        e.getClubName() != null ? e.getClubName() : "Club #" + e.getClubId(),
                        e.getEventName(),
                        e.getEventDate() != null ? e.getEventDate().toString().replace("T", " ") : "TBA",
                        e.getVenue() != null ? e.getVenue() : "TBA",
                        e.getCapacity(),
                        e.getRegisteredCount()
                });
            }
        }
    }

    public void loadRegistrations() {
        myRegistrationsTableModel.setRowCount(0);
        try {
            loadedRegistrations = eventService.getStudentRegistrations(student.getUserId());
            for (EventRegistration reg : loadedRegistrations) {
                myRegistrationsTableModel.addRow(new Object[]{
                        reg.getRegistrationId(),
                        reg.getEventId(),
                        reg.getEventName() != null ? reg.getEventName() : "Event #" + reg.getEventId(),
                        reg.getClubName() != null ? reg.getClubName() : "N/A",
                        reg.getEventDate() != null ? reg.getEventDate().toString().replace("T", " ") : "N/A",
                        reg.getLocation() != null ? reg.getLocation() : "Campus",
                        reg.getRegisteredAt() != null ? reg.getRegisteredAt().toLocalDate().toString() : "N/A",
                        reg.getRegistrationStatus()
                });
            }
        } catch (DatabaseOperationException ex) {
            System.err.println("Error loading registrations: " + ex.getMessage());
        }
        updateMetrics();
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

    public void loadRecommendations() {
        recommendationsListModel.clear();
        activeRecommendations.clear();
        try {
            List<Club> clubs = clubService.getActiveClubs();
            List<Membership> memberships = membershipService.getStudentMemberships(student.getUserId());
            activeRecommendations = recommendationEngine.recommendClubs(student, clubs, memberships);

            if (activeRecommendations.isEmpty()) {
                recommendationsListModel.addElement("No new recommendations available. You are already part of top matched clubs!");
            } else {
                for (int i = 0; i < activeRecommendations.size(); i++) {
                    ClubRecommendationEngine.ClubRecommendation r = activeRecommendations.get(i);
                    String item = String.format("#%d  ⭐ %s  [%s]  —  Score: %d pts  |  Reasons: %s",
                            (i + 1),
                            r.getClub().getClubName(),
                            r.getClub().getCategory(),
                            r.getScore(),
                            r.getReasonSummary());
                    recommendationsListModel.addElement(item);
                }
            }
        } catch (Exception ex) {
            recommendationsListModel.addElement("Unable to compute recommendations currently: " + ex.getMessage());
        }
        updateMetrics();
    }

    // ==========================================
    // Action Handlers
    // ==========================================

    private void handleJoinClub() {
        int selectedRow = clubsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a club from the table to join.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int clubId = (int) clubsTableModel.getValueAt(selectedRow, 0);
        String clubName = (String) clubsTableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Submit membership request to join: " + clubName + "?",
                "Confirm Club Application",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            membershipService.applyForMembership(student.getUserId(), clubId);
            JOptionPane.showMessageDialog(this,
                    "Application submitted successfully for " + clubName + "!\nAwaiting coordinator approval.",
                    "Application Sent",
                    JOptionPane.INFORMATION_MESSAGE);
            loadMemberships();
            loadRecommendations();
        } catch (DuplicateMembershipException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Already Member / Pending",
                    JOptionPane.WARNING_MESSAGE);
        } catch (ClubNotFoundException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Club Not Found",
                    JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseOperationException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database operation error: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error applying for club: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleLeaveClub() {
        int selectedRow = myMembershipsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a membership to withdraw/leave.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int clubId = (int) myMembershipsTableModel.getValueAt(selectedRow, 1);
        String clubName = (String) myMembershipsTableModel.getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to leave " + clubName + "?",
                "Confirm Leave Club",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            membershipService.leaveClub(student.getUserId(), clubId);
            JOptionPane.showMessageDialog(this, "You have left " + clubName + ".", "Membership Updated", JOptionPane.INFORMATION_MESSAGE);
            loadMemberships();
            loadClubs();
            loadRecommendations();
        } catch (DatabaseOperationException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleEventRegistration() {
        int selectedRow = eventsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an event to register for.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int eventId = (int) eventsTableModel.getValueAt(selectedRow, 0);
        String eventTitle = (String) eventsTableModel.getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Confirm registration for: " + eventTitle + "?",
                "Register for Event",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            eventService.registerForEvent(eventId, student.getUserId());
            JOptionPane.showMessageDialog(this, "Successfully registered for " + eventTitle + "!", "Registration Confirmed", JOptionPane.INFORMATION_MESSAGE);
            loadEvents();
            loadRegistrations();
        } catch (EventRegistrationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration Notice", JOptionPane.WARNING_MESSAGE);
        } catch (DatabaseOperationException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCancelRegistration() {
        int selectedRow = myRegistrationsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a registration to cancel.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int eventId = (int) myRegistrationsTableModel.getValueAt(selectedRow, 1);
        String eventTitle = (String) myRegistrationsTableModel.getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Cancel registration for event: " + eventTitle + "?",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            eventService.cancelRegistration(eventId, student.getUserId());
            JOptionPane.showMessageDialog(this, "Registration cancelled successfully.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            loadEvents();
            loadRegistrations();
        } catch (DatabaseOperationException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleApplyRecommendation() {
        int selectedIndex = recommendationsList.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= activeRecommendations.size()) {
            JOptionPane.showMessageDialog(this, "Please select a recommendation from the list.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ClubRecommendationEngine.ClubRecommendation rec = activeRecommendations.get(selectedIndex);
        Club club = rec.getClub();

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Apply to join recommended club: " + club.getClubName() + " (Score: " + rec.getScore() + ")?",
                "Apply to Recommended Club",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            membershipService.applyForMembership(student.getUserId(), club.getClubId());
            JOptionPane.showMessageDialog(this, "Application submitted for " + club.getClubName() + "!", "Application Sent", JOptionPane.INFORMATION_MESSAGE);
            loadMemberships();
            loadRecommendations();
        } catch (DuplicateMembershipException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Membership Notice", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSaveProfile() {
        String phone = profilePhoneField.getText().trim();
        String interests = profileInterestsField.getText().trim();

        student.setPhoneNumber(phone);
        student.setInterests(interests);

        try {
            studentService.updateProfile(student);
            JOptionPane.showMessageDialog(this, "Profile updated successfully!\nRecommendations updated.", "Profile Saved", JOptionPane.INFORMATION_MESSAGE);
            loadRecommendations();
        } catch (DatabaseOperationException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Student getStudent() {
        return student;
    }

    public JTable getClubsTable() {
        return clubsTable;
    }

    public JTable getEventsTable() {
        return eventsTable;
    }

    public DefaultTableModel getClubsTableModel() {
        return clubsTableModel;
    }
}
