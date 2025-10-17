package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.ActividadDao;
import ar.edu.csp.sistemadegestioncspgui.dao.ActividadDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.model.Actividad;
import ar.edu.csp.sistemadegestioncspgui.model.EstadoActividad;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ActividadesListController {

    @FXML private TableView<Actividad> tbl;
    @FXML private TableColumn<Actividad, String> colNombre, colDescripcion, colPrecio, colEstado;
    @FXML private TextField txtBuscar;

    private final ActividadDao dao = new ActividadDaoImpl();
    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("es","AR"));

    @FXML
    private void initialize() {
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(nz(c.getValue().getNombre())));
        colDescripcion.setCellValueFactory(c -> new SimpleStringProperty(nz(c.getValue().getDescripcion())));
        colPrecio.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPrecioDefault()==null ? "" : money.format(c.getValue().getPrecioDefault())
        ));
        colPrecio.setStyle("-fx-alignment: CENTER-RIGHT;");
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstado()==null ? "Activa" : c.getValue().getEstado().toLabel()
        ));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item==null) { setText(null); setStyle(""); return; }
                setText(item);
                boolean activa = "Activa".equalsIgnoreCase(item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: " + (activa? "seagreen" : "crimson") + ";");
            }
        });

        txtBuscar.setOnKeyPressed(e -> { if (e.getCode()== KeyCode.ENTER) onBuscar(); });
        tbl.setRowFactory(tv -> {
            TableRow<Actividad> r = new TableRow<>();
            r.setOnMouseClicked(ev -> { if (ev.getClickCount()==2 && !r.isEmpty()) onEditar(); });
            return r;
        });

        refrescar();
    }

    private static String nz(String s){ return s==null? "": s; }

    private void refrescar() {
        try {
            List<Actividad> xs = dao.listarTodas();
            tbl.getItems().setAll(xs);
        } catch (Exception e) { error("No se pudo listar actividades", e); }
    }

    @FXML private void onRefrescar() { refrescar(); }

    @FXML
    private void onBuscar() {
        String pref = nz(txtBuscar.getText()).trim();
        if (pref.isEmpty()) { refrescar(); return; }
        try {
            // filtro en memoria (simple). Si preferís SQL, agregamos SELECT con LIKE.
            tbl.getItems().setAll(dao.listarTodas().stream()
                    .filter(a -> a.getNombre()!=null && a.getNombre().toLowerCase().startsWith(pref.toLowerCase()))
                    .toList());
        } catch (Exception e) { error("Error al buscar", e); }
    }

    @FXML
    private void onNuevo() {
        var act = ActividadForm.showDialog(null);
        act.ifPresent(a -> {
            try { dao.crear(a); refrescar(); }
            catch (Exception e) { error("No se pudo crear la actividad", e); }
        });
    }

    @FXML
    private void onEditar() {
        var sel = tbl.getSelectionModel().getSelectedItem();
        if (sel == null) { info("Seleccioná una actividad"); return; }
        var act = ActividadForm.showDialog(sel);
        act.ifPresent(a -> {
            try { dao.actualizar(a); refrescar(); }
            catch (Exception e) { error("No se pudo actualizar", e); }
        });
    }

    @FXML
    private void onToggleEstado() {
        var sel = tbl.getSelectionModel().getSelectedItem();
        if (sel == null) { info("Seleccioná una actividad"); return; }
        try {
            var nuevo = (sel.getEstado()==EstadoActividad.ACTIVA? EstadoActividad.INACTIVA : EstadoActividad.ACTIVA);
            dao.cambiarEstado(sel.getId(), nuevo);
            refrescar();
        } catch (Exception e) { error("No se pudo cambiar el estado", e); }
    }

    @FXML
    private void onEliminar() {
        var sel = tbl.getSelectionModel().getSelectedItem();
        if (sel == null) { info("Seleccioná una actividad"); return; }
        var ok = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar la actividad \"" + sel.getNombre() + "\"?\n" +
                        "Si tiene inscripciones relacionadas, el DELETE fallará.",
                ButtonType.OK, ButtonType.CANCEL)
                .showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;
        if (!ok) return;

        try (var cn = ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory.get().getConnection();
             var ps = cn.prepareStatement("DELETE FROM actividad WHERE id=?")) {
            ps.setLong(1, sel.getId());
            int rows = ps.executeUpdate();
            if (rows==0) info("No se eliminó ninguna fila (¿tiene inscripciones?).");
            refrescar();
        } catch (Exception e) {
            // FK común: inscripcion.actividad_id
            error("No se pudo eliminar. Probablemente hay inscripciones asociadas.", e);
        }
    }

    private void error(String msg, Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, msg + "\n" + (e.getMessage()==null? e.toString(): e.getMessage())).showAndWait();
    }
    private void info(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
}
