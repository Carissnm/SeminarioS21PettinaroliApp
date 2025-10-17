package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.ActividadDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.dao.CuentaDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.dao.InscripcionDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.model.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class SocioDetalleController {

    @FXML private Label lblNombre, lblSaldo, lblDatos;
    @FXML private TextField txtPagoImporte, txtPagoConcepto;
    @FXML private TableView<MovimientoCuenta> tblMovs;
    @FXML private TableColumn<MovimientoCuenta, String> colMovFecha, colMovConcepto, colMovImporte;

    @FXML private ComboBox<Actividad> cbActividad;
    @FXML private TableView<Inscripcion> tblInscripciones;
    @FXML private TableColumn<Inscripcion, String> colActNombre, colActCuota, colInsEstado, colInsAlta, colInsBaja;

    private final CuentaDaoImpl cuentaDao = new CuentaDaoImpl();
    private final InscripcionDaoImpl inscDao = new InscripcionDaoImpl();
    private final ActividadDaoImpl actDao = new ActividadDaoImpl();

    private Socio socio;
    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setSocio(Socio s) {
        this.socio = s;
        lblNombre.setText(s.getNombreCompleto() + "  (DNI " + s.getDni() + ")");
        lblDatos.setText(
                "Email: " + nz(s.getEmail()) + "\n" +
                        "Tel: " + nz(s.getTelefono()) + "\n" +
                        "Domicilio: " + nz(s.getDomicilio()) + "\n" +
                        "Estado: " + (s.isActivo() ? "ACTIVO" : "INACTIVO")
        );
        cargarSaldoYMovs();
        cargarActividadesEInscripciones();
    }

    @FXML
    private void initialize() {
        // Cuenta
        colMovFecha.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFecha()==null ? "" : df.format(c.getValue().getFecha())
        ));
        colMovConcepto.setCellValueFactory(c -> new SimpleStringProperty(nz(c.getValue().getDescripcion())));
        colMovImporte.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getImporte()==null ? "" : money.format(c.getValue().getImporte())
        ));
        colMovImporte.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item==null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.contains("-") ? "-fx-text-fill: crimson;" : "-fx-text-fill: seagreen;");
            }
        });

        // Inscripciones
        colActNombre.setCellValueFactory(c -> new SimpleStringProperty(nz(c.getValue().getActividadNombre())));
        colActCuota.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCuotaMensual()==null ? "" : money.format(c.getValue().getCuotaMensual())
        ));
        colInsEstado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstado()==null ? "ACTIVA" : c.getValue().getEstado().name()
        ));
        colInsAlta.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaAlta()==null ? "" : df.format(c.getValue().getFechaAlta())
        ));
        colInsBaja.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaBaja()==null ? "" : df.format(c.getValue().getFechaBaja())
        ));

        cbActividad.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Actividad item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item==null ? "" : item.getNombre());
            }
        });
        cbActividad.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Actividad item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item==null ? "" : item.getNombre());
            }
        });
    }

    private void cargarSaldoYMovs() {
        try {
            var saldo = cuentaDao.saldo(socio.getId());
            lblSaldo.setText(money.format(saldo));
            lblSaldo.setStyle(saldo.signum()<0? "-fx-text-fill: crimson; -fx-font-weight: bold;"
                    : "-fx-text-fill: seagreen; -fx-font-weight: bold;");
            tblMovs.getItems().setAll(cuentaDao.listarMovimientos(socio.getId()));
        } catch (Exception e) {
            error("No se pudo cargar la cuenta", e);
        }
    }

    private void cargarActividadesEInscripciones() {
        try {
            cbActividad.getItems().setAll(actDao.listarActivas());
            tblInscripciones.getItems().setAll(inscDao.listarPorSocio(socio.getId()));
        } catch (Exception e) {
            error("No se pudieron cargar inscripciones/actividades", e);
        }
    }

    @FXML
    private void onRegistrarPago() {
        try {
            var impTxt = txtPagoImporte.getText().trim().replace(",", "."); // simple
            var imp = new BigDecimal(impTxt);
            if (imp.signum() <= 0) { warn("El importe debe ser > 0"); return; }
            var concepto = nz(txtPagoConcepto.getText());
            cuentaDao.registrarPago(socio.getId(), imp, concepto.isBlank()? "Pago" : concepto);
            txtPagoImporte.clear(); txtPagoConcepto.clear();
            cargarSaldoYMovs();
        } catch (NumberFormatException nfe) {
            warn("Importe inválido.");
        } catch (Exception e) {
            error("No se pudo registrar el pago", e);
        }
    }

    @FXML
    private void onInscribir() {
        var act = cbActividad.getValue();
        if (act == null) { warn("Elegí una actividad."); return; }
        try {
            inscDao.inscribir(socio.getId(), act.getId()); // crea insc + cargo de cuota
            cargarSaldoYMovs();
            cargarActividadesEInscripciones();
        } catch (Exception e) {
            error("No se pudo inscribir", e);
        }
    }

    @FXML
    private void onBajaInscripcion() {
        var sel = tblInscripciones.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Seleccioná una inscripción."); return; }
        if (sel.getEstado() == EstadoInscripcion.INACTIVA) { warn("La inscripción ya fue dada de baja."); return; }
        try {
            inscDao.baja(sel.getId());
            cargarActividadesEInscripciones();
        } catch (Exception e) {
            error("No se pudo dar de baja la inscripción", e);
        }
    }

    private static String nz(String s) { return s==null? "": s; }
    private void warn(String msg) { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }
    private void error(String msg, Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, msg + "\n" + (e.getMessage()==null? e.toString(): e.getMessage())).showAndWait();
    }
}
