package com.buildpay.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Controls the Projects Management module: a responsive grid of project
 * cards (progress, crew size, budget, labour cost, timeline, status) with
 * search/filter/pagination, and a read-only Project Details dialog.
 */
public class ProjectsController {

    private static final int PAGE_SIZE = 6;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    // ---------------------------------------------------------------
    // Domain model
    // ---------------------------------------------------------------

    public enum ProjectStatus {
        ONGOING("Ongoing", "status-ongoing"),
        COMPLETED("Completed", "status-completed"),
        ON_HOLD("On Hold", "status-on-hold"),
        DELAYED("Delayed", "status-delayed");

        private final String label;
        private final String styleClass;

        ProjectStatus(String label, String styleClass) {
            this.label = label;
            this.styleClass = styleClass;
        }

        public String getStyleClass() { return styleClass; }

        @Override
        public String toString() { return label; }
    }

    public static class Project {
        private final String name;
        private final String location;
        private final ProjectStatus status;
        private final int progressPercent;
        private final int workersAssigned;
        private final double budget;
        private final double labourCost;
        private final LocalDate startDate;
        private final LocalDate endDate;

        public Project(String name, String location, ProjectStatus status, int progressPercent,
                        int workersAssigned, double budget, double labourCost,
                        LocalDate startDate, LocalDate endDate) {
            this.name = name;
            this.location = location;
            this.status = status;
            this.progressPercent = progressPercent;
            this.workersAssigned = workersAssigned;
            this.budget = budget;
            this.labourCost = labourCost;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public String getName() { return name; }
        public String getLocation() { return location; }
        public ProjectStatus getStatus() { return status; }
        public int getProgressPercent() { return progressPercent; }
        public int getWorkersAssigned() { return workersAssigned; }
        public double getBudget() { return budget; }
        public double getLabourCost() { return labourCost; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
    }

    // ---------------------------------------------------------------
    // FXML fields
    // ---------------------------------------------------------------

    @FXML private VBox rootPane;
    @FXML private Button backButton;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;

    @FXML private ScrollPane cardsScroll;
    @FXML private FlowPane projectsFlowPane;

    @FXML private Label paginationSummaryLabel;
    @FXML private Button prevPageButton;
    @FXML private HBox pageNumbersBox;
    @FXML private Button nextPageButton;

    // ---------------------------------------------------------------
    // State
    // ---------------------------------------------------------------

    private final List<Project> allProjects = new ArrayList<>();
    private List<Project> filteredProjects = new ArrayList<>();
    private int currentPage = 0;

    @FXML
    public void initialize() {
        seedProjects();
        setupFilters();
        applyFilters();
    }

    // ---------------------------------------------------------------
    // Sample data (project names/budgets match the Dashboard's cost summary
    // for the four in-progress sites, for consistency across BuildPay)
    // ---------------------------------------------------------------

    private void seedProjects() {
        LocalDate today = LocalDate.now();

        allProjects.add(new Project("Riverside Apartments", "Nairobi \u2014 Riverside", ProjectStatus.ONGOING,
                72, 34, 2_100_000, 1_180_000, today.minusMonths(5), today.plusMonths(2)));
        allProjects.add(new Project("Westgate Office Park", "Nairobi \u2014 Westlands", ProjectStatus.ONGOING,
                48, 26, 1_400_000, 640_000, today.minusMonths(3), today.plusMonths(4)));
        allProjects.add(new Project("Northbridge Housing", "Kiambu \u2014 Northbridge", ProjectStatus.DELAYED,
                31, 19, 980_000, 510_000, today.minusMonths(4), today.minusDays(10)));
        allProjects.add(new Project("Coast Road Extension", "Mombasa \u2014 Coast Rd", ProjectStatus.ONGOING,
                58, 22, 620_000, 310_000, today.minusMonths(2), today.plusMonths(3)));
        allProjects.add(new Project("Lakeview Retail Plaza", "Kisumu \u2014 Lakeview", ProjectStatus.ON_HOLD,
                20, 8, 1_150_000, 220_000, today.minusMonths(2), today.plusMonths(6)));
        allProjects.add(new Project("Greenfield Primary School", "Nakuru \u2014 Greenfield", ProjectStatus.COMPLETED,
                100, 15, 540_000, 410_000, today.minusMonths(8), today.minusMonths(1)));
        allProjects.add(new Project("Summit Ridge Warehouse", "Nairobi \u2014 Industrial Area", ProjectStatus.ONGOING,
                85, 28, 1_760_000, 990_000, today.minusMonths(6), today.plusWeeks(3)));
        allProjects.add(new Project("Meru Water Treatment Plant", "Meru \u2014 Kaaga", ProjectStatus.ONGOING,
                12, 12, 2_400_000, 260_000, today.minusWeeks(3), today.plusMonths(9)));
    }

    // ---------------------------------------------------------------
    // Filters
    // ---------------------------------------------------------------

    private void setupFilters() {
        statusFilter.getItems().setAll("All Statuses", "Ongoing", "Completed", "On Hold", "Delayed");
        statusFilter.setValue("All Statuses");
        searchField.textProperty().addListener((o, ov, nv) -> applyFilters());
        statusFilter.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String status = statusFilter.getValue();

        filteredProjects = allProjects.stream()
                .filter(p -> query.isEmpty() || p.getName().toLowerCase().contains(query)
                        || p.getLocation().toLowerCase().contains(query))
                .filter(p -> status == null || status.equals("All Statuses") || p.getStatus().toString().equals(status))
                .toList();

        currentPage = 0;
        refreshCurrentPageView();
    }

    // ---------------------------------------------------------------
    // Card rendering
    // ---------------------------------------------------------------

    private VBox buildProjectCard(Project p) {
        VBox card = new VBox(14);
        card.getStyleClass().add("project-card");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(2);
        Label nameLabel = new Label(p.getName());
        nameLabel.getStyleClass().add("project-name");
        Label locationLabel = new Label(p.getLocation());
        locationLabel.getStyleClass().add("project-location");
        titleBox.getChildren().addAll(nameLabel, locationLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label statusChip = new Label(p.getStatus().toString());
        statusChip.getStyleClass().addAll("status-chip", p.getStatus().getStyleClass());
        header.getChildren().addAll(titleBox, spacer, statusChip);

        VBox progressBox = new VBox(6);
        HBox progressLabelRow = new HBox();
        Label progressTitle = new Label("Progress");
        progressTitle.getStyleClass().add("progress-title");
        Region progressSpacer = new Region();
        HBox.setHgrow(progressSpacer, Priority.ALWAYS);
        Label progressPct = new Label(p.getProgressPercent() + "%");
        progressPct.getStyleClass().add("progress-pct");
        progressLabelRow.getChildren().addAll(progressTitle, progressSpacer, progressPct);

        StackPane track = new StackPane();
        track.getStyleClass().add("progress-track");
        Region fill = new Region();
        fill.getStyleClass().add(progressFillClass(p));
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);
        track.widthProperty().addListener((obs, oldW, newW) ->
                fill.setPrefWidth(newW.doubleValue() * (p.getProgressPercent() / 100.0)));
        fill.setPrefWidth(0);

        progressBox.getChildren().addAll(progressLabelRow, track);

        HBox metaRow = new HBox(18);
        metaRow.getChildren().addAll(
                metaItem("Workers", p.getWorkersAssigned() + " assigned"),
                metaItem("Budget", formatMoney(p.getBudget())),
                metaItem("Labour Cost", formatMoney(p.getLabourCost()))
        );

        Separator sep = new Separator();

        HBox timelineRow = new HBox();
        VBox timelineDates = new VBox(2);
        Label datesLabel = new Label(p.getStartDate().format(DATE_FMT) + "  \u2192  " + p.getEndDate().format(DATE_FMT));
        datesLabel.getStyleClass().add("timeline-dates");
        Label timelineNote = new Label(timelineNote(p));
        timelineNote.getStyleClass().add(timelineNoteStyleClass(p));
        timelineDates.getChildren().addAll(datesLabel, timelineNote);
        Region timelineSpacer = new Region();
        HBox.setHgrow(timelineSpacer, Priority.ALWAYS);
        Button detailsBtn = new Button("View Details");
        detailsBtn.getStyleClass().add("details-btn");
        detailsBtn.setOnAction(e -> showProjectDetails(p));
        timelineRow.getChildren().addAll(timelineDates, timelineSpacer, detailsBtn);
        timelineRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(header, progressBox, metaRow, sep, timelineRow);
        return card;
    }

    private VBox metaItem(String label, String value) {
        VBox box = new VBox(2);
        Label l = new Label(label);
        l.getStyleClass().add("meta-label");
        Label v = new Label(value);
        v.getStyleClass().add("meta-value");
        box.getChildren().addAll(l, v);
        return box;
    }

    private String progressFillClass(Project p) {
        if (p.getStatus() == ProjectStatus.DELAYED) return "progress-fill-delayed";
        if (p.getStatus() == ProjectStatus.COMPLETED) return "progress-fill-completed";
        if (p.getStatus() == ProjectStatus.ON_HOLD) return "progress-fill-hold";
        return "progress-fill-ongoing";
    }

    private String timelineNote(Project p) {
        if (p.getStatus() == ProjectStatus.COMPLETED) return "Completed";
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), p.getEndDate());
        if (daysLeft < 0) return "Overdue by " + Math.abs(daysLeft) + " days";
        return daysLeft + " days remaining";
    }

