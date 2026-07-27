package com.payroll.controllers;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.AuthenticationService;
import java.io.File;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Label messageLabel;
    @FXML private HBox loginCard;
    @FXML private VBox rightPanel;
    @FXML private ImageView bgImage;

    private final AuthenticationService auth = new AuthenticationService();
    private PauseTransition revealTimeout;

    @FXML
    public void initialize() {
        revealTimeout = new PauseTransition(Duration.seconds(3));
        revealTimeout.setOnFinished(e -> showPasswordReleased());
        if (passwordVisibleField != null) {
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
        }
        if (bgImage != null && loginCard != null) {
            bgImage.fitWidthProperty().bind(loginCard.widthProperty().divide(2.0));
            bgImage.fitHeightProperty().bind(loginCard.heightProperty());
        }
    }

    @FXML
    public void login() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            messageLabel.setText("Please enter username and password");
            return;
        }
        var user = auth.login(username, password);
        if (user != null) {
            String session = auth.createSession(user.getUsername());
            messageLabel.setText("Logged in successfully");
            try {
                File f = new File("src/ui/dashboard/Dashboard.fxml");
                Parent root = FXMLLoader.load(f.toURI().toURL());
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setTitle("BuildPay — Dashboard");
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                messageLabel.setText("Login successful but failed to load dashboard");
            }
        } else {
            messageLabel.setText("Invalid credentials");
        }
    }

    @FXML
    private void showPasswordPressed() {
        if (passwordVisibleField == null || passwordField == null) return;
        passwordVisibleField.setText(passwordField.getText());
        passwordVisibleField.setVisible(true);
        passwordVisibleField.setManaged(true);
        passwordField.setVisible(false);
        passwordField.setManaged(false);
        if (revealTimeout != null) {
            revealTimeout.playFromStart();
        }
    }

    @FXML
    private void showPasswordReleased() {
        if (passwordVisibleField == null || passwordField == null) return;
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);
        passwordField.setVisible(true);
        passwordField.setManaged(true);
        if (revealTimeout != null) {
            revealTimeout.stop();
        }
    }
}
