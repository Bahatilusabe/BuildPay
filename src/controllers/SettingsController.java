package com.buildpay.controller;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Controls the Settings module: ten switchable sections (Company Information,
 * Payroll Settings, Tax Settings, Theme, Notifications, Backup, Restore,
 * Database, Users, Permissions).
 *
 * IMPORTANT: BuildPay has no shared persistence layer in this build, so
 * "Save" actions here update in-memory state and confirm to the user, but do
 * not write to a real database or propagate to other modules. Backup/Restore
 * and Database Test Connection are similarly self-contained demonstrations
 * (backup writes a real text file; restore reads it back; the DB test is
 * simulated since no live database is wired up). Wire these to your actual
 * backend before relying on them in production.
 */
public class SettingsController {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    // ---------------------------------------------------------------
    // Domain model (Users / Permissions)
    // ---------------------------------------------------------------

    public enum UserRole { ADMIN("Admin"), HR_MANAGER("HR Manager"), PAYROLL_OFFICER("Payroll Officer"), VIEWER("Viewer");
        private final String label;
        UserRole(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum UserStatus { ACTIVE("Active", "status-active"), INACTIVE("Inactive", "status-inactive");
        private final String label; private final String styleClass;
        UserStatus(String label, String styleClass) { this.label = label; this.styleClass = styleClass; }
        public String getStyleClass() { return styleClass; }
        @Override public String toString() { return label; }
    }

    public static class SystemUser {
        String name, email;
        UserRole role;
        UserStatus status;
        SystemUser(String name, String email, UserRole role, UserStatus status) {
            this.name = name; this.email = email; this.role = role; this.status = status;
        }
    }

    // ---------------------------------------------------------------
    // FXML fields
    // ---------------------------------------------------------------

    @FXML private VBox rootPane;
    @FXML private Button backButton;

    @FXML private StackPane contentStack;
    @FXML private VBox companyPanel, payrollPanel, taxPanel, themePanel, notificationsPanel,
            backupPanel, restorePanel, databasePanel, usersPanel, permissionsPanel;

    // Company
    @FXML private TextField companyNameField, companyPinField, companyAddressField, companyPhoneField, companyEmailField;

    // Payroll
    @FXML private TextField payDayField, overtimeMultiplierField, defaultHousingField, defaultTransportField;

    // Tax
    @FXML private TextField nssfTier1Field, nssfUelField, nssfRateField, payeReliefField;

    // Theme
    @FXML private StackPane darkModeTrack;
    @FXML private Circle darkModeKnob;
    @FXML private HBox accentSwatchRow;

    // Notifications
    @FXML private VBox notificationsList;

    // Backup
    @FXML private Label lastBackupLabel;
    @FXML private ComboBox<String> backupFrequencyCombo;

    // Restore
    @FXML private Label restoreFileLabel;

    // Database
    @FXML private TextField dbHostField, dbPortField, dbNameField;
    @FXML private Label dbStatusChip;

    // Users
    @FXML private Label usersCountLabel;
    @FXML private TableView<SystemUser> usersTable;
    @FXML private TableColumn<SystemUser, String> userNameColumn, userEmailColumn, userRoleColumn;
    @FXML private TableColumn<SystemUser, SystemUser> userStatusColumn, userActionsColumn;

    // Permissions
    @FXML private GridPane permissionsGrid;

    // ---------------------------------------------------------------
    // State
    // ---------------------------------------------------------------

    private boolean darkMode = false;
    private File selectedRestoreFile;
    private final List<SystemUser> systemUsers = new ArrayList<>();
    private final String[] permissionRows = {
            "View Employees", "Edit Employees", "Process Payroll", "Approve Payroll",
            "View Reports", "Manage Settings", "Manage Users"
    };
    private final Map<String, Map<UserRole, CheckBox>> permissionChecks = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        seedUsers();
        setupUsersTable();
        setupBackup();
        buildAccentSwatches();
        buildNotificationsList();
        buildPermissionsGrid();
    }

    // ---------------------------------------------------------------
    // Section navigation
    // ---------------------------------------------------------------

    private void showOnly(VBox panelToShow) {
        for (VBox panel : List.of(companyPanel, payrollPanel, taxPanel, themePanel, notificationsPanel,
                backupPanel, restorePanel, databasePanel, usersPanel, permissionsPanel)) {
            boolean show = panel == panelToShow;
            panel.setVisible(show);
            panel.setManaged(show);
        }
    }

