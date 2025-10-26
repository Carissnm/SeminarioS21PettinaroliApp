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

// Controlador de la pantalla de búsqueda de socios.
// Configura la tabla y sus columnas, permite buscar por prefijo de dni,
// deja la tabla vacía al inicial y solo carga resultados cuando el usuario los busca.
public class SociosBuscarController {

    // Inyección de controles
    @FXML private TextField txtBuscarDni;
    @FXML private TableView<Socio> tblSocios;
    @FXML private TableColumn<Socio, String> colDni, colApellido, colNombre, colEmail, colTelefono, colActivo, colSaldo;


    // Lista Observable para respaldar la TableView
    private final ObservableList<Socio> data = FXCollections.observableArrayList();
    // Acceso a los datos para búsquedas y navegación
    private final SocioDao socioDao = new SocioDaoImpl();

    @FXML
    public void initialize() {
        //Título de la sección en el header general.
        Navigation.setSectionTitle("Socios");
        //mapeo de columnas a propiedades de la clase Socio.
        colDni.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getDni())));
        colApellido.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getApellido())));
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getNombre())));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getEmail())));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getTelefono())));
        colActivo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstado() == null ? "" : c.getValue().getEstado().name()));
        colSaldo.setCellValueFactory(c -> new SimpleStringProperty(fmtMoney(c.getValue().getSaldo())));
        // Al hacer doble click en una fila se abre el detalle del socio
        tblSocios.setRowFactory(tv -> {
            TableRow<Socio> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) abrir(row.getItem());
            });
            return row;
        });

        //Conección de la lista observable con la tabla
        tblSocios.setItems(data);
        // La tabla inicia vacía al ingresar.
    }

    // ======= Acciones =======
    @FXML
    private void onBuscar() {
        //Toma el prefijo del campo y ejecuta una búsqueda parcial por dni
        String prefijo = ns(txtBuscarDni.getText()).trim();
        // Con el campo vacío se limpian los resultados y se envía un mensaje de aviso
        if (prefijo.isEmpty()) { info("Ingrese al menos 1 dígito de DNI."); data.clear(); return; }
        try {
            // Se almacena el resultado de la búsqueda por dni en la lista r
            List<Socio> r = socioDao.buscarPorDni(prefijo);
            data.setAll(r);
            if (r.isEmpty()) info("No se encontraron socios para ese DNI.");
        } catch (Exception e) {
            error("No se pudo buscar:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void onLimpiar() {
        //Limpia el campo y los resultados
        txtBuscarDni.clear(); data.clear();
    }

    @FXML
    private void onVolver() {
        //Vuelve a la pantalla anterior o al menú de socios
        Navigation.backOr("/socios-menu-view.fxml", "Socios");
    }

    @FXML
    private void onAbrir() {
        //Abre el detalle del socio seleccionado
        Socio s = tblSocios.getSelectionModel().getSelectedItem();
        if (s == null) {
            info("Seleccione un socio de la lista."); return;
        }
        abrir(s);
    }


    private void abrir(Socio s) {
        //Carga el detalle del socio en la pantalla de detalle
        SelectionContext.setSocioActual(s);
        Navigation.loadInMain("/socio-detalle-view.fxml", "Socios");
    }

    @FXML private void onNuevo() {
        //Navega al formulario de alta de socio
        Navigation.loadInMain("/socio-form-view.fxml", "Socios");
    }

    @FXML
    private void onEditar() {
        // Navega al formulario de edición del socio seleccionado.
        Socio s = tblSocios.getSelectionModel().getSelectedItem();
        if (s == null) {
            info("Seleccione un socio para editar."); return;
        }
        SelectionContext.setSocioActual(s);
        Navigation.loadInMain("/socio-form-view.fxml", "Socios");
    }

    // ======= Acciones =======
    // muestra "" en vez de null en las celdas
    private static String ns(String s) {
        return s == null ? "" : s;
    }

    // Formateo de moneda
    private static String fmtMoney(Object saldo) {
        if (saldo == null) return "";
        NumberFormat f = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        if (saldo instanceof BigDecimal b) return f.format(b);
        if (saldo instanceof Number n) return f.format(n.doubleValue());
        try {
            return f.format(new BigDecimal(saldo.toString()));
        } catch (Exception e) {
            return saldo.toString();
        }
    }

    private static void info(String m){ new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
    private static void error(String m) { new Alert(Alert.AlertType.ERROR, m, ButtonType.OK).showAndWait(); }
}
