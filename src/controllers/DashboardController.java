package com.buildpay.controller;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.WritableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;

/**
 * Controls the BuildPay Dashboard: populates statistics, draws the payroll
 * trend and attendance doughnut charts on canvas, animates cards and the
 * theme switch, and handles sidebar / quick-action navigation.
 */
public class DashboardController {

    // ---- Sample data (replace with real service calls) ----------------
    private static final String[] PAYROLL_MONTHS = {"Feb", "Mar", "Apr", "May", "Jun", "Jul"};
    private static final double[] PAYROLL_VALUES = {3.1, 3.4, 3.6, 3.9, 4.0, 4.2}; // in millions
    private static final double ATTENDANCE_PRESENT = 0.82;
    private static final double ATTENDANCE_LATE = 0.11;
    private static final double ATTENDANCE_ABSENT = 0.07;
    private static final double[] PROJECT_COST_RATIOS = {0.92, 0.61, 0.43, 0.27}; // relative bar fill

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private VBox navContainer;
    @FXML private ToggleButton navDashboardBtn;
    @FXML private ToggleButton navEmployeesBtn;
    @FXML private ToggleButton navAttendanceBtn;
    @FXML private ToggleButton navPayrollBtn;
    @FXML private ToggleButton navReportsBtn;

    @FXML private StackPane themeToggleTrack;
    @FXML private Circle themeToggleKnob;

    @FXML private VBox userCard;
    @FXML private Label userCardName;
    @FXML private Label userCardRole;

    @FXML private HBox headerBar;
    @FXML private Label welcomeLabel;
    @FXML private HBox skylineBox;
    @FXML private ComboBox<String> monthSelector;
    @FXML private StackPane notificationBell;
    @FXML private Label notificationBadge;
    @FXML private HBox profileWidget;

    @FXML private HBox statsRow;
    @FXML private VBox totalEmployeesCard;
    @FXML private VBox projectsCard;
    @FXML private VBox payrollCard;
    @FXML private VBox pendingPayslipsCard;
    @FXML private Label totalEmployeesValue;
    @FXML private Label projectsValue;
    @FXML private Label payrollValue;
    @FXML private Label pendingPayslipsValue;
    @FXML private Label totalEmployeesTrend;
    @FXML private Label projectsTrend;
    @FXML private Label payrollTrend;
    @FXML private Label pendingPayslipsTrend;

    @FXML private HBox chartsRow;
    @FXML private VBox payrollChartCard;
    @FXML private Canvas payrollCanvas;
    @FXML private VBox attendanceChartCard;
    @FXML private Canvas attendanceCanvas;
    @FXML private Label attendanceCenterValue;
    @FXML private VBox attendanceLegend;
    @FXML private Label legendPresentValue;
    @FXML private Label legendLateValue;
    @FXML private Label legendAbsentValue;

    @FXML private HBox bottomRow;
    @FXML private VBox activitiesCard;
    @FXML private VBox activitiesList;
    @FXML private VBox projectCostCard;
    @FXML private VBox projectCostList;
    @FXML private Region costFill1;
    @FXML private Region costFill2;
    @FXML private Region costFill3;
    @FXML private Region costFill4;
    @FXML private VBox quickActionsCard;
    @FXML private Button addEmployeeBtn;
    @FXML private Button attendanceBtn;
    @FXML private Button payrollBtn;
    @FXML private Button reportsBtn;

    private boolean darkMode = false;

    @FXML
    public void initialize() {
        setupMonthSelector();
        setupNavHoverGuards();
        setupStatCardHoverAnimations();
        setupQuickActionHoverAnimations();
        animateStatValues();
        drawPayrollChart();
        drawAttendanceChart();
        animateCostBars();
        Platform.runLater(this::playEntrance);
    }

    // ---------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------

    private void setupMonthSelector() {
        monthSelector.getItems().setAll(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        );
        monthSelector.setValue("July");
        monthSelector.setOnAction(e -> {
            // TODO: reload statistics / charts for the selected month.
        });
    }

