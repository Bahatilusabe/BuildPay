package com.buildpay.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controls the Payslip Viewer: renders a single employee's payslip for a
 * chosen pay period, and offers PDF export (via Apache PDFBox) and printing.
 *
 * NOTE: PDF export requires Apache PDFBox on the classpath. Add to your
 * build:
 *   Maven:  org.apache.pdfbox:pdfbox:2.0.31
 *   Gradle: implementation 'org.apache.pdfbox:pdfbox:2.0.31'
 *
 * NSSF/PAYE use the same statutory calculation as the Payroll module
 * (KRA progressive PAYE bands, NSSF Tier I/II Year 4 rates effective
 * Feb 2026) so figures stay consistent across BuildPay.
 */
public class PayslipController {

    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter PAY_DATE_FMT = DateTimeFormatter.ofPattern("d MMMM yyyy");

    private static final double NSSF_TIER1_LIMIT = 9_000;
    private static final double NSSF_UEL = 108_000;
    private static final double NSSF_RATE = 0.06;
    private static final double PERSONAL_RELIEF = 2_400;

    private record EmployeeRef(String id, String name, String department, String position, double basicSalary,
                                double housing, double transport, double overtime, double bonuses,
                                double otherAllowances, double otherDeductions) {
        @Override
        public String toString() { return name; }
    }

    @FXML private VBox rootPane;
    @FXML private Button backButton;
    @FXML private ComboBox<EmployeeRef> employeeSelector;
    @FXML private ComboBox<String> periodSelector;
    @FXML private Button downloadPdfButton;
    @FXML private Button printButton;

    @FXML private VBox payslipCard;
    @FXML private Label periodValueLabel;
    @FXML private Label employeeNameLabel;
    @FXML private Label employeeIdLabel;
    @FXML private Label departmentLabel;
    @FXML private Label positionLabel;
    @FXML private Label payPeriodLabel;
    @FXML private Label payDateLabel;

    @FXML private Label basicValueLabel;
    @FXML private Label housingValueLabel;
    @FXML private Label transportValueLabel;
    @FXML private Label overtimeValueLabel;
    @FXML private Label bonusesValueLabel;
    @FXML private Label otherAllowancesValueLabel;
    @FXML private Label grossPayValueLabel;

    @FXML private Label nssfValueLabel;
    @FXML private Label payeValueLabel;
    @FXML private Label otherDeductionsValueLabel;
    @FXML private Label totalDeductionsValueLabel;

    @FXML private Label netSalaryValueLabel;
    @FXML private Label employeeSignatureDateLabel;
    @FXML private Label authSignatureDateLabel;
    @FXML private Label generatedTimestampLabel;

    private final List<EmployeeRef> roster = new ArrayList<>();

    @FXML
    public void initialize() {
        buildRoster();
        setupEmployeeSelector();
        setupPeriodSelector();
        refreshPayslip();
    }

    // ---------------------------------------------------------------
    // Sample data
    // ---------------------------------------------------------------

    private void buildRoster() {
        // {name, department, position, basic, housing, transport, overtime, bonuses, otherAllowances, otherDeductions}
        Object[][] rows = {
                {"James Otieno", "Site Supervision", "Site Supervisor", 65000.0, 15000.0, 6000.0, 4200.0, 5000.0, 0.0, 1500.0},
                {"Grace Wanjiru", "Administration", "HR Officer", 58000.0, 12000.0, 5000.0, 0.0, 3000.0, 0.0, 0.0},
                {"Peter Kamau", "Electrical", "Electrician", 42000.0, 9000.0, 4000.0, 3100.0, 0.0, 0.0, 800.0},
                {"Mary Achieng", "Finance", "Accountant", 70000.0, 16000.0, 6000.0, 0.0, 8000.0, 2000.0, 0.0},
                {"Samuel Njoroge", "Carpentry", "Carpenter", 38000.0, 8000.0, 3500.0, 2500.0, 0.0, 0.0, 0.0},
        };
        for (Object[] r : rows) {
            String id = String.format("EMP-%04d", 1001 + roster.size());
            roster.add(new EmployeeRef(id, (String) r[0], (String) r[1], (String) r[2],
                    (double) r[3], (double) r[4], (double) r[5], (double) r[6], (double) r[7],
                    (double) r[8], (double) r[9]));
        }
    }

    private void setupEmployeeSelector() {
        employeeSelector.getItems().setAll(roster);
        employeeSelector.setValue(roster.get(0));
        employeeSelector.setOnAction(e -> refreshPayslip());
    }

