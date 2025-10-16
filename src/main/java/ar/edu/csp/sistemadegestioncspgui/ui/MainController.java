package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.application.Platform;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MainController {

    @FXML private StackPane centerPane;

    // Cache simple de vistas ya cargadas
    private final Map<String, Parent> viewCache = new HashMap<>();

    /** Carga un FXML (desde /resources) y lo coloca en el centerPane. */
    private void navigate(String absoluteFxmlPath) {
        try {
            Parent view = viewCache.computeIfAbsent(absoluteFxmlPath, this::loadViewOrThrow);
            centerPane.getChildren().setAll(view);
        } catch (Exception e) {
            showLoadError(absoluteFxmlPath, e);
        }
    }

    /** Variante que permite configurar el controller recién cargado (para inyección/config). */
    private <T> void navigate(String absoluteFxmlPath, Class<T> controllerType, Consumer<T> controllerSetup) {
        try {
            Parent view = viewCache.get(absoluteFxmlPath);
            if (view == null) {
                FXMLLoader loader = new FXMLLoader(requireResource(absoluteFxmlPath));
                view = loader.load();
                Object controller = loader.getController();
                if (controllerType.isInstance(controller) && controllerSetup != null) {
                    controllerSetup.accept(controllerType.cast(controller));
                }
                viewCache.put(absoluteFxmlPath, view);
            }
            centerPane.getChildren().setAll(view);
        } catch (Exception e) {
            showLoadError(absoluteFxmlPath, e);
        }
    }

    private Parent loadViewOrThrow(String absoluteFxmlPath) {
        try {
            return FXMLLoader.load(requireResource(absoluteFxmlPath));
        } catch (IOException e) {
            throw new RuntimeException("Error cargando FXML: " + absoluteFxmlPath, e);
        }
    }

    private URL requireResource(String path) {
        URL url = getClass().getResource(path);
        if (url == null) throw new IllegalArgumentException("Recurso no encontrado: " + path + " (¿falta en resources?)");
        return url;
    }

    private void showLoadError(String path, Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR,
                "No se pudo cargar la vista: " + path + "\n\n" +
                        (e.getMessage() == null ? e.toString() : e.getMessage()))
                .showAndWait();
    }

    // --- Handlers de menú/botones ---
    @FXML private void onSocios()      { navigate("/socios-list-view.fxml"); }
    @FXML private void onActividades() { navigate("/placeholder.fxml"); }
    @FXML private void onPagos()       { navigate("/placeholder.fxml"); }
    @FXML private void onReportes()    { navigate("/placeholder.fxml"); }
    @FXML private void onConfig()      { navigate("/placeholder.fxml"); }
    @FXML private void onSalir()       { Platform.exit(); }
}
