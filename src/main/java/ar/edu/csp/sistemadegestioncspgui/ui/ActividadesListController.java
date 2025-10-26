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
//Controlador de la pantalla donde se listan las actividades
//permite filtrar por nombre o descripción
public class ActividadesListController {
    //Inyección de controles del FXML
    @FXML private TextField txtFiltro;
    @FXML private TableView<ActividadVM> tblActividades;
    @FXML private TableColumn<ActividadVM, Long> colId;
    @FXML private TableColumn<ActividadVM, String> colNombre;
    @FXML private TableColumn<ActividadVM, String> colPrecio;
    @FXML private TableColumn<ActividadVM, String> colEstado;

    //Lista observable que respalda la tabla
    private final ObservableList<ActividadVM> data = FXCollections.observableArrayList();
    //Acceso a los datos
    private final ActividadDao actividadDao = new ActividadDaoImpl();
    //Formateador de moneda
    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("es","AR"));

    @FXML
    public void initialize() {
        //Título de la sección en el layout ppal
        Navigation.setSectionTitle("Actividades");

        //mapeo de columnas: cada columna toma un campo de la vista.
        colId.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().id()).asObject());
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombre()));
        colPrecio.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().precio()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().estado()));
        // Enlace de la lista observable a la tabla
        tblActividades.setItems(data);
        // Texto que se muestra por pantalla cuando no hay datos
        tblActividades.setPlaceholder(new Label("No hay actividades para mostrar"));

        buscar(); // carga inicial (sin filtro)
    }

    //Ejecuta la búsqueda / filtrado y refresca la tabla
    @FXML
    private void buscar() {
        String filtro = txtFiltro.getText() == null ? "" : txtFiltro.getText().trim().toLowerCase();
        try {
            // Trae todas las actividades desde el DAO
            List<Actividad> todas = actividadDao.listarTodas();
            //Aplica filtro y proyecta la vista
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
            data.setAll(vms); //Reemplaza el contenido observable (la tabla se actualiza sola)
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "No fue posible listar actividades:\n" + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    //Permite la navegación al formulario de alta de actividad embebido.
    @FXML private void nuevaActividad() {
        Navigation.loadInMain("/actividad-form-view.fxml", "Actividades"); // embebido
    }

    //Placeholder para una futura pantalla de detalle
    @FXML private void verDetalle() {
        new Alert(Alert.AlertType.INFORMATION, "Pantalla de detalle no implementada.").showAndWait();
    }

    //Navega al formulario para editar la actividad seleccionada.
    @FXML private void editarSeleccionada() {
        var vm = tblActividades.getSelectionModel().getSelectedItem();
        if (vm == null) {
            new Alert(Alert.AlertType.INFORMATION, "Seleccione una actividad para editar.").showAndWait();
            return;
        }
        Navigation.loadInMain("/actividad-form-view.fxml", "Actividades");
    }
    // Vuelve a la vista anterior usando el historial de Navigation.
    @FXML private void volver() {
        Navigation.back();
    }

    //Vista para facilitar el formateo y evitar lógica en celdas.
    public record ActividadVM(Long id, String nombre, String precio, String estado) {}
}