    private void setupNavHoverGuards() {
        // Prevent the active nav item from being toggled off by a repeat click.
        for (ToggleButton btn : List.of(navDashboardBtn, navEmployeesBtn, navAttendanceBtn, navPayrollBtn, navReportsBtn)) {
            btn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                if (btn.isSelected()) {
                    btn.setSelected(true);
                }
            });
        }
    }

    private void setupStatCardHoverAnimations() {
        for (VBox card : List.of(totalEmployeesCard, projectsCard, payrollCard, pendingPayslipsCard)) {
            addLiftOnHover(card, -6, 1.02);
        }
    }

    private void setupQuickActionHoverAnimations() {
        for (Button btn : List.of(addEmployeeBtn, attendanceBtn, payrollBtn, reportsBtn)) {
            ScaleTransition grow = new ScaleTransition(Duration.millis(120), btn);
            grow.setToX(1.04);
            grow.setToY(1.04);
            ScaleTransition shrink = new ScaleTransition(Duration.millis(120), btn);
            shrink.setToX(1.0);
            shrink.setToY(1.0);
            btn.setOnMouseEntered(e -> {
                btn.setCursor(Cursor.HAND);
                grow.stop();
                grow.playFromStart();
            });
            btn.setOnMouseExited(e -> {
                shrink.stop();
                shrink.playFromStart();
            });
        }
    }

    /** Lifts a card slightly and deepens its shadow on hover, for a tactile SaaS feel. */
    private void addLiftOnHover(Node card, double liftY, double scale) {
        TranslateTransition up = new TranslateTransition(Duration.millis(150), card);
        up.setToY(liftY);
        ScaleTransition grow = new ScaleTransition(Duration.millis(150), card);
        grow.setToX(scale);
        grow.setToY(scale);

        TranslateTransition down = new TranslateTransition(Duration.millis(150), card);
        down.setToY(0);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(150), card);
        shrink.setToX(1.0);
        shrink.setToY(1.0);

        card.setOnMouseEntered(e -> {
            card.setCursor(Cursor.HAND);
            up.stop(); grow.stop();
            up.playFromStart(); grow.playFromStart();
        });
        card.setOnMouseExited(e -> {
            down.stop(); shrink.stop();
            down.playFromStart(); shrink.playFromStart();
        });
    }

    private void playEntrance() {
        FadeTransition fade = new FadeTransition(Duration.millis(400), rootPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    // ---------------------------------------------------------------
    // Statistics count-up animation
    // ---------------------------------------------------------------

    private void animateStatValues() {
        animateCount(totalEmployeesValue, 0, 248, v -> String.valueOf((int) v));
        animateCount(projectsValue, 0, 18, v -> String.valueOf((int) v));
        animateCount(payrollValue, 0, 4.2, v -> String.format("KES %.1fM", v));
        animateCount(pendingPayslipsValue, 0, 12, v -> String.valueOf((int) v));
    }

    private void animateCount(Label label, double from, double to, java.util.function.DoubleFunction<String> formatter) {
        WritableValue<Double> proxy = new javafx.beans.property.SimpleDoubleProperty(from);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(proxy, from)),
                new KeyFrame(Duration.millis(900), e -> label.setText(formatter.apply(to)),
                        new KeyValue(proxy, to))
        );
        // Update label text on every frame for a smooth count-up rather than a single jump.
        timeline.currentTimeProperty().addListener((obs, oldT, newT) -> {
            double fraction = Math.min(1.0, newT.toMillis() / 900.0);
            double current = from + (to - from) * fraction;
            label.setText(formatter.apply(current));
        });
        timeline.play();
    }

    // ---------------------------------------------------------------
    // Charts (Canvas-drawn, consistent with the rest of BuildPay)
    // ---------------------------------------------------------------

    private void drawPayrollChart() {
        GraphicsContext gc = payrollCanvas.getGraphicsContext2D();
        double w = payrollCanvas.getWidth();
        double h = payrollCanvas.getHeight();
        double paddingLeft = 40;
        double paddingBottom = 28;
        double paddingTop = 12;
        double chartW = w - paddingLeft - 12;
        double chartH = h - paddingBottom - paddingTop;

        gc.clearRect(0, 0, w, h);

        double max = 5.0;
        // Horizontal gridlines
        gc.setStroke(Color.web("#E7ECF5"));
        gc.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double y = paddingTop + chartH - (chartH * i / 4.0);
            gc.strokeLine(paddingLeft, y, w - 12, y);
            gc.setFill(Color.web("#9AA6BF"));
            gc.setFont(Font.font("Inter", 10));
            gc.fillText(String.format("%.1fM", max * i / 4.0), 4, y + 3);
        }

        int n = PAYROLL_VALUES.length;
        double stepX = chartW / (n - 1);

        // Filled area under the line
        gc.setFill(Color.web("#2E6BFF", 0.12));
        gc.beginPath();
        gc.moveTo(paddingLeft, paddingTop + chartH);
        for (int i = 0; i < n; i++) {
            double x = paddingLeft + stepX * i;
            double y = paddingTop + chartH - (chartH * (PAYROLL_VALUES[i] / max));
            gc.lineTo(x, y);
        }
        gc.lineTo(paddingLeft + stepX * (n - 1), paddingTop + chartH);
        gc.closePath();
        gc.fill();

        // Line
        gc.setStroke(Color.web("#2E6BFF"));
        gc.setLineWidth(2.5);
        gc.beginPath();
        for (int i = 0; i < n; i++) {
            double x = paddingLeft + stepX * i;
            double y = paddingTop + chartH - (chartH * (PAYROLL_VALUES[i] / max));
            if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
        }
        gc.stroke();

        // Points + month labels
        gc.setFont(Font.font("Inter", 11));
        for (int i = 0; i < n; i++) {
            double x = paddingLeft + stepX * i;
            double y = paddingTop + chartH - (chartH * (PAYROLL_VALUES[i] / max));

            gc.setFill(Color.web("#FFFFFF"));
            gc.fillOval(x - 4, y - 4, 8, 8);
            gc.setStroke(Color.web("#2E6BFF"));
            gc.setLineWidth(2);
            gc.strokeOval(x - 4, y - 4, 8, 8);

            gc.setFill(Color.web("#6B7A99"));
            gc.fillText(PAYROLL_MONTHS[i], x - 10, h - 8);
        }
    }

    private void drawAttendanceChart() {
        GraphicsContext gc = attendanceCanvas.getGraphicsContext2D();
        double w = attendanceCanvas.getWidth();
        double h = attendanceCanvas.getHeight();
        double centerX = w / 2;
        double centerY = h / 2;
        double outerRadius = Math.min(w, h) / 2 - 6;
        double innerRadius = outerRadius * 0.62;

        gc.clearRect(0, 0, w, h);

        double presentAngle = ATTENDANCE_PRESENT * 360.0;
        double lateAngle = ATTENDANCE_LATE * 360.0;
        double absentAngle = ATTENDANCE_ABSENT * 360.0;

        double start = 90;
        start = drawDoughnutSegment(gc, centerX, centerY, outerRadius, innerRadius, start, presentAngle, Color.web("#2E6BFF"));
        start = drawDoughnutSegment(gc, centerX, centerY, outerRadius, innerRadius, start, lateAngle, Color.web("#F5A524"));
        drawDoughnutSegment(gc, centerX, centerY, outerRadius, innerRadius, start, absentAngle, Color.web("#E5484D"));

        attendanceCenterValue.setText(String.format("%.0f%%", ATTENDANCE_PRESENT * 100));
        legendPresentValue.setText(String.format("%.0f%%", ATTENDANCE_PRESENT * 100));
        legendLateValue.setText(String.format("%.0f%%", ATTENDANCE_LATE * 100));
        legendAbsentValue.setText(String.format("%.0f%%", ATTENDANCE_ABSENT * 100));
    }

    /** Draws one doughnut wedge as a ring (outer arc forward, inner arc backward) and returns the next start angle. */
    private double drawDoughnutSegment(GraphicsContext gc, double cx, double cy, double outerR, double innerR,
                                        double startAngleDeg, double sweepDeg, Color color) {
        gc.setFill(color);
        gc.beginPath();
        gc.arc(cx, cy, outerR, outerR, startAngleDeg, -sweepDeg);
        double endAngleDeg = startAngleDeg - sweepDeg;
        gc.arc(cx, cy, innerR, innerR, endAngleDeg, sweepDeg);
        gc.closePath();
        gc.fill();
        return endAngleDeg;
    }

    // ---------------------------------------------------------------
    // Project cost bars
    // ---------------------------------------------------------------

    private void animateCostBars() {
        Region[] fills = {costFill1, costFill2, costFill3, costFill4};
        double trackWidth = 300; // matches .cost-track pref width in CSS

        Platform.runLater(() -> {
            for (int i = 0; i < fills.length; i++) {
                Region fill = fills[i];
                double targetWidth = trackWidth * PROJECT_COST_RATIOS[i];
                Timeline tl = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(fill.maxWidthProperty(), 0)),
                        new KeyFrame(Duration.millis(700 + i * 100L),
                                new KeyValue(fill.maxWidthProperty(), targetWidth, javafx.animation.Interpolator.EASE_OUT))
                );
                tl.play();
            }
        });
    }

    // ---------------------------------------------------------------
    // Theme switch
    // ---------------------------------------------------------------

    @FXML
    private void handleThemeToggle() {
        darkMode = !darkMode;
        TranslateTransition slide = new TranslateTransition(Duration.millis(160), themeToggleKnob);
        slide.setToX(darkMode ? 17 : 3);
        slide.play();

        if (darkMode) {
            if (!rootPane.getStyleClass().contains("dark-theme")) {
                rootPane.getStyleClass().add("dark-theme");
            }
            themeToggleTrack.getStyleClass().add("theme-toggle-track-on");
        } else {
            rootPane.getStyleClass().remove("dark-theme");
            themeToggleTrack.getStyleClass().remove("theme-toggle-track-on");
        }
    }

    // ---------------------------------------------------------------
    // Header actions
    // ---------------------------------------------------------------

    @FXML
    private void handleNotifications() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText(null);
        alert.setContentText(
                "3 new notifications:\n" +
                "- Payroll for July is ready to review\n" +
                "- 2 attendance records flagged for approval"
        );
        alert.showAndWait();
    }

    @FXML
    private void handleProfileMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem profileItem = new MenuItem("View Profile");
        MenuItem settingsItem = new MenuItem("Settings");
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> logout());
        menu.getItems().addAll(profileItem, settingsItem, logoutItem);
        menu.show(profileWidget, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    private void logout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/buildpay/view/Login.fxml"));
            Parent loginRoot = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene loginScene = new Scene(loginRoot, stage.getScene().getWidth(), stage.getScene().getHeight());
            stage.setScene(loginScene);
            stage.centerOnScreen();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load Login.fxml", ex);
        }
    }

    // ---------------------------------------------------------------
    // Navigation (sidebar + quick actions)
    // ---------------------------------------------------------------

    @FXML
    private void handleNavDashboard() {
        // Already on the Dashboard — nothing to load.
    }

    @FXML
    private void handleNavEmployees() {
        navigateTo("/com/buildpay/view/Employees.fxml", navEmployeesBtn);
    }

    @FXML
    private void handleNavAttendance() {
        navigateTo("/com/buildpay/view/Attendance.fxml", navAttendanceBtn);
    }

    @FXML
    private void handleNavPayroll() {
        navigateTo("/com/buildpay/view/Payroll.fxml", navPayrollBtn);
    }

    @FXML
    private void handleNavReports() {
        navigateTo("/com/buildpay/view/Reports.fxml", navReportsBtn);
    }

    @FXML
    private void handleAddEmployee() {
        navigateTo("/com/buildpay/view/Employees.fxml", navEmployeesBtn);
    }

    /**
     * Loads another BuildPay screen on the same stage. If that screen hasn't been
     * built yet, shows a friendly placeholder instead of crashing, and restores the
     * Dashboard nav item as selected.
     */
    private void navigateTo(String fxmlPath, ToggleButton correspondingNavBtn) {
        var resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Coming Soon");
            alert.setHeaderText(null);
            alert.setContentText("This section is under construction.");
            alert.showAndWait();
            navDashboardBtn.setSelected(true);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Parent nextRoot = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene nextScene = new Scene(nextRoot, stage.getScene().getWidth(), stage.getScene().getHeight());
            stage.setScene(nextScene);
            stage.centerOnScreen();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load " + fxmlPath, ex);
        }
    }
}
