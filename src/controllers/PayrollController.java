package com.buildpay.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controls the Payroll Processing module: a live salary calculator using
 * current KRA PAYE bands and NSSF Tier I/II rates, payroll generation and
 * approval, and a searchable/filterable payroll table.
 *
 * Statutory rates (verify against KRA/NSSF before relying on this in
 * production, since rates are set by the Finance Act and NSSF regulations
 * and can change):
 *   NSSF (Year 4, effective Feb 2026): 6% of pensionable pay, Tier I up to
 *     KES 9,000 and Tier II from KES 9,001 up to the KES 108,000 UEL
 *     (max employee contribution KES 6,480/month).
 *   PAYE: 10% / 25% / 30% / 32.5% / 35% progressive bands on income after
 *     NSSF, less the KES 2,400/month personal relief.
 */
public class PayrollController {

    private static final int PAGE_SIZE = 8;
    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private static final double NSSF_TIER1_LIMIT = 9_000;
    private static final double NSSF_UEL = 108_000;
    private static final double NSSF_RATE = 0.06;
    private static final double PERSONAL_RELIEF = 2_400;

    // ---------------------------------------------------------------
    // Domain model
    // ---------------------------------------------------------------

    public enum PayrollStatus {
        PENDING("Pending Approval", "status-pending"),
        APPROVED("Approved", "status-approved");

        private final String label;
        private final String styleClass;

        PayrollStatus(String label, String styleClass) {
            this.label = label;
            this.styleClass = styleClass;
        }

        public String getStyleClass() { return styleClass; }

        @Override
        public String toString() { return label; }
    }

    private record EmployeeRef(String id, String name, String department, double basicSalary) {
        @Override
        public String toString() { return name; }
    }

    public static class PayrollRecord {
        private final String employeeId;
        private final String employeeName;
        private final String department;
        private final String period;
        private final double basicSalary;
        private final double allowances;
        private final double housing;
        private final double transport;
        private final double overtime;
        private final double bonuses;
        private final double otherDeductions;
        private final double nssf;
        private final double paye;
        private final double grossPay;
        private final double netPay;
        private PayrollStatus status;

        public PayrollRecord(String employeeId, String employeeName, String department, String period,
                              double basicSalary, double allowances, double housing, double transport,
                              double overtime, double bonuses, double otherDeductions,
                              double nssf, double paye, double grossPay, double netPay, PayrollStatus status) {
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.department = department;
            this.period = period;
            this.basicSalary = basicSalary;
            this.allowances = allowances;
            this.housing = housing;
            this.transport = transport;
            this.overtime = overtime;
            this.bonuses = bonuses;
            this.otherDeductions = otherDeductions;
            this.nssf = nssf;
            this.paye = paye;
            this.grossPay = grossPay;
            this.netPay = netPay;
            this.status = status;
        }

        public String getEmployeeId() { return employeeId; }
        public String getEmployeeName() { return employeeName; }
        public String getDepartment() { return department; }
        public String getPeriod() { return period; }
        public double getBasicSalary() { return basicSalary; }
        public double getAllowances() { return allowances; }
        public double getHousing() { return housing; }
        public double getTransport() { return transport; }
        public double getOvertime() { return overtime; }
        public double getBonuses() { return bonuses; }
        public double getOtherDeductions() { return otherDeductions; }
        public double getNssf() { return nssf; }
        public double getPaye() { return paye; }
        public double getGrossPay() { return grossPay; }
        public double getNetPay() { return netPay; }
        public PayrollStatus getStatus() { return status; }
        public void setStatus(PayrollStatus status) { this.status = status; }
    }

    // ---------------------------------------------------------------
    // FXML fields
    // ---------------------------------------------------------------

    @FXML private VBox rootPane;
    @FXML private Button backButton;
    @FXML private ComboBox<String> periodSelector;
    @FXML private Button exportButton;
    @FXML private Button printButton;
    @FXML private Button approveButton;

