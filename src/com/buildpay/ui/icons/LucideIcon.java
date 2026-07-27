package com.buildpay.ui.icons;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Tiny local Lucide-like icon component.
 *
 * Note: This is a small, local approximation for a subset of Lucide icons so the project
 * can render vector icons without introducing a third-party dependency. If you prefer the
 * official Lucide JavaFX library, replace this control with that library's component.
 */
public class LucideIcon extends StackPane {

    private final StringProperty iconName = new SimpleStringProperty(this, "iconName", "");
    private final SVGPath svg = new SVGPath();

    public LucideIcon() {
        getChildren().add(svg);
        svg.setFill(Color.web("#8b8b9e"));
        svg.setScaleX(1.0);
        svg.setScaleY(1.0);

        iconName.addListener((obs, oldV, newV) -> updatePath(newV));
    }

    public String getIconName() {
        return iconName.get();
    }

    public void setIconName(String name) {
        this.iconName.set(name);
    }

    public StringProperty iconNameProperty() {
        return iconName;
    }

    private void updatePath(String name) {
        if (name == null) {
            svg.setContent("");
            return;
        }

        // Simple SVG path approximations for the most common icons used in the sidebar.
        switch (name) {
            case "LayoutDashboard":
                // 3x2 grid
                svg.setContent("M1 1h10v6H1z M13 1h10v2H13z M13 5h10v2H13z");
                break;
            case "Users":
                // Two user circles and a shoulder
                svg.setContent("M6 4a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M12 8c-3 0-6 1.5-6 4v2h12v-2c0-2.5-3-4-6-4z");
                break;
            case "CalendarCheck":
                svg.setContent("M2 3h20v18H2V3zm4-3v4M18 15l-3 3-2-2");
                break;
            case "Wallet":
                svg.setContent("M2 6h20v12H2z M18 10a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3z");
                break;
            case "FileText":
                svg.setContent("M4 2h12l4 4v14H4z M8 12h8 M8 16h8 M8 8h8");
                break;
            case "BarChart3":
                svg.setContent("M4 18h4V8H4z M10 18h4V4h-4z M16 18h4V12h-4z");
                break;
            case "Settings":
                svg.setContent("M12 8.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7z M19 12a7 7 0 0 0 .1-1l2.1-1.6-2-3.4-2.5.6A7 7 0 0 0 14 4.9l-.7-2.3H9.6L9 4.9A7 7 0 0 0 6.4 6l-2.5-.6-2 3.4L3 11a7 7 0 0 0 0 2l-2.1 1.6 2 3.4 2.5-.6A7 7 0 0 0 9 19.1l.7 2.3h3.7l.7-2.3c.7-.2 1.4-.5 2-1l2.5.6 2-3.4L19 13a7 7 0 0 0-.1-1z");
                break;
            case "Bell":
                svg.setContent("M6 2v2a6 6 0 0 0 12 0V2M4 18h16");
                break;
            case "MessageSquare":
                svg.setContent("M2 3h20v14H6l-4 4V3z");
                break;
            case "LogOut":
                svg.setContent("M10 6v4H2v4h8v4l6-6-6-6z");
                break;
            case "Building2":
                svg.setContent("M3 3h6v18H3z M11 7h6v14h-6z M5 5v2 M5 9v2 M13 9v2 M13 13v2");
                break;
            default:
                // Fallback small square so there's a visible node.
                svg.setContent("M2 2h16v16H2z");
                break;
        }
    }
}
