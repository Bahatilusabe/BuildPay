package com.payroll.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.chart.PieChart;
import javafx.scene.layout.BorderPane;

import java.util.LinkedHashMap;
import java.util.Map;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import java.io.File;

public class DashboardController {

    @FXML
    private VBox menuBox;

    @FXML
    private Label dashboardTitle;

    @FXML
    private PieChart departmentChart;

    @FXML
    private BorderPane rootPane;

    private final Map<String, HBox> menuItems = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        // Build the menu in order
        addMenuItem("Dashboard", true);
        addMenuItem("Employees", false);
        addMenuItem("Attendance", false);
        addMenuItem("Payroll", false);
        addMenuItem("Payslips", false);
        addMenuItem("Reports", false);
        addMenuItem("Settings", false);

        // sample pie data if pie exists
        if (departmentChart != null) {
            departmentChart.getData().add(new PieChart.Data("Engineers", 20));
            departmentChart.getData().add(new PieChart.Data("Technicians", 40));
            departmentChart.getData().add(new PieChart.Data("Laborers", 68));
        }
    }

    // FXML event handler stubs referenced in Dashboard.fxml
    @FXML
    private void goToDashboard(javafx.scene.input.MouseEvent e) {
        handleMenuClick("Dashboard", e);
    }

    @FXML
    private void goToEmployees(javafx.scene.input.MouseEvent e) {
        handleMenuClick("Employees", e);
    }

    @FXML
    private void goToAttendance(javafx.scene.input.MouseEvent e) {
        handleMenuClick("Attendance", e);
    }

    @FXML
    private void goToPayroll(javafx.scene.input.MouseEvent e) {
        handleMenuClick("Payroll", e);
    }

    @FXML
    private void goToPayslips(javafx.scene.input.MouseEvent e) {
        handleMenuClick("Payslips", e);
    }

    @FXML
    private void goToReports(javafx.scene.input.MouseEvent e) {
        handleMenuClick("Reports", e);
    }

    @FXML
    private void goToProjects(javafx.scene.input.MouseEvent e) {
        handleMenuClick("Projects", e);
    }

    @FXML
    private void goToSettings(javafx.scene.input.MouseEvent e) {
        handleMenuClick("Settings", e);
    }

    @FXML
    private void onThemeToggle() {
        System.out.println("Theme toggle clicked");
    }

    // Quick actions
    @FXML
    private void quickAddEmployee() { handleAddEmployee(); }

    @FXML
    private void quickMarkAttendance() { System.out.println("Quick mark attendance (stub)"); }

    @FXML
    private void quickProcessPayroll() { handleGeneratePayroll(); }

    @FXML
    private void quickGenerateReport() { handleExportReport(); }

    private void addMenuItem(String title, boolean active) {
        HBox item = new HBox();
        item.getStyleClass().add("menu-item");
        item.setSpacing(14);
        item.setPrefHeight(48);

        Region leftIndicator = new Region();
        leftIndicator.getStyleClass().add("left-indicator");

        Label lbl = new Label(title);
        lbl.getStyleClass().add("menu-label");

        item.getChildren().addAll(leftIndicator, lbl);
        item.setOnMouseClicked(e -> handleMenuClick(title, e));

        if (active) {
            item.getStyleClass().add("active");
            leftIndicator.getStyleClass().add("left-indicator");
        }

        menuItems.put(title, item);
        if (menuBox != null) menuBox.getChildren().add(item);
    }

    private void handleMenuClick(String title, MouseEvent e) {
        // clear actives
        menuItems.forEach((k, v) -> v.getStyleClass().remove("active"));
        // set clicked
        HBox clicked = menuItems.get(title);
        if (clicked != null) clicked.getStyleClass().add("active");
        if (dashboardTitle != null) dashboardTitle.setText(title);
        // TODO: load center content per menu
        // load corresponding fxml based on title
        switch (title) {
            case "Dashboard" -> loadCenter("src/ui/dashboard/Dashboard.fxml");
            case "Employees" -> loadCenter("src/ui/employees/Employees.fxml");
            case "Attendance" -> loadCenter("src/ui/attendance/Attendance.fxml");
            case "Payroll" -> loadCenter("src/ui/payroll/Payroll.fxml");
            case "Payslips" -> loadCenter("src/ui/payslip/Payslip.fxml");
            case "Reports" -> loadCenter("src/ui/reports/Reports.fxml");
            case "Projects" -> {
                // no projects FXML yet; reuse reports as placeholder
                loadCenter("src/ui/reports/Reports.fxml");
            }
            case "Settings" -> loadCenter("src/ui/settings/settings.fxml");
        }
    }

    private void loadCenter(String fxmlPath) {
        try {
            File f = new File(fxmlPath);
            if (!f.exists()) {
                System.out.println("Missing FXML: " + f.getAbsolutePath());
                return;
            }
            Node center = FXMLLoader.load(f.toURI().toURL());
            if (rootPane != null) rootPane.setCenter(center);
            // ensure dashboard.css is applied to newly loaded content
            if (center != null && center.getStyleClass() != null) {
                // nothing special here; main scene has stylesheet already
            }
            System.out.println("Loaded: " + fxmlPath);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // placeholder method for switching center content
    public void showAttendance() {
        // will be implemented to load attendance UI into rootPane center
    }

    @FXML
    private void handleAddEmployee() {
        System.out.println("Add Employee action triggered");
        // TODO: Open Add Employee dialog/modal
        // Load EmployeeForm.fxml or show a dialog with input fields:
        // - First Name, Last Name, Email, Phone, Department, Position, Salary
    }

    @FXML
    private void handleGeneratePayroll() {
        System.out.println("Generate Payroll action triggered");
        // TODO: Open Generate Payroll dialog/modal
        // Load PayrollForm.fxml or show a dialog with input fields:
        // - Month, Year, Department filter, Preview button
    }

    @FXML
    private void handleCreatePayslip() {
        System.out.println("Create Payslip action triggered");
        // TODO: Open Create Payslip dialog/modal
        // Load PayslipForm.fxml or show a dialog with input fields:
        // - Employee selection, Pay period, Preview, Generate PDF
    }

    @FXML
    private void handleExportReport() {
        System.out.println("Export Report action triggered");
        // TODO: Open Export Report dialog/modal
        // Load ReportForm.fxml or show a dialog with input fields:
        // - Report type (Payroll, Attendance, etc.), Date range, Format (PDF, Excel)
    }
}