    @FXML private void handleShowCompany() { showOnly(companyPanel); }
    @FXML private void handleShowPayroll() { showOnly(payrollPanel); }
    @FXML private void handleShowTax() { showOnly(taxPanel); }
    @FXML private void handleShowTheme() { showOnly(themePanel); }
    @FXML private void handleShowNotifications() { showOnly(notificationsPanel); }
    @FXML private void handleShowBackup() { showOnly(backupPanel); }
    @FXML private void handleShowRestore() { showOnly(restorePanel); }
    @FXML private void handleShowDatabase() { showOnly(databasePanel); }
    @FXML private void handleShowUsers() { showOnly(usersPanel); }
    @FXML private void handleShowPermissions() { showOnly(permissionsPanel); }

    // ---------------------------------------------------------------
    // Company / Payroll / Tax (save = confirm only, no persistence layer)
    // ---------------------------------------------------------------

    @FXML
    private void handleSaveCompany() {
        showInfo("Company Information Saved", "Company details have been updated for this session.");
    }

    @FXML
    private void handleSavePayroll() {
        showInfo("Payroll Settings Saved", "Payroll defaults have been updated for this session.");
    }

    @FXML
    private void handleSaveTax() {
        showInfo("Tax Settings Saved", "Statutory rates have been updated for this session.");
    }

    // ---------------------------------------------------------------
    // Theme
    // ---------------------------------------------------------------

    @FXML
    private void handleThemeToggle() {
        darkMode = !darkMode;
        TranslateTransition slide = new TranslateTransition(Duration.millis(160), darkModeKnob);
        slide.setToX(darkMode ? 17 : 3);
        slide.play();
        if (darkMode) {
            if (!rootPane.getStyleClass().contains("dark-theme")) rootPane.getStyleClass().add("dark-theme");
            darkModeTrack.getStyleClass().add("toggle-track-on");
        } else {
            rootPane.getStyleClass().remove("dark-theme");
            darkModeTrack.getStyleClass().remove("toggle-track-on");
        }
    }

    private void buildAccentSwatches() {
        String[] names = {"Blue", "Green", "Purple", "Orange"};
        String[] colors = {"#2E6BFF", "#12B886", "#7C5CFC", "#F5A524"};
        List<StackPane> swatches = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            StackPane swatch = new StackPane();
            swatch.getStyleClass().add("accent-swatch");
            swatch.setStyle("-fx-background-color:" + colors[i] + ";");
            if (i == 0) swatch.getStyleClass().add("accent-swatch-selected");
            swatch.setOnMouseClicked(e -> {
                for (StackPane s : swatches) s.getStyleClass().remove("accent-swatch-selected");
                swatch.getStyleClass().add("accent-swatch-selected");
            });
            swatches.add(swatch);
            accentSwatchRow.getChildren().add(swatch);
        }
    }

    // ---------------------------------------------------------------
    // Notifications
    // ---------------------------------------------------------------

    private void buildNotificationsList() {
        addNotificationToggle("Email Notifications", "Payroll and approval updates by email", true);
        addNotificationToggle("SMS Notifications", "Critical alerts sent by text message", false);
        addNotificationToggle("Payroll Reminders", "Remind admins when payroll is due", true);
        addNotificationToggle("Attendance Alerts", "Notify on absences and late arrivals", true);
        addNotificationToggle("System Updates", "News about new BuildPay features", false);
    }

