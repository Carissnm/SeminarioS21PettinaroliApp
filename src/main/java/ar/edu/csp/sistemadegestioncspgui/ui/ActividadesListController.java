package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ActividadesListController {

    @FXML private TextField txtFiltro;
    @FXML private TableView<ActividadVM> tblActividades;
    @FXML private TableColumn<ActividadVM, Long> colId;
    @FXML private TableColumn<ActividadVM, String> colNombre;
    @FXML private TableColumn<ActividadVM, String> colPrecio;
    @FXML private TableColumn<ActividadVM, String> colEstado;

    private final ObservableList<ActividadVM> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Actividades");
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleLongProperty(c.getValue().id).asObject());
        colNombre.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().nombre));
        colPrecio.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().precio));
        colEstado.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().estado));
        tblActividades.setItems(data);
        buscar();
    }

    @FXML
    private void buscar() {
        String filtro = txtFiltro.getText() != null ? txtFiltro.getText().trim() : "";
        data.clear();
        // TODO: traer de servicio real
        data.addAll(new ActividadVM(1L, "Fútbol", "15000", "ACTIVA"),
                new ActividadVM(2L, "Natación", "18000", "ACTIVA"));
    }

    @FXML private void nuevaActividad() { Navigation.loadInMain("ui/ActividadForm.fxml", "Actividades"); }
    @FXML private void verDetalle() { Navigation.loadInMain("ui/ActividadDetalle.fxml", "Actividades"); }
    @FXML private void editarSeleccionada() { Navigation.loadInMain("ui/ActividadForm.fxml", "Actividades"); }
    @FXML private void volver() { Navigation.back(); }

    public record ActividadVM(Long id, String nombre, String precio, String estado) {}
}
