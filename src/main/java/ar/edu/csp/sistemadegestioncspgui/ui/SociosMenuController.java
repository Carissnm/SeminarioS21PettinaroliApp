package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXML;

public class SociosMenuController {

    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Socios");
    }

    @FXML
    private void irAltaSocio() {
        Navigation.loadInMain("/socio-form-view.fxml", "Socios");
    }

    @FXML
    private void irListarSocios() {
        Navigation.loadInMain("/socios-list-view.fxml", "Socios");
    }

    @FXML
    private void irInscripciones() {
        Navigation.loadInMain("/inscripcion-menu-view.fxml", "Socios");
    }

    @FXML
    private void volver() {
        Navigation.back();
    }
}

