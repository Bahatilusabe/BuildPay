package com.buildpay.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controls the Reports &amp; Analytics module: dashboard summary cards, four
 * canvas-drawn charts (Revenue, Payroll, Attendance, Projects), report
 * generation, and PDF / Excel / CSV export plus printing.
 *
 * NOTE: exports require two extra libraries on the classpath in addition to
 * plain JavaFX:
 *   PDF   -> org.apache.pdfbox:pdfbox:2.0.31        (same as the Payslip module)
 *   Excel -> org.apache.poi:poi-ooxml:5.2.5
 *
 * Sample figures here intentionally mirror the ones already shown on the
 * Dashboard (payroll trend, attendance split, project costs, active project
 * count) so the numbers agree across BuildPay rather than diverging.
 */
public class ReportsController {

    private static final int PAGE_SIZE = 8;
    private static final DateTimeFormatter GENERATED_FMT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    // ---- Sample data (kept consistent with the Dashboard module) ------
    private final String[] months = {"Feb", "Mar", "Apr", "May", "Jun", "Jul"};
    private final double[] revenueSeries = {5.8, 6.0, 6.3, 6.5, 6.7, 6.9};   // KES millions
    private final double[] payrollSeries = {3.1, 3.4, 3.6, 3.9, 4.0, 4.2};   // KES millions (matches Dashboard)
    private final double attendancePresent = 0.82;
    private final double attendanceLate = 0.11;
    private final double attendanceAbsent = 0.07;
    private final String[] projectNames = {"Riverside Apts", "Westgate Office", "Northbridge Hsg", "Coast Rd Ext"};
    private final double[] projectCosts = {2.1, 1.4, 0.98, 0.62};           // KES millions (matches Dashboard)
    private final int activeProjectsCount = 18;                             // matches Dashboard

    // ---------------------------------------------------------------
    // Domain model
    // ---------------------------------------------------------------

    public enum ReportType {
        REVENUE("Revenue", "chip-revenue"),
        PAYROLL("Payroll", "chip-payroll"),
        ATTENDANCE("Attendance", "chip-attendance"),
        PROJECTS("Projects", "chip-projects"),
        ALL("Full Overview", "chip-all");

        private final String label;
        private final String styleClass;

        ReportType(String label, String styleClass) {
            this.label = label;
            this.styleClass = styleClass;
        }

        public String getStyleClass() { return styleClass; }

        @Override
        public String toString() { return label; }
    }

    public static class GeneratedReport {
        private final String name;
        private final ReportType type;
        private final String period;
        private final LocalDateTime generatedOn;
        private final double revenueValue;
        private final double payrollValue;
        private final double attendanceRate;
        private final int activeProjects;

        public GeneratedReport(String name, ReportType type, String period, LocalDateTime generatedOn,
                                double revenueValue, double payrollValue, double attendanceRate, int activeProjects) {
            this.name = name;
            this.type = type;
            this.period = period;
            this.generatedOn = generatedOn;
            this.revenueValue = revenueValue;
            this.payrollValue = payrollValue;
            this.attendanceRate = attendanceRate;
            this.activeProjects = activeProjects;
        }

        public String getName() { return name; }
        public ReportType getType() { return type; }
        public String getPeriod() { return period; }
        public LocalDateTime getGeneratedOn() { return generatedOn; }
        public double getRevenueValue() { return revenueValue; }
        public double getPayrollValue() { return payrollValue; }
        public double getAttendanceRate() { return attendanceRate; }
        public int getActiveProjects() { return activeProjects; }
    }

    // ---------------------------------------------------------------
    // FXML fields
    // ---------------------------------------------------------------

    @FXML private VBox rootPane;
    @FXML private Button backButton;
    @FXML private Button exportPdfButton;
    @FXML private Button exportExcelButton;
    @FXML private Button exportCsvButton;
    @FXML private Button printButton;

    @FXML private VBox mainContent;
    @FXML private ComboBox<ReportType> reportTypeSelector;
    @FXML private ComboBox<String> periodSelector;
    @FXML private Button generateReportButton;

    @FXML private Label statRevenueValue;
    @FXML private Label statRevenueTrend;
    @FXML private Label statPayrollValue;
    @FXML private Label statPayrollTrend;
    @FXML private Label statAttendanceValue;
    @FXML private Label statAttendanceTrend;
    @FXML private Label statProjectsValue;
    @FXML private Label statProjectsTrend;

