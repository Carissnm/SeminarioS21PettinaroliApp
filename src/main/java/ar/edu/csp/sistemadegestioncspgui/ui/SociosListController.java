package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.SocioDao;
import ar.edu.csp.sistemadegestioncspgui.dao.SocioDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.model.EstadoSocio;
import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SociosListController {

    @FXML private TableView<Socio> tblSocios;
    @FXML private TableColumn<Socio, String>  colDni, colApellido, colNombre, colEmail, colTelefono, colActivo; // <- colActivo ahora es String
    @FXML private TextField txtBuscarDni;
    @FXML private Button btnBuscar, btnNuevo, btnEditar, btnEliminar;

    private final SocioDao dao = new SocioDaoImpl();
    private final ObservableList<Socio> data = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Columnas null-safe
        colDni.setCellValueFactory(c -> new SimpleStringProperty(nz(c.getValue().getDni())));
        colApellido.setCellValueFactory(c -> new SimpleStringProperty(nz(c.getValue().getApellido())));
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(nz(c.getValue().getNombre())));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(nz(c.getValue().getEmail())));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(nz(c.getValue().getTelefono())));
        colActivo.setCellValueFactory(c -> {
            EstadoSocio est = c.getValue().getEstado();
            String label = (est == null || est == EstadoSocio.ACTIVO) ? "Activo" : "Inactivo";
            return new SimpleStringProperty(label);
        });

        // (Opcional) pinta verde/rojo Activo/Inactivo
        colActivo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: " + ("Activo".equals(item) ? "green" : "crimson") + ";");
            }
        });

        tblSocios.setItems(data);

        // Enter en búsqueda => buscar
        txtBuscarDni.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) onBuscar(); });
        // Doble click => editar
        tblSocios.setRowFactory(tv -> {
            TableRow<Socio> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) onEditar();
            });
            return row;
        });

        refrescar();
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private void refrescar() {
        try {
            List<Socio> lista = dao.listarTodos();
            data.setAll(lista);
        } catch (Exception e) {
            error("No se pudo listar socios", e);
        }
    }

    @FXML
    private void onBuscar() {
        try {
            String pref = txtBuscarDni.getText() == null ? "" : txtBuscarDni.getText().trim();
            data.setAll(dao.buscarPorDni(pref));
        } catch (Exception e) {
            error("Error al buscar", e);
        }
    }

    @FXML private void onRefrescar() { refrescar(); }

    @FXML
    private void onNuevo() {
        // ======= Versión A: si SocioForm.showDialog devuelve Optional<Socio> =======
        Optional<Socio> resOpt = SocioForm.showDialog(tblSocios.getScene().getWindow(), null);
        resOpt.ifPresent(res -> {
            try {
                dao.crear(res);
                refrescar();
            } catch (Exception e) {
                error("No se pudo crear el socio", e);
            }
        });

        // ======= Versión B (si tu SocioForm todavía devuelve Socio o null): =======
        // Socio res = SocioForm.showDialog(null);
        // if (res != null) { dao.crear(res); refrescar(); }
    }

    @FXML
    private void onEditar() {
        Socio sel = tblSocios.getSelectionModel().getSelectedItem();
        if (sel == null) { info("Seleccioná un socio"); return; }

        // Versión A (Optional)
        Optional<Socio> resOpt = SocioForm.showDialog(tblSocios.getScene().getWindow(), sel);
        resOpt.ifPresent(res -> {
            try {
                dao.actualizar(res);
                refrescar();
            } catch (Exception e) {
                error("No se pudo actualizar", e);
            }
        });

        // Versión B (Socio o null)
        // Socio res = SocioForm.showDialog(sel);
        // if (res != null) { dao.actualizar(res); refrescar(); }
    }

    @FXML
    private void onEliminar() {
        Socio sel = tblSocios.getSelectionModel().getSelectedItem();
        if (sel == null) { info("Seleccioná un socio"); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar a " + sel.getApellido() + ", " + sel.getNombre() + "?", ButtonType.OK, ButtonType.CANCEL);
        boolean ok = confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
        if (!ok) return;

        try {
            // --- Opción 1: borrado físico (tal como tenés) ---
            boolean done = dao.eliminar(sel.getId());
            if (!done) info("No se eliminó ninguna fila (verificá restricciones).");
            // --- Opción 2 (recomendada si hay FKs): baja lógica ---
            // sel.setActivo(false);
            // sel.setFechaBaja(LocalDate.now());
            // dao.actualizar(sel);

            refrescar();
        } catch (Exception e) {
            error("No se pudo eliminar", e);
        }
    }

    private void error(String msg, Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, msg + "\n" + (e.getMessage() == null ? e.toString() : e.getMessage())).showAndWait();
    }
    private void info(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}