    private void setupPeriodSelector() {
        YearMonth current = YearMonth.now();
        List<String> periods = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            periods.add(current.minusMonths(i).format(PERIOD_FMT));
        }
        periodSelector.getItems().setAll(periods);
        periodSelector.setValue(current.format(PERIOD_FMT));
        periodSelector.setOnAction(e -> refreshPayslip());
    }

    // ---------------------------------------------------------------
    // Statutory calculations (consistent with the Payroll module)
    // ---------------------------------------------------------------

    private double calculateNssf(double gross) {
        double tier1 = Math.min(gross, NSSF_TIER1_LIMIT) * NSSF_RATE;
        double tier2 = Math.max(0, Math.min(gross, NSSF_UEL) - NSSF_TIER1_LIMIT) * NSSF_RATE;
        return Math.round(tier1 + tier2);
    }

    private double calculatePaye(double taxableIncome) {
        double remaining = Math.max(0, taxableIncome);
        double tax = 0;

        double b1 = Math.min(remaining, 24_000); tax += b1 * 0.10; remaining -= b1;
        double b2 = Math.min(remaining, 8_333); tax += b2 * 0.25; remaining -= b2;
        double b3 = Math.min(remaining, 467_667); tax += b3 * 0.30; remaining -= b3;
        double b4 = Math.min(remaining, 300_000); tax += b4 * 0.325; remaining -= b4;
        tax += remaining * 0.35;

        return Math.max(0, Math.round(tax - PERSONAL_RELIEF));
    }

    private String formatMoney(double value) {
        return String.format("KES %,.0f", value);
    }

    // ---------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------

    private void refreshPayslip() {
        EmployeeRef emp = employeeSelector.getValue();
        String period = periodSelector.getValue();
        if (emp == null || period == null) return;

        double gross = emp.basicSalary() + emp.housing() + emp.transport() + emp.overtime()
                + emp.bonuses() + emp.otherAllowances();
        double nssf = calculateNssf(gross);
        double paye = calculatePaye(gross - nssf);
        double totalDeductions = nssf + paye + emp.otherDeductions();
        double net = Math.max(0, gross - totalDeductions);

        periodValueLabel.setText(period);
        employeeNameLabel.setText(emp.name());
        employeeIdLabel.setText(emp.id());
        departmentLabel.setText(emp.department());
        positionLabel.setText(emp.position());
        payPeriodLabel.setText(period);
        payDateLabel.setText(lastDayOf(period).format(PAY_DATE_FMT));

        basicValueLabel.setText(formatMoney(emp.basicSalary()));
        housingValueLabel.setText(formatMoney(emp.housing()));
        transportValueLabel.setText(formatMoney(emp.transport()));
        overtimeValueLabel.setText(formatMoney(emp.overtime()));
        bonusesValueLabel.setText(formatMoney(emp.bonuses()));
        otherAllowancesValueLabel.setText(formatMoney(emp.otherAllowances()));
        grossPayValueLabel.setText(formatMoney(gross));

        nssfValueLabel.setText("-" + formatMoney(nssf));
        payeValueLabel.setText("-" + formatMoney(paye));
        otherDeductionsValueLabel.setText("-" + formatMoney(emp.otherDeductions()));
        totalDeductionsValueLabel.setText("-" + formatMoney(totalDeductions));

        netSalaryValueLabel.setText(formatMoney(net));
        generatedTimestampLabel.setText("Generated on "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy")));
    }

    private LocalDate lastDayOf(String period) {
        YearMonth ym = YearMonth.parse(period, PERIOD_FMT);
        return ym.atEndOfMonth();
    }

    // ---------------------------------------------------------------
    // PDF export (Apache PDFBox)
    // ---------------------------------------------------------------

    @FXML
    private void handleDownloadPdf() {
        EmployeeRef emp = employeeSelector.getValue();
        String period = periodSelector.getValue();
        if (emp == null || period == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Payslip");
        chooser.setInitialFileName("Payslip_" + emp.id() + "_" + period.replace(" ", "_") + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file == null) return;

        try {
            writePayslipPdf(file, emp, period);
            showInfo("PDF Saved", "Payslip saved to " + file.getName() + ".");
        } catch (IOException ex) {
            showInfo("Export Failed", "Could not generate the PDF. Please try again.");
        }
    }

    private void writePayslipPdf(File file, EmployeeRef emp, String period) throws IOException {
        double gross = emp.basicSalary() + emp.housing() + emp.transport() + emp.overtime()
                + emp.bonuses() + emp.otherAllowances();
        double nssf = calculateNssf(gross);
        double paye = calculatePaye(gross - nssf);
        double totalDeductions = nssf + paye + emp.otherDeductions();
        double net = Math.max(0, gross - totalDeductions);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float margin = 50;
            float width = page.getMediaBox().getWidth() - 2 * margin;
            float y = page.getMediaBox().getHeight() - margin;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                y = pdfText(cs, "BUILDPAY", margin, y, PDType1Font.HELVETICA_BOLD, 20);
                y = pdfText(cs, "Construction Payroll System", margin, y - 4, PDType1Font.HELVETICA, 10);
                y = pdfText(cs, "PAYSLIP \u2014 " + period, margin, y - 14, PDType1Font.HELVETICA_BOLD, 12);
                y = pdfLine(cs, margin, y - 10, margin + width, y - 10) - 16;

                y = pdfRow(cs, margin, width, y, "Employee Name", emp.name());
                y = pdfRow(cs, margin, width, y, "Employee ID", emp.id());
                y = pdfRow(cs, margin, width, y, "Department", emp.department());
                y = pdfRow(cs, margin, width, y, "Position", emp.position());
                y = pdfRow(cs, margin, width, y, "Pay Date", lastDayOf(period).format(PAY_DATE_FMT));
                y = pdfLine(cs, margin, y - 6, margin + width, y - 6) - 16;

                y = pdfText(cs, "EARNINGS", margin, y, PDType1Font.HELVETICA_BOLD, 11);
                y -= 14;
                y = pdfRow(cs, margin, width, y, "Basic Salary", formatMoney(emp.basicSalary()));
                y = pdfRow(cs, margin, width, y, "Housing Allowance", formatMoney(emp.housing()));
                y = pdfRow(cs, margin, width, y, "Transport Allowance", formatMoney(emp.transport()));
                y = pdfRow(cs, margin, width, y, "Overtime", formatMoney(emp.overtime()));
                y = pdfRow(cs, margin, width, y, "Bonuses", formatMoney(emp.bonuses()));
                y = pdfRow(cs, margin, width, y, "Other Allowances", formatMoney(emp.otherAllowances()));
                y = pdfRowBold(cs, margin, width, y, "Gross Pay", formatMoney(gross));
                y = pdfLine(cs, margin, y - 6, margin + width, y - 6) - 16;

                y = pdfText(cs, "DEDUCTIONS", margin, y, PDType1Font.HELVETICA_BOLD, 11);
                y -= 14;
                y = pdfRow(cs, margin, width, y, "NSSF", "-" + formatMoney(nssf));
                y = pdfRow(cs, margin, width, y, "PAYE", "-" + formatMoney(paye));
                y = pdfRow(cs, margin, width, y, "Other Deductions", "-" + formatMoney(emp.otherDeductions()));
                y = pdfRowBold(cs, margin, width, y, "Total Deductions", "-" + formatMoney(totalDeductions));
                y = pdfLine(cs, margin, y - 6, margin + width, y - 6) - 20;

                y = pdfRowBold(cs, margin, width, y, "NET SALARY", formatMoney(net));
                y -= 40;

                float sigColWidth = width / 2 - 20;
                y = pdfLine(cs, margin, y, margin + sigColWidth, y);
                pdfLine(cs, margin + sigColWidth + 40, y, margin + width, y);
                y -= 14;
                pdfText(cs, "Employee Signature", margin, y, PDType1Font.HELVETICA, 9);
                pdfText(cs, "Authorized Signature", margin + sigColWidth + 40, y, PDType1Font.HELVETICA, 9);
                y -= 34;

                pdfText(cs, "This is a system-generated payslip and does not require a physical stamp.",
                        margin, y, PDType1Font.HELVETICA_OBLIQUE, 8);
                pdfText(cs, "Generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy")),
                        margin, y - 12, PDType1Font.HELVETICA, 8);
            }

            doc.save(file);
        }
    }

    private float pdfText(PDPageContentStream cs, String text, float x, float y,
                          PDType1Font font, float size) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y;
    }

    private float pdfLine(PDPageContentStream cs, float x1, float y1, float x2, float y2) throws IOException {
        cs.setLineWidth(0.75f);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
        return y1;
    }

    /** Draws a label-left / value-right row and returns the next available y position. */
    private float pdfRow(PDPageContentStream cs, float x, float width, float y, String label, String value)
            throws IOException {
        pdfText(cs, label, x, y, PDType1Font.HELVETICA, 10);
        float valueWidth = PDType1Font.HELVETICA.getStringWidth(value) / 1000f * 10;
        pdfText(cs, value, x + width - valueWidth, y, PDType1Font.HELVETICA, 10);
        return y - 16;
    }

    private float pdfRowBold(PDPageContentStream cs, float x, float width, float y, String label, String value)
            throws IOException {
        pdfText(cs, label, x, y, PDType1Font.HELVETICA_BOLD, 11);
        float valueWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(value) / 1000f * 11;
        pdfText(cs, value, x + width - valueWidth, y, PDType1Font.HELVETICA_BOLD, 11);
        return y - 18;
    }

    // ---------------------------------------------------------------
    // Print
    // ---------------------------------------------------------------

    @FXML
    private void handlePrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showInfo("Print Unavailable", "No printer could be found on this system.");
            return;
        }
        boolean proceed = job.showPrintDialog(rootPane.getScene().getWindow());
        if (proceed) {
            boolean success = job.printPage(payslipCard);
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