    @FXML private VBox calculatorCard;
    @FXML private ComboBox<EmployeeRef> employeeSelector;
    @FXML private Label basicSalaryValueLabel;
    @FXML private TextField housingField;
    @FXML private TextField transportField;
    @FXML private TextField overtimeField;
    @FXML private TextField bonusesField;
    @FXML private TextField allowancesField;
    @FXML private TextField deductionsField;
    @FXML private Label nssfValueLabel;
    @FXML private Label payeValueLabel;
    @FXML private Label grossPayLabel;
    @FXML private Label totalDeductionsLabel;
    @FXML private Label netPayLabel;
    @FXML private Button generatePayrollButton;

    @FXML private Label tableHeaderLabel;
    @FXML private Label recordCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<PayrollRecord> payrollTable;
    @FXML private TableColumn<PayrollRecord, String> idColumn;
    @FXML private TableColumn<PayrollRecord, String> nameColumn;
    @FXML private TableColumn<PayrollRecord, String> departmentColumn;
    @FXML private TableColumn<PayrollRecord, String> basicColumn;
    @FXML private TableColumn<PayrollRecord, String> grossColumn;
    @FXML private TableColumn<PayrollRecord, String> nssfColumn;
    @FXML private TableColumn<PayrollRecord, String> payeColumn;
    @FXML private TableColumn<PayrollRecord, String> netColumn;
    @FXML private TableColumn<PayrollRecord, PayrollRecord> statusColumn;
    @FXML private TableColumn<PayrollRecord, PayrollRecord> actionsColumn;

    @FXML private Label paginationSummaryLabel;
    @FXML private Button prevPageButton;
    @FXML private HBox pageNumbersBox;
    @FXML private Button nextPageButton;

    // ---------------------------------------------------------------
    // State
    // ---------------------------------------------------------------

    private final List<EmployeeRef> roster = new ArrayList<>();
    private final ObservableList<PayrollRecord> allRecords = FXCollections.observableArrayList();
    private FilteredList<PayrollRecord> filteredList;
    private int currentPage = 0;

    @FXML
    public void initialize() {
        buildRoster();
        filteredList = new FilteredList<>(allRecords, r -> true);

        setupPeriodSelector();
        setupEmployeeSelector();
        setupCalculatorListeners();
        setupTableColumns();
        setupTableFilters();

        recalculateLive();
        applyFilters();
    }

    // ---------------------------------------------------------------
    // Sample roster
    // ---------------------------------------------------------------

    private void buildRoster() {
        Object[][] rows = {
                {"James Otieno", "Site Supervision", 65000.0}, {"Grace Wanjiru", "Administration", 58000.0},
                {"Peter Kamau", "Electrical", 42000.0}, {"Mary Achieng", "Finance", 70000.0},
                {"Samuel Njoroge", "Carpentry", 38000.0}, {"Faith Mutua", "Plumbing", 40000.0},
                {"David Kiprotich", "Site Supervision", 60000.0}, {"Lucy Nyambura", "Administration", 32000.0},
                {"John Mwangi", "Masonry", 36000.0}, {"Esther Chebet", "Logistics", 45000.0},
                {"Brian Ochieng", "Electrical", 41000.0}, {"Ann Wambui", "Finance", 48000.0},
                {"Kevin Mutiso", "Carpentry", 37000.0}, {"Diana Auma", "Plumbing", 30000.0},
                {"Patrick Kariuki", "Site Supervision", 52000.0},
        };
        int seq = 1;
        for (Object[] r : rows) {
            roster.add(new EmployeeRef(String.format("EMP-%04d", 1000 + seq), (String) r[0], (String) r[1], (double) r[2]));
            seq++;
        }
    }

    // ---------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------