    private String timelineNoteStyleClass(Project p) {
        if (p.getStatus() == ProjectStatus.COMPLETED) return "timeline-note-ok";
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), p.getEndDate());
        return daysLeft < 0 ? "timeline-note-overdue" : "timeline-note-ok";
    }

    private String formatMoney(double value) {
        return String.format("KES %,.0f", value);
    }

    // ---------------------------------------------------------------
    // Pagination
    // ---------------------------------------------------------------

    private int totalPages() {
        return Math.max(1, (int) Math.ceil(filteredProjects.size() / (double) PAGE_SIZE));
    }

    private void refreshCurrentPageView() {
        int total = filteredProjects.size();
        int totalPages = totalPages();
        currentPage = Math.min(currentPage, totalPages - 1);
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);

        List<Project> pageItems = total == 0 ? List.of() : filteredProjects.subList(from, to);
        projectsFlowPane.getChildren().clear();
        for (Project p : pageItems) {
            projectsFlowPane.getChildren().add(buildProjectCard(p));
        }

        paginationSummaryLabel.setText(total == 0
                ? "No projects found"
                : String.format("Showing %d\u2013%d of %d", from + 1, to, total));

        prevPageButton.setDisable(currentPage == 0);
        nextPageButton.setDisable(currentPage >= totalPages - 1);

        pageNumbersBox.getChildren().clear();
        for (int i = 0; i < totalPages; i++) {
            int pageIndex = i;
            Button pageBtn = new Button(String.valueOf(i + 1));
            pageBtn.getStyleClass().add("pagination-page-btn");
            if (i == currentPage) pageBtn.getStyleClass().add("pagination-page-btn-active");
            pageBtn.setOnAction(e -> { currentPage = pageIndex; refreshCurrentPageView(); });
            pageNumbersBox.getChildren().add(pageBtn);
        }
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) { currentPage--; refreshCurrentPageView(); }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages() - 1) { currentPage++; refreshCurrentPageView(); }
    }

    // ---------------------------------------------------------------
    // Project details dialog
    // ---------------------------------------------------------------

    private void showProjectDetails(Project p) {
        Label title = new Label(p.getName());
        title.getStyleClass().add("details-title");
        Label location = new Label(p.getLocation());
        location.getStyleClass().add("details-location");
        Label statusChip = new Label(p.getStatus().toString());
        statusChip.getStyleClass().addAll("status-chip", p.getStatus().getStyleClass());

        HBox headerRow = new HBox(10, title, statusChip);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox progressBox = new VBox(6);
        HBox progressLabelRow = new HBox();
        Label progressTitle = new Label("Overall Progress");
        progressTitle.getStyleClass().add("progress-title");
        Region progressSpacer = new Region();
        HBox.setHgrow(progressSpacer, Priority.ALWAYS);
        Label progressPct = new Label(p.getProgressPercent() + "%");
        progressPct.getStyleClass().add("progress-pct");
        progressLabelRow.getChildren().addAll(progressTitle, progressSpacer, progressPct);

        StackPane track = new StackPane();
        track.getStyleClass().add("progress-track");
        track.setPrefHeight(10);
        Region fill = new Region();
        fill.getStyleClass().add(progressFillClass(p));
        fill.setPrefHeight(10);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);
        track.widthProperty().addListener((obs, oldW, newW) ->
                fill.setPrefWidth(newW.doubleValue() * (p.getProgressPercent() / 100.0)));
        progressBox.getChildren().addAll(progressLabelRow, track);

        double remainingBudget = p.getBudget() - p.getLabourCost();
        HBox statsGrid = new HBox(20,
                detailStat("Workers Assigned", p.getWorkersAssigned() + ""),
                detailStat("Budget", formatMoney(p.getBudget())),
                detailStat("Labour Cost", formatMoney(p.getLabourCost())),
                detailStat("Remaining Budget", formatMoney(remainingBudget))
        );

        VBox timelineBox = new VBox(8);
        Label timelineHeader = new Label("TIMELINE");
        timelineHeader.getStyleClass().add("section-header");
        HBox timelineDatesRow = new HBox(20,
                detailStat("Start Date", p.getStartDate().format(DATE_FMT)),
                detailStat("End Date", p.getEndDate().format(DATE_FMT)),
                detailStat("Status", timelineNote(p))
        );
        timelineBox.getChildren().addAll(timelineHeader, timelineDatesRow);

        Button close = new Button("Close");
        close.getStyleClass().add("form-cancel-btn");
        HBox buttons = new HBox(close);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(18, headerRow, location, new Separator(), progressBox,
                new Separator(), statsGrid, new Separator(), timelineBox, buttons);
        card.getStyleClass().add("details-card");
        card.setMaxWidth(520);
        card.setMinWidth(520);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("form-root");

        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(rootPane.getScene().getWindow());
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        close.setOnAction(e -> dialog.close());
        dialog.showAndWait();
    }

    private VBox detailStat(String label, String value) {
        VBox box = new VBox(4);
        Label l = new Label(label);
        l.getStyleClass().add("meta-label");
        Label v = new Label(value);
        v.getStyleClass().add("detail-stat-value");
        box.getChildren().addAll(l, v);
        return box;
    }

    // ---------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/buildpay/view/Dashboard.fxml"));
            Parent dashboardRoot = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(dashboardRoot, stage.getScene().getWidth(), stage.getScene().getHeight());
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load Dashboard.fxml", ex);
        }
    }
}
