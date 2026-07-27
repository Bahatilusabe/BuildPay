package ui.login;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.payroll.controllers.LoginController;

import java.io.File;
import java.lang.reflect.Field;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;

public class AutoLoginLauncher extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        File f = new File("src/ui/login/Login.fxml");
        FXMLLoader loader = new FXMLLoader(f.toURI().toURL());
        Parent root = loader.load();
        Object controller = loader.getController();

        // try to set username/password via reflection
        try {
            Field userF = controller.getClass().getDeclaredField("usernameField");
            Field passF = controller.getClass().getDeclaredField("passwordField");
            userF.setAccessible(true);
            passF.setAccessible(true);
            ((TextField)userF.get(controller)).setText("admin");
            ((PasswordField)passF.get(controller)).setText("admin123");
        } catch (NoSuchFieldException nsf) {
            System.out.println("Could not set fields via reflection: " + nsf.getMessage());
        }

        // call login() if present
        try {
            controller.getClass().getMethod("login").invoke(controller);
        } catch (NoSuchMethodException ns) {
            System.out.println("No login() method on controller");
        }

        stage.setTitle("AutoLogin Test");
        stage.setScene(new Scene(root));
        stage.show();

        System.out.println("AutoLoginLauncher: started scene, attempted auto-login");
        System.out.println("Scene stylesheets: " + stage.getScene().getStylesheets());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
