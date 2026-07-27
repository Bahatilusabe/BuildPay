package com.buildpay.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controls the Employee Management module: search, filters, a sortable table
 * view and an alternate card view (sharing the same paginated data window),
 * statistics, export/print, and the Add / Edit / View / Delete dialogs.
 */
public class EmployeeController {

    private static final int PAGE_SIZE = 8;

    // ---------------------------------------------------------------
    // Domain model (kept in this file to stay within the requested file set)
    // ---------------------------------------------------------------

    public enum Status {
        ACTIVE("Active", "status-active"),
        ON_LEAVE("On Leave", "status-on-leave"),
        INACTIVE("Inactive", "status-inactive");

        private final String label;
        private final String styleClass;

        Status(String label, String styleClass) {
            this.label = label;
            this.styleClass = styleClass;
        }

        public String getStyleClass() { return styleClass; }

        @Override
        public String toString() { return label; }
    }

    public static class Employee {
        private final String id;
        private final String name;
        private final String department;
        private final String position;
        private final String phone;
        private final double salary;
        private final Status status;

        public Employee(String id, String name, String department, String position,
                         String phone, double salary, Status status) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.position = position;
            this.phone = phone;
            this.salary = salary;
            this.status = status;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDepartment() { return department; }
        public String getPosition() { return position; }
        public String getPhone() { return phone; }
        public double getSalary() { return salary; }
        public Status getStatus() { return status; }

        public String getInitials() {
            String[] parts = name.trim().split("\\s+");
            String initials = String.valueOf(parts[0].charAt(0));
            if (parts.length > 1) {
                initials += parts[parts.length - 1].charAt(0);
            }
            return initials.toUpperCase();
        }
    }

    // ---------------------------------------------------------------
    // FXML fields
    // ---------------------------------------------------------------

    @FXML private VBox rootPane;
    @FXML private Button backButton;
    @FXML private Button exportButton;
    @FXML private Button printButton;
    @FXML private Button addEmployeeButton;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ToggleButton tableViewToggle;
    @FXML private ToggleButton cardViewToggle;

    @FXML private Label statTotalValue;
    @FXML private Label statActiveValue;
    @FXML private Label statOnLeaveValue;
    @FXML private Label statDepartmentsValue;

    @FXML private StackPane contentStack;
    @FXML private VBox tableWrapper;
    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, String> idColumn;
    @FXML private TableColumn<Employee, Employee> photoColumn;
    @FXML private TableColumn<Employee, String> nameColumn;
    @FXML private TableColumn<Employee, String> departmentColumn;
    @FXML private TableColumn<Employee, String> positionColumn;
    @FXML private TableColumn<Employee, String> phoneColumn;
    @FXML private TableColumn<Employee, String> salaryColumn;
    @FXML private TableColumn<Employee, Employee> statusColumn;
    @FXML private TableColumn<Employee, Employee> actionsColumn;

    @FXML private VBox cardsWrapper;
    @FXML private FlowPane cardsFlowPane;

    @FXML private Label paginationSummaryLabel;
    @FXML private Button prevPageButton;
    @FXML private HBox pageNumbersBox;
    @FXML private Button nextPageButton;

    // ---------------------------------------------------------------
    // State
    // ---------------------------------------------------------------

    private final ObservableList<Employee> masterList = FXCollections.observableArrayList();
    private FilteredList<Employee> filteredList;
    private int currentPage = 0;
    private int nextEmployeeSequence;

    @FXML
    public void initialize() {
        seedSampleData();
        filteredList = new FilteredList<>(masterList, e -> true);

        setupFilters();
        setupTableColumns();
        updateStatistics();
        applyFilters();
    }

    // ---------------------------------------------------------------
    // Sample data
    // ---------------------------------------------------------------

