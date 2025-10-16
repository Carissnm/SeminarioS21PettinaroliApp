package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;

public class SociosListController {
    @FXML private TableView<?> tblSocios;
    @FXML private TableColumn<?, ?> colId, colNombre, colEmail, colEstado;

    @FXML
    private void initialize() {
        // TODO: cuando armemos el DAO, cargamos acá los datos reales
        // Por ahora, tabla vacía para probar el layout
    }

    @FXML private void onNuevo()    { System.out.println("[Socios] Nuevo"); }
    @FXML private void onEditar()   { System.out.println("[Socios] Editar"); }
    @FXML private void onEliminar() { System.out.println("[Socios] Eliminar"); }
}
