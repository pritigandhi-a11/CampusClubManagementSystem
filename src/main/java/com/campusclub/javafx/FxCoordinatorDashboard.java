package com.campusclub.javafx;

import com.campusclub.dao.AnnouncementDAO;
import com.campusclub.dao.MembershipDAO;
import com.campusclub.model.*;
import com.campusclub.service.ClubService;
import com.campusclub.service.EventService;
import com.campusclub.service.MembershipService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JavaFX Dashboard for Club Coordinators and External Teacher Reviewers.
 * Features:
 * - Metric cards
 * - Tab 1: Club Info & Description Management
 * - Tab 2: Student Membership Applications (Approve/Reject)
 * - Tab 3: Club Events Scheduling
 * - Tab 4: Broadcast Announcements
 * - Teacher Reviewer Mode: Strictly read-only evaluation oversight with mutations blocked.
 */
public class FxCoordinatorDashboard extends VBox {

    private final ClubCoordinator coordinator;
    private final boolean isReviewer;
    private final ClubService clubService;
    private final MembershipService membershipService;
    private final EventService eventService;
    private final AnnouncementDAO announcementDAO;
    private final MembershipDAO membershipDAO;

    private Club managedClub;

    // KPI Cards
    private VBox clubNameCard;
    private VBox memberCountCard;
    private VBox pendingRequestsCard;
    private VBox eventCountCard;

    // Tab 1: Info
    private Label lblClubName;
    private Label lblCategory;
    private Label lblStatus;
    private TextArea txtDescription;

    // Tab 2: Applications
    private TableView<Membership> appsTable;
    private ObservableList<Membership> appsList = FXCollections.observableArrayList();

    // Tab 3: Events
    private TableView<Event> eventsTable;
    private ObservableList<Event> eventsList = FXCollections.observableArrayList();

    // Tab 4: Announcements
    private TableView<Announcement> noticesTable;
    private ObservableList<Announcement> noticesList = FXCollections.observableArrayList();

    public FxCoordinatorDashboard(ClubCoordinator coordinator) {
        this(coordinator, coordinator != null && coordinator.getRole() == UserRole.TEACHER);
    }

    public FxCoordinatorDashboard(ClubCoordinator coordinator, boolean isReviewer) {
        this.coordinator = coordinator != null ? coordinator : new ClubCoordinator();
        this.isReviewer = isReviewer || "teacher_reviewer".equalsIgnoreCase(this.coordinator.getUsername()) || this.coordinator.getRole() == UserRole.TEACHER;
        this.clubService = new ClubService();
        this.membershipService = new MembershipService();
        this.eventService = new EventService();
        this.announcementDAO = new AnnouncementDAO();
        this.membershipDAO = new MembershipDAO();

        setSpacing(16);
        setPadding(new Insets(20));
        VBox.setVgrow(this, Priority.ALWAYS);

        initReviewerBanner();
        initKpiCards();
        initTabs();
        loadAllData();
    }

    private void initReviewerBanner() {
        if (isReviewer) {
            HBox banner = new HBox(10);
            banner.setAlignment(Pos.CENTER_LEFT);
            banner.setStyle("-fx-background-color: #FEF3C7; -fx-border-color: #F59E0B; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 10px 14px;");

            Label icon = new Label("🔍");
            icon.setStyle("-fx-font-size: 18px;");

            Label text = new Label("EXTERNAL EVALUATOR MODE: Strictly Read-Only Oversight. Mutation actions are disabled.");
            text.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #92400E;");

            banner.getChildren().addAll(icon, text);
            getChildren().add(banner);
        } else {
            // Web Link Toolbar
            HBox toolbar = new HBox(12);
            toolbar.setAlignment(Pos.CENTER_LEFT);

            Label badge = new Label("👨‍🏫 Faculty / Club Coordinator Workspace");
            badge.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #192A56;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnWeb = FxUITheme.createSecondaryButton("🌐 Open Web View (/teacherDashboard)");
            btnWeb.setOnAction(e -> openWebView());

            toolbar.getChildren().addAll(badge, spacer, btnWeb);
            getChildren().add(toolbar);
        }
    }

