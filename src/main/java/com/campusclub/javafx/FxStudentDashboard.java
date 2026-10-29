package com.campusclub.javafx;

import com.campusclub.exception.ClubNotFoundException;
import com.campusclub.exception.DatabaseOperationException;
import com.campusclub.exception.DuplicateMembershipException;
import com.campusclub.exception.EventRegistrationException;
import com.campusclub.model.*;
import com.campusclub.recommendation.ClubRecommendationEngine;
import com.campusclub.service.ClubService;
import com.campusclub.service.EventService;
import com.campusclub.service.MembershipService;
import com.campusclub.service.StudentService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX Dashboard for Students.
 * Features:
 * - 4 Summary KPI Cards
 * - Tab 1: Discover Clubs (Filter, Search, Apply)
 * - Tab 2: My Memberships (View status, Withdraw)
 * - Tab 3: Upcoming Events (Search, Register)
 * - Tab 4: My Event Passes (View, Cancel)
 * - Tab 5: Campus Announcements
 * - Tab 6: Smart Recommendations (Max-Heap scoring + Direct Apply)
 * - Tab 7: Profile & Interests (Edit phone & matching interests)
 */
public class FxStudentDashboard extends VBox {

    private final Student student;
    private final ClubService clubService;
    private final MembershipService membershipService;
    private final EventService eventService;
    private final StudentService studentService;
    private final ClubRecommendationEngine recommendationEngine;

    // KPI Summary Cards
    private VBox joinedClubsCard;
    private VBox registeredEventsCard;
    private VBox availableEventsCard;
    private VBox recommendationsCard;

    // Tab 1: Discover Clubs
    private TableView<Club> clubsTable;
    private ObservableList<Club> clubsList = FXCollections.observableArrayList();
    private List<Club> allLoadedClubs = new ArrayList<>();
    private TextField clubSearchField;
    private ComboBox<String> clubCategoryBox;

    // Tab 2: My Memberships
    private TableView<Membership> membershipsTable;
    private ObservableList<Membership> membershipsList = FXCollections.observableArrayList();

    // Tab 3: Upcoming Events
    private TableView<Event> eventsTable;
    private ObservableList<Event> eventsList = FXCollections.observableArrayList();
    private List<Event> allLoadedEvents = new ArrayList<>();
    private TextField eventSearchField;

    // Tab 4: My Registrations
    private TableView<EventRegistration> registrationsTable;
    private ObservableList<EventRegistration> registrationsList = FXCollections.observableArrayList();

    // Tab 5: Announcements
    private TableView<Announcement> announcementsTable;
    private ObservableList<Announcement> announcementsList = FXCollections.observableArrayList();

    // Tab 6: Recommendations
    private ListView<String> recommendationsListView;
    private ObservableList<String> recommendationsList = FXCollections.observableArrayList();
    private List<ClubRecommendationEngine.ClubRecommendation> activeRecommendations = new ArrayList<>();

    // Tab 7: Profile
    private TextField profilePhoneField;
    private TextField profileInterestsField;

    public FxStudentDashboard(Student student) {
        this(student, new ClubService(), new MembershipService(), new EventService(), new StudentService(), new ClubRecommendationEngine());
    }

    public FxStudentDashboard(Student student, ClubService clubService, MembershipService membershipService,
                              EventService eventService, StudentService studentService, ClubRecommendationEngine recommendationEngine) {
        this.student = student != null ? student : new Student();
        this.clubService = clubService != null ? clubService : new ClubService();
        this.membershipService = membershipService != null ? membershipService : new MembershipService();
        this.eventService = eventService != null ? eventService : new EventService();
        this.studentService = studentService != null ? studentService : new StudentService();
        this.recommendationEngine = recommendationEngine != null ? recommendationEngine : new ClubRecommendationEngine();

        setSpacing(16);
        setPadding(new Insets(20));
        VBox.setVgrow(this, Priority.ALWAYS);

        initKpiCards();
        initTabs();
        loadAllData();
    }

