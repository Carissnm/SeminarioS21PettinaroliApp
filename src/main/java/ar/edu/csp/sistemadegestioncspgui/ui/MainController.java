package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML private StackPane centerPane; // debe matchear el fx:id del main-view.fxml

    // Carga un FXML en el pane central
    private void loadCenter(String absoluteFxmlPath) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(absoluteFxmlPath));
            centerPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo cargar: " + absoluteFxmlPath + "\n" + e.getMessage())
                    .showAndWait();
        }
    }

    // --- Handlers de menú/botones ---
    @FXML private void onSocios()      { loadCenter("/socios-list-view.fxml"); }
    @FXML private void onActividades() { loadCenter("/actividades-list-view.fxml"); }
    @FXML private void onPagos()       { loadCenter("/placeholder.fxml"); }
    @FXML private void onReportes()    { loadCenter("/placeholder.fxml"); }
    @FXML private void onConfig()      { loadCenter("/placeholder.fxml"); }
    @FXML private void onSalir()       { Platform.exit(); }
}
