package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXML;

public class SociosMenuController {

    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Socios");
    }

    @FXML
    private void irAltaSocio() {
        SelectionContext.setSocioActual(null); // <-- importante
        Navigation.loadInMain("/socio-form-view.fxml", "Socios");
    }
    @FXML
    private void irListarSocios() {
        Navigation.loadInMain("/socios-list-view.fxml", "Socios");
    }

    @FXML private void irBuscarSocio()  { Navigation.loadInMain("/socios-buscar-view.fxml", "Socios"); } // búsqueda

    @FXML
    private void irInscripciones() {
        Navigation.loadInMain("/inscripcion-menu-view.fxml", "Socios");
    }

    @FXML
    private void volver() {
        Navigation.back();
    }
}

