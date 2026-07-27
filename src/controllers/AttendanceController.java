package com.buildpay.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controls the Attendance module: a month calendar for picking the working
 * day, a status breakdown chart, statistics chips, a searchable/filterable
 * table, and the Mark Attendance / Bulk Attendance dialogs.
 */
public class AttendanceController {

    private static final int PAGE_SIZE = 8;
    private static final DateTimeFormatter MONTH_YEAR_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter HEADER_DATE_FMT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");

    // ---------------------------------------------------------------
    // Domain model
    // ---------------------------------------------------------------

    public enum AttendanceStatus {
        PRESENT("Present", "chip-present", "#12B886"),
        LATE("Late", "chip-late", "#F5A524"),
        OVERTIME("Overtime", "chip-overtime", "#2E6BFF"),
        LEAVE("Leave", "chip-leave", "#7C5CFC"),
        ABSENT("Absent", "chip-absent", "#E5484D");

        private final String label;
        private final String styleClass;
        private final String colorHex;

        AttendanceStatus(String label, String styleClass, String colorHex) {
            this.label = label;
            this.styleClass = styleClass;
            this.colorHex = colorHex;
        }

        public String getStyleClass() { return styleClass; }
        public String getColorHex() { return colorHex; }

        @Override
        public String toString() { return label; }
    }

    private record EmployeeRef(String id, String name, String department) {
        @Override
        public String toString() { return name; }
    }

    public static class AttendanceRecord {
        private final String employeeId;
        private final String employeeName;
        private final String department;
        private final LocalDate date;
        private AttendanceStatus status;
        private String checkIn;
        private String checkOut;
        private double hoursWorked;

        public AttendanceRecord(String employeeId, String employeeName, String department, LocalDate date,
                                 AttendanceStatus status, String checkIn, String checkOut, double hoursWorked) {
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.department = department;
            this.date = date;
            this.status = status;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.hoursWorked = hoursWorked;
        }

        public String getEmployeeId() { return employeeId; }
        public String getEmployeeName() { return employeeName; }
        public String getDepartment() { return department; }
        public LocalDate getDate() { return date; }
        public AttendanceStatus getStatus() { return status; }
        public String getCheckIn() { return checkIn; }
        public String getCheckOut() { return checkOut; }
        public double getHoursWorked() { return hoursWorked; }

        public void update(AttendanceStatus status, String checkIn, String checkOut, double hoursWorked) {
            this.status = status;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.hoursWorked = hoursWorked;
        }

        public String getInitials() {
            String[] parts = employeeName.trim().split("\\s+");
            String initials = String.valueOf(parts[0].charAt(0));
            if (parts.length > 1) initials += parts[parts.length - 1].charAt(0);
            return initials.toUpperCase();
        }
    }

    // ---------------------------------------------------------------
    // FXML fields
    // ---------------------------------------------------------------

    @FXML private VBox rootPane;
    @FXML private Button backButton;
    @FXML private Label pageSubtitleLabel;
    @FXML private Button bulkAttendanceButton;
    @FXML private Button markAttendanceButton;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> statusFilter;

    @FXML private Label statPresentValue;
    @FXML private Label statAbsentValue;
    @FXML private Label statLeaveValue;
    @FXML private Label statLateValue;
    @FXML private Label statOvertimeValue;

    @FXML private VBox calendarCard;
    @FXML private Button prevMonthButton;
    @FXML private Button nextMonthButton;
    @FXML private Label monthYearLabel;
    @FXML private GridPane weekdayHeaderGrid;
    @FXML private GridPane calendarGrid;
    @FXML private Button todayButton;

    @FXML private VBox chartCard;
    @FXML private Label chartDateLabel;
    @FXML private Canvas attendanceBarCanvas;
    @FXML private VBox chartLegend;

