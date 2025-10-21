package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.SocioDao;
import ar.edu.csp.sistemadegestioncspgui.dao.SocioDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SociosBuscarController {

    @FXML private TextField txtBuscarDni;
    @FXML private TableView<Socio> tblSocios;
    @FXML private TableColumn<Socio, String> colDni, colApellido, colNombre, colEmail, colTelefono, colActivo, colSaldo;


    private final ObservableList<Socio> data = FXCollections.observableArrayList();
    private final SocioDao socioDao = new SocioDaoImpl();

    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Socios");

        colDni.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getDni())));
        colApellido.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getApellido())));
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getNombre())));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getEmail())));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getTelefono())));
        colActivo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstado() == null ? "" : c.getValue().getEstado().name()));
        colSaldo.setCellValueFactory(c -> new SimpleStringProperty(fmtMoney(c.getValue().getSaldo())));
        tblSocios.setRowFactory(tv -> {
            TableRow<Socio> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) abrir(row.getItem());
            });
            return row;
        });
        tblSocios.setItems(data);
        // NO cargamos nada al entrar (arranca vacía)
    }

    // ----- Acciones -----
    @FXML
    private void onBuscar() {
        String prefijo = ns(txtBuscarDni.getText()).trim();
        if (prefijo.isEmpty()) { info("Ingresá al menos 1 dígito de DNI."); data.clear(); return; }
        try {
            // tu DAO ya hace búsqueda parcial (LIKE 'prefijo%')
            List<Socio> r = socioDao.buscarPorDni(prefijo);
            data.setAll(r);
            if (r.isEmpty()) info("No se encontraron socios para ese DNI.");
        } catch (Exception e) {
            error("No se pudo buscar:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void onLimpiar() { txtBuscarDni.clear(); data.clear(); }

    @FXML
    private void onVolver() {
        Navigation.backOr("/socios-menu-view.fxml", "Socios");
    }

    @FXML
    private void onAbrir() {
        Socio s = tblSocios.getSelectionModel().getSelectedItem();
        if (s == null) { info("Seleccioná un socio de la lista."); return; }
        abrir(s);
    }


    private void abrir(Socio s) {
        SelectionContext.setSocioActual(s);
        Navigation.loadInMain("/socio-detalle-view.fxml", "Socios");
    }

    @FXML private void onNuevo() { Navigation.loadInMain("/socio-form-view.fxml", "Socios"); }

    @FXML
    private void onEditar() {
        Socio s = tblSocios.getSelectionModel().getSelectedItem();
        if (s == null) { info("Seleccioná un socio para editar."); return; }
        SelectionContext.setSocioActual(s);
        Navigation.loadInMain("/socio-form-view.fxml", "Socios");
    }

    // ----- Helpers -----
    private static String ns(String s) { return s == null ? "" : s; }

    private static String fmtMoney(Object saldo) {
        if (saldo == null) return "";
        NumberFormat f = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        if (saldo instanceof BigDecimal b) return f.format(b);
        if (saldo instanceof Number n) return f.format(n.doubleValue());
        try { return f.format(new BigDecimal(saldo.toString())); } catch (Exception e) { return saldo.toString(); }
    }

    private static void info(String m){ new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
    private static void error(String m) { new Alert(Alert.AlertType.ERROR, m, ButtonType.OK).showAndWait(); }
}
