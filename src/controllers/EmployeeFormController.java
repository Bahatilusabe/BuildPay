package com.buildpay.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.function.Consumer;

import com.buildpay.controller.EmployeeController.Employee;
import com.buildpay.controller.EmployeeController.Status;

/**
 * Controller for EmployeeForm.fxml, reused for three purposes: adding a new
 * employee, editing an existing one, and (in read-only mode) viewing one.
 */
public class EmployeeFormController {

    /** Which purpose the dialog is currently serving. */
    public enum Mode { ADD, EDIT, VIEW }

    @FXML private javafx.scene.layout.StackPane formRoot;
    @FXML private Label formTitleLabel;
    @FXML private Button closeIconButton;

    @FXML private Circle avatarCircle;
    @FXML private Text avatarInitialsText;

    @FXML private TextField nameField;
    @FXML private Label nameErrorLabel;
    @FXML private TextField idField;
    @FXML private ComboBox<String> departmentField;
    @FXML private TextField positionField;
    @FXML private Label positionErrorLabel;
    @FXML private TextField phoneField;
    @FXML private Label phoneErrorLabel;
    @FXML private TextField salaryField;
    @FXML private Label salaryErrorLabel;
    @FXML private ComboBox<Status> statusField;

    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    private Mode mode = Mode.ADD;
    private Consumer<Employee> onSave;
    private String editingId;

    @FXML
    public void initialize() {
        departmentField.getItems().setAll(
                "Site Supervision", "Carpentry", "Electrical", "Plumbing",
                "Masonry", "Administration", "Finance", "Logistics"
        );
        statusField.getItems().setAll(Status.values());

        nameField.textProperty().addListener((obs, oldV, newV) -> {
            avatarInitialsText.setText(initialsOf(newV));
            if (!newV.isBlank()) setFieldError(nameField, nameErrorLabel, false);
        });
        positionField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.isBlank()) setFieldError(positionField, positionErrorLabel, false);
        });
        phoneField.textProperty().addListener((obs, oldV, newV) -> {
            if (isValidPhone(newV)) setFieldError(phoneField, phoneErrorLabel, false);
        });
        salaryField.textProperty().addListener((obs, oldV, newV) -> {
            if (isValidSalary(newV)) setFieldError(salaryField, salaryErrorLabel, false);
        });
    }

    // ---------------------------------------------------------------
    // Configuration (called by EmployeeController before showing)
    // ---------------------------------------------------------------

    public void setOnSave(Consumer<Employee> callback) {
        this.onSave = callback;
    }

    /** Configures the dialog for adding a new employee, pre-filling the generated ID. */
    public void configureForAdd(String generatedId) {
        mode = Mode.ADD;
        editingId = generatedId;
        formTitleLabel.setText("Add Employee");
        idField.setText(generatedId);
        statusField.setValue(Status.ACTIVE);
        saveButton.setText("Save Employee");
        setFieldsEditable(true);
    }

    /** Configures the dialog for editing an existing employee, pre-filling all fields. */
    public void configureForEdit(Employee employee) {
        mode = Mode.EDIT;
        editingId = employee.getId();
        formTitleLabel.setText("Edit Employee");
        populateFrom(employee);
        saveButton.setText("Update Employee");
        setFieldsEditable(true);
    }

    /** Configures the dialog as a read-only detail view. */
    public void configureForView(Employee employee) {
        mode = Mode.VIEW;
        editingId = employee.getId();
        formTitleLabel.setText("Employee Details");
        populateFrom(employee);
        saveButton.setText("Close");
        setFieldsEditable(false);
    }

    private void populateFrom(Employee e) {
        nameField.setText(e.getName());
        idField.setText(e.getId());
        departmentField.setValue(e.getDepartment());
        positionField.setText(e.getPosition());
        phoneField.setText(e.getPhone());
        salaryField.setText(String.valueOf((long) e.getSalary()));
        statusField.setValue(e.getStatus());
        avatarInitialsText.setText(initialsOf(e.getName()));
    }

    private void setFieldsEditable(boolean editable) {
        nameField.setEditable(editable);
        departmentField.setDisable(!editable);
        positionField.setEditable(editable);
        phoneField.setEditable(editable);
        salaryField.setEditable(editable);
        statusField.setDisable(!editable);
    }

    // ---------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------

    @FXML
    private void handleSave() {
        if (mode == Mode.VIEW) {
            closeDialog();
            return;
        }
        if (!validate()) {
            return;
        }
        Employee employee = new Employee(
                editingId,
                nameField.getText().trim(),
                departmentField.getValue(),
                positionField.getText().trim(),
                phoneField.getText().trim(),
                Double.parseDouble(salaryField.getText().trim()),
                statusField.getValue()
        );
        if (onSave != null) {
            onSave.accept(employee);
        }
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) formRoot.getScene().getWindow();
        stage.close();
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    private boolean validate() {
        boolean nameValid = !nameField.getText().isBlank();
        boolean positionValid = !positionField.getText().isBlank();
        boolean phoneValid = isValidPhone(phoneField.getText());
        boolean salaryValid = isValidSalary(salaryField.getText());

        setFieldError(nameField, nameErrorLabel, !nameValid);
        setFieldError(positionField, positionErrorLabel, !positionValid);
        setFieldError(phoneField, phoneErrorLabel, !phoneValid);
        setFieldError(salaryField, salaryErrorLabel, !salaryValid);

        return nameValid && positionValid && phoneValid && salaryValid;
    }

    private boolean isValidPhone(String text) {
        return text != null && text.trim().matches("^[0-9+()\\-\\s]{7,15}$");
    }

    private boolean isValidSalary(String text) {
        if (text == null || text.isBlank()) return false;
        try {
            double v = Double.parseDouble(text.trim());
            return v > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void setFieldError(Region field, Label errorLabel, boolean hasError) {
        field.getStyleClass().removeIf(s -> s.equals("field-error"));
        if (hasError) {
            field.getStyleClass().add("field-error");
        }
        errorLabel.setVisible(hasError);
        errorLabel.setManaged(hasError);
    }

    private String initialsOf(String name) {
        if (name == null || name.isBlank()) return "EM";
        String[] parts = name.trim().split("\\s+");
        String initials = String.valueOf(parts[0].charAt(0));
        if (parts.length > 1) {
            initials += parts[parts.length - 1].charAt(0);
        }
        return initials.toUpperCase();
    }
}