    private void seedSampleData() {
        Object[][] rows = {
                {"James Otieno", "Site Supervision", "Site Supervisor", "0712 345 678", 65000.0, Status.ACTIVE},
                {"Grace Wanjiru", "Administration", "HR Officer", "0722 456 789", 58000.0, Status.ACTIVE},
                {"Peter Kamau", "Electrical", "Electrician", "0733 567 890", 42000.0, Status.ACTIVE},
                {"Mary Achieng", "Finance", "Accountant", "0744 678 901", 70000.0, Status.ACTIVE},
                {"Samuel Njoroge", "Carpentry", "Carpenter", "0755 789 012", 38000.0, Status.ON_LEAVE},
                {"Faith Mutua", "Plumbing", "Plumber", "0766 890 123", 40000.0, Status.ACTIVE},
                {"David Kiprotich", "Site Supervision", "Foreman", "0777 901 234", 60000.0, Status.ACTIVE},
                {"Lucy Nyambura", "Administration", "Receptionist", "0788 012 345", 32000.0, Status.INACTIVE},
                {"John Mwangi", "Masonry", "Mason", "0799 123 456", 36000.0, Status.ACTIVE},
                {"Esther Chebet", "Logistics", "Logistics Officer", "0700 234 567", 45000.0, Status.ACTIVE},
                {"Brian Ochieng", "Electrical", "Electrician", "0711 345 678", 41000.0, Status.ON_LEAVE},
                {"Ann Wambui", "Finance", "Payroll Clerk", "0722 345 671", 48000.0, Status.ACTIVE},
                {"Kevin Mutiso", "Carpentry", "Carpenter", "0733 345 672", 37000.0, Status.ACTIVE},
                {"Diana Auma", "Plumbing", "Plumber Assistant", "0744 345 673", 30000.0, Status.ACTIVE},
                {"Patrick Kariuki", "Site Supervision", "Safety Officer", "0755 345 674", 52000.0, Status.ACTIVE},
                {"Josephine Adhiambo", "Administration", "Office Manager", "0766 345 675", 62000.0, Status.ACTIVE},
                {"Isaac Wafula", "Masonry", "Mason", "0777 345 676", 35000.0, Status.INACTIVE},
                {"Purity Nekesa", "Finance", "Finance Assistant", "0788 345 677", 43000.0, Status.ACTIVE},
                {"Moses Cheruiyot", "Logistics", "Driver", "0799 345 678", 34000.0, Status.ACTIVE},
                {"Rose Wairimu", "Electrical", "Electrical Assistant", "0700 345 679", 33000.0, Status.ON_LEAVE},
                {"Dennis Omondi", "Carpentry", "Carpentry Lead", "0711 345 671", 47000.0, Status.ACTIVE},
                {"Caroline Jepkosgei", "Site Supervision", "Site Supervisor", "0722 345 672", 64000.0, Status.ACTIVE},
                {"Hassan Abdi", "Plumbing", "Plumber", "0733 345 673", 39000.0, Status.ACTIVE},
                {"Naomi Chepkoech", "Administration", "HR Assistant", "0744 345 674", 36000.0, Status.ACTIVE},
        };

        int seq = 1;
        for (Object[] row : rows) {
            String id = String.format("EMP-%04d", 1000 + seq);
            masterList.add(new Employee(id, (String) row[0], (String) row[1], (String) row[2],
                    (String) row[3], (double) row[4], (Status) row[5]));
            seq++;
        }
        nextEmployeeSequence = seq;
    }

    private String generateNextId() {
        String id = String.format("EMP-%04d", 1000 + nextEmployeeSequence);
        nextEmployeeSequence++;
        return id;
    }

    // ---------------------------------------------------------------
    // Filters / search
    // ---------------------------------------------------------------

    private void setupFilters() {
        List<String> departments = new ArrayList<>();
        departments.add("All Departments");
        masterList.stream().map(Employee::getDepartment).distinct().sorted().forEach(departments::add);
        departmentFilter.getItems().setAll(departments);
        departmentFilter.setValue("All Departments");

        statusFilter.getItems().setAll("All Statuses", "Active", "On Leave", "Inactive");
        statusFilter.setValue("All Statuses");

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        departmentFilter.setOnAction(e -> applyFilters());
        statusFilter.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String dept = departmentFilter.getValue();
        String status = statusFilter.getValue();

        filteredList.setPredicate(emp -> {
            boolean matchesQuery = query.isEmpty()
                    || emp.getName().toLowerCase().contains(query)
                    || emp.getId().toLowerCase().contains(query);
            boolean matchesDept = dept == null || dept.equals("All Departments") || emp.getDepartment().equals(dept);
            boolean matchesStatus = status == null || status.equals("All Statuses")
                    || emp.getStatus().toString().equals(status);
            return matchesQuery && matchesDept && matchesStatus;
        });

        currentPage = 0;
        refreshCurrentPageView();
    }

    // ---------------------------------------------------------------
    // Table setup
    // ---------------------------------------------------------------

