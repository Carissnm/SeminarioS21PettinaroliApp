package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.*;
import ar.edu.csp.sistemadegestioncspgui.model.EstadoSocio;
import ar.edu.csp.sistemadegestioncspgui.model.Inscripcion;
import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Controlador de la pantalla de detalle del socio
public class SocioDetalleController extends BaseController {

    // ----- UI -----
    @FXML private Label lblNombre;
    @FXML private Label lblEmailTel;
    @FXML private Label lblEstadoSaldo;
    @FXML private Label lblAptoMedico;

    @FXML private TableView<Inscripcion> tblInscripciones;
    @FXML private TableColumn<Inscripcion, String> colActividad;
    @FXML private TableColumn<Inscripcion, String> colEstado;
    @FXML private TableColumn<Inscripcion, String> colSaldo;
    @FXML private TableColumn<Inscripcion, String> colFechaAlta;
    @FXML private TableColumn<Inscripcion, String> colFechaBaja;

    // ----- DAO -----
    private final AptoMedicoDao aptoDao = new AptoMedicoDaoImpl();
    private final InscripcionDao inscDao = new InscripcionDaoImpl();
    private final MovimientoCuentaDao mcDao = new MovimientoCuentaDaoImpl();

    // ----- Estado -----
    private Socio socio;
    private static final long CLUB_ROW_ID = -1L;

