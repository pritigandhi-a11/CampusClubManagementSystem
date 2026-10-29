package com.campusclub.javafx;

import com.campusclub.dao.AnnouncementDAO;
import com.campusclub.dao.UserDAO;
import com.campusclub.model.*;
import com.campusclub.service.ClubService;
import com.campusclub.service.EventService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JavaFX Dashboard for System Administrators.
 * Features:
 * - KPI summary cards
 * - Tab 1: Campus Clubs management (Register, Toggle status, Filter)
 * - Tab 2: User Registry (Filter by role, inspect accounts)
 * - Tab 3: Campus Events overview and scheduling
 * - Tab 4: Broadcast Announcements
 */
public class FxAdminDashboard extends VBox {

    private final Admin admin;
    private final ClubService clubService;
    private final EventService eventService;
    private final UserDAO userDAO;
    private final AnnouncementDAO announcementDAO;

    // KPI Cards
    private VBox totalUsersCard;
    private VBox totalClubsCard;
    private VBox activeClubsCard;
    private VBox totalEventsCard;

    // Clubs Tab
    private TableView<Club> clubsTable;
    private ObservableList<Club> clubsList = FXCollections.observableArrayList();
    private TextField clubSearchField;

    // Users Tab
    private TableView<User> usersTable;
    private ObservableList<User> usersList = FXCollections.observableArrayList();
    private ComboBox<String> roleFilterBox;

    // Events Tab
    private TableView<Event> eventsTable;
    private ObservableList<Event> eventsList = FXCollections.observableArrayList();

    // Announcements Tab
    private TableView<Announcement> announcementsTable;
    private ObservableList<Announcement> announcementsList = FXCollections.observableArrayList();

    public FxAdminDashboard(Admin admin) {
        this(admin, new ClubService(), new EventService(), new UserDAO(), new AnnouncementDAO());
    }

    public FxAdminDashboard(Admin admin, ClubService clubService, EventService eventService, UserDAO userDAO, AnnouncementDAO announcementDAO) {
        this.admin = admin != null ? admin : new Admin();
        this.clubService = clubService != null ? clubService : new ClubService();
        this.eventService = eventService != null ? eventService : new EventService();
        this.userDAO = userDAO != null ? userDAO : new UserDAO();
        this.announcementDAO = announcementDAO != null ? announcementDAO : new AnnouncementDAO();

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

        totalUsersCard = FxUITheme.createKpiCard("TOTAL USERS", "0", "Registered accounts in system");
        totalClubsCard = FxUITheme.createKpiCard("TOTAL CLUBS", "0", "Campus organizations");
        activeClubsCard = FxUITheme.createKpiCard("ACTIVE CLUBS", "0", "Operational student clubs");
        totalEventsCard = FxUITheme.createKpiCard("TOTAL EVENTS", "0", "Scheduled campus events");

        kpiBox.getChildren().addAll(totalUsersCard, totalClubsCard, activeClubsCard, totalEventsCard);
        getChildren().add(kpiBox);
    }

    private void initTabs() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab tabClubs = new Tab("🏫 Clubs Management", buildClubsTab());
        Tab tabUsers = new Tab("👥 User Registry", buildUsersTab());
        Tab tabEvents = new Tab("🗓️ Campus Events", buildEventsTab());
        Tab tabNotices = new Tab("📢 Broadcast Notices", buildAnnouncementsTab());

