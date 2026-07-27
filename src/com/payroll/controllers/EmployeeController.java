package com.payroll.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.collections.FXCollections;
import javafx.stage.Modality;
import javafx.stage.Stage;

import models.Employee;
import repositories.EmployeeRepository;
import utils.ExportUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class EmployeeController {

    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, String> idCol;
    @FXML private TableColumn<Employee, String> numberCol;
    @FXML private TableColumn<Employee, String> nameCol;
    @FXML private TableColumn<Employee, String> positionCol;
    @FXML private TableColumn<Employee, String> departmentCol;
    @FXML private TableColumn<Employee, String> salaryCol;
    @FXML private TableColumn<Employee, String> statusCol;
    @FXML private TableColumn<Employee, String> photoCol;
    @FXML private TextField searchField;
    @FXML private javafx.scene.control.ChoiceBox<String> statusFilter;

    private EmployeeRepository repository = new EmployeeRepository();

    @FXML
    public void initialize() {
        // wire simple string cell factories
        idCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        numberCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmployeeNumber()));
        // thumbnail column
        photoCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhotoPath()));
        photoCol.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(48);
                iv.setFitHeight(48);
                iv.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null || path.isBlank()) {
                    setGraphic(null);
                } else {
                    try {
                        // try load relative to project database folder
                        File img = new File(path);
                        if (!img.exists()) {
                            // try relative to database/photos
                            img = new File("database" + File.separator + path);
                        }
                        Image im = new Image(img.toURI().toString(), 48, 48, true, true);
                        iv.setImage(im);
                        setGraphic(new StackPane(iv));
                    } catch (Exception e) {
                        setGraphic(null);
                    }
                }
            }
        });
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        positionCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPosition()));
        departmentCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDepartment()));
        salaryCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%,.2f", c.getValue().getBasicSalary())));
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        statusFilter.getItems().addAll("ALL","ACTIVE","INACTIVE","ON_LEAVE");
        statusFilter.getSelectionModel().select("ALL");

        refreshTable();
    }

    private void refreshTable() {
        List<Employee> rows = repository.findAll();
        employeeTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    public void showAddDialog() {
        try {
            File f = new File("src/ui/employee/EmployeeDialog.fxml");
            FXMLLoader loader = new FXMLLoader(f.toURI().toURL());
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add Employee");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            if (employeeTable != null && employeeTable.getScene() != null) {
                stage.initOwner(employeeTable.getScene().getWindow());
            }
            // show and wait; controller will close the stage when done
            stage.showAndWait();
            refreshTable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onSearch() {
        String q = searchField.getText();
        if (q == null || q.trim().isEmpty()) { refreshTable(); return; }
        employeeTable.setItems(FXCollections.observableArrayList(repository.searchByName(q.trim())));
    }

    @FXML
    public void onFilterChange() {
        String sel = statusFilter.getValue();
        if (sel == null || "ALL".equals(sel)) { refreshTable(); return; }
        employeeTable.setItems(FXCollections.observableArrayList(repository.searchByName(sel)));
    }

    @FXML
    public void filterActive() { statusFilter.getSelectionModel().select("ACTIVE"); onFilterChange(); }
    @FXML
    public void filterInactive() { statusFilter.getSelectionModel().select("INACTIVE"); onFilterChange(); }

    @FXML
    public void exportPdf() {
        ExportUtil.exportEmployeesToPdf(repository.findAll());
    }

    @FXML
    public void exportExcel() {
        ExportUtil.exportEmployeesToExcel(repository.findAll());
    }

    @FXML
    public void onView() {
        Employee sel = employeeTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        // show image in a simple view dialog if present
        String p = sel.getPhotoPath();
        if (p == null || p.isBlank()) {
            System.out.println("View " + sel.getEmployeeNumber());
            return;
        }
        try {
            File imgFile = new File(p);
            if (!imgFile.exists()) imgFile = new File("database" + File.separator + p);
            if (!imgFile.exists()) { System.out.println("Photo not found: " + p); return; }
            ImageView iv = new ImageView(new Image(imgFile.toURI().toString()));
            iv.setPreserveRatio(true);
            iv.setFitWidth(600);
            Stage stage = new Stage();
            StackPane root = new StackPane(iv);
            stage.setScene(new Scene(root));
            stage.setTitle(sel.getFullName());
            stage.initModality(Modality.APPLICATION_MODAL);
            if (employeeTable != null && employeeTable.getScene() != null) stage.initOwner(employeeTable.getScene().getWindow());
            stage.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void onEdit() {
        Employee sel = employeeTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            File f = new File("src/ui/employee/EmployeeDialog.fxml");
            FXMLLoader loader = new FXMLLoader(f.toURI().toURL());
            Parent root = loader.load();
            // pass selected employee to controller
            com.payroll.controllers.EmployeeDialogController ctrl = loader.getController();
            ctrl.setEmployeeToEdit(sel);

            Stage stage = new Stage();
            stage.setTitle("Edit Employee");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            if (employeeTable != null && employeeTable.getScene() != null) {
                stage.initOwner(employeeTable.getScene().getWindow());
            }
            stage.showAndWait();
            refreshTable();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void onDelete() {
        Employee sel = employeeTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        repository.delete(sel.getId());
        refreshTable();
    }

    @FXML
    public void handleAttendanceClick() {
        try {
            java.net.URL res = getClass().getResource("/ui/attendance/attendance.fxml");
            if (res == null) res = getClass().getResource("/attendance.fxml");
            if (res == null) throw new IOException("attendance.fxml not found on classpath");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(res);
            javafx.scene.Parent root = loader.load();

            Stage stage = null;
            if (employeeTable != null && employeeTable.getScene() != null) {
                stage = (Stage) employeeTable.getScene().getWindow();
            }
            if (stage != null) {
                Scene scene = new Scene(root);
                java.net.URL css = getClass().getResource("/ui/attendance/attendance.css");
                if (css == null) css = getClass().getResource("/attendance.css");
                if (css != null) scene.getStylesheets().add(css.toExternalForm());
                stage.setTitle("BuildPay - Attendance");
                stage.setScene(scene);
                stage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