    @FXML private Label tableHeaderLabel;
    @FXML private Label recordCountLabel;
    @FXML private TableView<AttendanceRecord> attendanceTable;
    @FXML private TableColumn<AttendanceRecord, String> idColumn;
    @FXML private TableColumn<AttendanceRecord, AttendanceRecord> photoColumn;
    @FXML private TableColumn<AttendanceRecord, String> nameColumn;
    @FXML private TableColumn<AttendanceRecord, String> departmentColumn;
    @FXML private TableColumn<AttendanceRecord, String> checkInColumn;
    @FXML private TableColumn<AttendanceRecord, String> checkOutColumn;
    @FXML private TableColumn<AttendanceRecord, String> hoursColumn;
    @FXML private TableColumn<AttendanceRecord, AttendanceRecord> statusColumn;
    @FXML private TableColumn<AttendanceRecord, AttendanceRecord> actionsColumn;

    @FXML private Label paginationSummaryLabel;
    @FXML private Button prevPageButton;
    @FXML private HBox pageNumbersBox;
    @FXML private Button nextPageButton;

    // ---------------------------------------------------------------
    // State
    // ---------------------------------------------------------------

    private final List<EmployeeRef> roster = new ArrayList<>();
    private final ObservableList<AttendanceRecord> allRecords = FXCollections.observableArrayList();
    private FilteredList<AttendanceRecord> filteredList;

    private LocalDate selectedDate = LocalDate.now();
    private YearMonth currentMonth = YearMonth.from(selectedDate);
    private int currentPage = 0;

    @FXML
    public void initialize() {
        buildRoster();
        generateAttendanceData();
        filteredList = new FilteredList<>(allRecords, r -> true);

        setupFilters();
        setupTableColumns();
        setupCalendarColumns();
        buildCalendar();
        refreshForSelectedDate();
    }

    // ---------------------------------------------------------------
    // Sample data
    // ---------------------------------------------------------------

    private void buildRoster() {
        Object[][] rows = {
                {"James Otieno", "Site Supervision"}, {"Grace Wanjiru", "Administration"},
                {"Peter Kamau", "Electrical"}, {"Mary Achieng", "Finance"},
                {"Samuel Njoroge", "Carpentry"}, {"Faith Mutua", "Plumbing"},
                {"David Kiprotich", "Site Supervision"}, {"Lucy Nyambura", "Administration"},
                {"John Mwangi", "Masonry"}, {"Esther Chebet", "Logistics"},
                {"Brian Ochieng", "Electrical"}, {"Ann Wambui", "Finance"},
                {"Kevin Mutiso", "Carpentry"}, {"Diana Auma", "Plumbing"},
                {"Patrick Kariuki", "Site Supervision"},
        };
        int seq = 1;
        for (Object[] r : rows) {
            roster.add(new EmployeeRef(String.format("EMP-%04d", 1000 + seq), (String) r[0], (String) r[1]));
            seq++;
        }
    }

    private void generateAttendanceData() {
        Random random = new Random(42);
        LocalDate start = LocalDate.now().minusDays(29);
        for (EmployeeRef emp : roster) {
            for (LocalDate d = start; !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
                AttendanceStatus status = pickWeightedStatus(random);
                double[] times = defaultTimesFor(status, random);
                allRecords.add(new AttendanceRecord(
                        emp.id(), emp.name(), emp.department(), d, status,
                        formatTime(times[0]), formatTime(times[1]), times[2]
                ));
            }
        }
    }

    private AttendanceStatus pickWeightedStatus(Random random) {
        int roll = random.nextInt(100);
        if (roll < 72) return AttendanceStatus.PRESENT;
        if (roll < 83) return AttendanceStatus.LATE;
        if (roll < 90) return AttendanceStatus.ABSENT;
        if (roll < 96) return AttendanceStatus.LEAVE;
        return AttendanceStatus.OVERTIME;
    }

