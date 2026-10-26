package com.campusclub.gui;

import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.model.Announcement;
import com.campusclub.model.Club;
import com.campusclub.model.ClubCoordinator;
import com.campusclub.model.Event;
import com.campusclub.model.EventRegistration;
import com.campusclub.model.Membership;
import com.campusclub.service.ClubService;
import com.campusclub.service.EventService;
import com.campusclub.service.MembershipService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Swing Dashboard Panel for Club Coordinators.
 * Features:
 * - Metric summary cards (Managed Club, Total Members, Pending Requests, Scheduled Events)
 * - Tab 1: Club Information & Description Management
 * - Tab 2: Membership Applications (Approve/Reject requests)
 * - Tab 3: Event Scheduling & Attendee Roster Viewing
 * - Tab 4: Club Announcements Broadcast & History
 */
public class CoordinatorDashboard extends JPanel {
    private final ClubCoordinator coordinator;
    private final ClubService clubService;
    private final MembershipService membershipService;
    private final EventService eventService;

    private Club managedClub;

    // Top Metric Cards
    private DashboardCard clubNameCard;
    private DashboardCard memberCountCard;
    private DashboardCard pendingRequestsCard;
    private DashboardCard eventCountCard;

    // Tab 1: Club Info
    private JLabel clubNameLabel;
    private JLabel clubCatLabel;
    private JLabel clubEstLabel;
    private JLabel clubStatusLabel;
    private JTextArea clubDescArea;

    // Tab 2: Membership Requests
    private JComboBox<String> membershipFilterBox;
    private JTable membershipRequestsTable;
    private DefaultTableModel membershipRequestsTableModel;
    private List<Membership> loadedMemberships = new ArrayList<>();

    // Tab 3: Manage Events
    private JTable clubEventsTable;
    private DefaultTableModel clubEventsTableModel;
    private List<Event> loadedEvents = new ArrayList<>();

    // Tab 4: Announcements
    private JTable announcementsTable;
    private DefaultTableModel announcementsTableModel;
    private JTextField announcementTitleField;
    private JTextArea announcementContentArea;
    private JComboBox<String> priorityBox;

    public CoordinatorDashboard(ClubCoordinator coordinator) {
        this(coordinator, new ClubService(), new MembershipService(), new EventService());
    }

