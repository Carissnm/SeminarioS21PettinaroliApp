package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;

public final class Navigation {

    // ====== Config y estado ======
    private static StackPane container;        // donde inyectamos las vistas (centerPane)
    private static Label sectionLabel;         // título de sección
    private static final Deque<Node> history = new ArrayDeque<>();

    // Fallbacks
    private static final String FALLBACK_FXML = "/home-view.fxml";
    private static final String FALLBACK_TITLE = "Inicio";

    private Navigation() {}

    // ====== Inicialización ======
    public static void init(StackPane root, Label lbl) {
        container = root;
        sectionLabel = lbl;
    }

    // ====== Utilidades ======
    public static void setSectionTitle(String title) {
        if (sectionLabel != null) sectionLabel.setText(title);
    }

    // Normaliza y resuelve un recurso en el classpath
    private static URL resolve(String fxmlPath) {
        String normalized = (fxmlPath == null) ? "" : (fxmlPath.startsWith("/") ? fxmlPath : "/" + fxmlPath);
        return Navigation.class.getResource(normalized);
    }

    private static void ensureInit() {
        if (container == null) throw new IllegalStateException("Navigation no inicializado (falta init).");
    }

    private static void applyView(Node view, boolean pushHistory) {
        if (!container.getChildren().isEmpty() && pushHistory) {
            history.push(container.getChildren().get(0));
        }
        container.getChildren().setAll(view);
    }

    private static void invokeOnShow(FXMLLoader loader) {
        Object controller = loader.getController();
        if (controller instanceof ViewOnShow v) {
            javafx.application.Platform.runLater(v::onShow);
        }
    }

    private static void loadInternal(String fxmlPath, String title, boolean pushHistory) {
        ensureInit();

        URL url = resolve(fxmlPath);
        if (url == null) {
            // Fallback silencioso a HOME en lugar de romper
            url = resolve(FALLBACK_FXML);
            title = FALLBACK_TITLE;
            if (url == null) {
                throw new RuntimeException("No se encontró el FXML solicitado ni el fallback: "
                        + fxmlPath + " / " + FALLBACK_FXML);
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(url);
            Node view = loader.load();
            applyView(view, pushHistory);
            if (title != null && !title.isBlank()) setSectionTitle(title);
            invokeOnShow(loader);
        } catch (IOException e) {
            // Si algo falla en la carga, intentamos fallback UNA vez
            if (!FALLBACK_FXML.equals(fxmlPath)) {
                loadInternal(FALLBACK_FXML, FALLBACK_TITLE, pushHistory);
            } else {
                throw new RuntimeException("No fue posible cargar " + fxmlPath + " ni fallback.", e);
            }
        }
    }

    // ====== API pública ======
    /** Carga y APILA en historial (para ir y volver). */
    public static void loadInMain(String fxmlPath, String sectionTitle) {
        loadInternal(fxmlPath, sectionTitle, true);
    }

    /** Carga REEMPLAZANDO (sin apilar). Ideal para Guardar/Cancelar o entradas “limpias”. */
    public static void loadInMainReplace(String fxmlPath, String sectionTitle) {
        loadInternal(fxmlPath, sectionTitle, false);
    }

    /** Limpia historial y carga (entrada a un módulo). */
    public static void loadInMainReset(String fxmlPath, String sectionTitle) {
        clearHistory();
        loadInMainReplace(fxmlPath, sectionTitle);
    }

    public static void clearHistory() {
        history.clear();
    }

    /** Volver si hay historial; si no, ir a fallback HOME. */
    public static void back() {
        ensureInit();
        if (!history.isEmpty()) {
            container.getChildren().setAll(history.pop());
        } else {
            loadInMainReplace(FALLBACK_FXML, FALLBACK_TITLE);
        }
    }

    /** Volver si hay historial; si no, ir al fallback indicado (con fallback final a HOME). */
    public static void backOr(String fallbackFxml, String sectionTitle) {
        ensureInit();
        if (!history.isEmpty()) {
            container.getChildren().setAll(history.pop());
            return; // preserva el título previo
        }
        // Si el fallback falla, se aplica el fallback HOME internamente
        loadInMainReplace(fallbackFxml, sectionTitle);
    }
}