    @FXML private Button btnInscribir;
    @FXML private Button btnReactivar;
    // ----- Formateadores -----
    private final DateTimeFormatter df  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("es","AR"));

    @FXML
    public void initialize() {
        // 1) Traer socio del contexto (si no hay, volver a la lista)
        socio = SelectionContext.getSocioActual();
        if (socio == null) {
            new Alert(Alert.AlertType.WARNING, "No hay socio seleccionado.").showAndWait();
            Navigation.backOr("/socios-list-view.fxml", "Socios");
            return;
        }

        // 2) Header con datos básicos
        lblNombre.setText(socio.getApellido() + ", " + socio.getNombre() + " (" + socio.getDni() + ")");
        lblEmailTel.setText(nv(socio.getEmail()) + " / " + nv(socio.getTelefono()));

        // 3) Asegurar cargos del mes (idempotente) ANTES de calcular saldos
        asegurarCargosDelMes(socio);

        // 4) Apto médico
        cargarAptoMedico(socio);

        // 5) Saldos: Club / Actividades / Total
        try {
            var cuentaDao = new ar.edu.csp.sistemadegestioncspgui.dao.CuentaDaoImpl();

            var saldoClub   = cuentaDao.saldoCuotaClub(socio.getId()); // +crédito / -deuda
            var saldoTotal  = cuentaDao.saldo(socio.getId());          // club + actividades
            var saldoActiv  = saldoTotal.subtract(saldoClub);          // parte de actividades

            // Convertimos a “deuda” (solo magnitud del negativo)
            java.math.BigDecimal deudaClub  = saldoClub.signum() < 0 ? saldoClub.negate() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal deudaActiv = saldoActiv.signum() < 0 ? saldoActiv.negate() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal deudaTotal = saldoTotal.signum() < 0 ? saldoTotal.negate() : java.math.BigDecimal.ZERO;

            lblEstadoSaldo.setText(
                    (socio.getEstado()==null? "" : socio.getEstado().name()) +
                            " | Club: " + (deudaClub.signum()>0 ? "-" + money.format(deudaClub) : money.format(java.math.BigDecimal.ZERO)) +
                            " | Actividades: " + (deudaActiv.signum()>0 ? "-" + money.format(deudaActiv) : money.format(java.math.BigDecimal.ZERO)) +
                            " | Total: " + (deudaTotal.signum()>0 ? "-" + money.format(deudaTotal) : money.format(java.math.BigDecimal.ZERO))
            );
        } catch (Exception e) {
            lblEstadoSaldo.setText("Error al obtener saldos");
        }

        // 6) Config de columnas
        colActividad.setCellValueFactory(c ->
                new SimpleStringProperty(nv(c.getValue().getActividadNombre()))
        );

        colEstado.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEstado()==null ? "" : c.getValue().getEstado().name())
        );

        colFechaAlta.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFechaAlta()==null ? "" : df.format(c.getValue().getFechaAlta()))
        );

        colFechaBaja.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFechaBaja()==null ? "" : df.format(c.getValue().getFechaBaja()))
        );

        // 7) columna de Saldo por actividad (verde si >=0, rojo si <0)
        colSaldo.setCellValueFactory(c -> {
            try {
                var insc = c.getValue();
                var cuentaDao = new ar.edu.csp.sistemadegestioncspgui.dao.CuentaDaoImpl();

                String texto;
                if (insc.getId() != null && insc.getId() == CLUB_ROW_ID) {
                    // Fila “Cuota del club”: mostrar DEUDA (con signo negativo para que pinte rojo)
                    var saldoClub = cuentaDao.saldoCuotaClub(socio.getId()); // +crédito / -deuda
                    var deudaClub  = (saldoClub.signum() < 0) ? saldoClub.negate() : java.math.BigDecimal.ZERO;

                    texto = (deudaClub.signum() > 0)
                            ? "-" + money.format(deudaClub)
                            : money.format(java.math.BigDecimal.ZERO);
                } else {
                    // Actividad: mostrar saldo de la inscripción
                    var saldoAct = cuentaDao.saldoPorInscripcion(insc.getId());
                    texto = money.format(saldoAct);
                }

                return new javafx.beans.property.SimpleStringProperty(texto);
            } catch (Exception e) {
                return new javafx.beans.property.SimpleStringProperty("-");
            }
        });
        colSaldo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                if (item.startsWith("-")) {
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                }
            }
        });

        boolean inactivo = (socio.getEstado() != null && socio.getEstado() == EstadoSocio.INACTIVO);

        // Deshabilitar "Inscribir" si está INACTIVO
        if (btnInscribir != null) btnInscribir.setDisable(inactivo);

        // Mostrar botón "Reactivar socio" SOLO si está INACTIVO
        if (btnReactivar != null) {
            btnReactivar.setVisible(inactivo);
            btnReactivar.setManaged(inactivo);
        }

        // 8) Cargar inscripciones
        cargarInscripciones();
        refrescarBotoneraPorEstado();
    }

    // --------- Apto Médico ---------
    private void cargarAptoMedico(Socio socio) {
        try {
            boolean vigente = aptoDao.tieneAptoVigente(socio.getId());
            var ultVtoOpt = aptoDao.ultimoVencimiento(socio.getId()); // Optional<LocalDate> (ajustar si tu DAO difiere)

            if (vigente) {
                String hasta = ultVtoOpt.map(d -> d.format(DMY)).orElse("—");
                lblAptoMedico.setText("Apto médico: ✅ VIGENTE (vence: " + hasta + ")");
                lblAptoMedico.setStyle("-fx-text-fill: #14854F; -fx-font-weight: bold;");
            } else {
                String hasta = ultVtoOpt.map(d -> d.format(DMY)).orElse("sin registro");
                lblAptoMedico.setText("Apto médico: ❌ NO VIGENTE (último vto: " + hasta + ")");
                lblAptoMedico.setStyle("-fx-text-fill: #B00020; -fx-font-weight: bold;");
            }
        } catch (Exception e) {
            lblAptoMedico.setText("Apto médico: ⚠️ Error al consultar");
            lblAptoMedico.setStyle("-fx-text-fill: #B00020;");
        }
    }

    // --------- Cargos Mensuales (idempotente) ---------
    private void asegurarCargosDelMes(Socio socio) {
        try {
            mcDao.generarCargosMensuales(socio.getId(), YearMonth.now());
        } catch (RuntimeException ex) {
            // No rompemos la UI por esto; log simple
            System.err.println("No se pudieron generar cargos del mes: " + ex.getMessage());
        }
    }

    // --------- Tabla de inscripciones ---------
    private void cargarInscripciones() {
        try {
            var lista = inscDao.listarPorSocio(socio.getId());
            var filaClub = new Inscripcion();
            filaClub.setId(CLUB_ROW_ID);
            filaClub.setActividadNombre("Cuota del club");
            // resto de campos pueden quedar null

            // Solo inscripciones activas
            var activas = lista.stream()
                    .filter(i -> i.getFechaBaja() == null &&
                            (i.getEstado()==null || "ACTIVA".equals(i.getEstado().name())))
                    .toList();

            // Items a mostrar
            var items = new java.util.ArrayList<Inscripcion>();
            items.add(filaClub);
            items.addAll(activas);

            tblInscripciones.getItems().setAll(items);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "No fue posible cargar inscripciones:\n" + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    // --------- Acciones ---------
    @FXML
    private void onInscribir() {
        if (socio == null) {
            warn("No existe un socio cargado en el detalle.");
            return;
        }
        if (socio.getEstado() != null && socio.getEstado() == EstadoSocio.INACTIVO) {
            warn("El socio está INACTIVO. Reactivalo para poder inscribirlo en actividades.");
            return;
        }

        SelectionContext.setSocioActual(socio);
        SelectionContext.setReturnToSocioDetalle(true);
        SelectionContext.setSkipOldDetalleOnce(true);
        Navigation.loadInMain("/inscripcion-menu-view.fxml","Inscripciones");
    }

    @FXML
    private void onDarBaja() {
        var sel = tblInscripciones.getSelectionModel().getSelectedItem();
        if (sel == null) { info("Seleccione una inscripción."); return; }

        if (sel.getId() != null && sel.getId() == CLUB_ROW_ID) {
            warn("La cuota del club no es una inscripción y no puede darse de baja aquí.");
            return;
        }

        if (sel.getFechaBaja() != null) {
            info("La inscripción ya está dada de baja.");
            return;
        }

        var actividad = (sel.getActividadNombre() == null ? "(actividad)" : sel.getActividadNombre());
        var conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Desea dar de baja la inscripción a " + actividad + "?",
                ButtonType.YES, ButtonType.NO);

        conf.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    boolean ok = inscDao.darDeBaja(sel.getId(), java.time.LocalDate.now());
                    if (ok) {
                        cargarInscripciones();
                        info("Inscripción dada de baja con éxito.");
                    } else {
                        info("No fue posible dar de baja la inscripción.");
                    }
                } catch (Exception e) {
                    error("Error al dar de baja:\n" + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onAptoMedico() {
        var dialog = new javafx.scene.control.Dialog<javafx.scene.control.ButtonType>();
        var dp = new javafx.scene.control.DatePicker(LocalDate.now());
        dialog.setTitle("Apto médico");
        dialog.setHeaderText("Ingrese fecha de emisión (vigencia: 1 año)");
        dialog.getDialogPane().setContent(dp);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                var emision = dp.getValue();
                if (emision == null) { error("Elegí una fecha"); return; }
                var venc = emision.plusYears(1);
                try {
                    aptoDao.upsertApto(socio.getId(), emision, venc);
                    info("Apto registrado con éxito. Vence el " + venc.format(DMY));
                    cargarAptoMedico(socio);
                } catch (Exception e) {
                    error("No se pudo registrar:\n" + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onIrAPago() {
        if (socio == null) { warn("No hay un socio cargado en el detalle."); return; }
        SelectionContext.setSocioActual(socio);
        SelectionContext.setReturnToSocioDetalle(true);
        Navigation.loadInMain("/pagos-form-view.fxml", "Pagos");
    }

    private void refrescarBotoneraPorEstado() {
        boolean inactivo = (socio.getEstado() == EstadoSocio.INACTIVO);
        if (btnInscribir != null) btnInscribir.setDisable(inactivo);
        if (btnReactivar != null) {
            btnReactivar.setVisible(inactivo);
            btnReactivar.setManaged(inactivo);
        }
    }

    @FXML
    private void onReactivarSocio() {
        if (socio == null) { warn("No hay socio cargado."); return; }
        if (socio.getEstado() == EstadoSocio.ACTIVO) { info("El socio ya está ACTIVO."); return; }

        var conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Confirmás reactivar al socio " + socio.getNombreCompleto() + "?", ButtonType.YES, ButtonType.NO);

        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    SocioDao socioDao = new SocioDaoImpl();
                    boolean ok = socioDao.reactivarSocio(socio.getId());  // <<< AQUÍ
                    if (ok) {
                        socio.setEstado(EstadoSocio.ACTIVO);
                        refrescarBotoneraPorEstado();  // habilita Inscribir, oculta Reactivar

                        lblEstadoSaldo.setText(
                                (socio.getEstado()==null? "" : socio.getEstado().name()) +
                                        " | " + lblEstadoSaldo.getText().replaceFirst("^(ACTIVO|INACTIVO) \\|\\s*", "")
                        );

                        info("Socio reactivado con éxito.");
                    } else {
                        error("No fue posible reactivar al socio.");
                    }
                } catch (Exception e) {
                    error("Error al reactivar:\n" + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }



    @FXML
    private void onVolver() {
        if (SelectionContext.isSkipOldDetalleOnce()) {
            SelectionContext.setSkipOldDetalleOnce(false);
            Navigation.loadInMainReplace("/socios-list-view.fxml", "Socios");
        } else {
            Navigation.backOr("/socios-list-view.fxml", "Socios");
        }
    }
}
