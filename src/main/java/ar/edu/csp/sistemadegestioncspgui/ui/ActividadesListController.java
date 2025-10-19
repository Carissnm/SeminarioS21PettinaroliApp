package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.ActividadDao;
import ar.edu.csp.sistemadegestioncspgui.dao.ActividadDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.model.Actividad;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ActividadesListController {

    @FXML private TextField txtFiltro;
    @FXML private TableView<ActividadVM> tblActividades;
    @FXML private TableColumn<ActividadVM, Long> colId;
    @FXML private TableColumn<ActividadVM, String> colNombre;
    @FXML private TableColumn<ActividadVM, String> colPrecio;
    @FXML private TableColumn<ActividadVM, String> colEstado;

    private final ObservableList<ActividadVM> data = FXCollections.observableArrayList();
    private final ActividadDao actividadDao = new ActividadDaoImpl();
    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("es","AR"));

    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Actividades");

        colId.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().id()).asObject());
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombre()));
        colPrecio.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().precio()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().estado()));

        tblActividades.setItems(data);
        tblActividades.setPlaceholder(new Label("No hay actividades para mostrar"));

        buscar(); // carga inicial
    }

    @FXML
    private void buscar() {
        String filtro = txtFiltro.getText() == null ? "" : txtFiltro.getText().trim().toLowerCase();
        try {
            List<Actividad> todas = actividadDao.listarTodas();
            List<ActividadVM> vms = todas.stream()
                    .filter(a ->
                            filtro.isEmpty() ||
                                    (a.getNombre()!=null && a.getNombre().toLowerCase().contains(filtro)) ||
                                    (a.getDescripcion()!=null && a.getDescripcion().toLowerCase().contains(filtro))
                    )
                    .map(a -> new ActividadVM(
                            a.getId(),
                            a.getNombre(),
                            a.getPrecioDefault()==null ? money.format(0) : money.format(a.getPrecioDefault()),
                            a.getEstado()==null ? "" : a.getEstado().toLabel()
                    ))
                    .collect(Collectors.toList());
            data.setAll(vms);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "No se pudo listar actividades:\n" + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    @FXML private void nuevaActividad() {
        Navigation.loadInMain("/actividad-form-view.fxml", "Actividades"); // embebido
    }

    @FXML private void verDetalle() {
        // Si aún no implementaste detalle, podés navegar a la edición o quitar este botón.
        new Alert(Alert.AlertType.INFORMATION, "Pantalla de detalle no implementada.").showAndWait();
    }

    @FXML private void editarSeleccionada() {
        var vm = tblActividades.getSelectionModel().getSelectedItem();
        if (vm == null) {
            new Alert(Alert.AlertType.INFORMATION, "Seleccioná una actividad para editar.").showAndWait();
            return;
        }
        // Navegar al form embebido; si tenés un mecanismo de pasar el objeto, podés usar SelectionContext
        Navigation.loadInMain("/actividad-form-view.fxml", "Actividades");
    }

    @FXML private void volver() { Navigation.back(); }

    public record ActividadVM(Long id, String nombre, String precio, String estado) {}
}
