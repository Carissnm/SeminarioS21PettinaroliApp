package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

// Controlador del layout principal de la aplicación.
// Inicializa el sistema de navegación con el contenedor central y el label de sección.
// Define la vista inicial (home) al arrancar la aplicación.
// Expone handlers de menú/botones de la barra principal para navegar entre secciones.
public class MainController {

    //Inyección de FXML en el panel central.
    @FXML private StackPane centerPane;
    @FXML private Label lblSectionTitle;


    @FXML
    public void initialize() {
        // 1) Se inicializa Navigation con los nodos del main layout (contenedor y label de título)
        Navigation.init(centerPane, lblSectionTitle);

        // 2) Título por defecto de la sección
        Navigation.setSectionTitle("Inicio");

        // 3) Se carga la vista inicial
        Navigation.loadInMain("/home-view.fxml", "Inicio");
    }

    //Muestra un aviso estándar para funcionalidades aún no implementadas
    private void mostrarEnDesarrollo(String nombre) {
        new Alert(Alert.AlertType.INFORMATION,
                "La funcionalidad '" + nombre + "' todavía no se encuentra actualmente disponible.\nSerá incorporada en la próxima versión.",
                ButtonType.OK).showAndWait();
    }

    // Handlers de los botones para acceder a las distintas pantallas
    @FXML private void onSocios()      { Navigation.loadInMain("socios-menu-view.fxml", "Socios"); }
    @FXML private void onActividades() { Navigation.loadInMain("actividades-menu-view.fxml", "Actividades"); }
    @FXML private void onPagos()       { Navigation.loadInMain("pagos-form-view.fxml", "Pagos"); }
    @FXML private void onReportes()    { mostrarEnDesarrollo("Reportes"); }
    @FXML private void onConfig()      { mostrarEnDesarrollo("Configuración"); }
    @FXML private void onSalir()       { Platform.exit(); }
}
