package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.SocioDao;
import ar.edu.csp.sistemadegestioncspgui.dao.SocioDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class SociosListController {

    // IDs EXACTOS del FXML de LISTAR
    @FXML private TextField txtBuscarDni;
    @FXML private TableView<Socio> tblSocios;
    @FXML private TableColumn<Socio, String> colDni, colApellido, colNombre, colEmail, colTelefono, colActivo, colSaldo;

    private final ObservableList<Socio> data = FXCollections.observableArrayList();
    private final SocioDao socioDao = new SocioDaoImpl();

    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Socios");

        // Mapeo de columnas
        colDni.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getDni())));
        colApellido.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getApellido())));
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getNombre())));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getEmail())));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getTelefono())));
        colActivo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstado() == null ? "" : c.getValue().getEstado().name()));
        colSaldo.setCellValueFactory(c -> new SimpleStringProperty(fmtMoney(c.getValue().getSaldo())));

        tblSocios.setItems(data);

        // LISTA COMPLETA AL ENTRAR
        onRefrescar();
    }

    // ----- Acciones -----
    @FXML private void onRefrescar() {
        try {
            List<Socio> todos = socioDao.listarTodos();
            data.setAll(todos);
        } catch (Exception e) {
            error("No se pudo listar los socios:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void onBuscar() {
        Navigation.loadInMain("/socios-buscar-view.fxml", "Socios");
    }

    @FXML private void onAbrir() {
        Socio s = tblSocios.getSelectionModel().getSelectedItem();
        if (s == null) { info("Seleccioná un socio para abrir."); return; }
        SelectionContext.setSocioActual(s);
        Navigation.loadInMain("/socio-detalle-view.fxml", "Socios");
    }

    @FXML
    private void onNuevo() {
        SelectionContext.setSocioActual(null);
        Navigation.loadInMain("/socio-form-view.fxml", "Socios"); // ✅ embebido
    }

    @FXML private void onEditar() {
        Socio s = tblSocios.getSelectionModel().getSelectedItem();
        if (s == null) { info("Seleccioná un socio para editar."); return; }
        SelectionContext.setSocioActual(s);
        Navigation.loadInMain("/socio-form-view.fxml", "Socios"); // ✅ embebido
    }

    @FXML private void onEliminar() {
        Socio s = tblSocios.getSelectionModel().getSelectedItem();
        if (s == null) { info("Seleccioná un socio para eliminar."); return; }
        var conf = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar al socio seleccionado?", ButtonType.YES, ButtonType.NO);
        conf.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    boolean ok = socioDao.eliminar(s.getId());
                    if (ok) {
                        data.remove(s);
                    } else {
                        info("No se pudo eliminar el socio.");
                    }
                } catch (Exception e) {
                    error("No se pudo eliminar:\n" + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML private void onVolver() {
        Navigation.backOr("/socios-menu-view.fxml", "Socios");
    }

    // ----- Helpers -----
    private static String ns(String s) { return s == null ? "" : s; }

    private static String fmtMoney(Object saldo) {
        if (saldo == null) return "";
        NumberFormat f = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        if (saldo instanceof BigDecimal b) return f.format(b);
        if (saldo instanceof Number n) return f.format(n.doubleValue());
        try { return f.format(new java.math.BigDecimal(saldo.toString())); } catch (Exception e) { return saldo.toString(); }
    }

    private static void info(String m)  { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private static void error(String m) { new Alert(Alert.AlertType.ERROR, m, ButtonType.OK).showAndWait(); }
}
