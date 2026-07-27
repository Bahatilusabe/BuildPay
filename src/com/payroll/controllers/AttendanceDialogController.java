package com.payroll.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Attendance;
import models.Employee;
import services.AttendanceService;

import java.util.List;

public class AttendanceDialogController {

    @FXML private ChoiceBox<Employee> employeeChoice;
    @FXML private DatePicker datePicker;
    @FXML private ChoiceBox<String> statusChoice;
    @FXML private TextField overtimeField;

    private final AttendanceService service = new AttendanceService();
    private Attendance editing;

    @FXML
    public void initialize() {
        statusChoice.getItems().addAll("PRESENT","ABSENT","LEAVE");
    }

    public void setEmployees(List<Employee> employees) {
        employeeChoice.setItems(javafx.collections.FXCollections.observableArrayList(employees));
    }

    public void setEditing(Attendance a) {
        this.editing = a;
        if (a == null) return;
        // prefill
        for (Employee e : employeeChoice.getItems()) if (e.getId() == a.getEmployeeId()) employeeChoice.setValue(e);
        datePicker.setValue(java.time.LocalDate.parse(a.getAttendanceDate()));
        statusChoice.setValue(a.getStatus());
        overtimeField.setText(String.valueOf(a.getOvertimeHours()));
    }

    @FXML
    public void onSave() {
        Attendance a = new Attendance();
        Employee e = employeeChoice.getValue();
        if (e == null) return;
        a.setEmployeeId(e.getId());
        a.setAttendanceDate(datePicker.getValue().toString());
        a.setStatus(statusChoice.getValue());
        try { a.setOvertimeHours(Double.parseDouble(overtimeField.getText())); } catch(Exception ex) { a.setOvertimeHours(0); }

        if (editing != null) {
            a.setId(editing.getId());
            service.editAttendance(a);
        } else {
            service.addAttendance(a);
        }
        closeStage();
    }

    @FXML
    public void onCancel() { closeStage(); }

    private Stage getStage() { return (Stage) employeeChoice.getScene().getWindow(); }
    private void closeStage() { Stage s = getStage(); if (s != null) s.close(); }
}
