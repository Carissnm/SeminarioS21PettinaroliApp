package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SociosListController {

    @FXML private TextField txtFiltro;
    @FXML private TableView<Socio> tblSocios;
    @FXML private TableColumn<Socio, Long> colId;
    @FXML private TableColumn<Socio, String> colApellido;
    @FXML private TableColumn<Socio, String> colNombre;
    @FXML private TableColumn<Socio, String> colDocumento;
    @FXML private TableColumn<Socio, String> colEstado;

    private final ObservableList<Socio> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Socios");

        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleLongProperty(
                c.getValue().getId() != null ? c.getValue().getId() : 0L).asObject());
        colApellido.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(ns(c.getValue().getApellido())));
        colNombre.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(ns(c.getValue().getNombre())));
        colDocumento.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(ns(c.getValue().getDni())));
        colEstado.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getEstado() != null ? c.getValue().getEstado().name() : ""));

        tblSocios.setItems(data);
        buscar();
    }

    @FXML
    private void buscar() {
        String filtro = txtFiltro.getText() != null ? txtFiltro.getText().trim() : "";
        data.clear();
        // TODO: reemplazar por socioService.buscar(filtro)
    }

    @FXML
    private void nuevoSocio() {
        Navigation.loadInMain("ui/SocioForm.fxml", "Socios");
    }

    @FXML
    private void verDetalle() {
        Socio s = tblSocios.getSelectionModel().getSelectedItem();
        if (s == null) { info("Seleccioná un socio para ver el detalle."); return; }
        SelectionContext.setSocioActual(s);
        Navigation.loadInMain("ui/SocioDetalle.fxml", "Socios");
    }

    @FXML
    private void inscribirSeleccionado() {
        Socio s = tblSocios.getSelectionModel().getSelectedItem();
        if (s == null) { info("Seleccioná un socio para inscribir."); return; }
        SelectionContext.setSocioActual(s);
        Navigation.loadInMain("ui/InscripcionForm.fxml", "Socios");
    }

    @FXML
    private void registrarPagoSeleccionado() {
        Socio s = tblSocios.getSelectionModel().getSelectedItem();
        if (s == null) { info("Seleccioná un socio para registrar pago."); return; }
        SelectionContext.setSocioActual(s);
        Navigation.loadInMain("ui/PagoForm.fxml", "Socios"); // si existe
    }

    @FXML
    private void volver() { Navigation.back(); }

    private static void info(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private static String ns(String s) { return s == null ? "" : s; }
}
