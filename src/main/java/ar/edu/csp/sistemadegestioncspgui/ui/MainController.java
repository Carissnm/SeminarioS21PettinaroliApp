package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML private StackPane centerPane; // debe matchear el fx:id del main-view.fxml
    @FXML private Label lblSectionTitle;
    // Carga un FXML en el pane central

    @FXML
    public void initialize() {
        // 1) Inicializar Navigation con los nodos del main layout
        Navigation.init(centerPane, lblSectionTitle);

        // 2) (Opcional) Título por defecto
        Navigation.setSectionTitle("Inicio");

        // 3) Cargar la vista inicial (si querés mostrar el logo/home)
        //    Asegurate de tener /home-view.fxml en resources
        Navigation.loadInMain("/home-view.fxml", "Inicio");
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

    private void mostrarEnDesarrollo(String nombre) {
        new Alert(Alert.AlertType.INFORMATION,
                "La funcionalidad '" + nombre + "' todavía no está disponible.\nSerá incorporada en la próxima versión.",
                ButtonType.OK).showAndWait();
    }

    // --- Handlers de menú/botones ---
    @FXML private void onSocios()      { Navigation.loadInMain("socios-menu-view.fxml", "Socios"); }
    @FXML private void onActividades() { Navigation.loadInMain("actividades-menu-view.fxml", "Actividades"); }
    @FXML private void onPagos()       { mostrarEnDesarrollo("Pagos"); }
    @FXML private void onReportes()    { mostrarEnDesarrollo("Reportes"); }
    @FXML private void onConfig()      { mostrarEnDesarrollo("Configuración"); }
    @FXML private void onSalir()       { Platform.exit(); }
}
