package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXML;

public class ActividadesMenuController {

    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Actividades");
    }

    @FXML private void irAltaActividad() { Navigation.loadInMain("/actividad-form-view.fxml", "Actividades"); }
    @FXML private void irListarActiv() { Navigation.loadInMain("/actividades-list-view.fxml", "Actividades"); }
    @FXML private void volver() { Navigation.loadInMain("/home-view.fxml", "Inicio");}
}
