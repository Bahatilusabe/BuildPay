package com.buildpay;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.net.URL;
import java.util.ResourceBundle;

public class ReportsController implements Initializable {

    @FXML private Pane barChartPane;
    @FXML private Pane donutPane;

    // ── Payroll monthly data (in millions KES) ──────────────────────────────
    private static final String[] MONTHS = {
        "Jan","Feb","Apr","May","Jul","Aug","Sep","Oct","Nov","Dec"
    };
    private static final double[] VALUES = {
        1.2, 1.5, 1.8, 2.0, 1.6, 2.1, 2.3, 2.45, 2.2, 2.6
    };

    // ── Department donut data ────────────────────────────────────────────────
    private static final String[] DEPT_LABELS = {"Construction","Engineering","Management","Other"};
    private static final double[] DEPT_PCT    = { 45, 25, 15, 15 };
    private static final Color[]  DEPT_COLORS = {
        Color.web("#3b5bff"),
        Color.web("#22c55e"),
        Color.web("#f97316"),
        Color.web("#ef4444")
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Draw after layout pass so pane sizes are known
        barChartPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // Wait for layout
                barChartPane.layoutBoundsProperty().addListener((o, ov, nv) -> {
                    if (nv.getWidth() > 0) drawBarChart();
                });
                donutPane.layoutBoundsProperty().addListener((o, ov, nv) -> {
                    if (nv.getWidth() > 0) drawDonutChart();
                });
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BAR CHART
    // ════════════════════════════════════════════════════════════════════════
    private void drawBarChart() {
        double W = barChartPane.getWidth();
        double H = barChartPane.getHeight();
        if (W <= 0 || H <= 0) return;

        barChartPane.getChildren().clear();

        Canvas canvas = new Canvas(W, H);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // --- layout margins ---
        double padLeft   = 40;
        double padRight  = 14;
        double padTop    = 14;
        double padBottom = 4;

        double chartW = W - padLeft - padRight;
        double chartH = H - padTop  - padBottom;

        double maxVal = 3.0; // 3M ceiling

        int   n        = VALUES.length;
        double groupW  = chartW / n;
        double barW    = groupW * 0.55;
        double barGap  = (groupW - barW) / 2.0;

        // ── Horizontal grid lines & Y labels ──────────────────────────────
        gc.setFont(Font.font("Segoe UI", 9));
        gc.setTextAlign(TextAlignment.RIGHT);

        int gridLines = 4; // 0, 1M, 2M, 3M, 4M  → 5 lines
        for (int i = 0; i <= gridLines; i++) {
            double val  = (maxVal / gridLines) * i;
            double yPos = padTop + chartH - (val / maxVal) * chartH;

            // grid line
            gc.setStroke(Color.web("#e2e8f0"));
            gc.setLineWidth(1);
            gc.strokeLine(padLeft, yPos, W - padRight, yPos);

            // y label
            gc.setFill(Color.web("#a0aec0"));
            String label = (val == 0) ? "0" : (int) val + "M";
            gc.fillText(label, padLeft - 4, yPos + 4);
        }

        // ── Bars ─────────────────────────────────────────────────────────
        Color barColor     = Color.web("#7c3aed");
        Color barColorHigh = Color.web("#a855f7");  // lighter for taller bars

        for (int i = 0; i < n; i++) {
            double x       = padLeft + i * groupW + barGap;
            double barH    = (VALUES[i] / maxVal) * chartH;
            double y       = padTop + chartH - barH;

            // gradient feel: use lighter color for taller bars
            gc.setFill(VALUES[i] >= 2.0 ? barColorHigh : barColor);

            // rounded top bar: draw rect then round top manually
            double radius = 5;
            gc.beginPath();
            gc.moveTo(x + radius, y);
            gc.lineTo(x + barW - radius, y);
            gc.arcTo(x + barW, y, x + barW, y + radius, radius);
            gc.lineTo(x + barW, y + barH);
            gc.lineTo(x, y + barH);
            gc.lineTo(x, y + radius);
            gc.arcTo(x, y, x + radius, y, radius);
            gc.closePath();
            gc.fill();
        }

        barChartPane.getChildren().add(canvas);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DONUT CHART
    // ════════════════════════════════════════════════════════════════════════
    private void drawDonutChart() {
        double W = donutPane.getWidth();
        double H = donutPane.getHeight();
        if (W <= 0 || H <= 0) return;

        donutPane.getChildren().clear();

        Canvas canvas = new Canvas(W, H);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double cx       = W / 2.0;
        double cy       = H / 2.0;
        double radius   = Math.min(W, H) / 2.0 - 6;
        double innerR   = radius * 0.56;  // donut hole size

        double startAngle = -90.0; // start from top
        double total      = 0;
        for (double v : DEPT_PCT) total += v;

        // ── Draw arcs ────────────────────────────────────────────────────
        for (int i = 0; i < DEPT_PCT.length; i++) {
            double sweep = (DEPT_PCT[i] / total) * 360.0;

            gc.setFill(DEPT_COLORS[i]);
            gc.fillArc(cx - radius, cy - radius,
                       radius * 2, radius * 2,
                       startAngle, sweep,
                       javafx.scene.shape.ArcType.ROUND);

            startAngle += sweep;
        }

        // ── White hole (donut effect) ─────────────────────────────────────
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

        donutPane.getChildren().add(canvas);
    }
}