    private void openWebView() {
        try {
            int port = com.campusclub.web.TeacherDashboardWebServer.startServerIfNotRunning();
            String url = "http://localhost:" + port + "/teacherDashboard";
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {}
    }

    private void initKpiCards() {
        HBox kpiBox = new HBox(12);
        kpiBox.setAlignment(Pos.CENTER);

        clubNameCard = FxUITheme.createKpiCard("MANAGED CLUB", "Coding Club", "Assigned campus organization");
        memberCountCard = FxUITheme.createKpiCard("TOTAL MEMBERS", "0", "Active club roster");
        pendingRequestsCard = FxUITheme.createKpiCard("PENDING APPS", "0", "Awaiting review");
        eventCountCard = FxUITheme.createKpiCard("SCHEDULED EVENTS", "0", "Upcoming club sessions");

        kpiBox.getChildren().addAll(clubNameCard, memberCountCard, pendingRequestsCard, eventCountCard);
        getChildren().add(kpiBox);
    }

    private void initTabs() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab tabInfo = new Tab("ℹ️ Club Overview", buildInfoTab());
        Tab tabApps = new Tab("📋 Membership Applications", buildAppsTab());
        Tab tabEvents = new Tab("🗓️ Club Events", buildEventsTab());
        Tab tabNotices = new Tab("📢 Broadcast Notices", buildAnnouncementsTab());

        tabPane.getTabs().addAll(tabInfo, tabApps, tabEvents, tabNotices);
        getChildren().add(tabPane);
    }

    private Node buildInfoTab() {
        VBox root = new VBox(14);
        root.setPadding(new Insets(14));

        lblClubName = new Label("Coding & Algorithms Club");
        lblClubName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        lblCategory = new Label("Category: Technical");
        lblCategory.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        lblStatus = new Label("Status: ACTIVE");
        lblStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #10B981;");

        Label lblDescTitle = new Label("Club Mission & Description:");
        lblDescTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        txtDescription = new TextArea("Dedicated to competitive programming, hackathons, data structures, and computer science excellence.");
        txtDescription.setPrefRowCount(4);
        txtDescription.setEditable(!isReviewer);

        HBox btnBox = new HBox(10);
        if (!isReviewer) {
            Button btnSave = FxUITheme.createPrimaryButton("Save Changes");
            btnSave.setOnAction(e -> handleSaveDescription());
            btnBox.getChildren().add(btnSave);
        }

        root.getChildren().addAll(lblClubName, lblCategory, lblStatus, lblDescTitle, txtDescription, btnBox);
        return FxUITheme.createSectionCard("", root);
    }

    private void handleSaveDescription() {
        if (managedClub != null) {
            try {
                managedClub.setDescription(txtDescription.getText().trim());
                clubService.updateClub(managedClub);
                showInfo("Club description saved successfully!");
            } catch (Exception ex) {
                showError("Could not update club description: " + ex.getMessage());
            }
        }
    }

