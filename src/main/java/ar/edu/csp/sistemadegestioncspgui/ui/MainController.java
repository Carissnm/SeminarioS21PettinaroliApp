package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.application.Platform;

public class MainController {

    @FXML private StackPane centerPane;

    private void loadCenter(String fxml) {
        try {
            Node view = FXMLLoader.load(getClass().getResource("/" + fxml));
            centerPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            var alert = new Alert(Alert.AlertType.ERROR, "No se pudo cargar: " + fxml + "\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    // Handlers de botones
    @FXML private void onSocios()        { loadCenter("socios-list-view.fxml"); }
    @FXML private void onActividades()   { loadCenter("placeholder.fxml"); }
    @FXML private void onPagos()         { loadCenter("placeholder.fxml"); }
    @FXML private void onReportes()      { loadCenter("placeholder.fxml"); }
    @FXML private void onConfig()        { loadCenter("placeholder.fxml"); }
    @FXML private void onSalir()         { Platform.exit(); }
}