    @FXML private Canvas revenueCanvas;
    @FXML private Canvas payrollCanvas;
    @FXML private Canvas attendanceCanvas;
    @FXML private Label attendanceCenterLabel;
    @FXML private Canvas projectsCanvas;

    @FXML private Label reportCountLabel;
    @FXML private TableView<GeneratedReport> reportsTable;
    @FXML private TableColumn<GeneratedReport, String> nameColumn;
    @FXML private TableColumn<GeneratedReport, GeneratedReport> typeColumn;
    @FXML private TableColumn<GeneratedReport, String> periodColumn;
    @FXML private TableColumn<GeneratedReport, String> generatedColumn;
    @FXML private TableColumn<GeneratedReport, GeneratedReport> actionsColumn;

    @FXML private Label paginationSummaryLabel;
    @FXML private Button prevPageButton;
    @FXML private HBox pageNumbersBox;
    @FXML private Button nextPageButton;

    // ---------------------------------------------------------------
    // State
    // ---------------------------------------------------------------

    private final List<GeneratedReport> allReports = new ArrayList<>();
    private int currentPage = 0;

    @FXML
    public void initialize() {
        setupGenerator();
        setupTableColumns();
        updateStatCards();
        drawRevenueChart();
        drawPayrollChart();
        drawAttendanceChart();
        drawProjectsChart();
        refreshCurrentPageView();
    }

    // ---------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------

    private void setupGenerator() {
        reportTypeSelector.getItems().setAll(ReportType.values());
        reportTypeSelector.setValue(ReportType.ALL);

        periodSelector.getItems().setAll("Last 3 Months", "Last 6 Months", "Last 12 Months");
        periodSelector.setValue("Last 6 Months");
    }