    private void setupPeriodSelector() {
        YearMonth current = YearMonth.now();
        List<String> periods = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            periods.add(current.minusMonths(i).format(PERIOD_FMT));
        }
        periodSelector.getItems().setAll(periods);
        periodSelector.setValue(current.format(PERIOD_FMT));
        periodSelector.setOnAction(e -> {
            tableHeaderLabel.setText("Payroll \u2014 " + periodSelector.getValue());
            applyFilters();
        });
        tableHeaderLabel.setText("Payroll \u2014 " + periodSelector.getValue());
    }

    private void setupEmployeeSelector() {
        employeeSelector.getItems().setAll(roster);
        employeeSelector.setOnAction(e -> {
            EmployeeRef selected = employeeSelector.getValue();
            basicSalaryValueLabel.setText(selected == null ? "KES 0" : formatMoney(selected.basicSalary()));
            recalculateLive();
        });
    }

    private void setupCalculatorListeners() {
        for (TextField field : List.of(housingField, transportField, overtimeField, bonusesField,
                allowancesField, deductionsField)) {
            field.textProperty().addListener((o, ov, nv) -> recalculateLive());
        }
    }

    private void setupTableFilters() {
        statusFilter.getItems().setAll("All Statuses", "Pending Approval", "Approved");
        statusFilter.setValue("All Statuses");
        searchField.textProperty().addListener((o, ov, nv) -> applyFilters());
        statusFilter.setOnAction(e -> applyFilters());
    }

    // ---------------------------------------------------------------
    // Live calculation
    // ---------------------------------------------------------------

    private double parse(TextField field) {
        try {
            double v = Double.parseDouble(field.getText().trim());
            return Math.max(0, v);
        } catch (Exception ex) {
            return 0;
        }
    }

    /** Recomputes NSSF/PAYE/Gross/Net from the current calculator inputs and updates the display. */
    private void recalculateLive() {
        EmployeeRef emp = employeeSelector.getValue();
        double basic = emp == null ? 0 : emp.basicSalary();

        double housing = parse(housingField);
        double transport = parse(transportField);
        double overtime = parse(overtimeField);
        double bonuses = parse(bonusesField);
        double allowances = parse(allowancesField);
        double otherDeductions = parse(deductionsField);

        double gross = basic + housing + transport + overtime + bonuses + allowances;
        double nssf = calculateNssf(gross);
        double paye = calculatePaye(gross - nssf);
        double totalDeductions = nssf + paye + otherDeductions;
        double net = gross - totalDeductions;

        nssfValueLabel.setText(formatMoney(nssf));
        payeValueLabel.setText(formatMoney(paye));
        grossPayLabel.setText(formatMoney(gross));
        totalDeductionsLabel.setText(formatMoney(totalDeductions));
        netPayLabel.setText(formatMoney(Math.max(0, net)));
    }

    /** NSSF Tier I + Tier II, Year 4 rates (effective Feb 2026): 6% up to a KES 108,000 UEL. */
    private double calculateNssf(double gross) {
        double tier1 = Math.min(gross, NSSF_TIER1_LIMIT) * NSSF_RATE;
        double tier2 = Math.max(0, Math.min(gross, NSSF_UEL) - NSSF_TIER1_LIMIT) * NSSF_RATE;
        return Math.round(tier1 + tier2);
    }

    /** KRA progressive PAYE bands (10/25/30/32.5/35%) less the KES 2,400 monthly personal relief. */
    private double calculatePaye(double taxableIncome) {
        double remaining = Math.max(0, taxableIncome);
        double tax = 0;

        double b1 = Math.min(remaining, 24_000);
        tax += b1 * 0.10;
        remaining -= b1;

        double b2 = Math.min(remaining, 8_333);
        tax += b2 * 0.25;
        remaining -= b2;

        double b3 = Math.min(remaining, 467_667);
        tax += b3 * 0.30;
        remaining -= b3;

        double b4 = Math.min(remaining, 300_000);
        tax += b4 * 0.325;
        remaining -= b4;

        tax += remaining * 0.35;

        return Math.max(0, Math.round(tax - PERSONAL_RELIEF));
    }

    private String formatMoney(double value) {
        return String.format("KES %,.0f", value);
    }

    // ---------------------------------------------------------------
    // Generate payroll
    // ---------------------------------------------------------------

    @FXML
    private void handleGeneratePayroll() {
        EmployeeRef emp = employeeSelector.getValue();
        if (emp == null) {
            showInfo("Select an Employee", "Choose an employee before generating payroll.");
            return;
        }

        double housing = parse(housingField);
        double transport = parse(transportField);
        double overtime = parse(overtimeField);
        double bonuses = parse(bonusesField);
        double allowances = parse(allowancesField);
        double otherDeductions = parse(deductionsField);

        double gross = emp.basicSalary() + housing + transport + overtime + bonuses + allowances;
        double nssf = calculateNssf(gross);
        double paye = calculatePaye(gross - nssf);
        double net = Math.max(0, gross - nssf - paye - otherDeductions);
        String period = periodSelector.getValue();

        allRecords.removeIf(r -> r.getEmployeeId().equals(emp.id()) && r.getPeriod().equals(period));
        allRecords.add(new PayrollRecord(emp.id(), emp.name(), emp.department(), period,
                emp.basicSalary(), allowances, housing, transport, overtime, bonuses, otherDeductions,
                nssf, paye, gross, net, PayrollStatus.PENDING));

        resetCalculatorInputs();
        applyFilters();
        showInfo("Payroll Generated", "Payroll for " + emp.name() + " (" + period + ") is pending approval.");
    }

    private void resetCalculatorInputs() {
        employeeSelector.setValue(null);
        basicSalaryValueLabel.setText("KES 0");
        for (TextField field : List.of(housingField, transportField, overtimeField, bonusesField,
                allowancesField, deductionsField)) {
            field.setText("0");
        }
        recalculateLive();
    }

    // ---------------------------------------------------------------
    // Approve payroll
    // ---------------------------------------------------------------

    @FXML
    private void handleApprovePayroll() {
        PayrollRecord selected = payrollTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("No Row Selected", "Select a payroll record in the table first.");
            return;
        }
        approveRecord(selected);
    }

    private void approveRecord(PayrollRecord record) {
        if (record.getStatus() == PayrollStatus.APPROVED) {
            showInfo("Already Approved", record.getEmployeeName() + "'s payroll is already approved.");
            return;
        }
        record.setStatus(PayrollStatus.APPROVED);
        payrollTable.refresh();
        showInfo("Payroll Approved", record.getEmployeeName() + "'s payroll for " + record.getPeriod() + " has been approved.");
    }

    // ---------------------------------------------------------------
    // Table setup
    // ---------------------------------------------------------------

    private void setupTableColumns() {
        idColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getEmployeeId()));
        nameColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getEmployeeName()));
        departmentColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDepartment()));
        basicColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(formatMoney(d.getValue().getBasicSalary())));
        grossColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(formatMoney(d.getValue().getGrossPay())));
        nssfColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(formatMoney(d.getValue().getNssf())));
        payeColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(formatMoney(d.getValue().getPaye())));
        netColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(formatMoney(d.getValue().getNetPay())));

        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(PayrollRecord ignored, boolean empty) {
                super.updateItem(ignored, empty);
                PayrollRecord r = (getTableRow() != null) ? getTableRow().getItem() : null;
                if (empty || r == null) { setGraphic(null); return; }
                Label chip = new Label(r.getStatus().toString());
                chip.getStyleClass().addAll("status-chip", r.getStatus().getStyleClass());
                setGraphic(chip);
            }
        });

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(PayrollRecord ignored, boolean empty) {
                super.updateItem(ignored, empty);
                PayrollRecord r = (getTableRow() != null) ? getTableRow().getItem() : null;
                if (empty || r == null) { setGraphic(null); return; }

                Button view = new Button("View");
                view.getStyleClass().add("row-action-btn");
                view.setOnAction(e -> showPayslipDialog(r));

                Button approve = new Button("Approve");
                approve.getStyleClass().addAll("row-action-btn", "row-action-approve");
                approve.setDisable(r.getStatus() == PayrollStatus.APPROVED);
                approve.setOnAction(e -> approveRecord(r));

                HBox box = new HBox(6, view, approve);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
    }

    // ---------------------------------------------------------------
    // Filters / pagination
    // ---------------------------------------------------------------

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String status = statusFilter.getValue();
        String period = periodSelector.getValue();

        filteredList.setPredicate(r -> {
            boolean matchesPeriod = period == null || r.getPeriod().equals(period);
            boolean matchesQuery = query.isEmpty()
                    || r.getEmployeeName().toLowerCase().contains(query)
                    || r.getEmployeeId().toLowerCase().contains(query);
            boolean matchesStatus = status == null || status.equals("All Statuses")
                    || r.getStatus().toString().equals(status);
            return matchesPeriod && matchesQuery && matchesStatus;
        });

        currentPage = 0;
        refreshCurrentPageView();
    }

    private int totalPages() {
        return Math.max(1, (int) Math.ceil(filteredList.size() / (double) PAGE_SIZE));
    }

    private void refreshCurrentPageView() {
        int total = filteredList.size();
        int totalPages = totalPages();
        currentPage = Math.min(currentPage, totalPages - 1);
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);

        List<PayrollRecord> pageItems = total == 0 ? List.of() : filteredList.subList(from, to);
        payrollTable.setItems(FXCollections.observableArrayList(pageItems));

        recordCountLabel.setText(total + (total == 1 ? " record" : " records"));
        paginationSummaryLabel.setText(total == 0
                ? "No payroll records for this period"
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
    // Payslip view (read-only breakdown popup)
    // ---------------------------------------------------------------

    private void showPayslipDialog(PayrollRecord r) {
        Label title = new Label("Payslip \u2014 " + r.getEmployeeName());
        title.getStyleClass().add("form-title");

        Label sub = new Label(r.getPeriod() + "  \u00B7  " + r.getEmployeeId() + "  \u00B7  " + r.getDepartment());
        sub.getStyleClass().add("card-subtitle");

        VBox lines = new VBox(8,
                payslipLine("Basic Salary", r.getBasicSalary(), false),
                payslipLine("Housing Allowance", r.getHousing(), false),
                payslipLine("Transport Allowance", r.getTransport(), false),
                payslipLine("Overtime", r.getOvertime(), false),
                payslipLine("Bonuses", r.getBonuses(), false),
                payslipLine("Other Allowances", r.getAllowances(), false),
                new javafx.scene.control.Separator(),
                payslipLine("Gross Pay", r.getGrossPay(), false),
                new javafx.scene.control.Separator(),
                payslipLine("NSSF", r.getNssf(), true),
                payslipLine("PAYE", r.getPaye(), true),
                payslipLine("Other Deductions", r.getOtherDeductions(), true),
                new javafx.scene.control.Separator(),
                payslipLine("Net Pay", r.getNetPay(), false)
        );

        Button close = new Button("Close");
        close.getStyleClass().add("form-cancel-btn");
        HBox buttons = new HBox(close);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(14, title, sub, lines, buttons);
        card.getStyleClass().add("enterprise-card");
        card.setMaxWidth(400);
        card.setMinWidth(400);

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

    private HBox payslipLine(String label, double amount, boolean isDeduction) {
        Label l = new Label(label);
        l.getStyleClass().add("payslip-line-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label v = new Label((isDeduction ? "-" : "") + formatMoney(Math.abs(amount)));
        v.getStyleClass().add(isDeduction ? "payslip-line-value-negative" : "payslip-line-value");
        HBox row = new HBox(l, spacer, v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ---------------------------------------------------------------
    // Export / Print
    // ---------------------------------------------------------------

    @FXML
    private void handleExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Payroll");
        chooser.setInitialFileName("buildpay-payroll.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file == null) return;

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Employee ID,Name,Department,Period,Basic,Gross,NSSF,PAYE,Net Pay,Status\n");
            for (PayrollRecord r : filteredList) {
                writer.write(String.join(",",
                        r.getEmployeeId(), csvSafe(r.getEmployeeName()), csvSafe(r.getDepartment()),
                        csvSafe(r.getPeriod()), String.valueOf(r.getBasicSalary()), String.valueOf(r.getGrossPay()),
                        String.valueOf(r.getNssf()), String.valueOf(r.getPaye()), String.valueOf(r.getNetPay()),
                        csvSafe(r.getStatus().toString())
                ));
                writer.write("\n");
            }
            showInfo("Export Complete", "Payroll data exported to " + file.getName() + ".");
        } catch (IOException ex) {
            showInfo("Export Failed", "Could not write the export file. Please try again.");
        }
    }

    private String csvSafe(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @FXML
    private void handlePrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showInfo("Print Unavailable", "No printer could be found on this system.");
            return;
        }
        boolean proceed = job.showPrintDialog(rootPane.getScene().getWindow());
        if (proceed) {
            boolean success = job.printPage(payrollTable);
            if (success) job.endJob();
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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
