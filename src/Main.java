package com.buildpay;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * BuildPay – Application Entry Point
 * Launches the Login screen.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/buildpay/Login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1100, 720);

        // App icon (put your logo in resources/images/)
        try {
            primaryStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/images/logo_icon.png")));
        } catch (Exception ignored) { /* icon optional */ }

        primaryStage.setScene(scene);
        primaryStage.setTitle("BuildPay – Construction Payroll System");
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
