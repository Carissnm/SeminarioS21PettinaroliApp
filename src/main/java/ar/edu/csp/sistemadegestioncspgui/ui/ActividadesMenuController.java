package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXML;

public class ActividadesMenuController {

    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Actividades");
    }

    @FXML private void irAltaActividad() { Navigation.loadInMain("ui/ActividadForm.fxml", "Actividades"); }
    @FXML private void irListarActiv() { Navigation.loadInMain("ui/ActividadesList.fxml", "Actividades"); }
    @FXML private void volver() { Navigation.back(); }
}
