package com.payroll.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Employee;
import services.EmployeeService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Controller for the Add / Edit Employee dialog.
 * Supports pre-filling when editing and uploading a photo path.
 */
public class EmployeeDialogController {

    @FXML private TextField employeeNumberField;
    @FXML private TextField fullNameField;
    @FXML private ChoiceBox<String> genderChoice;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField positionField;
    @FXML private TextField departmentField;
    @FXML private TextField basicSalaryField;
    @FXML private ChoiceBox<String> statusChoice;

    private final EmployeeService service = new EmployeeService();
    private Employee editing; // null when creating
    private String photoPath;

    @FXML
    public void initialize() {
        genderChoice.getItems().addAll("M","F","Other");
        statusChoice.getItems().addAll("ACTIVE","INACTIVE","ON_LEAVE");
    }

    /** Called by owner to set employee to edit. */
    public void setEmployeeToEdit(Employee e) {
        this.editing = e;
        if (e == null) return;
        employeeNumberField.setText(e.getEmployeeNumber());
        fullNameField.setText(e.getFullName());
        genderChoice.setValue(e.getGender());
        phoneField.setText(e.getPhone());
        emailField.setText(e.getEmail());
        positionField.setText(e.getPosition());
        departmentField.setText(e.getDepartment());
        basicSalaryField.setText(String.valueOf(e.getBasicSalary()));
        statusChoice.setValue(e.getStatus());
        photoPath = e.getPhotoPath();
    }

    @FXML
    public void onSave() {
        Employee e = buildFromForm();
        Employee created = service.addEmployee(e);
        if (created != null) closeStage();
    }

    @FXML
    public void onUpdate() {
        if (editing == null) return;
        Employee e = buildFromForm();
        e.setId(editing.getId());
        boolean ok = service.editEmployee(e);
        if (ok) closeStage();
    }

    @FXML
    public void onClear() {
        employeeNumberField.clear(); fullNameField.clear(); phoneField.clear(); emailField.clear();
        positionField.clear(); departmentField.clear(); basicSalaryField.clear();
        genderChoice.getSelectionModel().clearSelection(); statusChoice.getSelectionModel().clearSelection();
        photoPath = null;
    }

    @FXML
    public void onCancel() {
        closeStage();
    }

    @FXML
    public void onUploadPhoto() {
        Stage st = getStage();
        if (st == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Employee Photo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File f = chooser.showOpenDialog(st);
        if (f != null) {
            // ensure photos directory
            try {
                Path photosDir = Path.of("database", "photos");
                if (!Files.exists(photosDir)) Files.createDirectories(photosDir);
                String destName = System.currentTimeMillis() + "_" + f.getName();
                Path dest = photosDir.resolve(destName);
                Files.copy(f.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                // store relative filename only (we'll resolve against database/photos when loading)
                photoPath = destName;
            } catch (IOException io) {
                io.printStackTrace();
                photoPath = f.getAbsolutePath();
            }
        }
    }

    private Employee buildFromForm() {
        Employee e = (editing != null) ? new Employee() : new Employee();
        e.setEmployeeNumber(employeeNumberField.getText());
        e.setFullName(fullNameField.getText());
        e.setGender(genderChoice.getValue());
        e.setPhone(phoneField.getText());
        e.setEmail(emailField.getText());
        e.setPosition(positionField.getText());
        e.setDepartment(departmentField.getText());
        try { e.setBasicSalary(Double.parseDouble(basicSalaryField.getText())); } catch(Exception ex) { e.setBasicSalary(0); }
        e.setStatus(statusChoice.getValue());
        e.setPhotoPath(photoPath);
        return e;
    }

    private Stage getStage() {
        if (employeeNumberField == null) return null;
        return (Stage) employeeNumberField.getScene().getWindow();
    }

    private void closeStage() {
        Stage s = getStage();
        if (s != null) s.close();
    }

}
