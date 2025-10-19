package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Window;

import java.util.Optional;

public class SociosMenuController {

    @FXML private Label lblTitulo; // ya existe en el FXML

    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Socios");
    }

    @FXML
    private void irAltaSocio() {
        SelectionContext.setSocioActual(null);
        Navigation.loadInMain("/socio-form-view.fxml", "Socios"); // ✅ embebido en main
    }

    @FXML
    private void irListarSocios() {
        Navigation.loadInMain("/socios-list-view.fxml", "Socios");
    }

    @FXML private void irBuscarSocio()  { Navigation.loadInMain("/socios-buscar-view.fxml", "Socios"); }

    @FXML
    private void irInscripciones() {
        Navigation.loadInMain("/inscripcion-menu-view.fxml", "Socios");
    }

    @FXML
    private void volver() {
        Navigation.back();
    }
}
