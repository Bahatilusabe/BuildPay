package com.buildpay;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * BuildPay LoginController
 * ─────────────────────────────────────────────
 *  • Regular user login  (username / password)
 *  • Admin login         (separate credentials)
 *  • Password visibility toggle
 *  • "Forgot password?"  → opens mailto: to admin
 */
public class LoginController implements Initializable {

    // ── FXML injections ─────────────────────────────────────────────────────
    @FXML private TextField      usernameField;
    @FXML private PasswordField  passwordField;
    @FXML private TextField      passwordVisible;   // plain-text mirror
    @FXML private Button         eyeBtn;
    @FXML private CheckBox       rememberMe;
    @FXML private Label          errorLabel;
    @FXML private Button         signInBtn;
    @FXML private Button         adminBtn;
    @FXML private ImageView      bgImageView;

    // ── Password visibility state ────────────────────────────────────────────
    private boolean pwVisible = false;

    // ──────────────────────────────────────────────────────────────────────────
    //  CREDENTIALS  (in production, replace with DB / hashed lookup)
    // ──────────────────────────────────────────────────────────────────────────

    /** Regular users:  username → password */
    private static final Map<String, String> USER_CREDENTIALS = new HashMap<>();

    /** Admin credentials */
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin@2025";

    /** Admin contact email for "Forgot password?" */
    private static final String ADMIN_EMAIL    = "admin@buildpay.co.ke";

    static {
        USER_CREDENTIALS.put("jmwangi",  "Pass@1234");
        USER_CREDENTIALS.put("powino",   "Pass@1234");
        USER_CREDENTIALS.put("dotieno",  "Pass@1234");
        USER_CREDENTIALS.put("skariuki", "Pass@1234");
        USER_CREDENTIALS.put("jmutua",   "Pass@1234");
    }

