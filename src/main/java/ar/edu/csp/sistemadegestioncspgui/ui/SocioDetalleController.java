package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.AptoMedicoDao;
import ar.edu.csp.sistemadegestioncspgui.dao.AptoMedicoDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.dao.InscripcionDao;
import ar.edu.csp.sistemadegestioncspgui.dao.InscripcionDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.model.Inscripcion;
import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class SocioDetalleController {

    @FXML private Label lblNombre;            // Apellido, Nombre (DNI)
    @FXML private Label lblEmailTel;          // Email / Teléfono
    @FXML private Label lblEstadoSaldo;       // Estado / Saldo
    @FXML private TableView<Inscripcion> tblInscripciones;
    @FXML private TableColumn<Inscripcion, String> colActividad;
    @FXML private TableColumn<Inscripcion, String> colEstado;
    @FXML private TableColumn<Inscripcion, String> colPrecioAlta;
    @FXML private TableColumn<Inscripcion, String> colFechaAlta;
    @FXML private TableColumn<Inscripcion, String> colFechaBaja;

    private final AptoMedicoDao aptoDao = new AptoMedicoDaoImpl();
    private final InscripcionDao inscDao = new InscripcionDaoImpl();
    private Socio socio;
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("es","AR"));

    @FXML
    public void initialize() {
        socio = SelectionContext.getSocioActual();
        if (socio == null) {
            new Alert(Alert.AlertType.WARNING, "No hay socio seleccionado.").showAndWait();
            Navigation.backOr("/socios-list-view.fxml", "Socios");
            return;
        }
        // Header
        lblNombre.setText(socio.getApellido() + ", " + socio.getNombre() + " (" + socio.getDni() + ")");
        lblEmailTel.setText(nv(socio.getEmail()) + " / " + nv(socio.getTelefono()));
        lblEstadoSaldo.setText((socio.getEstado()==null? "" : socio.getEstado().name()) +
                "  |  Saldo: " + (socio.getSaldo()==null ? money.format(0) : money.format(socio.getSaldo())));

        // Tabla
        colActividad.setCellValueFactory(c -> new SimpleStringProperty(nv(c.getValue().getActividadNombre())));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstado()==null ? "" : c.getValue().getEstado().name()));
        colPrecioAlta.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPrecioAlta()==null ? "" : money.format(c.getValue().getPrecioAlta())));
        colFechaAlta.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaAlta()==null ? "" : df.format(c.getValue().getFechaAlta())));
        colFechaBaja.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaBaja()==null ? "" : df.format(c.getValue().getFechaBaja())));

        cargarInscripciones();
    }

    private void cargarInscripciones() {
        try {
            var lista = inscDao.listarPorSocio(socio.getId());
            // Filtrar solo activas (estado ACTIVA y sin fecha_baja)
            var activas = lista.stream()
                    .filter(i -> i.getFechaBaja() == null && (i.getEstado()==null || i.getEstado().name().equals("ACTIVA")))
                    .toList();
            tblInscripciones.getItems().setAll(activas);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "No se pudieron cargar inscripciones:\n" + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    private void onInscribir() {
        // Dejo el socio en el contexto y voy al form
        SelectionContext.setSocioActual(socio);
        Navigation.loadInMain("/inscripcion-menu-view.fxml", "Socios");
    }

    @FXML
    private void onDarBaja() {
        var sel = tblInscripciones.getSelectionModel().getSelectedItem();
        if (sel == null) { info("Seleccioná una inscripción."); return; }

        if (sel.getFechaBaja() != null) { info("La inscripción ya está dada de baja."); return; }

        var conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Dar de baja la inscripción a " + sel.getActividadNombre() + "?", ButtonType.YES, ButtonType.NO);
        conf.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    boolean ok = inscDao.darDeBaja(sel.getId(), java.time.LocalDate.now());
                    if (ok) {
                        cargarInscripciones(); info("Inscripción dada de baja.");
                    }
                    else {
                        info("No se pudo dar de baja.");
                    }
                } catch (Exception e) {
                    error("Error al dar de baja:\n" + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onAptoMedico() {
        // pedir fecha de emisión, vencimiento = emision.plusYears(1)
        var dialog = new javafx.scene.control.Dialog<javafx.scene.control.ButtonType>();
        var dp = new javafx.scene.control.DatePicker(LocalDate.now());
        dialog.setTitle("Apto médico");
        dialog.setHeaderText("Elegí fecha de emisión (vigencia: 1 año)");
        dialog.getDialogPane().setContent(dp);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                var emision = dp.getValue();
                if (emision == null) { error("Elegí una fecha"); return; }
                var venc = emision.plusYears(1);
                try {
                    aptoDao.upsertApto(socio.getId(), emision, venc);
                    info("Apto registrado. Vence el " + venc.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                } catch (Exception e) { error("No se pudo registrar:\n" + e.getMessage()); }
            }
        });
    }


    @FXML private void onVolver() { Navigation.backOr("/socios-list-view.fxml", "Socios"); }

    private static String nv(String s){ return s==null ? "" : s; }
    private static void info(String m){ new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
    private static void error(String m){ new Alert(Alert.AlertType.ERROR, m).showAndWait(); }
}