        tabPane.getTabs().addAll(tabClubs, tabUsers, tabEvents, tabNotices);
        getChildren().add(tabPane);
    }

    // =========================================================================
    // 1. CLUBS TAB
    // =========================================================================
    private Node buildClubsTab() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(12));

        // Toolbar
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        clubSearchField = new TextField();
        clubSearchField.setPromptText("🔍 Search clubs by name...");
        clubSearchField.setPrefWidth(240);
        clubSearchField.textProperty().addListener((obs, oldVal, newVal) -> filterClubs(newVal));

        Button btnAddClub = FxUITheme.createPrimaryButton("➕ Register Club");
        btnAddClub.setOnAction(e -> showRegisterClubDialog());

        Button btnToggle = FxUITheme.createSecondaryButton("🔄 Toggle Status");
        btnToggle.setOnAction(e -> handleToggleClubStatus());

        Button btnRefresh = FxUITheme.createSecondaryButton("↻ Refresh");
        btnRefresh.setOnAction(e -> loadClubs());

        toolbar.getChildren().addAll(clubSearchField, btnAddClub, btnToggle, btnRefresh);

        // Table
        clubsTable = new TableView<>(clubsList);
        clubsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(clubsTable, Priority.ALWAYS);

        TableColumn<Club, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getClubId()));
        colId.setMaxWidth(60);

        TableColumn<Club, String> colName = new TableColumn<>("Club Name");
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getClubName()));

        TableColumn<Club, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));

        TableColumn<Club, String> colCoord = new TableColumn<>("Coordinator");
        colCoord.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCoordinatorName() != null ? c.getValue().getCoordinatorName() : "Faculty"));

        TableColumn<Club, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        TableColumn<Club, Number> colMem = new TableColumn<>("Members");
        colMem.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getMemberCount()));

        clubsTable.getColumns().addAll(colId, colName, colCat, colCoord, colStatus, colMem);

        root.getChildren().addAll(toolbar, clubsTable);
        return root;
    }

    private void showRegisterClubDialog() {
        Dialog<Club> dialog = new Dialog<>();
        dialog.setTitle("Register New Campus Club");
        dialog.setHeaderText("Create and authorize a new campus club");

        ButtonType btnTypeSave = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnTypeSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Club Name");

        ComboBox<String> catBox = new ComboBox<>(FXCollections.observableArrayList("Technical", "Cultural", "Sports", "Academic"));
        catBox.setValue("Technical");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Club description and objectives");
        descArea.setPrefRowCount(3);

        grid.add(new Label("Club Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(catBox, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descArea, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnTypeSave) {
                if (nameField.getText().trim().isEmpty()) return null;
                Club club = new Club();
                club.setClubName(nameField.getText().trim());
                club.setCategory(catBox.getValue());
                club.setDescription(descArea.getText().trim());
                club.setStatus("ACTIVE");
                return club;
            }
            return null;
        });

        Optional<Club> result = dialog.showAndWait();
        result.ifPresent(club -> {
            try {
                clubService.createClub(club);
                loadClubs();
            } catch (Exception ex) {
                showError("Could not create club: " + ex.getMessage());
            }
        });
    }

    private void handleToggleClubStatus() {
        Club selected = clubsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Please select a club from the table first.");
            return;
        }
        try {
            String newStatus = "ACTIVE".equalsIgnoreCase(selected.getStatus()) ? "INACTIVE" : "ACTIVE";
            Club club = clubService.getClubById(selected.getClubId());
            club.setStatus(newStatus);
            clubService.updateClub(club);
            loadClubs();
        } catch (Exception ex) {
            showError("Could not toggle status: " + ex.getMessage());
        }
    }

    // =========================================================================
    // 2. USERS TAB
    // =========================================================================
    private Node buildUsersTab() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(12));

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label lblFilter = new Label("Role Filter:");
        lblFilter.setStyle("-fx-font-weight: bold;");

        roleFilterBox = new ComboBox<>(FXCollections.observableArrayList("ALL", "ADMIN", "COORDINATOR", "STUDENT"));
        roleFilterBox.setValue("ALL");
        roleFilterBox.setOnAction(e -> filterUsers());

        Button btnRefresh = FxUITheme.createSecondaryButton("↻ Refresh Users");
        btnRefresh.setOnAction(e -> loadUsers());

        toolbar.getChildren().addAll(lblFilter, roleFilterBox, btnRefresh);

        usersTable = new TableView<>(usersList);
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(usersTable, Priority.ALWAYS);

        TableColumn<User, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(u -> new SimpleIntegerProperty(u.getValue().getUserId()));
        colId.setMaxWidth(60);

        TableColumn<User, String> colUser = new TableColumn<>("Username");
        colUser.setCellValueFactory(u -> new SimpleStringProperty(u.getValue().getUsername()));

        TableColumn<User, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(u -> new SimpleStringProperty(u.getValue().getFullName()));

        TableColumn<User, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(u -> new SimpleStringProperty(u.getValue().getEmail()));

        TableColumn<User, String> colRole = new TableColumn<>("Role");
        colRole.setCellValueFactory(u -> new SimpleStringProperty(u.getValue().getRole().name()));

        usersTable.getColumns().addAll(colId, colUser, colName, colEmail, colRole);

        root.getChildren().addAll(toolbar, usersTable);
        return root;
    }

    // =========================================================================
    // 3. EVENTS TAB
    // =========================================================================
    private Node buildEventsTab() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(12));

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnAddEvent = FxUITheme.createPrimaryButton("➕ Schedule Event");
        btnAddEvent.setOnAction(e -> showScheduleEventDialog());

        Button btnRefresh = FxUITheme.createSecondaryButton("↻ Refresh Events");
        btnRefresh.setOnAction(e -> loadEvents());

        toolbar.getChildren().addAll(btnAddEvent, btnRefresh);

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

        TableColumn<Event, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(ev -> new SimpleStringProperty(ev.getValue().getStatus()));

        eventsTable.getColumns().addAll(colId, colName, colDate, colVenue, colCap, colStatus);

        root.getChildren().addAll(toolbar, eventsTable);
        return root;
    }

    private void showScheduleEventDialog() {
        Dialog<Event> dialog = new Dialog<>();
        dialog.setTitle("Schedule Campus Event");
        dialog.setHeaderText("Create a new event schedule");

        ButtonType btnTypeSave = new ButtonType("Schedule", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnTypeSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 120, 10, 10));

        TextField titleField = new TextField();
        titleField.setPromptText("e.g. National Coding Hackathon");

        TextField venueField = new TextField();
        venueField.setPromptText("e.g. Seminar Hall A");

        TextField capField = new TextField("100");

        grid.add(new Label("Event Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Venue:"), 0, 1);
        grid.add(venueField, 1, 1);
        grid.add(new Label("Capacity:"), 0, 2);
        grid.add(capField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == btnTypeSave) {
                if (titleField.getText().trim().isEmpty()) return null;
                Event ev = new Event();
                ev.setEventName(titleField.getText().trim());
                ev.setVenue(venueField.getText().trim());
                ev.setClubId(1); // Default to primary campus club
                ev.setEventDate(LocalDateTime.now().plusDays(7));
                try {
                    ev.setCapacity(Integer.parseInt(capField.getText().trim()));
                } catch (Exception ignored) {
                    ev.setCapacity(100);
                }
                ev.setStatus("UPCOMING");
                return ev;
            }
            return null;
        });

        Optional<Event> res = dialog.showAndWait();
        res.ifPresent(ev -> {
            try {
                eventService.createEvent(ev);
                loadEvents();
            } catch (Exception ex) {
                showError("Could not schedule event: " + ex.getMessage());
            }
        });
    }

    // =========================================================================
    // 4. ANNOUNCEMENTS TAB
    // =========================================================================
    private Node buildAnnouncementsTab() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(12));

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnAddNotice = FxUITheme.createPrimaryButton("📢 Publish Notice");
        btnAddNotice.setOnAction(e -> showPublishNoticeDialog());

        Button btnRefresh = FxUITheme.createSecondaryButton("↻ Refresh Notices");
        btnRefresh.setOnAction(e -> loadAnnouncements());

        toolbar.getChildren().addAll(btnAddNotice, btnRefresh);

        announcementsTable = new TableView<>(announcementsList);
        announcementsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(announcementsTable, Priority.ALWAYS);

        TableColumn<Announcement, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(a -> new SimpleIntegerProperty(a.getValue().getAnnouncementId()));
        colId.setMaxWidth(60);

        TableColumn<Announcement, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(a -> new SimpleStringProperty(a.getValue().getTitle()));

        TableColumn<Announcement, String> colClub = new TableColumn<>("Club");
        colClub.setCellValueFactory(a -> new SimpleStringProperty(a.getValue().getClubName() != null ? a.getValue().getClubName() : "Campus General"));

        TableColumn<Announcement, String> colPri = new TableColumn<>("Priority");
        colPri.setCellValueFactory(a -> new SimpleStringProperty(a.getValue().getPriority()));

        TableColumn<Announcement, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(a -> new SimpleStringProperty(a.getValue().getCreatedAt() != null ? a.getValue().getCreatedAt().toString() : "Recent"));

        TableColumn<Announcement, String> colMsg = new TableColumn<>("Message");
        colMsg.setCellValueFactory(a -> new SimpleStringProperty(a.getValue().getMessage()));

        announcementsTable.getColumns().addAll(colId, colTitle, colClub, colPri, colDate, colMsg);

        root.getChildren().addAll(toolbar, announcementsTable);
        return root;
    }

    private void showPublishNoticeDialog() {
        Dialog<Announcement> dialog = new Dialog<>();
        dialog.setTitle("Publish Campus Announcement");
        dialog.setHeaderText("Broadcast a campus-wide notice");

        ButtonType btnTypeSave = new ButtonType("Publish", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnTypeSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 120, 10, 10));

        TextField titleField = new TextField();
        titleField.setPromptText("Announcement Title");

        ComboBox<String> priorityBox = new ComboBox<>(FXCollections.observableArrayList("HIGH", "MEDIUM", "LOW"));
        priorityBox.setValue("HIGH");

        TextArea msgArea = new TextArea();
        msgArea.setPromptText("Detailed announcement message");
        msgArea.setPrefRowCount(3);

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Priority:"), 0, 1);
        grid.add(priorityBox, 1, 1);
        grid.add(new Label("Message:"), 0, 2);
        grid.add(msgArea, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == btnTypeSave) {
                if (titleField.getText().trim().isEmpty()) return null;
                Announcement a = new Announcement();
                a.setTitle(titleField.getText().trim());
                a.setMessage(msgArea.getText().trim());
                a.setPriority(priorityBox.getValue());
                a.setClubId(1);
                a.setCreatedAt(LocalDateTime.now());
                return a;
            }
            return null;
        });

        Optional<Announcement> res = dialog.showAndWait();
        res.ifPresent(a -> {
            try {
                announcementDAO.createAnnouncement(a);
                loadAnnouncements();
            } catch (Exception ex) {
                showError("Could not publish notice: " + ex.getMessage());
            }
        });
    }

    // =========================================================================
    // Data Loading & Filtering Helpers
    // =========================================================================
    public void loadAllData() {
        loadClubs();
        loadUsers();
        loadEvents();
        loadAnnouncements();
    }

    private void loadClubs() {
        try {
            List<Club> list = clubService.getAllClubs();
            clubsList.setAll(list != null ? list : List.of());
            updateKpiMetrics();
        } catch (Exception ignored) {
            clubsList.clear();
        }
    }

    private void filterClubs(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadClubs();
            return;
        }
        String q = query.trim().toLowerCase();
        try {
            List<Club> all = clubService.getAllClubs();
            if (all != null) {
                clubsList.setAll(all.stream()
                        .filter(c -> c.getClubName().toLowerCase().contains(q) || c.getCategory().toLowerCase().contains(q))
                        .toList());
            }
        } catch (Exception ignored) {}
    }

    private void loadUsers() {
        try {
            List<User> list = userDAO.findAll();
            usersList.setAll(list != null ? list : List.of());
            updateKpiMetrics();
        } catch (Exception ignored) {
            usersList.clear();
        }
    }

    private void filterUsers() {
        String role = roleFilterBox.getValue();
        try {
            List<User> all = userDAO.findAll();
            if (all != null) {
                if ("ALL".equalsIgnoreCase(role)) {
                    usersList.setAll(all);
                } else {
                    usersList.setAll(all.stream().filter(u -> u.getRole().name().equalsIgnoreCase(role)).toList());
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadEvents() {
        try {
            List<Event> list = eventService.getAllEvents();
            eventsList.setAll(list != null ? list : List.of());
            updateKpiMetrics();
        } catch (Exception ignored) {
            eventsList.clear();
        }
    }

    private void loadAnnouncements() {
        try {
            List<Announcement> list = announcementDAO.findAll();
            announcementsList.setAll(list != null ? list : List.of());
        } catch (Exception ignored) {
            announcementsList.clear();
        }
    }

    private void updateKpiMetrics() {
        updateCardValue(totalUsersCard, String.valueOf(usersList.size()));
        updateCardValue(totalClubsCard, String.valueOf(clubsList.size()));
        long active = clubsList.stream().filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus())).count();
        updateCardValue(activeClubsCard, String.valueOf(active));
        updateCardValue(totalEventsCard, String.valueOf(eventsList.size()));
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