    private void initKpiCards() {
        HBox kpiBox = new HBox(12);
        kpiBox.setAlignment(Pos.CENTER);

        joinedClubsCard = FxUITheme.createKpiCard("MY CLUBS", "0", "Approved / Pending memberships");
        registeredEventsCard = FxUITheme.createKpiCard("MY EVENT PASSES", "0", "Active event registrations");
        availableEventsCard = FxUITheme.createKpiCard("CAMPUS EVENTS", "0", "Scheduled upcoming events");
        recommendationsCard = FxUITheme.createKpiCard("SMART MATCHES", "0", "Clubs matched to your interests");

        kpiBox.getChildren().addAll(joinedClubsCard, registeredEventsCard, availableEventsCard, recommendationsCard);
        getChildren().add(kpiBox);
    }

    private void initTabs() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab tabDiscover = new Tab("🔍 Discover Clubs", buildDiscoverClubsTab());
        Tab tabMemberships = new Tab("🏷️ My Memberships", buildMembershipsTab());
        Tab tabEvents = new Tab("📅 Upcoming Events", buildEventsTab());
        Tab tabPasses = new Tab("🎟️ My Event Passes", buildRegistrationsTab());
        Tab tabAnnounce = new Tab("📢 Announcements", buildAnnouncementsTab());
        Tab tabRecs = new Tab("⭐ Recommendations", buildRecommendationsTab());
        Tab tabProfile = new Tab("👤 My Profile", buildProfileTab());

