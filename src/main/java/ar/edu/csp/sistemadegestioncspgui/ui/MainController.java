package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML private StackPane centerPane; // debe matchear el fx:id del main-view.fxml
    @FXML private Label lblSectionTitle;
    // Carga un FXML en el pane central

    @FXML
    public void initialize() {
        // Inicializa el helper una sola vez
        Navigation.init(centerPane, lblSectionTitle);

        // (Opcional) pantalla inicial
        // Navigation.loadInMain("/socios-menu-view.fxml", "Socios");
    }
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
    @FXML private void onSocios()      { Navigation.loadInMain("socios-menu-view.fxml", "Socios"); }
    @FXML private void onActividades() { Navigation.loadInMain("actividades-menu-view.fxml", "Actividades"); }
    @FXML private void onPagos()       { Navigation.loadInMain("placeholder.fxml", "Pagos"); }
    @FXML private void onReportes()    { Navigation.loadInMain("placeholder.fxml", "Reportes"); }
    @FXML private void onConfig()      { Navigation.loadInMain("placeholder.fxml", "Configuración"); }
    @FXML private void onSalir()       { Platform.exit(); }
}