    private Node buildAppsTab() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(12));

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        if (!isReviewer) {
            Button btnApprove = FxUITheme.createSuccessButton("✓ Approve Selected");
            btnApprove.setOnAction(e -> handleReviewApp("APPROVED"));

            Button btnReject = FxUITheme.createDangerButton("✕ Reject Selected");
            btnReject.setOnAction(e -> handleReviewApp("REJECTED"));

            toolbar.getChildren().addAll(btnApprove, btnReject);
        }

        Button btnRefresh = FxUITheme.createSecondaryButton("↻ Refresh Applications");
        btnRefresh.setOnAction(e -> loadApplications());
        toolbar.getChildren().add(btnRefresh);

        appsTable = new TableView<>(appsList);
        appsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(appsTable, Priority.ALWAYS);

        TableColumn<Membership, Number> colId = new TableColumn<>("App ID");
        colId.setCellValueFactory(m -> new SimpleIntegerProperty(m.getValue().getMembershipId()));
        colId.setMaxWidth(70);

        TableColumn<Membership, String> colStudent = new TableColumn<>("Student Name");
        colStudent.setCellValueFactory(m -> new SimpleStringProperty(m.getValue().getStudentName() != null ? m.getValue().getStudentName() : "Student #" + m.getValue().getStudentId()));

        TableColumn<Membership, String> colReg = new TableColumn<>("Reg No");
        colReg.setCellValueFactory(m -> new SimpleStringProperty(m.getValue().getStudentUsn() != null ? m.getValue().getStudentUsn() : "N/A"));

        TableColumn<Membership, String> colClub = new TableColumn<>("Club");
        colClub.setCellValueFactory(m -> new SimpleStringProperty(m.getValue().getClubName() != null ? m.getValue().getClubName() : "Club #" + m.getValue().getClubId()));

        TableColumn<Membership, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(m -> new SimpleStringProperty(m.getValue().getMembershipStatus()));

        appsTable.getColumns().addAll(colId, colStudent, colReg, colClub, colStatus);

        root.getChildren().addAll(toolbar, appsTable);
        return root;
    }

    private void handleReviewApp(String decision) {
        Membership selected = appsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Please select an application from the table.");
            return;
        }
        try {
            membershipDAO.updateStatus(selected.getMembershipId(), decision);
            loadApplications();
        } catch (Exception ex) {
            showError("Could not update application: " + ex.getMessage());
        }
    }

    private Node buildEventsTab() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(12));

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        if (!isReviewer) {
            Button btnAddEvent = FxUITheme.createPrimaryButton("➕ Schedule Event");
            btnAddEvent.setOnAction(e -> showScheduleEventDialog());
            toolbar.getChildren().add(btnAddEvent);
        }

        Button btnRefresh = FxUITheme.createSecondaryButton("↻ Refresh Events");
        btnRefresh.setOnAction(e -> loadEvents());
        toolbar.getChildren().add(btnRefresh);

        eventsTable = new TableView<>(eventsList);
        eventsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(eventsTable, Priority.ALWAYS);

        TableColumn<Event, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(ev -> new SimpleIntegerProperty(ev.getValue().getEventId()));
        colId.setMaxWidth(60);

        TableColumn<Event, String> colName = new TableColumn<>("Event Title");
        colName.setCellValueFactory(ev -> new SimpleStringProperty(ev.getValue().getEventName()));

        TableColumn<Event, String> colDate = new TableColumn<>("Date & Time");
        colDate.setCellValueFactory(ev -> new SimpleStringProperty(ev.getValue().getEventDate() != null ? ev.getValue().getEventDate().toString() : "TBD"));

        TableColumn<Event, String> colVenue = new TableColumn<>("Venue");
        colVenue.setCellValueFactory(ev -> new SimpleStringProperty(ev.getValue().getVenue()));

        TableColumn<Event, Number> colCap = new TableColumn<>("Capacity");
        colCap.setCellValueFactory(ev -> new SimpleIntegerProperty(ev.getValue().getCapacity()));

        eventsTable.getColumns().addAll(colId, colName, colDate, colVenue, colCap);

        root.getChildren().addAll(toolbar, eventsTable);
        return root;
    }

    private void showScheduleEventDialog() {
        Dialog<Event> dialog = new Dialog<>();
        dialog.setTitle("Schedule Club Event");
        dialog.setHeaderText("Create an event for " + (managedClub != null ? managedClub.getClubName() : "Club"));

        ButtonType btnSave = new ButtonType("Schedule", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("Event Title");
        TextField venueField = new TextField();
        venueField.setPromptText("Venue");
        TextField capField = new TextField("60");

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Venue:"), 0, 1);
        grid.add(venueField, 1, 1);
        grid.add(new Label("Capacity:"), 0, 2);
        grid.add(capField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == btnSave) {
                if (titleField.getText().trim().isEmpty()) return null;
                Event ev = new Event();
                ev.setEventName(titleField.getText().trim());
                ev.setVenue(venueField.getText().trim());
                ev.setClubId(managedClub != null ? managedClub.getClubId() : 1);
                ev.setEventDate(LocalDateTime.now().plusDays(5));
                try { ev.setCapacity(Integer.parseInt(capField.getText().trim())); } catch (Exception ignored) { ev.setCapacity(60); }
                ev.setStatus("UPCOMING");
                return ev;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(ev -> {
            try {
                eventService.createEvent(ev);
                loadEvents();
            } catch (Exception ex) {
                showError("Could not schedule event: " + ex.getMessage());
            }
        });
    }

    private Node buildAnnouncementsTab() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(12));

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        if (!isReviewer) {
            Button btnAddNotice = FxUITheme.createPrimaryButton("📢 Broadcast Notice");
            btnAddNotice.setOnAction(e -> showBroadcastDialog());
            toolbar.getChildren().add(btnAddNotice);
        }

        Button btnRefresh = FxUITheme.createSecondaryButton("↻ Refresh Notices");
        btnRefresh.setOnAction(e -> loadAnnouncements());
        toolbar.getChildren().add(btnRefresh);

        noticesTable = new TableView<>(noticesList);
        noticesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(noticesTable, Priority.ALWAYS);

        TableColumn<Announcement, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(a -> new SimpleIntegerProperty(a.getValue().getAnnouncementId()));
        colId.setMaxWidth(60);

        TableColumn<Announcement, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(a -> new SimpleStringProperty(a.getValue().getTitle()));

        TableColumn<Announcement, String> colPri = new TableColumn<>("Priority");
        colPri.setCellValueFactory(a -> new SimpleStringProperty(a.getValue().getPriority()));

        TableColumn<Announcement, String> colMsg = new TableColumn<>("Message");
        colMsg.setCellValueFactory(a -> new SimpleStringProperty(a.getValue().getMessage()));

        noticesTable.getColumns().addAll(colId, colTitle, colPri, colMsg);

        root.getChildren().addAll(toolbar, noticesTable);
        return root;
    }

    private void showBroadcastDialog() {
        Dialog<Announcement> dialog = new Dialog<>();
        dialog.setTitle("Broadcast Club Notice");
        dialog.setHeaderText("Publish an announcement to members");

        ButtonType btnSave = new ButtonType("Publish", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        ComboBox<String> priBox = new ComboBox<>(FXCollections.observableArrayList("HIGH", "MEDIUM", "LOW"));
        priBox.setValue("MEDIUM");
        TextArea msgArea = new TextArea();
        msgArea.setPromptText("Notice content");
        msgArea.setPrefRowCount(3);

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Priority:"), 0, 1);
        grid.add(priBox, 1, 1);
        grid.add(new Label("Message:"), 0, 2);
        grid.add(msgArea, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == btnSave) {
                if (titleField.getText().trim().isEmpty()) return null;
                Announcement a = new Announcement();
                a.setTitle(titleField.getText().trim());
                a.setMessage(msgArea.getText().trim());
                a.setPriority(priBox.getValue());
                a.setClubId(managedClub != null ? managedClub.getClubId() : 1);
                a.setCreatedAt(LocalDateTime.now());
                return a;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(a -> {
            try {
                announcementDAO.createAnnouncement(a);
                loadAnnouncements();
            } catch (Exception ex) {
                showError("Could not broadcast notice: " + ex.getMessage());
            }
        });
    }

    public void loadAllData() {
        loadClubInfo();
        loadApplications();
        loadEvents();
        loadAnnouncements();
    }

    private void loadClubInfo() {
        try {
            List<Club> clubs = clubService.getAllClubs();
            if (clubs != null && !clubs.isEmpty()) {
                managedClub = clubs.get(0);
                lblClubName.setText(managedClub.getClubName());
                lblCategory.setText("Category: " + managedClub.getCategory());
                lblStatus.setText("Status: " + managedClub.getStatus());
                if (managedClub.getDescription() != null) {
                    txtDescription.setText(managedClub.getDescription());
                }
                updateCardValue(clubNameCard, managedClub.getClubName());
                updateCardValue(memberCountCard, String.valueOf(managedClub.getMemberCount()));
            }
        } catch (Exception ignored) {}
    }

    private void loadApplications() {
        try {
            int clubId = managedClub != null ? managedClub.getClubId() : 1;
            List<Membership> list = membershipService.getClubMembers(clubId);
            appsList.setAll(list != null ? list : List.of());
            long pending = appsList.stream().filter(m -> "PENDING".equalsIgnoreCase(m.getMembershipStatus())).count();
            updateCardValue(pendingRequestsCard, String.valueOf(pending));
        } catch (Exception ignored) {
            appsList.clear();
        }
    }

    private void loadEvents() {
        try {
            int clubId = managedClub != null ? managedClub.getClubId() : 1;
            List<Event> list = eventService.getEventsByClub(clubId);
            eventsList.setAll(list != null ? list : List.of());
            updateCardValue(eventCountCard, String.valueOf(eventsList.size()));
        } catch (Exception ignored) {
            eventsList.clear();
        }
    }

    private void loadAnnouncements() {
        try {
            List<Announcement> list = announcementDAO.findAll();
            noticesList.setAll(list != null ? list : List.of());
        } catch (Exception ignored) {
            noticesList.clear();
        }
    }

    private void updateCardValue(VBox card, String val) {
        if (card != null && card.getChildren().size() > 1 && card.getChildren().get(1) instanceof Label l) {
            l.setText(val);
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText("Operation Error");
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText("Notice");
        alert.showAndWait();
    }
}
