package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public final class Navigation {
    private static StackPane container;
    private static Label sectionLabel;
    private static final Deque<Node> history = new ArrayDeque<>();

    private Navigation() {}

    public static void init(StackPane root, Label lbl) {
        container = root;
        sectionLabel = lbl;
    }

    public static void setSectionTitle(String title) {
        if (sectionLabel != null) sectionLabel.setText(title);
    }

    public static void loadInMain(String fxmlPath, String sectionTitle) {
        if (container == null) throw new IllegalStateException("Navigation no inicializado");

        String normalized = fxmlPath.startsWith("/") ? fxmlPath : "/" + fxmlPath;
        var url = Navigation.class.getResource(normalized);
        if (url == null) {
            throw new RuntimeException("FXML no encontrado en classpath: " + normalized +
                    "\nVerifica el nombre del archivo y que esté en src/main/resources (o su subcarpeta).");
        }
        try {
            Node view = FXMLLoader.load(url);
            if (!container.getChildren().isEmpty()) history.push(container.getChildren().get(0));
            container.getChildren().setAll(view);
            setSectionTitle(sectionTitle);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar " + normalized, e);
        }
    }

    public static void back() {
        if (!history.isEmpty() && container != null) {
            container.getChildren().setAll(history.pop());
        }
    }

    public static void backOr(String fallbackFxml, String sectionTitle) {
        if (container != null && !history.isEmpty()) {
            container.getChildren().setAll(history.pop());
            setSectionTitle(sectionTitle);
        } else {
            loadInMain(fallbackFxml, sectionTitle);
        }
    }

}
