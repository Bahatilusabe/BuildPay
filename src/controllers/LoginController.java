package com.buildpay.controller;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controls the BuildPay Login screen: field validation, keyboard shortcuts,
 * entrance / feedback animations, and navigation to the Dashboard on success.
 */
public class LoginController {

    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final double CARD_RADIUS = 28.0;

    @FXML private StackPane rootPane;
    @FXML private HBox loginCard;
    @FXML private StackPane leftPanel;
    @FXML private VBox leftContent;
    @FXML private VBox rightPanel;
    @FXML private VBox formBox;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label usernameErrorLabel;
    @FXML private Label passwordErrorLabel;
    @FXML private CheckBox rememberMeCheck;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Button signInButton;
    @FXML private Button adminLoginButton;
    @FXML private Label footerLabel;

    @FXML
    public void initialize() {
        clipCardCorners();
        prepareInitialState();
        playEntranceAnimation();
        attachFieldValidationListeners();
        attachHoverAnimations();
        Platform.runLater(this::registerKeyboardShortcuts);
    }

    // ---------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------

    /** Clips the login card to a 28px rounded rectangle so both panels share one silhouette. */
    private void clipCardCorners() {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(CARD_RADIUS * 2);
        clip.setArcHeight(CARD_RADIUS * 2);
        clip.widthProperty().bind(loginCard.widthProperty());
        clip.heightProperty().bind(loginCard.heightProperty());
        loginCard.setClip(clip);
    }

    private void prepareInitialState() {
        leftContent.setOpacity(0);
        formBox.setOpacity(0);
        formBox.setTranslateY(16);
    }

    private void playEntranceAnimation() {
        FadeTransition leftFade = new FadeTransition(Duration.millis(500), leftContent);
        leftFade.setFromValue(0);
        leftFade.setToValue(1);

        FadeTransition formFade = new FadeTransition(Duration.millis(500), formBox);
        formFade.setFromValue(0);
        formFade.setToValue(1);

        TranslateTransition formSlide = new TranslateTransition(Duration.millis(500), formBox);
        formSlide.setFromY(16);
        formSlide.setToY(0);
        formSlide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition formIntro = new ParallelTransition(formFade, formSlide);
        formIntro.setDelay(Duration.millis(150));

        new ParallelTransition(leftFade, formIntro).play();
    }

    /** Enter submits, Escape clears the form, Ctrl+Shift+A jumps straight to the admin quick-login. */
    private void registerKeyboardShortcuts() {
        Scene scene = rootPane.getScene();
        if (scene == null) {
            return;
        }
        scene.getAccelerators().put(
                KeyCombination.keyCombination("ESCAPE"),
                this::clearForm
        );
        scene.getAccelerators().put(
                KeyCombination.keyCombination("Ctrl+Shift+A"),
                this::handleAdminLogin
        );
        // Password field submits on Enter in addition to the button's defaultButton behavior.
        passwordField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode().toString().equals("ENTER")) {
                handleSignIn();
            }
        });
    }

    private void attachHoverAnimations() {
        addHoverScale(signInButton);
        addHoverScale(adminLoginButton);
    }

    private void addHoverScale(Node node) {
        ScaleTransition grow = new ScaleTransition(Duration.millis(120), node);
        grow.setToX(1.03);
        grow.setToY(1.03);

        ScaleTransition shrink = new ScaleTransition(Duration.millis(120), node);
        shrink.setToX(1.0);
        shrink.setToY(1.0);

        node.setOnMouseEntered(e -> {
            grow.stop();
            grow.playFromStart();
            node.setCursor(Cursor.HAND);
        });
        node.setOnMouseExited(e -> {
            shrink.stop();
            shrink.playFromStart();
        });
    }

    /** Clears inline errors as the user edits so mistakes don't stay flagged after correction. */
    private void attachFieldValidationListeners() {
        usernameField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.isBlank()) {
                setFieldError(usernameField, usernameErrorLabel, false);
            }
        });
        passwordField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.length() >= MIN_PASSWORD_LENGTH) {
                setFieldError(passwordField, passwordErrorLabel, false);
            }
        });
    }

    // ---------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------

    @FXML
    private void handleSignIn() {
        boolean valid = validateForm();
        if (!valid) {
            shake(formBox);
            return;
        }
        // TODO: replace with real authentication against the payroll backend.
        navigateToDashboard();
    }

    @FXML
    private void handleAdminLogin() {
        // Quick-access path for demo / admin sessions — bypasses field validation.
        usernameField.setText("admin");
        navigateToDashboard();
    }

    @FXML
    private void handleForgotPassword() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Forgot Password");
        alert.setHeaderText(null);
        alert.setContentText("Please contact your BuildPay administrator to reset your password.");
        alert.showAndWait();
    }

    private void clearForm() {
        usernameField.clear();
        passwordField.clear();
        rememberMeCheck.setSelected(false);
        setFieldError(usernameField, usernameErrorLabel, false);
        setFieldError(passwordField, passwordErrorLabel, false);
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    private boolean validateForm() {
        boolean usernameValid = !usernameField.getText().isBlank();
        boolean passwordValid = passwordField.getText().length() >= MIN_PASSWORD_LENGTH;

        setFieldError(usernameField, usernameErrorLabel, !usernameValid);
        setFieldError(passwordField, passwordErrorLabel, !passwordValid);

        return usernameValid && passwordValid;
    }

    private void setFieldError(Region field, Label errorLabel, boolean hasError) {
        field.getStyleClass().removeIf(s -> s.equals("field-error"));
        if (hasError) {
            field.getStyleClass().add("field-error");
        }
        errorLabel.setVisible(hasError);
        errorLabel.setManaged(hasError);
    }

    private void shake(Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), node);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> node.setTranslateX(0));
        shake.play();
    }

    // ---------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------

    private void navigateToDashboard() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), rootPane);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> loadDashboardScene());

        SequentialTransition exit = new SequentialTransition(fadeOut);
        exit.play();
    }

    private void loadDashboardScene() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/buildpay/view/Dashboard.fxml")
            );
            Parent dashboardRoot = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene dashboardScene = new Scene(
                    dashboardRoot,
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight()
            );

            stage.setScene(dashboardScene);
            stage.centerOnScreen();
        } catch (IOException ex) {
            // Dashboard.fxml must live at src/main/resources/com/buildpay/view/Dashboard.fxml
            // with a matching DashboardController on the classpath.
            throw new IllegalStateException("Unable to load Dashboard.fxml", ex);
        }
    }
}
