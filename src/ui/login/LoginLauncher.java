package ui.login;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.text.Font;

import java.io.File;
import java.io.FileInputStream;

public class LoginLauncher extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Attempt to load local fonts from src/resources/fonts/ if present.
        try {
            String[] fonts = new String[]{
                    "Montserrat-ExtraBold.ttf",
                    "Montserrat-Medium.ttf",
                    "Inter-Medium.ttf",
                    "Inter-SemiBold.ttf",
                    "Inter-Bold.ttf"
            };
            ClassLoader cl = LoginLauncher.class.getClassLoader();
            boolean anyLoaded = false;
            for (String f : fonts) {
                boolean loaded = false;
                try (java.io.InputStream is = cl.getResourceAsStream("resources/fonts/" + f)) {
                    if (is != null) {
                        Font font = Font.loadFont(is, 12);
                        loaded = (font != null);
                    }
                } catch (Exception ignore) {}

                if (!loaded) {
                    File fontFile = new File("src/resources/fonts/" + f);
                    if (fontFile.exists()) {
                        try (FileInputStream fis = new FileInputStream(fontFile)) {
                            Font font = Font.loadFont(fis, 12);
                            loaded = (font != null);
                        } catch (Exception ignore) {}
                    }
                }

                anyLoaded = anyLoaded || loaded;
            }

            if (!anyLoaded) {
                System.out.println("No custom fonts found in src/resources/fonts/. To load Inter/Poppins, place the TTF files there to remove font warnings.");
            }
        } catch (Exception ex) {
            System.err.println("Error loading fonts: " + ex.getMessage());
            ex.printStackTrace();
        }

        // Load FXML directly from the project source so we don't need to copy resources.
        File f = new File("src/ui/login/Login.fxml");
        Parent root = FXMLLoader.load(f.toURI().toURL());
        Scene scene = new Scene(root, 1536, 1024);
        stage.setTitle("BuildPay — Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
