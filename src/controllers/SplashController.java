package com.buildpay.controller;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controls the BuildPay splash screen: logo entrance animation, pulsing glow,
 * a synchronized progress bar / loading-text sequence, and the automatic
 * hand-off to the Login screen once loading completes.
 */
public class SplashController {

    private static final Duration TOTAL_DURATION = Duration.seconds(3);

    private static final String[] LOADING_STEPS = {
            "Initializing...",
            "Loading Modules...",
            "Connecting Database...",
            "Preparing Dashboard...",
            "Ready..."
    };

    @FXML private StackPane rootPane;
    @FXML private VBox contentBox;
    @FXML private StackPane logoContainer;
    @FXML private Circle glowCircle;
    @FXML private Rectangle logoBadge;
    @FXML private Text logoText;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private VBox loadingBox;
    @FXML private ProgressBar loadingBar;
    @FXML private Label loadingTextLabel;

    @FXML
    public void initialize() {
        prepareInitialState();
        applyGlowEffect();
        playEntranceAnimation();
        playGlowPulse();
        playLoadingSequence();
    }

    /** Hide everything that should fade/scale in, so the first frame is clean. */
    private void prepareInitialState() {
        logoContainer.setOpacity(0);
        logoContainer.setScaleX(0.8);
        logoContainer.setScaleY(0.8);

        titleLabel.setOpacity(0);
        subtitleLabel.setOpacity(0);
        loadingBox.setOpacity(0);

        loadingBar.setProgress(0);
    }

    /** Soft glow behind the badge using a blurred, semi-transparent DropShadow. */
    private void applyGlowEffect() {
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#3E8CFF", 0.85));
        glow.setRadius(45);
        glow.setSpread(0.25);
        logoBadge.setEffect(glow);

        DropShadow circleGlow = new DropShadow();
        circleGlow.setColor(Color.web("#3E8CFF", 0.55));
        circleGlow.setRadius(60);
        circleGlow.setSpread(0.15);
        glowCircle.setEffect(circleGlow);
    }

    /** Logo fades in and scales 0.8 -> 1.0, then title / subtitle / loading box fade in. */
    private void playEntranceAnimation() {
        FadeTransition logoFade = new FadeTransition(Duration.millis(700), logoContainer);
        logoFade.setFromValue(0);
        logoFade.setToValue(1);

        ScaleTransition logoScale = new ScaleTransition(Duration.millis(700), logoContainer);
        logoScale.setFromX(0.8);
        logoScale.setFromY(0.8);
        logoScale.setToX(1.0);
        logoScale.setToY(1.0);
        logoScale.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition logoIntro = new ParallelTransition(logoFade, logoScale);

        FadeTransition titleFade = new FadeTransition(Duration.millis(450), titleLabel);
        titleFade.setFromValue(0);
        titleFade.setToValue(1);

        FadeTransition subtitleFade = new FadeTransition(Duration.millis(450), subtitleLabel);
        subtitleFade.setFromValue(0);
        subtitleFade.setToValue(1);

        FadeTransition loadingFade = new FadeTransition(Duration.millis(450), loadingBox);
        loadingFade.setFromValue(0);
        loadingFade.setToValue(1);

        SequentialTransition sequence = new SequentialTransition(
                logoIntro,
                titleFade,
                subtitleFade,
                loadingFade
        );
        sequence.play();
    }

    /** Gentle continuous pulse on the glow so the logo feels alive while loading. */
    private void playGlowPulse() {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1100), glowCircle);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.12);
        pulse.setToY(1.12);
        pulse.setCycleCount(ScaleTransition.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();
    }

    /**
     * Drives the progress bar from 0 -> 1 over the total duration and swaps the
     * loading label text at even intervals across the same span, then hands off
     * to the login screen.
     */
    private void playLoadingSequence() {
        int steps = LOADING_STEPS.length;
        double stepSeconds = TOTAL_DURATION.toSeconds() / steps;

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.ZERO, new KeyValue(loadingBar.progressProperty(), 0))
        );

        for (int i = 0; i < steps; i++) {
            double atSeconds = stepSeconds * (i + 1);
            double progressValue = (double) (i + 1) / steps;
            String text = LOADING_STEPS[i];

            timeline.getKeyFrames().add(new KeyFrame(
                    Duration.seconds(atSeconds),
                    e -> loadingTextLabel.setText(text),
                    new KeyValue(loadingBar.progressProperty(), progressValue, Interpolator.EASE_BOTH)
            ));
        }

        timeline.setOnFinished(e -> goToLogin());
        timeline.play();
    }

    /** Fades the splash out, then loads and shows the Login scene on the same stage. */
    private void goToLogin() {
        PauseTransition hold = new PauseTransition(Duration.millis(200));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), rootPane);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition exit = new SequentialTransition(hold, fadeOut);
        exit.setOnFinished(e -> loadLoginScene());
        exit.play();
    }

    private void loadLoginScene() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/buildpay/view/Login.fxml")
            );
            Parent loginRoot = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene loginScene = new Scene(loginRoot, stage.getScene().getWidth(), stage.getScene().getHeight());

            stage.setScene(loginScene);
            stage.centerOnScreen();
        } catch (IOException ex) {
            // Login.fxml must live at src/main/resources/com/buildpay/view/Login.fxml
            // with a matching LoginController on the classpath.
            throw new IllegalStateException("Unable to load Login.fxml", ex);
        }
    }
}