        tabPane.getTabs().addAll(tabDiscover, tabMemberships, tabEvents, tabPasses, tabAnnounce, tabRecs, tabProfile);
        getChildren().add(tabPane);
    }

    // ==========================================
    // Tab 1: Discover Clubs
    // ==========================================
    private VBox buildDiscoverClubsTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));

        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Label searchLbl = new Label("Search Clubs:");
        searchLbl.getStyleClass().add("table-column-header");
        clubSearchField = new TextField();
        clubSearchField.setPromptText("Name or description...");
        clubSearchField.setPrefWidth(220);

        Label catLbl = new Label("Category:");
        catLbl.getStyleClass().add("table-column-header");
        clubCategoryBox = new ComboBox<>(FXCollections.observableArrayList(
                "All Categories", "Technical", "Cultural", "Sports", "Social", "Academic", "Literary"
        ));
        clubCategoryBox.setValue("All Categories");

        Button filterBtn = FxUITheme.createPrimaryButton("Filter");
        filterBtn.setOnAction(e -> applyClubFilter());

        Button resetBtn = FxUITheme.createSecondaryButton("Reset");
        resetBtn.setOnAction(e -> {
            clubSearchField.clear();
            clubCategoryBox.setValue("All Categories");
            applyClubFilter();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button joinBtn = FxUITheme.createSuccessButton("Join Selected Club");
        joinBtn.setOnAction(e -> handleJoinClub());

        Button refreshBtn = FxUITheme.createSecondaryButton("Refresh");
        refreshBtn.setOnAction(e -> loadClubs());

        filterBar.getChildren().addAll(searchLbl, clubSearchField, catLbl, clubCategoryBox, filterBtn, resetBtn, spacer, joinBtn, refreshBtn);

        // Table
        clubsTable = new TableView<>(clubsList);
        clubsTable.setPlaceholder(new Label("No campus clubs available."));
        VBox.setVgrow(clubsTable, Priority.ALWAYS);

        TableColumn<Club, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getClubId()));
        idCol.setPrefWidth(50);

        TableColumn<Club, String> nameCol = new TableColumn<>("Club Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClubName()));
        nameCol.setPrefWidth(180);

        TableColumn<Club, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        catCol.setPrefWidth(110);

        TableColumn<Club, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        descCol.setPrefWidth(280);

        TableColumn<Club, String> coordCol = new TableColumn<>("Coordinator");
        coordCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCoordinatorName() != null ? d.getValue().getCoordinatorName() : "Unassigned"
        ));
        coordCol.setPrefWidth(140);

        TableColumn<Club, Number> membersCol = new TableColumn<>("Active Members");
        membersCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getMemberCount()));
        membersCol.setPrefWidth(110);

        clubsTable.getColumns().addAll(idCol, nameCol, catCol, descCol, coordCol, membersCol);

        box.getChildren().addAll(filterBar, clubsTable);
        return box;
    }

    // ==========================================
    // Tab 2: My Memberships
    // ==========================================
    private VBox buildMembershipsTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));

        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Your Club Memberships & Applications");
        title.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button leaveBtn = FxUITheme.createDangerButton("Leave / Withdraw Membership");
        leaveBtn.setOnAction(e -> handleLeaveClub());

        Button refreshBtn = FxUITheme.createSecondaryButton("Refresh");
        refreshBtn.setOnAction(e -> loadMemberships());

        topBar.getChildren().addAll(title, spacer, leaveBtn, refreshBtn);

        membershipsTable = new TableView<>(membershipsList);
        membershipsTable.setPlaceholder(new Label("You haven't joined any clubs yet. Use Discover Clubs to explore!"));
        VBox.setVgrow(membershipsTable, Priority.ALWAYS);

        TableColumn<Membership, Number> idCol = new TableColumn<>("Membership ID");
        idCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getMembershipId()));
        idCol.setPrefWidth(110);

        TableColumn<Membership, String> clubCol = new TableColumn<>("Club Name");
        clubCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClubName()));
        clubCol.setPrefWidth(200);

        TableColumn<Membership, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getClubCategory() != null ? d.getValue().getClubCategory() : "General"
        ));
        catCol.setPrefWidth(120);

        TableColumn<Membership, String> dateCol = new TableColumn<>("Joined Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getJoinedAt() != null ? d.getValue().getJoinedAt().toLocalDate().toString() : "N/A"
        ));
        dateCol.setPrefWidth(110);

        TableColumn<Membership, String> roleCol = new TableColumn<>("Role In Club");
        roleCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getRoleInClub() != null ? d.getValue().getRoleInClub() : "MEMBER"
        ));
        roleCol.setPrefWidth(120);

        TableColumn<Membership, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStatus() != null ? d.getValue().getStatus() : "ACTIVE"
        ));
        statusCol.setPrefWidth(120);

        membershipsTable.getColumns().addAll(idCol, clubCol, catCol, dateCol, roleCol, statusCol);

        box.getChildren().addAll(topBar, membershipsTable);
        return box;
    }

    // ==========================================
    // Tab 3: Upcoming Events
    // ==========================================
    private VBox buildEventsTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));

        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label searchLbl = new Label("Search Events:");
        searchLbl.getStyleClass().add("table-column-header");
        eventSearchField = new TextField();
        eventSearchField.setPromptText("Title, club, or venue...");
        eventSearchField.setPrefWidth(220);

        Button searchBtn = FxUITheme.createPrimaryButton("Search");
        searchBtn.setOnAction(e -> applyEventSearch());

        Button resetBtn = FxUITheme.createSecondaryButton("Show All");
        resetBtn.setOnAction(e -> {
            eventSearchField.clear();
            applyEventSearch();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button registerBtn = FxUITheme.createSuccessButton("Register for Selected Event");
        registerBtn.setOnAction(e -> handleEventRegistration());

        Button refreshBtn = FxUITheme.createSecondaryButton("Refresh");
        refreshBtn.setOnAction(e -> loadEvents());

        topBar.getChildren().addAll(searchLbl, eventSearchField, searchBtn, resetBtn, spacer, registerBtn, refreshBtn);

        eventsTable = new TableView<>(eventsList);
        eventsTable.setPlaceholder(new Label("No upcoming campus events scheduled right now."));
        VBox.setVgrow(eventsTable, Priority.ALWAYS);

        TableColumn<Event, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getEventId()));
        idCol.setPrefWidth(50);

        TableColumn<Event, String> clubCol = new TableColumn<>("Organizing Club");
        clubCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getClubName() != null ? d.getValue().getClubName() : "Club #" + d.getValue().getClubId()
        ));
        clubCol.setPrefWidth(160);

        TableColumn<Event, String> titleCol = new TableColumn<>("Event Title");
        titleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEventName()));
        titleCol.setPrefWidth(200);

        TableColumn<Event, String> dateCol = new TableColumn<>("Date & Time");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEventDate() != null ? d.getValue().getEventDate().toString().replace("T", " ") : "TBA"
        ));
        dateCol.setPrefWidth(140);

        TableColumn<Event, String> venueCol = new TableColumn<>("Location");
        venueCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getVenue() != null ? d.getValue().getVenue() : "Campus Hall"
        ));
        venueCol.setPrefWidth(150);

        TableColumn<Event, Number> capCol = new TableColumn<>("Capacity");
        capCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCapacity()));
        capCol.setPrefWidth(80);

        TableColumn<Event, Number> regCol = new TableColumn<>("Registered");
        regCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getRegisteredCount()));
        regCol.setPrefWidth(90);

        eventsTable.getColumns().addAll(idCol, clubCol, titleCol, dateCol, venueCol, capCol, regCol);

        box.getChildren().addAll(topBar, eventsTable);
        return box;
    }

    // ==========================================
    // Tab 4: My Event Passes
    // ==========================================
    private VBox buildRegistrationsTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));

        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("My Active Event Registrations");
        title.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cancelBtn = FxUITheme.createDangerButton("Cancel Registration");
        cancelBtn.setOnAction(e -> handleCancelRegistration());

        Button refreshBtn = FxUITheme.createSecondaryButton("Refresh");
        refreshBtn.setOnAction(e -> loadRegistrations());

        topBar.getChildren().addAll(title, spacer, cancelBtn, refreshBtn);

        registrationsTable = new TableView<>(registrationsList);
        registrationsTable.setPlaceholder(new Label("No active event passes found. Check Upcoming Events to register!"));
        VBox.setVgrow(registrationsTable, Priority.ALWAYS);

        TableColumn<EventRegistration, Number> regIdCol = new TableColumn<>("Pass ID");
        regIdCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getRegistrationId()));
        regIdCol.setPrefWidth(80);

        TableColumn<EventRegistration, String> titleCol = new TableColumn<>("Event Title");
        titleCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEventName() != null ? d.getValue().getEventName() : "Event #" + d.getValue().getEventId()
        ));
        titleCol.setPrefWidth(220);

        TableColumn<EventRegistration, String> clubCol = new TableColumn<>("Club");
        clubCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getClubName() != null ? d.getValue().getClubName() : "N/A"
        ));
        clubCol.setPrefWidth(160);

        TableColumn<EventRegistration, String> dateCol = new TableColumn<>("Event Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEventDate() != null ? d.getValue().getEventDate().toString().replace("T", " ") : "N/A"
        ));
        dateCol.setPrefWidth(140);

        TableColumn<EventRegistration, String> venueCol = new TableColumn<>("Venue");
        venueCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getLocation() != null ? d.getValue().getLocation() : "Campus"
        ));
        venueCol.setPrefWidth(140);

        TableColumn<EventRegistration, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getRegistrationStatus() != null ? d.getValue().getRegistrationStatus() : "CONFIRMED"
        ));
        statusCol.setPrefWidth(110);

        registrationsTable.getColumns().addAll(regIdCol, titleCol, clubCol, dateCol, venueCol, statusCol);

        box.getChildren().addAll(topBar, registrationsTable);
        return box;
    }

    // ==========================================
    // Tab 5: Announcements
    // ==========================================
    private VBox buildAnnouncementsTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));

        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Campus & Club Announcements");
        title.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = FxUITheme.createSecondaryButton("Refresh");
        refreshBtn.setOnAction(e -> loadAnnouncements());

        topBar.getChildren().addAll(title, spacer, refreshBtn);

        announcementsTable = new TableView<>(announcementsList);
        announcementsTable.setPlaceholder(new Label("No announcements posted yet."));
        VBox.setVgrow(announcementsTable, Priority.ALWAYS);

        TableColumn<Announcement, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getAnnouncementId()));
        idCol.setPrefWidth(50);

        TableColumn<Announcement, String> clubCol = new TableColumn<>("Club / Source");
        clubCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getClubName() != null ? d.getValue().getClubName() : "Campus General"
        ));
        clubCol.setPrefWidth(160);

        TableColumn<Announcement, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        titleCol.setPrefWidth(200);

        TableColumn<Announcement, String> msgCol = new TableColumn<>("Message");
        msgCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMessage()));
        msgCol.setPrefWidth(300);

        TableColumn<Announcement, String> prioCol = new TableColumn<>("Priority");
        prioCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPriority() != null ? d.getValue().getPriority() : "NORMAL"
        ));
        prioCol.setPrefWidth(100);

        announcementsTable.getColumns().addAll(idCol, clubCol, titleCol, msgCol, prioCol);

        box.getChildren().addAll(topBar, announcementsTable);
        return box;
    }

    // ==========================================
    // Tab 6: Smart Recommendations
    // ==========================================
    private VBox buildRecommendationsTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));

        Label header = new Label("⭐ Rule-Based Max-Heap Club Recommendations");
        header.getStyleClass().add("card-title");

        Label desc = new Label("Ranked algorithm based on your academic department, declared interest keywords, and club membership popularity.");
        desc.getStyleClass().add("card-subtitle");

        recommendationsListView = new ListView<>(recommendationsList);
        recommendationsListView.setPlaceholder(new Label("No recommendations available. You may already be in all top matched clubs!"));
        VBox.setVgrow(recommendationsListView, Priority.ALWAYS);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button applyRecBtn = FxUITheme.createPrimaryButton("Apply to Selected Recommendation");
        applyRecBtn.setOnAction(e -> handleApplyRecommendation());

        Button recomputeBtn = FxUITheme.createSecondaryButton("Recompute Recommendations");
        recomputeBtn.setOnAction(e -> loadRecommendations());

        actions.getChildren().addAll(recomputeBtn, applyRecBtn);

        box.getChildren().addAll(header, desc, recommendationsListView, actions);
        return box;
    }

    // ==========================================
    // Tab 7: Profile & Interests
    // ==========================================
    private VBox buildProfileTab() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(20));

        Label header = new Label("👤 Student Profile & Interests");
        header.getStyleClass().add("card-title");

        Label desc = new Label("View your academic registration details and update interest keywords used for club matching.");
        desc.getStyleClass().add("card-subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setPadding(new Insets(12, 0, 12, 0));

        // Row 0: Full Name
        grid.add(new Label("Full Name:"), 0, 0);
        Label nameVal = new Label(student.getFullName() != null ? student.getFullName() : "N/A");
        nameVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B;");
        grid.add(nameVal, 1, 0);

        // Row 1: Register No. / USN
        grid.add(new Label("USN / Register No.:"), 0, 1);
        Label usnVal = new Label(student.getRegisterNumber() != null ? student.getRegisterNumber() : "N/A");
        usnVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B;");
        grid.add(usnVal, 1, 1);

        // Row 2: Department
        grid.add(new Label("Department:"), 0, 2);
        Label deptVal = new Label(student.getDepartment() != null ? student.getDepartment() : "General");
        grid.add(deptVal, 1, 2);

        // Row 3: Semester / Year
        grid.add(new Label("Semester / Year:"), 0, 3);
        Label semVal = new Label("Sem " + student.getSemester() + " (Year " + student.getYear() + ")");
        grid.add(semVal, 1, 3);

        // Row 4: Email
        grid.add(new Label("Email:"), 0, 4);
        Label emailVal = new Label(student.getEmail() != null ? student.getEmail() : "N/A");
        grid.add(emailVal, 1, 4);

        // Row 5: Editable Phone
        grid.add(new Label("Phone Number:"), 0, 5);
        profilePhoneField = new TextField(student.getPhoneNumber() != null ? student.getPhoneNumber() : "");
        profilePhoneField.setPrefWidth(280);
        grid.add(profilePhoneField, 1, 5);

        // Row 6: Editable Interests
        grid.add(new Label("Interests (comma separated):"), 0, 6);
        profileInterestsField = new TextField(student.getInterests() != null ? student.getInterests() : "AI, Coding, Robotics");
        profileInterestsField.setPrefWidth(350);
        grid.add(profileInterestsField, 1, 6);

        Button saveBtn = FxUITheme.createPrimaryButton("Save Profile Changes");
        saveBtn.setOnAction(e -> handleSaveProfile());

        HBox btnBox = new HBox(saveBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(header, desc, grid, btnBox);
        return box;
    }

    // ==========================================
    // Data Loading & Handlers
    // ==========================================

    public void loadAllData() {
        loadClubs();
        loadMemberships();
        loadEvents();
        loadRegistrations();
        loadAnnouncements();
        loadRecommendations();
        updateKpis();
    }

    private void updateKpis() {
        FxUITheme.updateKpiCard(joinedClubsCard, String.valueOf(membershipsList.size()));
        FxUITheme.updateKpiCard(registeredEventsCard, String.valueOf(registrationsList.size()));
        FxUITheme.updateKpiCard(availableEventsCard, String.valueOf(eventsList.size()));
        FxUITheme.updateKpiCard(recommendationsCard, String.valueOf(activeRecommendations.size()));
    }

    public void loadClubs() {
        try {
            allLoadedClubs = clubService.getActiveClubs();
            applyClubFilter();
        } catch (DatabaseOperationException ex) {
            FxUITheme.showNotification(Alert.AlertType.ERROR, "Database Error", "Failed to load clubs: " + ex.getMessage());
        }
    }

    private void applyClubFilter() {
        String keyword = clubSearchField != null ? clubSearchField.getText().trim().toLowerCase() : "";
        String cat = clubCategoryBox != null && clubCategoryBox.getValue() != null ? clubCategoryBox.getValue() : "All Categories";

        clubsList.clear();
        for (Club c : allLoadedClubs) {
            boolean matchesCat = "All Categories".equals(cat) || (c.getCategory() != null && c.getCategory().equalsIgnoreCase(cat));
            boolean matchesKey = keyword.isEmpty() ||
                    (c.getClubName() != null && c.getClubName().toLowerCase().contains(keyword)) ||
                    (c.getDescription() != null && c.getDescription().toLowerCase().contains(keyword));

            if (matchesCat && matchesKey) {
                clubsList.add(c);
            }
        }
    }

    public void loadMemberships() {
        membershipsList.clear();
        try {
            List<Membership> list = membershipService.getStudentMemberships(student.getUserId());
            membershipsList.addAll(list);
        } catch (DatabaseOperationException ex) {
            System.err.println("Error loading memberships: " + ex.getMessage());
        }
        updateKpis();
    }

    public void loadEvents() {
        try {
            allLoadedEvents = eventService.getUpcomingEvents();
            applyEventSearch();
        } catch (DatabaseOperationException ex) {
            System.err.println("Error loading events: " + ex.getMessage());
        }
        updateKpis();
    }

    private void applyEventSearch() {
        String query = eventSearchField != null ? eventSearchField.getText().trim().toLowerCase() : "";
        eventsList.clear();
        for (Event e : allLoadedEvents) {
            boolean match = query.isEmpty() ||
                    (e.getEventName() != null && e.getEventName().toLowerCase().contains(query)) ||
                    (e.getClubName() != null && e.getClubName().toLowerCase().contains(query)) ||
                    (e.getVenue() != null && e.getVenue().toLowerCase().contains(query));
            if (match) {
                eventsList.add(e);
            }
        }
    }

    public void loadRegistrations() {
        registrationsList.clear();
        try {
            List<EventRegistration> list = eventService.getStudentRegistrations(student.getUserId());
            registrationsList.addAll(list);
        } catch (DatabaseOperationException ex) {
            System.err.println("Error loading registrations: " + ex.getMessage());
        }
        updateKpis();
    }

    public void loadAnnouncements() {
        announcementsList.clear();
        try {
            List<Announcement> list = eventService.getAllAnnouncements();
            announcementsList.addAll(list);
        } catch (DatabaseOperationException ex) {
            System.err.println("Error loading announcements: " + ex.getMessage());
        }
    }

    public void loadRecommendations() {
        recommendationsList.clear();
        activeRecommendations.clear();
        try {
            List<Club> clubs = clubService.getActiveClubs();
            List<Membership> memberships = membershipService.getStudentMemberships(student.getUserId());
            activeRecommendations = recommendationEngine.recommendClubs(student, clubs, memberships);

            if (activeRecommendations.isEmpty()) {
                recommendationsList.add("No new recommendations available. You are already part of top matched clubs!");
            } else {
                for (int i = 0; i < activeRecommendations.size(); i++) {
                    ClubRecommendationEngine.ClubRecommendation r = activeRecommendations.get(i);
                    String item = String.format("#%d  ⭐ %s  [%s]  —  Score: %d pts  |  Reasons: %s",
                            (i + 1),
                            r.getClub().getClubName(),
                            r.getClub().getCategory(),
                            r.getScore(),
                            r.getReasonSummary());
                    recommendationsList.add(item);
                }
            }
        } catch (Exception ex) {
            recommendationsList.add("Unable to compute recommendations currently: " + ex.getMessage());
        }
        updateKpis();
    }

    // ==========================================
    // Actions
    // ==========================================

    private void handleJoinClub() {
        Club selected = clubsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxUITheme.showNotification(Alert.AlertType.WARNING, "Selection Required", "Please select a club from the table to join.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Club Application");
        confirm.setHeaderText("Apply to join " + selected.getClubName() + "?");
        confirm.setContentText("Your membership application will be sent to the club coordinator for review.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            membershipService.applyForMembership(student.getUserId(), selected.getClubId());
            FxUITheme.showNotification(Alert.AlertType.INFORMATION, "Application Sent",
                    "Application submitted successfully for " + selected.getClubName() + "!\nAwaiting coordinator approval.");
            loadMemberships();
            loadRecommendations();
        } catch (DuplicateMembershipException ex) {
            FxUITheme.showNotification(Alert.AlertType.WARNING, "Already Applied / Member", ex.getMessage());
        } catch (ClubNotFoundException ex) {
            FxUITheme.showNotification(Alert.AlertType.ERROR, "Club Not Found", ex.getMessage());
        } catch (Exception ex) {
            FxUITheme.showNotification(Alert.AlertType.ERROR, "Error", "Error applying for club: " + ex.getMessage());
        }
    }

    private void handleLeaveClub() {
        Membership selected = membershipsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxUITheme.showNotification(Alert.AlertType.WARNING, "Selection Required", "Please select a membership from the table to withdraw/leave.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Leave Club");
        confirm.setHeaderText("Withdraw from " + selected.getClubName() + "?");
        confirm.setContentText("Are you sure you want to withdraw your membership?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            membershipService.leaveClub(student.getUserId(), selected.getClubId());
            FxUITheme.showNotification(Alert.AlertType.INFORMATION, "Membership Updated", "You have left " + selected.getClubName() + ".");
            loadMemberships();
            loadClubs();
            loadRecommendations();
        } catch (DatabaseOperationException ex) {
            FxUITheme.showNotification(Alert.AlertType.ERROR, "Database Error", ex.getMessage());
        }
    }

    private void handleEventRegistration() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxUITheme.showNotification(Alert.AlertType.WARNING, "Selection Required", "Please select an event to register for.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Register for Event");
        confirm.setHeaderText("Register for: " + selected.getEventName() + "?");
        confirm.setContentText("Organized by: " + (selected.getClubName() != null ? selected.getClubName() : "Campus Club"));
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            eventService.registerForEvent(selected.getEventId(), student.getUserId());
            FxUITheme.showNotification(Alert.AlertType.INFORMATION, "Registration Confirmed", "Successfully registered for " + selected.getEventName() + "!");
            loadEvents();
            loadRegistrations();
        } catch (EventRegistrationException ex) {
            FxUITheme.showNotification(Alert.AlertType.WARNING, "Registration Notice", ex.getMessage());
        } catch (Exception ex) {
            FxUITheme.showNotification(Alert.AlertType.ERROR, "Registration Error", ex.getMessage());
        }
    }

    private void handleCancelRegistration() {
        EventRegistration selected = registrationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxUITheme.showNotification(Alert.AlertType.WARNING, "Selection Required", "Please select a registration to cancel.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Registration");
        confirm.setHeaderText("Cancel pass for: " + selected.getEventName() + "?");
        confirm.setContentText("Your spot will be released to other students.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            eventService.cancelRegistration(selected.getEventId(), student.getUserId());
            FxUITheme.showNotification(Alert.AlertType.INFORMATION, "Pass Cancelled", "Registration cancelled successfully.");
            loadEvents();
            loadRegistrations();
        } catch (DatabaseOperationException ex) {
            FxUITheme.showNotification(Alert.AlertType.ERROR, "Database Error", ex.getMessage());
        }
    }

    private void handleApplyRecommendation() {
        int idx = recommendationsListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= activeRecommendations.size()) {
            FxUITheme.showNotification(Alert.AlertType.WARNING, "Selection Required", "Please select a valid recommendation from the list.");
            return;
        }

        ClubRecommendationEngine.ClubRecommendation rec = activeRecommendations.get(idx);
        Club club = rec.getClub();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Apply via Smart Recommendation");
        confirm.setHeaderText("Apply to " + club.getClubName() + " (" + rec.getScore() + " pts match)?");
        confirm.setContentText("Match Reasons: " + rec.getReasonSummary());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            membershipService.applyForMembership(student.getUserId(), club.getClubId());
            FxUITheme.showNotification(Alert.AlertType.INFORMATION, "Application Sent",
                    "Application submitted successfully for " + club.getClubName() + "!\nAwaiting coordinator approval.");
            loadMemberships();
            loadRecommendations();
        } catch (DuplicateMembershipException ex) {
            FxUITheme.showNotification(Alert.AlertType.WARNING, "Already Member", ex.getMessage());
        } catch (Exception ex) {
            FxUITheme.showNotification(Alert.AlertType.ERROR, "Error", ex.getMessage());
        }
    }

    private void handleSaveProfile() {
        String phone = profilePhoneField.getText().trim();
        String interests = profileInterestsField.getText().trim();

        student.setPhoneNumber(phone);
        student.setInterests(interests);

        try {
            studentService.updateProfile(student);
            FxUITheme.showNotification(Alert.AlertType.INFORMATION, "Profile Updated", "Your contact details and interests have been saved.\nRecommendations will reflect updated interests.");
            loadRecommendations();
        } catch (DatabaseOperationException ex) {
            FxUITheme.showNotification(Alert.AlertType.ERROR, "Update Failed", "Could not save profile: " + ex.getMessage());
        }
    }
}