    // ────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Sync plain-text mirror with PasswordField
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!pwVisible) passwordVisible.setText(newVal);
        });
        passwordVisible.textProperty().addListener((obs, oldVal, newVal) -> {
            if (pwVisible) passwordField.setText(newVal);
        });

        // Hide plain-text field initially
        passwordVisible.setManaged(false);
        passwordVisible.setVisible(false);

        // Clear error on any keystroke
        usernameField.setOnKeyTyped(e -> clearError());
        passwordField.setOnKeyTyped(e -> clearError());

        // Allow Enter key to trigger login
        passwordField.setOnAction(e -> onSignIn());
        usernameField.setOnAction(e -> passwordField.requestFocus());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SIGN IN  (regular user)
    // ════════════════════════════════════════════════════════════════════════
    @FXML
    private void onSignIn() {
        String username = usernameField.getText().trim();
        String password = getCurrentPassword();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter your username and password.");
            return;
        }

        // Check credentials
        if (USER_CREDENTIALS.containsKey(username)
                && USER_CREDENTIALS.get(username).equals(password)) {

            navigateToDashboard(false);

        } else if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password)) {
            // Admin typed credentials in the regular form → route to admin dash
            navigateToDashboard(true);

        } else {
            showError("Invalid username or password. Please try again.\nIf you forgot your password, click \"Forgot password?\"");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LOGIN AS ADMIN
    // ════════════════════════════════════════════════════════════════════════
    @FXML
    private void onLoginAsAdmin() {
        // Show a small dialog asking for admin credentials
        Dialog<Boolean> dialog = buildAdminDialog();
        dialog.showAndWait().ifPresent(isAuthenticated -> {
            if (isAuthenticated) {
                navigateToDashboard(true);
            } else {
                showError("Admin authentication failed. Check credentials and try again.");
            }
        });
    }

    /**  Builds an admin-credential Dialog */
    private Dialog<Boolean> buildAdminDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Admin Login");
        dialog.setHeaderText("Enter Administrator Credentials");

        ButtonType loginBtnType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginBtnType, ButtonType.CANCEL);

        // Fields
        TextField     adminUser = new TextField();
        adminUser.setPromptText("Admin username");

        PasswordField adminPass = new PasswordField();
        adminPass.setPromptText("Admin password");

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10,
            new Label("Username:"), adminUser,
            new Label("Password:"), adminPass
        );
        box.setPadding(new javafx.geometry.Insets(16));
        dialog.getDialogPane().setContent(box);

        // Validate on Login click
        dialog.setResultConverter(btn -> {
            if (btn == loginBtnType) {
                return ADMIN_USERNAME.equals(adminUser.getText().trim())
                    && ADMIN_PASSWORD.equals(adminPass.getText());
            }
            return false;
        });

        return dialog;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD  →  mailto: to admin
    // ════════════════════════════════════════════════════════════════════════
    @FXML
    private void onForgotPassword() {
        String username = usernameField.getText().trim();
        String subject  = "Password Reset Request – BuildPay";
        String body     = "Hello Admin,\n\n"
            + "I have forgotten my BuildPay login password.\n"
            + "Username: " + (username.isEmpty() ? "[please fill in]" : username) + "\n\n"
            + "Please assist me with resetting my password.\n\n"
            + "Thank you.";

        try {
            String mailtoUri = "mailto:" + ADMIN_EMAIL
                + "?subject=" + encodeParam(subject)
                + "&body="    + encodeParam(body);

            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
                Desktop.getDesktop().mail(new URI(mailtoUri));
            } else {
                // Fallback: show info alert with admin email
                showInfoAlert(
                    "Contact Admin",
                    "Please email the administrator to reset your password:\n\n"
                    + ADMIN_EMAIL + "\n\n"
                    + "Include your username in the email."
                );
            }
        } catch (Exception ex) {
            showInfoAlert(
                "Contact Admin",
                "Could not open your email client.\n\n"
                + "Please manually email:\n" + ADMIN_EMAIL
                + "\nSubject: " + subject
            );
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TOGGLE PASSWORD VISIBILITY
    // ════════════════════════════════════════════════════════════════════════
    @FXML
    private void togglePasswordVisibility() {
        pwVisible = !pwVisible;

        if (pwVisible) {
            // Show plain text
            passwordVisible.setText(passwordField.getText());
            passwordVisible.setManaged(true);
            passwordVisible.setVisible(true);
            passwordField.setManaged(false);
            passwordField.setVisible(false);
            eyeBtn.setText("🙈");
        } else {
            // Show password field
            passwordField.setText(passwordVisible.getText());
            passwordField.setManaged(true);
            passwordField.setVisible(true);
            passwordVisible.setManaged(false);
            passwordVisible.setVisible(false);
            eyeBtn.setText("👁");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════════════════════════════════
    private void navigateToDashboard(boolean isAdmin) {
        try {
            String fxml = isAdmin ? "/com/buildpay/Dashboard.fxml"
                                  : "/com/buildpay/Dashboard.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            // If you have a DashboardController that needs the role:
            // DashboardController ctrl = loader.getController();
            // ctrl.setRole(isAdmin ? "ADMIN" : "USER");

            Stage stage = (Stage) signInBtn.getScene().getWindow();

            // Fade-out transition before switching
            FadeTransition fade = new FadeTransition(Duration.millis(250),
                    signInBtn.getScene().getRoot());
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> {
                Scene scene = new Scene(root, 1100, 650);
                stage.setScene(scene);
                stage.centerOnScreen();

                // Fade in new scene
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fade.play();

        } catch (IOException ex) {
            ex.printStackTrace();
            showError("Could not load dashboard. Please contact support.");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /** Returns the currently active password value */
    private String getCurrentPassword() {
        return pwVisible ? passwordVisible.getText() : passwordField.getText();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);

        // Shake animation
        javafx.animation.TranslateTransition shake =
            new javafx.animation.TranslateTransition(Duration.millis(60), errorLabel);
        shake.setFromX(-6); shake.setToX(6);
        shake.setCycleCount(4); shake.setAutoReverse(true);
        shake.play();
    }

    private void clearError() {
        errorLabel.setText("");
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /** URL-encode a string for use in mailto: URIs */
    private String encodeParam(String value) {
        return value
            .replace(" ", "%20")
            .replace("\n", "%0A")
            .replace(",", "%2C")
            .replace(":", "%3A")
            .replace("–", "%E2%80%93")
            .replace("[", "%5B")
            .replace("]", "%5D");
    }
}