    private void setupTableColumns() {
        nameColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getName()));
        periodColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getPeriod()));
        generatedColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getGeneratedOn().format(GENERATED_FMT)));

        typeColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(GeneratedReport ignored, boolean empty) {
                super.updateItem(ignored, empty);
                GeneratedReport r = (getTableRow() != null) ? getTableRow().getItem() : null;
                if (empty || r == null) { setGraphic(null); return; }
                Label chip = new Label(r.getType().toString());
                chip.getStyleClass().addAll("type-chip", r.getType().getStyleClass());
                setGraphic(chip);
            }
        });

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(GeneratedReport ignored, boolean empty) {
                super.updateItem(ignored, empty);
                GeneratedReport r = (getTableRow() != null) ? getTableRow().getItem() : null;
                if (empty || r == null) { setGraphic(null); return; }

                Button pdf = smallButton("PDF");
                pdf.setOnAction(e -> exportPdf(r));
                Button xls = smallButton("XLS");
                xls.setOnAction(e -> exportExcel(r));
                Button csv = smallButton("CSV");
                csv.setOnAction(e -> exportCsv(r));
                Button print = smallButton("Print");
                print.setOnAction(e -> printReport(r));

                HBox box = new HBox(4, pdf, xls, csv, print);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
    }

    private Button smallButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("row-action-btn");
        return b;
    }

    // ---------------------------------------------------------------
    // Dashboard cards
    // ---------------------------------------------------------------

    private void updateStatCards() {
        double revenueNow = revenueSeries[revenueSeries.length - 1];
        double revenuePrev = revenueSeries[revenueSeries.length - 2];
        double payrollNow = payrollSeries[payrollSeries.length - 1];
        double payrollPrev = payrollSeries[payrollSeries.length - 2];

        statRevenueValue.setText(String.format("KES %.1fM", revenueNow));
        statRevenueTrend.setText(String.format("\u25B2 %.1f%% this month", pctChange(revenuePrev, revenueNow)));

        statPayrollValue.setText(String.format("KES %.1fM", payrollNow));
        statPayrollTrend.setText(String.format("\u25B2 %.1f%% vs last month", pctChange(payrollPrev, payrollNow)));

        statAttendanceValue.setText(String.format("%.0f%%", attendancePresent * 100));
        statAttendanceTrend.setText("\u25B2 1.2% vs last month");

        statProjectsValue.setText(String.valueOf(activeProjectsCount));
        statProjectsTrend.setText("\u25B2 2 new sites");
    }

    private double pctChange(double prev, double now) {
        if (prev == 0) return 0;
        return Math.round(((now - prev) / prev) * 1000) / 10.0;
    }

    // ---------------------------------------------------------------
    // Charts
    // ---------------------------------------------------------------

    private void drawRevenueChart() {
        drawLineChart(revenueCanvas, revenueSeries, 8.0, Color.web("#12B886"));
    }

    private void drawPayrollChart() {
        drawLineChart(payrollCanvas, payrollSeries, 5.0, Color.web("#2E6BFF"));
    }

    private void drawLineChart(Canvas canvas, double[] values, double maxScale, Color color) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double paddingLeft = 42, paddingBottom = 26, paddingTop = 12;
        double chartW = w - paddingLeft - 12;
        double chartH = h - paddingBottom - paddingTop;

        gc.clearRect(0, 0, w, h);
        gc.setStroke(Color.web("#E7ECF5"));
        gc.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double y = paddingTop + chartH - (chartH * i / 4.0);
            gc.strokeLine(paddingLeft, y, w - 12, y);
            gc.setFill(Color.web("#9AA6BF"));
            gc.setFont(Font.font("Inter", 10));
            gc.fillText(String.format("%.1fM", maxScale * i / 4.0), 4, y + 3);
        }

        int n = values.length;
        double stepX = chartW / (n - 1);

        gc.setFill(color.deriveColor(0, 1, 1, 0.12));
        gc.beginPath();
        gc.moveTo(paddingLeft, paddingTop + chartH);
        for (int i = 0; i < n; i++) {
            double x = paddingLeft + stepX * i;
            double y = paddingTop + chartH - (chartH * (values[i] / maxScale));
            gc.lineTo(x, y);
        }
        gc.lineTo(paddingLeft + stepX * (n - 1), paddingTop + chartH);
        gc.closePath();
        gc.fill();

        gc.setStroke(color);
        gc.setLineWidth(2.5);
        gc.beginPath();
        for (int i = 0; i < n; i++) {
            double x = paddingLeft + stepX * i;
            double y = paddingTop + chartH - (chartH * (values[i] / maxScale));
            if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
        }
        gc.stroke();

        for (int i = 0; i < n; i++) {
            double x = paddingLeft + stepX * i;
            double y = paddingTop + chartH - (chartH * (values[i] / maxScale));
            gc.setFill(Color.WHITE);
            gc.fillOval(x - 4, y - 4, 8, 8);
            gc.setStroke(color);
            gc.setLineWidth(2);
            gc.strokeOval(x - 4, y - 4, 8, 8);
            gc.setFill(Color.web("#6B7A99"));
            gc.fillText(months[i], x - 10, h - 8);
        }
    }

    private void drawAttendanceChart() {
        GraphicsContext gc = attendanceCanvas.getGraphicsContext2D();
        double w = attendanceCanvas.getWidth();
        double h = attendanceCanvas.getHeight();
        double cx = w / 2, cy = h / 2;
        double outerR = Math.min(w, h) / 2 - 6;
        double innerR = outerR * 0.62;

        gc.clearRect(0, 0, w, h);

        double start = 90;
        start = drawDoughnutSegment(gc, cx, cy, outerR, innerR, start, attendancePresent * 360, Color.web("#2E6BFF"));
        start = drawDoughnutSegment(gc, cx, cy, outerR, innerR, start, attendanceLate * 360, Color.web("#F5A524"));
        drawDoughnutSegment(gc, cx, cy, outerR, innerR, start, attendanceAbsent * 360, Color.web("#E5484D"));

        attendanceCenterLabel.setText(String.format("%.0f%%", attendancePresent * 100));
    }

    private double drawDoughnutSegment(GraphicsContext gc, double cx, double cy, double outerR, double innerR,
                                        double startAngle, double sweep, Color color) {
        gc.setFill(color);
        gc.beginPath();
        gc.arc(cx, cy, outerR, outerR, startAngle, -sweep);
        double endAngle = startAngle - sweep;
        gc.arc(cx, cy, innerR, innerR, endAngle, sweep);
        gc.closePath();
        gc.fill();
        return endAngle;
    }

    private void drawProjectsChart() {
        GraphicsContext gc = projectsCanvas.getGraphicsContext2D();
        double w = projectsCanvas.getWidth();
        double h = projectsCanvas.getHeight();
        gc.clearRect(0, 0, w, h);

        double paddingBottom = 30, paddingTop = 14;
        double chartH = h - paddingBottom - paddingTop;
        double max = 2.5;
        int n = projectCosts.length;
        double barAreaW = w / n;
        double barW = barAreaW * 0.5;

        gc.setFont(Font.font("Inter", 10));
        for (int i = 0; i < n; i++) {
            double barH = chartH * (projectCosts[i] / max);
            double x = barAreaW * i + (barAreaW - barW) / 2;
            double y = paddingTop + chartH - barH;

            gc.setFill(Color.web("#7C5CFC"));
            gc.fillRoundRect(x, y, barW, barH, 6, 6);

            gc.setFill(Color.web("#33415C"));
            gc.fillText(String.format("%.1fM", projectCosts[i]), x - 4, y - 6);

            gc.setFill(Color.web("#9AA6BF"));
            gc.fillText(projectNames[i], barAreaW * i + 2, h - 8);
        }
    }

    // ---------------------------------------------------------------
    // Generate report
    // ---------------------------------------------------------------

    @FXML
    private void handleGenerateReport() {
        ReportType type = reportTypeSelector.getValue();
        String period = periodSelector.getValue();
        String name = type.toString() + " Report \u2014 " + period;

        GeneratedReport report = new GeneratedReport(name, type, period, LocalDateTime.now(),
                revenueSeries[revenueSeries.length - 1], payrollSeries[payrollSeries.length - 1],
                attendancePresent * 100, activeProjectsCount);

        allReports.add(0, report);
        currentPage = 0;
        refreshCurrentPageView();
        showInfo("Report Generated", name + " has been added to Generated Reports.");
    }

    // ---------------------------------------------------------------
    // Pagination
    // ---------------------------------------------------------------

    private int totalPages() {
        return Math.max(1, (int) Math.ceil(allReports.size() / (double) PAGE_SIZE));
    }

    private void refreshCurrentPageView() {
        int total = allReports.size();
        int totalPages = totalPages();
        currentPage = Math.min(currentPage, totalPages - 1);
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);

        List<GeneratedReport> pageItems = total == 0 ? List.of() : allReports.subList(from, to);
        reportsTable.setItems(FXCollections.observableArrayList(pageItems));

        reportCountLabel.setText(total + (total == 1 ? " report" : " reports"));
        paginationSummaryLabel.setText(total == 0
                ? "No reports generated yet"
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
    // Toolbar export (acts on the currently selected generator type/period)
    // ---------------------------------------------------------------

    @FXML
    private void handleExportPdf() { exportPdf(currentSnapshot()); }

    @FXML
    private void handleExportExcel() { exportExcel(currentSnapshot()); }

    @FXML
    private void handleExportCsv() { exportCsv(currentSnapshot()); }

    @FXML
    private void handlePrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showInfo("Print Unavailable", "No printer could be found on this system.");
            return;
        }
        boolean proceed = job.showPrintDialog(rootPane.getScene().getWindow());
        if (proceed) {
            boolean success = job.printPage(mainContent);
            if (success) job.endJob();
        }
    }

    private void printReport(GeneratedReport r) {
        handlePrint();
    }

    private GeneratedReport currentSnapshot() {
        ReportType type = reportTypeSelector.getValue();
        String period = periodSelector.getValue();
        return new GeneratedReport(type + " Report \u2014 " + period, type, period, LocalDateTime.now(),
                revenueSeries[revenueSeries.length - 1], payrollSeries[payrollSeries.length - 1],
                attendancePresent * 100, activeProjectsCount);
    }

    // ---------------------------------------------------------------
    // PDF export (Apache PDFBox)
    // ---------------------------------------------------------------

    private void exportPdf(GeneratedReport r) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Report as PDF");
        chooser.setInitialFileName(safeFileName(r.getName()) + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file == null) return;

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float margin = 50;
            float width = page.getMediaBox().getWidth() - 2 * margin;
            float y = page.getMediaBox().getHeight() - margin;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                y = pdfText(cs, "BUILDPAY", margin, y, PDType1Font.HELVETICA_BOLD, 20);
                y = pdfText(cs, "Reports & Analytics", margin, y - 4, PDType1Font.HELVETICA, 10);
                y = pdfText(cs, r.getName(), margin, y - 16, PDType1Font.HELVETICA_BOLD, 13);
                y = pdfText(cs, "Generated: " + r.getGeneratedOn().format(GENERATED_FMT), margin, y - 14,
                        PDType1Font.HELVETICA, 9);
                y -= 24;

                y = pdfRow(cs, margin, width, y, "Total Revenue", String.format("KES %.1fM", r.getRevenueValue()));
                y = pdfRow(cs, margin, width, y, "Total Payroll", String.format("KES %.1fM", r.getPayrollValue()));
                y = pdfRow(cs, margin, width, y, "Attendance Rate", String.format("%.0f%%", r.getAttendanceRate()));
                y = pdfRow(cs, margin, width, y, "Active Projects", String.valueOf(r.getActiveProjects()));
            }
            doc.save(file);
            showInfo("PDF Saved", "Report saved to " + file.getName() + ".");
        } catch (IOException ex) {
            showInfo("Export Failed", "Could not generate the PDF. Please try again.");
        }
    }

    private float pdfText(PDPageContentStream cs, String text, float x, float y, PDType1Font font, float size)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y;
    }

    private float pdfRow(PDPageContentStream cs, float x, float width, float y, String label, String value)
            throws IOException {
        pdfText(cs, label, x, y, PDType1Font.HELVETICA, 11);
        float valueWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(value) / 1000f * 11;
        pdfText(cs, value, x + width - valueWidth, y, PDType1Font.HELVETICA_BOLD, 11);
        return y - 20;
    }

    // ---------------------------------------------------------------
    // Excel export (Apache POI)
    // ---------------------------------------------------------------

    private void exportExcel(GeneratedReport r) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Report as Excel");
        chooser.setInitialFileName(safeFileName(r.getName()) + ".xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File file = chooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file == null) return;

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Summary");
            int rowIdx = 0;

            rowIdx = writeExcelRow(sheet, rowIdx, "Report Name", r.getName());
            rowIdx = writeExcelRow(sheet, rowIdx, "Type", r.getType().toString());
            rowIdx = writeExcelRow(sheet, rowIdx, "Period", r.getPeriod());
            rowIdx = writeExcelRow(sheet, rowIdx, "Generated On", r.getGeneratedOn().format(GENERATED_FMT));
            rowIdx++;

            rowIdx = writeExcelRow(sheet, rowIdx, "Total Revenue (KES M)", String.format("%.1f", r.getRevenueValue()));
            rowIdx = writeExcelRow(sheet, rowIdx, "Total Payroll (KES M)", String.format("%.1f", r.getPayrollValue()));
            rowIdx = writeExcelRow(sheet, rowIdx, "Attendance Rate (%)", String.format("%.0f", r.getAttendanceRate()));
            rowIdx = writeExcelRow(sheet, rowIdx, "Active Projects", String.valueOf(r.getActiveProjects()));
            rowIdx++;

            Row header = sheet.createRow(rowIdx++);
            header.createCell(0).setCellValue("Month");
            header.createCell(1).setCellValue("Revenue (KES M)");
            header.createCell(2).setCellValue("Payroll (KES M)");
            for (int i = 0; i < months.length; i++) {
                Row dataRow = sheet.createRow(rowIdx++);
                dataRow.createCell(0).setCellValue(months[i]);
                dataRow.createCell(1).setCellValue(revenueSeries[i]);
                dataRow.createCell(2).setCellValue(payrollSeries[i]);
            }

            for (int c = 0; c <= 2; c++) sheet.autoSizeColumn(c);

            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }
            showInfo("Excel Saved", "Report saved to " + file.getName() + ".");
        } catch (IOException ex) {
            showInfo("Export Failed", "Could not generate the Excel file. Please try again.");
        }
    }

    private int writeExcelRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        row.createCell(1).setCellValue(value);
        return rowIdx + 1;
    }

    // ---------------------------------------------------------------
    // CSV export
    // ---------------------------------------------------------------

    private void exportCsv(GeneratedReport r) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Report as CSV");
        chooser.setInitialFileName(safeFileName(r.getName()) + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file == null) return;

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Report Name," + csvSafe(r.getName()) + "\n");
            writer.write("Type," + csvSafe(r.getType().toString()) + "\n");
            writer.write("Period," + csvSafe(r.getPeriod()) + "\n");
            writer.write("Generated On," + r.getGeneratedOn().format(GENERATED_FMT) + "\n\n");

            writer.write("Metric,Value\n");
            writer.write(String.format("Total Revenue (KES M),%.1f%n", r.getRevenueValue()));
            writer.write(String.format("Total Payroll (KES M),%.1f%n", r.getPayrollValue()));
            writer.write(String.format("Attendance Rate (%%),%.0f%n", r.getAttendanceRate()));
            writer.write("Active Projects," + r.getActiveProjects() + "\n\n");

            writer.write("Month,Revenue (KES M),Payroll (KES M)\n");
            for (int i = 0; i < months.length; i++) {
                writer.write(months[i] + "," + revenueSeries[i] + "," + payrollSeries[i] + "\n");
            }
            showInfo("CSV Saved", "Report saved to " + file.getName() + ".");
        } catch (IOException ex) {
            showInfo("Export Failed", "Could not write the CSV file. Please try again.");
        }
    }

    private String csvSafe(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String safeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\- ]", "").replace(" ", "_");
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