    private void setupTableColumns() {
        idColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getId()));
        nameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        departmentColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDepartment()));
        positionColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPosition()));
        phoneColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPhone()));
        salaryColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.format("KES %,.0f", data.getValue().getSalary())));

        photoColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Employee ignored, boolean empty) {
                super.updateItem(ignored, empty);
                Employee e = (getTableRow() != null) ? getTableRow().getItem() : null;
                if (empty || e == null) {
                    setGraphic(null);
                } else {
                    setGraphic(buildAvatar(e, 16));
                }
            }
        });

        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Employee ignored, boolean empty) {
                super.updateItem(ignored, empty);
                Employee e = (getTableRow() != null) ? getTableRow().getItem() : null;
                if (empty || e == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(e.getStatus().toString());
                    badge.getStyleClass().addAll("status-badge", e.getStatus().getStyleClass());
                    setGraphic(badge);
                }
            }
        });

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Employee ignored, boolean empty) {
                super.updateItem(ignored, empty);
                Employee e = (getTableRow() != null) ? getTableRow().getItem() : null;
                if (empty || e == null) {
                    setGraphic(null);
                } else {
                    setGraphic(buildRowActions(e));
                }
            }
        });
    }

    private StackPane buildAvatar(Employee e, double radius) {
        StackPane pane = new StackPane();
        Circle circle = new Circle(radius);
        circle.getStyleClass().add("avatar-circle-" + ((Math.abs(e.getId().hashCode()) % 4) + 1));
        Text text = new Text(e.getInitials());
        text.getStyleClass().add("avatar-initials");
        pane.getChildren().addAll(circle, text);
        return pane;
    }

    private HBox buildRowActions(Employee e) {
        Button view = smallActionButton("View", "row-action-view");
        Button edit = smallActionButton("Edit", "row-action-edit");
        Button delete = smallActionButton("Delete", "row-action-delete");
        view.setOnAction(ev -> openEmployeeForm(f -> f.configureForView(e), null));
        edit.setOnAction(ev -> openEmployeeForm(f -> f.configureForEdit(e), this::replaceEmployee));
        delete.setOnAction(ev -> showDeleteConfirmation(e));
        HBox box = new HBox(6, view, edit, delete);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return box;
    }

    private Button smallActionButton(String text, String styleClass) {
        Button b = new Button(text);
        b.getStyleClass().addAll("row-action-btn", styleClass);
        return b;
    }

    // ---------------------------------------------------------------
    // Card view
    // ---------------------------------------------------------------

    private VBox buildEmployeeCard(Employee e) {
        VBox card = new VBox(12);
        card.getStyleClass().add("employee-card");

        HBox top = new HBox(12);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        StackPane avatar = buildAvatar(e, 22);
        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(e.getName());
        nameLabel.getStyleClass().add("card-emp-name");
        Label posLabel = new Label(e.getPosition());
        posLabel.getStyleClass().add("card-emp-position");
        nameBox.getChildren().addAll(nameLabel, posLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label badge = new Label(e.getStatus().toString());
        badge.getStyleClass().addAll("status-badge", e.getStatus().getStyleClass());
        top.getChildren().addAll(avatar, nameBox, spacer, badge);

        Label deptLabel = new Label(e.getDepartment() + "  \u00B7  " + e.getPhone());
        deptLabel.getStyleClass().add("card-emp-meta");

        Label salaryLabel = new Label(String.format("KES %,.0f", e.getSalary()));
        salaryLabel.getStyleClass().add("card-emp-salary");

        HBox actions = buildRowActions(e);
        actions.getStyleClass().add("card-actions-row");

        card.getChildren().addAll(top, deptLabel, salaryLabel, actions);
        return card;
    }

    // ---------------------------------------------------------------
    // View toggle
    // ---------------------------------------------------------------

    @FXML
    private void handleShowTableView() {
        tableWrapper.setVisible(true);
        tableWrapper.setManaged(true);
        cardsWrapper.setVisible(false);
        cardsWrapper.setManaged(false);
    }

    @FXML
    private void handleShowCardView() {
        tableWrapper.setVisible(false);
        tableWrapper.setManaged(false);
        cardsWrapper.setVisible(true);
        cardsWrapper.setManaged(true);
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

        List<Employee> pageItems = total == 0 ? List.of() : filteredList.subList(from, to);

        employeeTable.setItems(FXCollections.observableArrayList(pageItems));

        cardsFlowPane.getChildren().clear();
        for (Employee e : pageItems) {
            cardsFlowPane.getChildren().add(buildEmployeeCard(e));
        }

        paginationSummaryLabel.setText(total == 0
                ? "No employees found"
                : String.format("Showing %d\u2013%d of %d", from + 1, to, total));

        prevPageButton.setDisable(currentPage == 0);
        nextPageButton.setDisable(currentPage >= totalPages - 1);

        pageNumbersBox.getChildren().clear();
        for (int i = 0; i < totalPages; i++) {
            int pageIndex = i;
            Button pageBtn = new Button(String.valueOf(i + 1));
            pageBtn.getStyleClass().add("pagination-page-btn");
            if (i == currentPage) {
                pageBtn.getStyleClass().add("pagination-page-btn-active");
            }
            pageBtn.setOnAction(e -> {
                currentPage = pageIndex;
                refreshCurrentPageView();
            });
            pageNumbersBox.getChildren().add(pageBtn);
        }
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            refreshCurrentPageView();
        }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages() - 1) {
            currentPage++;
            refreshCurrentPageView();
        }
    }

    // ---------------------------------------------------------------
    // Statistics
    // ---------------------------------------------------------------

    private void updateStatistics() {
        long active = masterList.stream().filter(e -> e.getStatus() == Status.ACTIVE).count();
        long onLeave = masterList.stream().filter(e -> e.getStatus() == Status.ON_LEAVE).count();
        long departments = masterList.stream().map(Employee::getDepartment).distinct().count();

        statTotalValue.setText(String.valueOf(masterList.size()));
        statActiveValue.setText(String.valueOf(active));
        statOnLeaveValue.setText(String.valueOf(onLeave));
        statDepartmentsValue.setText(String.valueOf(departments));
    }

    // ---------------------------------------------------------------
    // Add / Edit / View dialogs
    // ---------------------------------------------------------------

    @FXML
    private void handleAddEmployee() {
        openEmployeeForm(f -> f.configureForAdd(generateNextId()), this::addEmployee);
    }

    private void openEmployeeForm(Consumer<EmployeeFormController> configurer, Consumer<Employee> onSave) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/buildpay/view/EmployeeForm.fxml"));
            Parent formRoot = loader.load();
            EmployeeFormController controller = loader.getController();
            if (onSave != null) {
                controller.setOnSave(onSave);
            }
            configurer.accept(controller);

            Stage dialog = new Stage();
            dialog.initStyle(StageStyle.TRANSPARENT);
            dialog.initModality(Modality.APPLICATION_MODAL);
            Window owner = rootPane.getScene().getWindow();
            dialog.initOwner(owner);

            Scene scene = new Scene(formRoot);
            scene.setFill(Color.TRANSPARENT);
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load EmployeeForm.fxml", ex);
        }
    }

    private void addEmployee(Employee employee) {
        masterList.add(employee);
        setupFilters(); // department list may have gained a new value
        updateStatistics();
        applyFilters();
    }

    private void replaceEmployee(Employee updated) {
        for (int i = 0; i < masterList.size(); i++) {
            if (masterList.get(i).getId().equals(updated.getId())) {
                masterList.set(i, updated);
                break;
            }
        }
        updateStatistics();
        applyFilters();
    }

    // ---------------------------------------------------------------
    // Delete confirmation (custom modal, no default Alert chrome)
    // ---------------------------------------------------------------

    private void showDeleteConfirmation(Employee employee) {
        Label icon = new Label("\u26A0");
        icon.getStyleClass().add("confirm-icon");

        Label title = new Label("Delete Employee?");
        title.getStyleClass().add("confirm-title");

        Label message = new Label("This will permanently remove " + employee.getName()
                + " (" + employee.getId() + ") from BuildPay. This action cannot be undone.");
        message.getStyleClass().add("confirm-message");
        message.setWrapText(true);

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("form-cancel-btn");

        Button confirm = new Button("Delete");
        confirm.getStyleClass().add("confirm-delete-btn");

        HBox buttons = new HBox(12, cancel, confirm);
        buttons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        VBox card = new VBox(14, icon, title, message, buttons);
        card.getStyleClass().add("confirm-card");
        card.setMaxWidth(360);

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
        confirm.setOnAction(e -> {
            masterList.remove(employee);
            updateStatistics();
            applyFilters();
            dialog.close();
        });

        dialog.showAndWait();
    }

    // ---------------------------------------------------------------
    // Export / Print
    // ---------------------------------------------------------------

    @FXML
    private void handleExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Employees");
        chooser.setInitialFileName("buildpay-employees.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file == null) return;

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Employee ID,Name,Department,Position,Phone,Salary,Status\n");
            for (Employee e : filteredList) {
                writer.write(String.join(",",
                        escapeCsv(e.getId()), escapeCsv(e.getName()), escapeCsv(e.getDepartment()),
                        escapeCsv(e.getPosition()), escapeCsv(e.getPhone()),
                        String.valueOf(e.getSalary()), escapeCsv(e.getStatus().toString())
                ));
                writer.write("\n");
            }
            showInfo("Export Complete", "Employee data exported to " + file.getName() + ".");
        } catch (IOException ex) {
            showInfo("Export Failed", "Could not write the export file. Please try again.");
        }
    }

    private String escapeCsv(String value) {
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
            boolean success = job.printPage(employeeTable);
            if (success) {
                job.endJob();
            }
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