    /** Returns {checkInHour, checkOutHour, hoursWorked} as doubles (hour-of-day), -1 meaning "no time". */
    private double[] defaultTimesFor(AttendanceStatus status, Random random) {
        return switch (status) {
            case PRESENT -> new double[]{8.0 + random.nextDouble() * 0.25, 17.0, 8.5};
            case LATE -> new double[]{9.0 + random.nextDouble() * 0.75, 17.0, 7.5};
            case OVERTIME -> new double[]{8.0, 19.5 + random.nextDouble() * 1.0, 11.5};
            case ABSENT, LEAVE -> new double[]{-1, -1, 0};
        };
    }

    private String formatTime(double hourOfDay) {
        if (hourOfDay < 0) return "--";
        int h = (int) hourOfDay;
        int m = (int) Math.round((hourOfDay - h) * 60);
        return String.format("%02d:%02d", h, m);
    }

    // ---------------------------------------------------------------
    // Filters / search
    // ---------------------------------------------------------------

    private void setupFilters() {
        List<String> depts = new ArrayList<>();
        depts.add("All Departments");
        roster.stream().map(EmployeeRef::department).distinct().sorted().forEach(depts::add);
        departmentFilter.getItems().setAll(depts);
        departmentFilter.setValue("All Departments");

        statusFilter.getItems().setAll("All Statuses", "Present", "Late", "Overtime", "Leave", "Absent");
        statusFilter.setValue("All Statuses");

        searchField.textProperty().addListener((o, ov, nv) -> applyFilters());
        departmentFilter.setOnAction(e -> applyFilters());
        statusFilter.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String dept = departmentFilter.getValue();
        String status = statusFilter.getValue();

        filteredList.setPredicate(r -> {
            boolean sameDate = r.getDate().equals(selectedDate);
            boolean matchesQuery = query.isEmpty()
                    || r.getEmployeeName().toLowerCase().contains(query)
                    || r.getEmployeeId().toLowerCase().contains(query);
            boolean matchesDept = dept == null || dept.equals("All Departments") || r.getDepartment().equals(dept);
            boolean matchesStatus = status == null || status.equals("All Statuses")
                    || r.getStatus().toString().equals(status);
            return sameDate && matchesQuery && matchesDept && matchesStatus;
        });

        currentPage = 0;
        refreshCurrentPageView();
    }

    private List<AttendanceRecord> recordsForDate(LocalDate date) {
        return allRecords.stream().filter(r -> r.getDate().equals(date)).toList();
    }

    // ---------------------------------------------------------------
    // Table setup
    // ---------------------------------------------------------------

