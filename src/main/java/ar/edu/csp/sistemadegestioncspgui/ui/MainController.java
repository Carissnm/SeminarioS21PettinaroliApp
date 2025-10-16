package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXML;
import javafx.application.Platform;

public class MainController {
    @FXML
    private void onSalir() {
        Platform.exit();
    }
}
