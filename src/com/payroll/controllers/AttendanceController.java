package com.payroll.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;

import models.Attendance;
import models.Employee;
import repositories.AttendanceRepository;
import repositories.EmployeeRepository;

import java.time.LocalDate;
import java.util.List;

public class AttendanceController {

    @FXML
    private ChoiceBox<Employee> employeeFilter;

    @FXML
    private DatePicker fromDate;

    @FXML
    private DatePicker toDate;

    @FXML
    private TableView<Attendance> attendanceTable;

    @FXML
    private TableColumn<Attendance, Integer> idCol;

    @FXML
    private TableColumn<Attendance, String> employeeCol;

    @FXML
    private TableColumn<Attendance, String> dateCol;

    @FXML
    private TableColumn<Attendance, String> statusCol;

    @FXML
    private TableColumn<Attendance, Double> overtimeCol;

    private AttendanceRepository repo = new AttendanceRepository();
    private EmployeeRepository employeeRepo = new EmployeeRepository();

    public void initialize() {

        fromDate.setValue(LocalDate.now().minusDays(30));
        toDate.setValue(LocalDate.now());

        List<Employee> emps = employeeRepo.findAll();
        employeeFilter.setItems(FXCollections.observableArrayList(emps));
        employeeFilter.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Employee e) { return e == null ? "" : e.getFullName(); }

            @Override
            public Employee fromString(String string) { return null; }
        });

        refreshTable();
    }

    @FXML
    public void onFilter() {
        Employee sel = employeeFilter.getValue();
        String from = fromDate.getValue() == null ? "0000-01-01" : fromDate.getValue().toString();
        String to = toDate.getValue() == null ? "9999-12-31" : toDate.getValue().toString();

        if (sel != null) {
            attendanceTable.setItems(FXCollections.observableArrayList(repo.findByEmployee(sel.getId())));
        } else {
            attendanceTable.setItems(FXCollections.observableArrayList(repo.findByDateRange(from, to)));
        }
    }

    @FXML
    public void showAddDialog() {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Add Attendance");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setContentText("Attendance creation UI not implemented yet.");
        d.showAndWait();
    }

    @FXML
    public void onEdit() {
        Attendance sel = attendanceTable.getSelectionModel().getSelectedItem();
        if (sel == null) { new Alert(Alert.AlertType.WARNING, "Select a record to edit.", ButtonType.OK).showAndWait(); return; }
        new Alert(Alert.AlertType.INFORMATION, "Edit not implemented.", ButtonType.OK).showAndWait();
    }

    @FXML
    public void onDelete() {
        Attendance sel = attendanceTable.getSelectionModel().getSelectedItem();
        if (sel == null) { new Alert(Alert.AlertType.WARNING, "Select a record to delete.", ButtonType.OK).showAndWait(); return; }
        boolean ok = repo.delete(sel.getId());
        if (ok) refreshTable();
    }

    private void refreshTable() {
        attendanceTable.setItems(FXCollections.observableArrayList(repo.findAllWithEmployeeNames()));
    }
}