    private void addNotificationToggle(String title, String desc, boolean initialOn) {
        HBox row = new HBox();
        row.getStyleClass().add("toggle-row");
        row.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("toggle-label");
        Label descLabel = new Label(desc);
        descLabel.getStyleClass().add("toggle-desc");
        textBox.getChildren().addAll(titleLabel, descLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane track = new StackPane();
        track.getStyleClass().add("toggle-track");
        Circle knob = new Circle(9);
        knob.getStyleClass().add("toggle-knob");
        knob.setTranslateX(initialOn ? 17 : 3);
        track.getChildren().add(knob);
        track.setAlignment(Pos.CENTER_LEFT);
        boolean[] state = {initialOn};
        if (initialOn) track.getStyleClass().add("toggle-track-on");

        track.setOnMouseClicked(e -> {
            state[0] = !state[0];
            TranslateTransition slide = new TranslateTransition(Duration.millis(160), knob);
            slide.setToX(state[0] ? 17 : 3);
            slide.play();
            if (state[0]) track.getStyleClass().add("toggle-track-on");
            else track.getStyleClass().remove("toggle-track-on");
        });

        row.getChildren().addAll(textBox, spacer, track);
        notificationsList.getChildren().add(row);
    }

    // ---------------------------------------------------------------
    // Backup
    // ---------------------------------------------------------------

    private void setupBackup() {
        backupFrequencyCombo.getItems().setAll("Daily", "Weekly", "Monthly");
        backupFrequencyCombo.setValue("Weekly");
    }

    @FXML
    private void handleBackupNow() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Backup");
        chooser.setInitialFileName("buildpay-backup-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Backup Files", "*.txt"));
        File file = chooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file == null) return;

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("BUILDPAY_BACKUP_V1\n");
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FMT) + "\n\n");
            writer.write("Company Name=" + companyNameField.getText() + "\n");
            writer.write("KRA PIN=" + companyPinField.getText() + "\n");
            writer.write("Address=" + companyAddressField.getText() + "\n");
            writer.write("Phone=" + companyPhoneField.getText() + "\n");
            writer.write("Email=" + companyEmailField.getText() + "\n");
            writer.write("Pay Day=" + payDayField.getText() + "\n");
            writer.write("NSSF Tier1=" + nssfTier1Field.getText() + "\n");
            writer.write("NSSF UEL=" + nssfUelField.getText() + "\n");
            writer.write("PAYE Relief=" + payeReliefField.getText() + "\n");
            lastBackupLabel.setText(LocalDateTime.now().format(TIMESTAMP_FMT));
            showInfo("Backup Complete", "Backup saved to " + file.getName() + ".");
        } catch (IOException ex) {
            showInfo("Backup Failed", "Could not write the backup file. Please try again.");
        }
    }

    // ---------------------------------------------------------------
    // Restore
    // ---------------------------------------------------------------

    @FXML
    private void handleChooseBackupFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Backup File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Backup Files", "*.txt"));
        File file = chooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file == null) return;
        selectedRestoreFile = file;
        restoreFileLabel.setText(file.getName());
    }

    @FXML
    private void handleRestore() {
        if (selectedRestoreFile == null) {
            showInfo("No File Selected", "Choose a backup file before restoring.");
            return;
        }
        showConfirm("Restore Data?",
                "This will overwrite the current settings shown on this screen with the contents of "
                        + selectedRestoreFile.getName() + ". This cannot be undone.",
                "Restore",
                () -> {
                    try {
                        List<String> lines = Files.readAllLines(selectedRestoreFile.toPath());
                        if (lines.isEmpty() || !lines.get(0).startsWith("BUILDPAY_BACKUP")) {
                            showInfo("Restore Failed", "This file doesn't look like a valid BuildPay backup.");
                            return;
                        }
                        for (String line : lines) {
                            applyRestoreLine(line);
                        }
                        showInfo("Restore Complete", "Settings restored from " + selectedRestoreFile.getName() + ".");
                    } catch (IOException ex) {
                        showInfo("Restore Failed", "Could not read the backup file.");
                    }
                });
    }

    private void applyRestoreLine(String line) {
        if (!line.contains("=")) return;
        String[] parts = line.split("=", 2);
        String key = parts[0].trim();
        String value = parts[1].trim();
        switch (key) {
            case "Company Name" -> companyNameField.setText(value);
            case "KRA PIN" -> companyPinField.setText(value);
            case "Address" -> companyAddressField.setText(value);
            case "Phone" -> companyPhoneField.setText(value);
            case "Email" -> companyEmailField.setText(value);
            case "Pay Day" -> payDayField.setText(value);
            case "NSSF Tier1" -> nssfTier1Field.setText(value);
            case "NSSF UEL" -> nssfUelField.setText(value);
            case "PAYE Relief" -> payeReliefField.setText(value);
            default -> { /* unrecognized key, ignore */ }
        }
    }

    // ---------------------------------------------------------------
    // Database (simulated — no live connection in this build)
    // ---------------------------------------------------------------

    @FXML
    private void handleTestConnection() {
        dbStatusChip.setText("Testing...");
        dbStatusChip.getStyleClass().removeAll("status-connected", "status-failed", "status-unknown");
        dbStatusChip.getStyleClass().add("status-unknown");

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(700));
        pause.setOnFinished(e -> {
            boolean hostLooksValid = !dbHostField.getText().isBlank() && !dbPortField.getText().isBlank();
            dbStatusChip.getStyleClass().removeAll("status-connected", "status-failed", "status-unknown");
            if (hostLooksValid) {
                dbStatusChip.setText("Connected");
                dbStatusChip.getStyleClass().add("status-connected");
            } else {
                dbStatusChip.setText("Connection Failed");
                dbStatusChip.getStyleClass().add("status-failed");
            }
        });
        pause.play();
    }

    // ---------------------------------------------------------------
    // Users
    // ---------------------------------------------------------------

    private void seedUsers() {
        systemUsers.add(new SystemUser("Admin User", "admin@buildpay.co.ke", UserRole.ADMIN, UserStatus.ACTIVE));
        systemUsers.add(new SystemUser("Grace Wanjiru", "grace.wanjiru@buildpay.co.ke", UserRole.HR_MANAGER, UserStatus.ACTIVE));
        systemUsers.add(new SystemUser("Mary Achieng", "mary.achieng@buildpay.co.ke", UserRole.PAYROLL_OFFICER, UserStatus.ACTIVE));
        systemUsers.add(new SystemUser("James Otieno", "james.otieno@buildpay.co.ke", UserRole.VIEWER, UserStatus.INACTIVE));
    }

    private void setupUsersTable() {
        userNameColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().name));
        userEmailColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().email));
        userRoleColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().role.toString()));

        userStatusColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(SystemUser ignored, boolean empty) {
                super.updateItem(ignored, empty);
                SystemUser u = (getTableRow() != null) ? getTableRow().getItem() : null;
                if (empty || u == null) { setGraphic(null); return; }
                Label chip = new Label(u.status.toString());
                chip.getStyleClass().addAll("status-chip", u.status.getStyleClass());
                setGraphic(chip);
            }
        });

        userActionsColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(SystemUser ignored, boolean empty) {
                super.updateItem(ignored, empty);
                SystemUser u = (getTableRow() != null) ? getTableRow().getItem() : null;
                if (empty || u == null) { setGraphic(null); return; }
                Button edit = new Button("Edit");
                edit.getStyleClass().add("row-action-btn");
                edit.setOnAction(e -> showUserForm(u));
                Button delete = new Button("Delete");
                delete.getStyleClass().addAll("row-action-btn", "row-action-delete");
                delete.setOnAction(e -> showConfirm("Remove User?",
                        "This will remove " + u.name + "'s access to BuildPay.", "Remove",
                        () -> { systemUsers.remove(u); refreshUsersTable(); }));
                HBox box = new HBox(6, edit, delete);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        refreshUsersTable();
    }

    private void refreshUsersTable() {
        usersTable.setItems(javafx.collections.FXCollections.observableArrayList(systemUsers));
        usersCountLabel.setText(systemUsers.size() + (systemUsers.size() == 1 ? " user" : " users"));
    }

    @FXML
    private void handleAddUser() {
        showUserForm(null);
    }

    private void showUserForm(SystemUser existing) {
        boolean editMode = existing != null;

        Label title = new Label(editMode ? "Edit User" : "Add User");
        title.getStyleClass().add("form-title");

        Label nameLabel = new Label("Full Name");
        nameLabel.getStyleClass().add("field-label");
        TextField nameField = new TextField(editMode ? existing.name : "");
        nameField.getStyleClass().add("field");

        Label emailLabel = new Label("Email");
        emailLabel.getStyleClass().add("field-label");
        TextField emailField = new TextField(editMode ? existing.email : "");
        emailField.getStyleClass().add("field");

        Label roleLabel = new Label("Role");
        roleLabel.getStyleClass().add("field-label");
        ComboBox<UserRole> roleCombo = new ComboBox<>();
        roleCombo.getItems().setAll(UserRole.values());
        roleCombo.setValue(editMode ? existing.role : UserRole.VIEWER);
        roleCombo.getStyleClass().add("combo");
        roleCombo.setMaxWidth(Double.MAX_VALUE);

        Label statusLabel = new Label("Status");
        statusLabel.getStyleClass().add("field-label");
        ComboBox<UserStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().setAll(UserStatus.values());
        statusCombo.setValue(editMode ? existing.status : UserStatus.ACTIVE);
        statusCombo.getStyleClass().add("combo");
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("secondary-btn");
        Button save = new Button(editMode ? "Update User" : "Add User");
        save.getStyleClass().add("save-btn");
        HBox buttons = new HBox(12, cancel, save);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(14, title,
                new VBox(6, nameLabel, nameField),
                new VBox(6, emailLabel, emailField),
                new VBox(6, roleLabel, roleCombo),
                new VBox(6, statusLabel, statusCombo),
                buttons);
        card.getStyleClass().add("settings-card");
        card.setMaxWidth(380);
        card.setMinWidth(380);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("dialog-root");

        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(rootPane.getScene().getWindow());
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        cancel.setOnAction(e -> dialog.close());
        save.setOnAction(e -> {
            if (nameField.getText().isBlank() || emailField.getText().isBlank()) return;
            if (editMode) {
                existing.name = nameField.getText().trim();
                existing.email = emailField.getText().trim();
                existing.role = roleCombo.getValue();
                existing.status = statusCombo.getValue();
            } else {
                systemUsers.add(new SystemUser(nameField.getText().trim(), emailField.getText().trim(),
                        roleCombo.getValue(), statusCombo.getValue()));
            }
            refreshUsersTable();
            dialog.close();
        });

        dialog.showAndWait();
    }

    // ---------------------------------------------------------------
    // Permissions
    // ---------------------------------------------------------------

    private void buildPermissionsGrid() {
        UserRole[] roles = UserRole.values();

        permissionsGrid.getColumnConstraints().add(new ColumnConstraints(180));
        for (int i = 0; i < roles.length; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / roles.length * 0.7);
            permissionsGrid.getColumnConstraints().add(cc);
        }

        for (int c = 0; c < roles.length; c++) {
            Label roleHeader = new Label(roles[c].toString());
            roleHeader.getStyleClass().add("permission-role-header");
            permissionsGrid.add(roleHeader, c + 1, 0);
        }

        boolean[][] defaults = {
                {true, true, true, true},   // View Employees
                {true, true, false, false}, // Edit Employees
                {true, false, true, false}, // Process Payroll
                {true, false, true, false}, // Approve Payroll
                {true, true, true, true},   // View Reports
                {true, false, false, false},// Manage Settings
                {true, false, false, false},// Manage Users
        };

        for (int r = 0; r < permissionRows.length; r++) {
            Label rowLabel = new Label(permissionRows[r]);
            rowLabel.getStyleClass().add("permission-row-label");
            permissionsGrid.add(rowLabel, 0, r + 1);

            Map<UserRole, CheckBox> rowChecks = new LinkedHashMap<>();
            for (int c = 0; c < roles.length; c++) {
                CheckBox cb = new CheckBox();
                cb.getStyleClass().add("permission-checkbox");
                cb.setSelected(defaults[r][c]);
                permissionsGrid.add(cb, c + 1, r + 1);
                rowChecks.put(roles[c], cb);
            }
            permissionChecks.put(permissionRows[r], rowChecks);
        }
    }

    @FXML
    private void handleSavePermissions() {
        showInfo("Permissions Saved", "Role permissions have been updated for this session.");
    }

    // ---------------------------------------------------------------
    // Shared dialogs
    // ---------------------------------------------------------------

    private void showConfirm(String title, String message, String confirmLabel, Runnable onConfirm) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("confirm-title");
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("confirm-message");
        messageLabel.setWrapText(true);

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("secondary-btn");
        Button confirm = new Button(confirmLabel);
        confirm.getStyleClass().add("danger-btn");
        HBox buttons = new HBox(12, cancel, confirm);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(14, titleLabel, messageLabel, buttons);
        card.getStyleClass().add("confirm-card");
        card.setMaxWidth(380);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("dialog-root");

        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(rootPane.getScene().getWindow());
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        cancel.setOnAction(e -> dialog.close());
        confirm.setOnAction(e -> { onConfirm.run(); dialog.close(); });

        dialog.showAndWait();
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