    private void setupTableColumns() {
        idColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getEmployeeId()));
        nameColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getEmployeeName()));
        departmentColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDepartment()));
        checkInColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getCheckIn()));
        checkOutColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getCheckOut()));
        hoursColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getHoursWorked() > 0 ? String.format("%.1f h", d.getValue().getHoursWorked()) : "--"));

        photoColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(AttendanceRecord ignored, boolean empty) {
                super.updateItem(ignored, empty);
                AttendanceRecord r = (getTableRow() != null) ? getTableRow().getItem() : null;
                setGraphic((empty || r == null) ? null : buildAvatar(r));
            }
        });

        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(AttendanceRecord ignored, boolean empty) {
                super.updateItem(ignored, empty);
                AttendanceRecord r = (getTableRow() != null) ? getTableRow().getItem() : null;
                if (empty || r == null) { setGraphic(null); return; }
                Label chip = new Label(r.getStatus().toString());
                chip.getStyleClass().addAll("status-chip", r.getStatus().getStyleClass());
                setGraphic(chip);
            }
        });

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(AttendanceRecord ignored, boolean empty) {
                super.updateItem(ignored, empty);
                AttendanceRecord r = (getTableRow() != null) ? getTableRow().getItem() : null;
                if (empty || r == null) { setGraphic(null); return; }
                Button edit = new Button("Edit");
                edit.getStyleClass().add("row-action-btn");
                edit.setOnAction(e -> showMarkAttendanceDialog(r));
                setGraphic(edit);
            }
        });
    }

    private StackPane buildAvatar(AttendanceRecord r) {
        StackPane pane = new StackPane();
        Circle circle = new Circle(15);
        circle.getStyleClass().add("avatar-circle-" + ((Math.abs(r.getEmployeeId().hashCode()) % 4) + 1));
        javafx.scene.text.Text text = new javafx.scene.text.Text(r.getInitials());
        text.getStyleClass().add("avatar-initials");
        pane.getChildren().addAll(circle, text);
        return pane;
    }

    // ---------------------------------------------------------------
    // Calendar
    // ---------------------------------------------------------------

    private void setupCalendarColumns() {
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            calendarGrid.getColumnConstraints().add(cc);
        }
    }

    private void buildCalendar() {
        monthYearLabel.setText(currentMonth.format(MONTH_YEAR_FMT));
        calendarGrid.getChildren().clear();

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int firstCol = firstOfMonth.getDayOfWeek().getValue() - 1; // Monday = 0
        int daysInMonth = currentMonth.lengthOfMonth();

        int row = 0;
        int col = firstCol;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            calendarGrid.add(buildDayCell(date), col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }
    }

    private StackPane buildDayCell(LocalDate date) {
        VBox content = new VBox(2);
        content.setAlignment(Pos.CENTER);
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.getStyleClass().add("calendar-day-number");
        content.getChildren().add(dayLabel);

        List<AttendanceRecord> dayRecords = recordsForDate(date);
        boolean hasIssue = dayRecords.stream().anyMatch(r ->
                r.getStatus() == AttendanceStatus.ABSENT || r.getStatus() == AttendanceStatus.LATE);
        if (!dayRecords.isEmpty()) {
            Circle dot = new Circle(2.5);
            dot.getStyleClass().add(hasIssue ? "calendar-dot-issue" : "calendar-dot-ok");
            content.getChildren().add(dot);
        }

        StackPane cell = new StackPane(content);
        cell.getStyleClass().add("calendar-day-cell");
        if (date.equals(LocalDate.now())) cell.getStyleClass().add("calendar-day-today");
        if (date.equals(selectedDate)) cell.getStyleClass().add("calendar-day-selected");
        cell.setOnMouseClicked(e -> {
            selectedDate = date;
            buildCalendar();
            refreshForSelectedDate();
        });
        return cell;
    }

    @FXML
    private void handlePrevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        buildCalendar();
    }

    @FXML
    private void handleNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        buildCalendar();
    }

    @FXML
    private void handleJumpToToday() {
        selectedDate = LocalDate.now();
        currentMonth = YearMonth.from(selectedDate);
        buildCalendar();
        refreshForSelectedDate();
    }

    // ---------------------------------------------------------------
    // Refresh pipeline
    // ---------------------------------------------------------------

    private void refreshForSelectedDate() {
        tableHeaderLabel.setText("Attendance for " + selectedDate.format(HEADER_DATE_FMT));
        chartDateLabel.setText(selectedDate.format(HEADER_DATE_FMT));
        applyFilters();
        updateStatistics();
        drawChart();
    }

    private void updateStatistics() {
        List<AttendanceRecord> dayRecords = recordsForDate(selectedDate);
        statPresentValue.setText(String.valueOf(countStatus(dayRecords, AttendanceStatus.PRESENT)));
        statAbsentValue.setText(String.valueOf(countStatus(dayRecords, AttendanceStatus.ABSENT)));
        statLeaveValue.setText(String.valueOf(countStatus(dayRecords, AttendanceStatus.LEAVE)));
        statLateValue.setText(String.valueOf(countStatus(dayRecords, AttendanceStatus.LATE)));
        statOvertimeValue.setText(String.valueOf(countStatus(dayRecords, AttendanceStatus.OVERTIME)));
    }

    private long countStatus(List<AttendanceRecord> records, AttendanceStatus status) {
        return records.stream().filter(r -> r.getStatus() == status).count();
    }

    // ---------------------------------------------------------------
    // Chart
    // ---------------------------------------------------------------

    private void drawChart() {
        GraphicsContext gc = attendanceBarCanvas.getGraphicsContext2D();
        double w = attendanceBarCanvas.getWidth();
        double h = attendanceBarCanvas.getHeight();
        gc.clearRect(0, 0, w, h);

        List<AttendanceRecord> dayRecords = recordsForDate(selectedDate);
        AttendanceStatus[] statuses = AttendanceStatus.values();
        int[] counts = new int[statuses.length];
        int max = 1;
        for (int i = 0; i < statuses.length; i++) {
            counts[i] = (int) countStatus(dayRecords, statuses[i]);
            max = Math.max(max, counts[i]);
        }

        double paddingBottom = 24;
        double paddingTop = 14;
        double chartH = h - paddingBottom - paddingTop;
        double barAreaW = w / statuses.length;
        double barW = barAreaW * 0.45;

        gc.setFont(Font.font("Inter", 10));
        chartLegend.getChildren().clear();

        for (int i = 0; i < statuses.length; i++) {
            AttendanceStatus s = statuses[i];
            double barH = chartH * (counts[i] / (double) max);
            double x = barAreaW * i + (barAreaW - barW) / 2;
            double y = paddingTop + chartH - barH;

            gc.setFill(Color.web(s.getColorHex()));
            gc.fillRoundRect(x, y, barW, barH, 6, 6);

            gc.setFill(Color.web("#33415C"));
            gc.fillText(String.valueOf(counts[i]), x + barW / 2 - 4, y - 6);

            gc.setFill(Color.web("#9AA6BF"));
            gc.fillText(s.toString(), barAreaW * i + 4, h - 6);

            HBox legendRow = new HBox(8);
            legendRow.setAlignment(Pos.CENTER_LEFT);
            Circle dot = new Circle(4);
            dot.setFill(Color.web(s.getColorHex()));
            Label label = new Label(s.toString());
            label.getStyleClass().add("legend-text");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label value = new Label(String.valueOf(counts[i]));
            value.getStyleClass().add("legend-value");
            legendRow.getChildren().addAll(dot, label, spacer, value);
            chartLegend.getChildren().add(legendRow);
        }
    }

    // ---------------------------------------------------------------
    // Pagination
    // ---------------------------------------------------------------

    private int totalPages() {
        return Math.max(1, (int) Math.ceil(filteredList.size() / (double) PAGE_SIZE));
    }

    private void refreshCurrentPageView() {
        int total = filteredList.size();
        int totalPages = totalPages();
        currentPage = Math.min(currentPage, totalPages - 1);
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);

        List<AttendanceRecord> pageItems = total == 0 ? List.of() : filteredList.subList(from, to);
        attendanceTable.setItems(FXCollections.observableArrayList(pageItems));

        recordCountLabel.setText(total + (total == 1 ? " record" : " records"));
        paginationSummaryLabel.setText(total == 0
                ? "No records found"
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
    // Mark Attendance dialog (single employee, add or edit)
    // ---------------------------------------------------------------

    @FXML
    private void handleMarkAttendance() {
        showMarkAttendanceDialog(null);
    }

    private void showMarkAttendanceDialog(AttendanceRecord existing) {
        boolean editMode = existing != null;

        Label title = new Label(editMode ? "Edit Attendance" : "Mark Attendance");
        title.getStyleClass().add("form-title");

        Label dateLabel = new Label(selectedDate.format(HEADER_DATE_FMT));
        dateLabel.getStyleClass().add("form-date-pill");

        Label employeeFieldLabel = new Label("Employee");
        employeeFieldLabel.getStyleClass().add("form-field-label");

        ComboBox<EmployeeRef> employeeCombo = new ComboBox<>();
        employeeCombo.getItems().setAll(roster);
        employeeCombo.setMaxWidth(Double.MAX_VALUE);
        employeeCombo.getStyleClass().add("form-combo");
        if (editMode) {
            roster.stream().filter(e -> e.id().equals(existing.getEmployeeId())).findFirst()
                    .ifPresent(employeeCombo::setValue);
            employeeCombo.setDisable(true);
        }

        Label statusFieldLabel = new Label("Status");
        statusFieldLabel.getStyleClass().add("form-field-label");

        AtomicReference<AttendanceStatus> selectedStatus = new AtomicReference<>(
                editMode ? existing.getStatus() : AttendanceStatus.PRESENT);

        Label checkInLabel = new Label("Check In");
        checkInLabel.getStyleClass().add("form-field-label");
        TextField checkInField = new TextField(editMode ? existing.getCheckIn() : "08:00");
        checkInField.getStyleClass().add("form-field");

        Label checkOutLabel = new Label("Check Out");
        checkOutLabel.getStyleClass().add("form-field-label");
        TextField checkOutField = new TextField(editMode ? existing.getCheckOut() : "17:00");
        checkOutField.getStyleClass().add("form-field");

        HBox chipRow = buildStatusChipRow(selectedStatus.get(), status -> selectedStatus.set(status));

        HBox timesRow = new HBox(14,
                labeledColumn(checkInLabel, checkInField),
                labeledColumn(checkOutLabel, checkOutField));

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("form-cancel-btn");
        Button save = new Button(editMode ? "Update" : "Save Attendance");
        save.getStyleClass().add("form-save-btn");

        HBox buttons = new HBox(12, cancel, save);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(16, title, dateLabel, labeledColumn(employeeFieldLabel, employeeCombo),
                labeledColumn(statusFieldLabel, chipRow), timesRow, buttons);
        card.getStyleClass().add("form-card");
        card.setMaxWidth(420);
        card.setMinWidth(420);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("form-root");

        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(rootPane.getScene().getWindow());
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        cancel.setOnAction(e -> dialog.close());
        save.setOnAction(e -> {
            EmployeeRef target = editMode
                    ? roster.stream().filter(r -> r.id().equals(existing.getEmployeeId())).findFirst().orElse(null)
                    : employeeCombo.getValue();
            if (target == null) return;

            AttendanceStatus status = selectedStatus.get();
            String checkIn = checkInField.getText().trim();
            String checkOut = checkOutField.getText().trim();
            double hours = estimateHours(checkIn, checkOut, status);

            if (editMode) {
                existing.update(status, checkIn, checkOut, hours);
            } else {
                allRecords.removeIf(r -> r.getEmployeeId().equals(target.id()) && r.getDate().equals(selectedDate));
                allRecords.add(new AttendanceRecord(target.id(), target.name(), target.department(),
                        selectedDate, status, checkIn, checkOut, hours));
            }
            refreshForSelectedDate();
            buildCalendar();
            dialog.close();
        });

        dialog.showAndWait();
    }

    private double estimateHours(String checkIn, String checkOut, AttendanceStatus status) {
        if (status == AttendanceStatus.ABSENT || status == AttendanceStatus.LEAVE) return 0;
        try {
            String[] inParts = checkIn.split(":");
            String[] outParts = checkOut.split(":");
            double in = Integer.parseInt(inParts[0]) + Integer.parseInt(inParts[1]) / 60.0;
            double out = Integer.parseInt(outParts[0]) + Integer.parseInt(outParts[1]) / 60.0;
            return Math.max(0, Math.round((out - in) * 10) / 10.0);
        } catch (Exception ex) {
            return status == AttendanceStatus.OVERTIME ? 11 : 8;
        }
    }

    private VBox labeledColumn(Label label, javafx.scene.Node field) {
        VBox box = new VBox(6, label, field);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    /** Builds a row of colored, single-select status chip buttons. */
    private HBox buildStatusChipRow(AttendanceStatus initial, java.util.function.Consumer<AttendanceStatus> onChange) {
        HBox row = new HBox(8);
        List<Button> buttons = new ArrayList<>();
        for (AttendanceStatus status : AttendanceStatus.values()) {
            Button chip = new Button(status.toString());
            chip.getStyleClass().addAll("status-chip-btn", status.getStyleClass());
            if (status == initial) chip.getStyleClass().add("status-chip-btn-selected");
            chip.setOnAction(e -> {
                for (Button b : buttons) b.getStyleClass().remove("status-chip-btn-selected");
                chip.getStyleClass().add("status-chip-btn-selected");
                onChange.accept(status);
            });
            buttons.add(chip);
            row.getChildren().add(chip);
        }
        return row;
    }

    // ---------------------------------------------------------------
    // Bulk Attendance dialog
    // ---------------------------------------------------------------

    @FXML
    private void handleBulkAttendance() {
        Label title = new Label("Bulk Attendance");
        title.getStyleClass().add("form-title");

        Label dateLabel = new Label(selectedDate.format(HEADER_DATE_FMT));
        dateLabel.getStyleClass().add("form-date-pill");

        Label statusFieldLabel = new Label("Apply Status");
        statusFieldLabel.getStyleClass().add("form-field-label");

        AtomicReference<AttendanceStatus> selectedStatus = new AtomicReference<>(AttendanceStatus.PRESENT);
        HBox chipRow = buildStatusChipRow(AttendanceStatus.PRESENT, selectedStatus::set);

        Label employeesLabel = new Label("Employees");
        employeesLabel.getStyleClass().add("form-field-label");

        CheckBox selectAll = new CheckBox("Select All");
        selectAll.getStyleClass().add("bulk-checkbox");

        VBox employeeListBox = new VBox(4);
        List<CheckBox> employeeChecks = new ArrayList<>();
        for (EmployeeRef emp : roster) {
            CheckBox cb = new CheckBox(emp.name() + "  \u00B7  " + emp.department());
            cb.getStyleClass().add("bulk-checkbox");
            employeeChecks.add(cb);
            employeeListBox.getChildren().add(cb);
        }
        selectAll.setOnAction(e -> employeeChecks.forEach(cb -> cb.setSelected(selectAll.isSelected())));

        ScrollPane scroll = new ScrollPane(employeeListBox);
        scroll.getStyleClass().add("bulk-scroll");
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(220);

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("form-cancel-btn");
        Button apply = new Button("Apply to Selected");
        apply.getStyleClass().add("form-save-btn");

        HBox buttons = new HBox(12, cancel, apply);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(16, title, dateLabel, labeledColumn(statusFieldLabel, chipRow),
                employeesLabel, selectAll, scroll, buttons);
        card.getStyleClass().add("form-card");
        card.setMaxWidth(440);
        card.setMinWidth(440);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("form-root");

        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(rootPane.getScene().getWindow());
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        cancel.setOnAction(e -> dialog.close());
        apply.setOnAction(e -> {
            AttendanceStatus status = selectedStatus.get();
            for (int i = 0; i < roster.size(); i++) {
                if (!employeeChecks.get(i).isSelected()) continue;
                EmployeeRef emp = roster.get(i);
                double[] times = defaultTimesFor(status, new Random());
                allRecords.removeIf(r -> r.getEmployeeId().equals(emp.id()) && r.getDate().equals(selectedDate));
                allRecords.add(new AttendanceRecord(emp.id(), emp.name(), emp.department(), selectedDate,
                        status, formatTime(times[0]), formatTime(times[1]), times[2]));
            }
            refreshForSelectedDate();
            buildCalendar();
            dialog.close();
        });

        dialog.showAndWait();
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