    public CoordinatorDashboard(ClubCoordinator coordinator, ClubService clubService, MembershipService membershipService, EventService eventService) {
        this.coordinator = coordinator != null ? coordinator : new ClubCoordinator();
        this.clubService = clubService != null ? clubService : new ClubService();
        this.membershipService = membershipService != null ? membershipService : new MembershipService();
        this.eventService = eventService != null ? eventService : new EventService();

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

        clubNameCard = new DashboardCard("Managed Club", "Loading...", "Assigned Activity", UITheme.PRIMARY_BLUE);
        memberCountCard = new DashboardCard("Total Members", "0", "Approved Active", UITheme.ACCENT_EMERALD);
        pendingRequestsCard = new DashboardCard("Pending Requests", "0", "Awaiting Action", UITheme.WARNING_AMBER);
        eventCountCard = new DashboardCard("Club Events", "0", "Active Schedules", UITheme.PURPLE_ACCENT);

        metricsPanel.add(clubNameCard);
        metricsPanel.add(memberCountCard);
        metricsPanel.add(pendingRequestsCard);
        metricsPanel.add(eventCountCard);

        add(metricsPanel, BorderLayout.NORTH);

        // Center: Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_HEADER);

        tabbedPane.addTab("🏢 Club Details", createClubInfoPanel());
        tabbedPane.addTab("👥 Membership Requests", createMembershipRequestsPanel());
        tabbedPane.addTab("📅 Manage Events & Rosters", createEventsPanel());
        tabbedPane.addTab("📢 Broadcast Announcements", createAnnouncementPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==========================================
    // Tab 1: Club Info Panel
    // ==========================================
    private JPanel createClubInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(UITheme.CARD_BG);

        JPanel header = UITheme.createSectionHeader("🏢 Club Overview & Settings", "Manage profile and details for your assigned club.");
        panel.add(header, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Club Name:"), gbc);
        gbc.gridx = 1;
        clubNameLabel = new JLabel("Loading...");
        clubNameLabel.setFont(UITheme.FONT_BOLD);
        formPanel.add(clubNameLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        clubCatLabel = new JLabel("-");
        formPanel.add(clubCatLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        clubStatusLabel = new JLabel("-");
        formPanel.add(clubStatusLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Established Date:"), gbc);
        gbc.gridx = 1;
        clubEstLabel = new JLabel("-");
        formPanel.add(clubEstLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        clubDescArea = new JTextArea(4, 30);
        clubDescArea.setLineWrap(true);
        clubDescArea.setWrapStyleWord(true);
        clubDescArea.setFont(UITheme.FONT_REGULAR);
        clubDescArea.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(6, 6, 6, 6)
        ));
        formPanel.add(new JScrollPane(clubDescArea), gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionsPanel.setOpaque(false);

        JButton saveDescBtn = UITheme.createPrimaryButton("Update Club Description");
        saveDescBtn.addActionListener(e -> handleUpdateDescription());

        actionsPanel.add(saveDescBtn);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // Tab 2: Membership Requests Panel
    // ==========================================
    private JPanel createMembershipRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UITheme.CARD_BG);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterBar.setOpaque(false);

        JLabel filterLbl = new JLabel("Filter by Status:");
        filterLbl.setFont(UITheme.FONT_HEADER);
        membershipFilterBox = new JComboBox<>(new String[]{"All Statuses", "PENDING", "APPROVED", "REJECTED"});
        membershipFilterBox.addActionListener(e -> applyMembershipFilter());

        filterBar.add(filterLbl);
        filterBar.add(membershipFilterBox);
        panel.add(filterBar, BorderLayout.NORTH);

        String[] columns = {"Membership ID", "Student ID", "Student Name", "USN", "Department", "Applied Date", "Status"};
        membershipRequestsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        membershipRequestsTable = new JTable(membershipRequestsTableModel);
        UITheme.styleTable(membershipRequestsTable);
        panel.add(new JScrollPane(membershipRequestsTable), BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionsPanel.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadMembershipRequests());

        JButton approveBtn = UITheme.createSuccessButton("Approve Request");
        approveBtn.addActionListener(e -> handleApprove());

        JButton rejectBtn = UITheme.createDangerButton("Reject Request");
        rejectBtn.addActionListener(e -> handleReject());

        actionsPanel.add(refreshBtn);
        actionsPanel.add(approveBtn);
        actionsPanel.add(rejectBtn);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // Tab 3: Manage Events Panel
    // ==========================================
    private JPanel createEventsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UITheme.CARD_BG);

        String[] columns = {"Event ID", "Title", "Date & Time", "Location", "Capacity", "Registrations", "Status"};
        clubEventsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        clubEventsTable = new JTable(clubEventsTableModel);
        UITheme.styleTable(clubEventsTable);
        panel.add(new JScrollPane(clubEventsTable), BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionsPanel.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadClubEvents());

        JButton rosterBtn = UITheme.createSecondaryButton("View Attendee Roster");
        rosterBtn.addActionListener(e -> handleViewAttendees());

        JButton newEventBtn = UITheme.createPrimaryButton("Create New Event");
        newEventBtn.addActionListener(e -> handleCreateEventDialog());

        JButton deleteEventBtn = UITheme.createDangerButton("Delete Event");
        deleteEventBtn.addActionListener(e -> handleDeleteEvent());

        actionsPanel.add(refreshBtn);
        actionsPanel.add(rosterBtn);
        actionsPanel.add(newEventBtn);
        actionsPanel.add(deleteEventBtn);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // Tab 4: Announcements Panel
    // ==========================================
    private JPanel createAnnouncementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setBackground(UITheme.CARD_BG);

        // Top Form: Create Announcement
        JPanel formContainer = UITheme.createCardPanel();
        formContainer.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLbl = new JLabel("Announcement Title:");
        titleLbl.setFont(UITheme.FONT_HEADER);
        announcementTitleField = new JTextField(25);

        JLabel contentLbl = new JLabel("Message / Details:");
        contentLbl.setFont(UITheme.FONT_HEADER);
        announcementContentArea = new JTextArea(3, 25);
        announcementContentArea.setLineWrap(true);
        announcementContentArea.setWrapStyleWord(true);

        JLabel priorityLbl = new JLabel("Priority Level:");
        priorityLbl.setFont(UITheme.FONT_HEADER);
        priorityBox = new JComboBox<>(new String[]{"MEDIUM", "HIGH", "LOW"});

        JButton postBtn = UITheme.createPrimaryButton("Broadcast Announcement");
        postBtn.addActionListener(e -> handlePostAnnouncement());

        gbc.gridx = 0; gbc.gridy = 0; formContainer.add(titleLbl, gbc);
        gbc.gridx = 1; gbc.gridy = 0; formContainer.add(announcementTitleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formContainer.add(contentLbl, gbc);
        gbc.gridx = 1; gbc.gridy = 1; formContainer.add(new JScrollPane(announcementContentArea), gbc);

        gbc.gridx = 0; gbc.gridy = 2; formContainer.add(priorityLbl, gbc);
        gbc.gridx = 1; gbc.gridy = 2; formContainer.add(priorityBox, gbc);

        gbc.gridx = 1; gbc.gridy = 3; formContainer.add(postBtn, gbc);

        panel.add(formContainer, BorderLayout.NORTH);

        // Bottom Table: Past Announcements
        String[] columns = {"ID", "Title", "Message", "Priority", "Posted Date"};
        announcementsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        announcementsTable = new JTable(announcementsTableModel);
        UITheme.styleTable(announcementsTable);

        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.setOpaque(false);
        tablePanel.add(new JLabel("Past Broadcasts for this Club:"), BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(announcementsTable), BorderLayout.CENTER);

        panel.add(tablePanel, BorderLayout.CENTER);

        return panel;
    }

    // ==========================================
    // Data Loading & Business Logic
    // ==========================================

    public void loadAllData() {
        loadCoordinatorClub();
    }

    private void loadCoordinatorClub() {
        try {
            Optional<Club> optionalClub = clubService.getClubByCoordinatorId(coordinator.getUserId());
            if (optionalClub.isPresent()) {
                managedClub = optionalClub.get();
                clubNameCard.setValue(managedClub.getClubName());
                clubNameCard.setSubtitle("Category: " + managedClub.getCategory());

                clubNameLabel.setText(managedClub.getClubName());
                clubCatLabel.setText(managedClub.getCategory());
                clubStatusLabel.setText(managedClub.getStatus() != null ? managedClub.getStatus() : "ACTIVE");
                clubEstLabel.setText(managedClub.getEstablishedDate() != null ? managedClub.getEstablishedDate().toString() : "N/A");
                clubDescArea.setText(managedClub.getDescription() != null ? managedClub.getDescription() : "");

                loadMembershipRequests();
                loadClubEvents();
                loadAnnouncements();
            } else {
                clubNameCard.setValue("Unassigned");
                clubNameLabel.setText("No club assigned to coordinator account.");
            }
        } catch (DatabaseOperationException ex) {
            clubNameLabel.setText("Error loading club details.");
        }
    }

    public void loadMembershipRequests() {
        if (managedClub == null) return;
        membershipRequestsTableModel.setRowCount(0);
        int pendingCount = 0;
        int approvedCount = 0;

        try {
            loadedMemberships = membershipService.getClubMembers(managedClub.getClubId());
            for (Membership m : loadedMemberships) {
                if ("PENDING".equalsIgnoreCase(m.getStatus())) pendingCount++;
                if ("APPROVED".equalsIgnoreCase(m.getStatus())) approvedCount++;

                membershipRequestsTableModel.addRow(new Object[]{
                        m.getMembershipId(),
                        m.getStudentId(),
                        m.getStudentName() != null ? m.getStudentName() : "Student #" + m.getStudentId(),
                        m.getStudentUsn() != null ? m.getStudentUsn() : "N/A",
                        m.getStudentDepartment() != null ? m.getStudentDepartment() : "General",
                        m.getJoinedAt() != null ? m.getJoinedAt().toLocalDate().toString() : "N/A",
                        m.getStatus()
                });
            }
            memberCountCard.setValue(approvedCount);
            pendingRequestsCard.setValue(pendingCount);
        } catch (DatabaseOperationException ex) {
            System.err.println("Error loading memberships: " + ex.getMessage());
        }
    }

    private void applyMembershipFilter() {
        String filter = (String) membershipFilterBox.getSelectedItem();
        membershipRequestsTableModel.setRowCount(0);
        for (Membership m : loadedMemberships) {
            if ("All Statuses".equals(filter) || m.getStatus().equalsIgnoreCase(filter)) {
                membershipRequestsTableModel.addRow(new Object[]{
                        m.getMembershipId(),
                        m.getStudentId(),
                        m.getStudentName() != null ? m.getStudentName() : "Student #" + m.getStudentId(),
                        m.getStudentUsn() != null ? m.getStudentUsn() : "N/A",
                        m.getStudentDepartment() != null ? m.getStudentDepartment() : "General",
                        m.getJoinedAt() != null ? m.getJoinedAt().toLocalDate().toString() : "N/A",
                        m.getStatus()
                });
            }
        }
    }

    public void loadClubEvents() {
        if (managedClub == null) return;
        clubEventsTableModel.setRowCount(0);
        try {
            loadedEvents = eventService.getEventsByClub(managedClub.getClubId());
            for (Event e : loadedEvents) {
                clubEventsTableModel.addRow(new Object[]{
                        e.getEventId(),
                        e.getEventName(),
                        e.getEventDate() != null ? e.getEventDate().toString().replace("T", " ") : "TBA",
                        e.getVenue() != null ? e.getVenue() : "TBA",
                        e.getCapacity(),
                        e.getRegisteredCount(),
                        e.getStatus() != null ? e.getStatus() : "UPCOMING"
                });
            }
            eventCountCard.setValue(loadedEvents.size());
        } catch (DatabaseOperationException ex) {
            System.err.println("Error loading club events: " + ex.getMessage());
        }
    }

    public void loadAnnouncements() {
        if (managedClub == null) return;
        announcementsTableModel.setRowCount(0);
        try {
            List<Announcement> list = eventService.getAnnouncementsForClub(managedClub.getClubId());
            for (Announcement a : list) {
                announcementsTableModel.addRow(new Object[]{
                        a.getAnnouncementId(),
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

    private void handleUpdateDescription() {
        if (managedClub == null) return;
        String newDesc = clubDescArea.getText().trim();
        managedClub.setDescription(newDesc);

        try {
            clubService.updateClub(managedClub);
            JOptionPane.showMessageDialog(this, "Club description updated successfully!", "Updated", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to update description: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleApprove() {
        int row = membershipRequestsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a membership application to approve.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int membershipId = (int) membershipRequestsTableModel.getValueAt(row, 0);
        String studentName = (String) membershipRequestsTableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Approve membership for " + studentName + "?",
                "Confirm Approval",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            membershipService.approveMembership(membershipId);
            JOptionPane.showMessageDialog(this, "Membership for " + studentName + " approved!", "Approved", JOptionPane.INFORMATION_MESSAGE);
            loadMembershipRequests();
        } catch (DatabaseOperationException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleReject() {
        int row = membershipRequestsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a membership application to reject.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int membershipId = (int) membershipRequestsTableModel.getValueAt(row, 0);
        String studentName = (String) membershipRequestsTableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Reject membership for " + studentName + "?",
                "Confirm Rejection",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            membershipService.rejectMembership(membershipId);
            JOptionPane.showMessageDialog(this, "Membership for " + studentName + " rejected.", "Rejected", JOptionPane.INFORMATION_MESSAGE);
            loadMembershipRequests();
        } catch (DatabaseOperationException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCreateEventDialog() {
        if (managedClub == null) {
            JOptionPane.showMessageDialog(this, "No managed club assigned to this account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField titleField = new JTextField(20);
        JTextField descField = new JTextField(20);
        JTextField locField = new JTextField(20);
        JTextField capField = new JTextField("50", 10);
        JTextField dateField = new JTextField("2026-10-25 14:00", 15);

        JPanel dialogPanel = new JPanel(new GridLayout(5, 2, 8, 8));
        dialogPanel.add(new JLabel("Event Title:"));
        dialogPanel.add(titleField);
        dialogPanel.add(new JLabel("Description:"));
        dialogPanel.add(descField);
        dialogPanel.add(new JLabel("Venue / Room:"));
        dialogPanel.add(locField);
        dialogPanel.add(new JLabel("Capacity (Seats):"));
        dialogPanel.add(capField);
        dialogPanel.add(new JLabel("Date & Time (yyyy-MM-dd HH:mm):"));
        dialogPanel.add(dateField);

        int result = JOptionPane.showConfirmDialog(
                this,
                dialogPanel,
                "Schedule New Event for " + managedClub.getClubName(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                String title = titleField.getText().trim();
                String desc = descField.getText().trim();
                String loc = locField.getText().trim();
                int cap = Integer.parseInt(capField.getText().trim());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime eventDate = LocalDateTime.parse(dateField.getText().trim(), formatter);

                Event event = new Event();
                event.setClubId(managedClub.getClubId());
                event.setEventName(title);
                event.setDescription(desc);
                event.setVenue(loc);
                event.setCapacity(cap);
                event.setEventDate(eventDate);
                event.setStatus("UPCOMING");

                eventService.createEvent(event);
                JOptionPane.showMessageDialog(this, "Event '" + title + "' scheduled successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadClubEvents();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to schedule event: " + ex.getMessage(), "Validation/Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleViewAttendees() {
        int row = clubEventsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an event to view attendees.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int eventId = (int) clubEventsTableModel.getValueAt(row, 0);
        String eventTitle = (String) clubEventsTableModel.getValueAt(row, 1);

        try {
            List<EventRegistration> participants = eventService.getEventParticipants(eventId);

            String[] cols = {"Reg ID", "Student ID", "Student Name", "Email", "Registered At", "Status", "Attended"};
            DefaultTableModel rosterModel = new DefaultTableModel(cols, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            for (EventRegistration r : participants) {
                rosterModel.addRow(new Object[]{
                        r.getRegistrationId(),
                        r.getStudentId(),
                        r.getStudentName() != null ? r.getStudentName() : "Student #" + r.getStudentId(),
                        r.getStudentEmail() != null ? r.getStudentEmail() : "N/A",
                        r.getRegisteredAt() != null ? r.getRegisteredAt().toLocalDate().toString() : "N/A",
                        r.getRegistrationStatus(),
                        r.isAttended() ? "YES" : "NO"
                });
            }

            JTable rosterTable = new JTable(rosterModel);
            UITheme.styleTable(rosterTable);
            JScrollPane scrollPane = new JScrollPane(rosterTable);
            scrollPane.setPreferredSize(new Dimension(720, 280));

            JPanel rosterPanel = new JPanel(new BorderLayout(8, 8));
            rosterPanel.add(scrollPane, BorderLayout.CENTER);

            JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
            JButton toggleAttendanceBtn = UITheme.createPrimaryButton("Mark Selected Attended / Absent");
            toggleAttendanceBtn.addActionListener(e -> {
                int selectedRow = rosterTable.getSelectedRow();
                if (selectedRow < 0) {
                    JOptionPane.showMessageDialog(this, "Please select an attendee from the roster.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int regId = (int) rosterModel.getValueAt(selectedRow, 0);
                String currentStatus = (String) rosterModel.getValueAt(selectedRow, 6);
                boolean newAttended = !"YES".equalsIgnoreCase(currentStatus);
                try {
                    eventService.markAttendance(regId, newAttended);
                    rosterModel.setValueAt(newAttended ? "YES" : "NO", selectedRow, 6);
                    JOptionPane.showMessageDialog(this, "Attendance updated to: " + (newAttended ? "PRESENT" : "ABSENT"), "Attendance Recorded", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to update attendance: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            btnBar.add(toggleAttendanceBtn);
            rosterPanel.add(btnBar, BorderLayout.SOUTH);

            JOptionPane.showMessageDialog(this, rosterPanel, "Attendee Roster: " + eventTitle + " (" + participants.size() + " registered)", JOptionPane.PLAIN_MESSAGE);
        } catch (DatabaseOperationException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteEvent() {
        int row = clubEventsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an event to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int eventId = (int) clubEventsTableModel.getValueAt(row, 0);
        String eventTitle = (String) clubEventsTableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete event: " + eventTitle + "?\nAll student registrations for this event will be removed.",
                "Confirm Delete Event",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            eventService.deleteEvent(eventId);
            JOptionPane.showMessageDialog(this, "Event deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            loadClubEvents();
        } catch (DatabaseOperationException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handlePostAnnouncement() {
        if (managedClub == null) {
            JOptionPane.showMessageDialog(this, "No managed club assigned to post announcements.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String title = announcementTitleField.getText().trim();
        String message = announcementContentArea.getText().trim();
        String priority = (String) priorityBox.getSelectedItem();

        if (title.isEmpty() || message.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please provide both title and announcement message.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Announcement announcement = new Announcement();
            announcement.setClubId(managedClub.getClubId());
            announcement.setTitle(title);
            announcement.setMessage(message);
            announcement.setPriority(priority);

            eventService.postAnnouncement(announcement);
            JOptionPane.showMessageDialog(this, "Announcement broadcasted successfully to all members!", "Broadcast Sent", JOptionPane.INFORMATION_MESSAGE);

            announcementTitleField.setText("");
            announcementContentArea.setText("");
            loadAnnouncements();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to post announcement: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public ClubCoordinator getCoordinator() {
        return coordinator;
    }

    public JTable getMembershipRequestsTable() {
        return membershipRequestsTable;
    }

    public JTable getClubEventsTable() {
        return clubEventsTable;
    }
}
