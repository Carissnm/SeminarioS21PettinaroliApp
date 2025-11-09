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
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

// Controlador de la pantalla que lista los socios del club.
// Configura la tabla que muestra los socios.
// Carga los datos desde el DAO al entrar y cuando se refresca.
public class SociosListController extends BaseController {

    //Referencias inyectadas desde FXML, los id deben coincidir de manera exacta.
    @FXML private TextField txtBuscarDni;
    @FXML private TableView<Socio> tblSocios; // tabla principal
    @FXML private TableColumn<Socio, String> colDni, colApellido, colNombre, colEmail, colTelefono, colActivo, colSaldo;

    // Lista observable que respalda a la TableView
    private final ObservableList<Socio> data = FXCollections.observableArrayList();
    // DAO para acceder a los socios
    private final SocioDao socioDao = new SocioDaoImpl();

    @FXML
    public void initialize() {
        //Título de la sección en el navbar de la aplicación
        Navigation.setSectionTitle("Socios");

        // CONFIGURACIÓN DE COLUMNAS DE LA TABLA:
        // Cada columna mapea una atributo del Socio a un StringProperty para mostrar en la Celda
        // ns(...) evita NullPointerException y normaliza null a "" en strings.
        colDni.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getDni())));
        colApellido.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getApellido())));
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getNombre())));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getEmail())));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getTelefono())));
        colActivo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado() == null ? "" : c.getValue().getEstado().name()));
        colSaldo.setCellValueFactory(c -> new SimpleStringProperty(fmtMoney(c.getValue().getSaldo())));

        //Conecta la lista observable a la tabla
        tblSocios.setItems(data);

        // Al entrar en la pantalla carga el listado completo
        onRefrescar();
    }

    // ======= Acciones =======

    @FXML private void onRefrescar() {
        // Vuelve a consultar todos los socios, tanto activos como inactivos, y refresca la tabla
        try {
            List<Socio> todos = socioDao.listarTodos();
            data.setAll(todos); // reemplaza el contenido observable (la tabla se actualiza sola)
        } catch (Exception e) {
            error("No fue posible listar los socios:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void onBuscar() {
        // Navega a la pantalla específica de búsqueda de socios
        Navigation.loadInMain("/socios-buscar-view.fxml", "Socios");
    }

    @FXML private void onAbrir() {
        //Abre la vista de detalle del socio seleccionado
        Socio s = tblSocios.getSelectionModel().getSelectedItem();
        if (s == null) { info("Seleccione un socio para abrir."); return; }
        SelectionContext.setEntryPoint(EntryPoint.LISTA);
        SelectionContext.setSocioActual(s); //Context global para pasar el socio a la siguiente vista.
        Navigation.loadInMain("/socio-detalle-view.fxml", "Socios");
    }

    @FXML
    private void onNuevo() {
        //Va al formulario para el Alta de un nuevo socio en el club
        SelectionContext.setSocioActual(null);
        Navigation.loadInMain("/socio-form-view.fxml", "Socios"); // Se embebe dentro del contenedor principal
    }

    @FXML private void onEditar() {
        //Direcciona al formulario en modo Edición para el socio seleccionado.
        Socio s = tblSocios.getSelectionModel().getSelectedItem();
        if (s == null) {
            info("Seleccione un socio para editar.");
            return;
        }
        SelectionContext.setSocioActual(s);
        Navigation.loadInMain("/socio-form-view.fxml", "Socios"); // ✅ embebido
    }

    @FXML private void onReactivar() {
        Socio s = tblSocios.getSelectionModel().getSelectedItem();
        if (s == null) { info("Seleccione un socio."); return; }
        if (s.getEstado() == EstadoSocio.ACTIVO) { info("Ese socio ya está ACTIVO."); return; }

        var conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Reactivar al socio " + s.getNombreCompleto() + "?", ButtonType.YES, ButtonType.NO);
        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    boolean ok = socioDao.reactivarSocio(s.getId());
                    if (ok) {
                        s.setEstado(EstadoSocio.ACTIVO);
                        s.setFechaBaja(null);
                        tblSocios.refresh();
                        info("Socio reactivado.");
                    } else {
                        error("No fue posible reactivar el socio.");
                    }
                } catch (Exception e) {
                    error("Error al reactivar:\n" + e.getMessage());
                }
            }
        });
    }

    @FXML private void onEliminar() {
        // Permite la baja lógica del socio seleccionado luego de confirmar la acción.
        Socio s = tblSocios.getSelectionModel().getSelectedItem();
        if (s == null) {
            info("Seleccione un socio para eliminar.");
            return;
        }
        //Cuadro de diálogo de confirmación (no permite continuar hasta que el usuario no responda)
        var conf = new Alert(Alert.AlertType.CONFIRMATION, "¿Desea eliminar al socio seleccionado?", ButtonType.YES, ButtonType.NO);
        conf.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    boolean ok = socioDao.eliminar(s.getId()); // El DAO gestiona las reglas de negocio con respecto a las deudas, inscripciones, etc.
                    if (ok) {
                        s.setEstado(EstadoSocio.INACTIVO);
                        s.setFechaBaja(LocalDate.now());
                        tblSocios.refresh(); // refleja el cambio en la Interfaz del Usuario
                    } else {
                        info("No fue posible eliminar el socio.");
                    }
                } catch (Exception e) {
                    error("No fue posible eliminar:\n" + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML private void onVolver() {
        Navigation.loadInMainReplace("/home-view.fxml", "Inicio");
    }

    // Convierte null a " " para evitar celdas con "null"
    private static String ns(String s) {
        return s == null ? "" : s;
    }

    // Formatea un monto como moneda local y devuelve "" si el saldo es null
    private static String fmtMoney(Object saldo) {
        if (saldo == null) return "";
        NumberFormat f = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        if (saldo instanceof BigDecimal b) return f.format(b);
        if (saldo instanceof Number n) return f.format(n.doubleValue());
        try {
            return f.format(new java.math.BigDecimal(saldo.toString()));
        } catch (Exception e) {
            return saldo.toString();
        }
    }
}
