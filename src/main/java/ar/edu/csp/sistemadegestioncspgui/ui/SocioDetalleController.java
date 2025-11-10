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

    @FXML private Button btnInscribir;
    @FXML private Button btnReactivar;

    // ----- DAO -----
    private final AptoMedicoDao aptoDao = new AptoMedicoDaoImpl();
    private final InscripcionDao inscDao = new InscripcionDaoImpl();
    private final MovimientoCuentaDao mcDao = new MovimientoCuentaDaoImpl();

    // ----- Estado -----
    private Socio socio;
    private static final long CLUB_ROW_ID = -1L;

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

        // 5) Saldos: Club / Actividades (solo ACTIVAS) / Total (firmados)
        try {
            refrescarHeaderYSaldos();
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

        // 7) Saldo por fila (verde si >=0, rojo si <0)
        colSaldo.setCellValueFactory(c -> {
            try {
                var insc = c.getValue();
                var cuentaDao = new CuentaDaoImpl();
                String texto;
                if (insc.getId() != null && insc.getId() == CLUB_ROW_ID) {
                    var sc = cuentaDao.saldoCuotaClub(socio.getId()); // firmado
                    texto = money.format(sc);
                } else {
                    var sa = cuentaDao.saldoPorInscripcion(insc.getId()); // firmado
                    texto = money.format(sa);
                }
                return new SimpleStringProperty(texto);
            } catch (Exception e) {
                return new SimpleStringProperty("-");
            }
        });
        colSaldo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.startsWith("-")
                        ? "-fx-text-fill: red; -fx-font-weight: bold;"
                        : "-fx-text-fill: green; -fx-font-weight: bold;");
            }
        });

        // 8) Botonera por estado
        refrescarBotoneraPorEstado();

        // 9) Cargar inscripciones
        cargarInscripciones();
    }

    // --------- Apto Médico ---------
    private void cargarAptoMedico(Socio socio) {
        try {
            boolean vigente = aptoDao.tieneAptoVigente(socio.getId());
            var ultVtoOpt = aptoDao.ultimoVencimiento(socio.getId());

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

    // --------- Cargos Mensuales ---------
    private void asegurarCargosDelMes(Socio socio) {
        try {
            mcDao.generarCargosMensuales(socio.getId(), YearMonth.now());
        } catch (RuntimeException ex) {
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

            var activas = lista.stream()
                    .filter(i -> i.getFechaBaja() == null &&
                            (i.getEstado()==null || "ACTIVA".equals(i.getEstado().name())))
                    .toList();

            var items = new java.util.ArrayList<Inscripcion>();
            items.add(filaClub);
            items.addAll(activas);

            tblInscripciones.getItems().setAll(items);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "No fue posible cargar inscripciones:\n" + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    private void refrescarHeaderYSaldos() {
        try {
            var cuentaDao = new CuentaDaoImpl();

            var saldoClub = cuentaDao.saldoCuotaClub(socio.getId()); // firmado
            var inscripciones = inscDao.listarPorSocio(socio.getId());
            var saldoActiv = java.math.BigDecimal.ZERO;
            for (var i : inscripciones) {
                boolean activa = i.getFechaBaja()==null &&
                        (i.getEstado()==null || "ACTIVA".equals(i.getEstado().name()));
                if (!activa) continue;
                saldoActiv = saldoActiv.add(cuentaDao.saldoPorInscripcion(i.getId()));
            }
            var saldoTotal = saldoClub.add(saldoActiv);

            lblEstadoSaldo.setText(
                    (socio.getEstado()==null? "" : socio.getEstado().name()) +
                            " | Club: " + money.format(saldoClub) +
                            " | Actividades: " + money.format(saldoActiv) +
                            " | Total: " + money.format(saldoTotal)
            );

            cargarInscripciones(); // para que la fila “Cuota del club” y actividades reflejen el nuevo saldo
        } catch (Exception e) {
            lblEstadoSaldo.setText("Error al obtener saldos");
        }
    }

    // --------- Acciones ---------
    @FXML
    private void onInscribir() {
        if (socio == null) {
            warn("No existe un socio cargado en el detalle.");
            return;
        }
        if (socio.getEstado() == EstadoSocio.INACTIVO) {
            warn("No fue posible realizar la inscripción. El socio se encuentra INACTIVO.");
            return;
        }

        SelectionContext.setSocioActual(socio);
        SelectionContext.setReturnToSocioDetalle(true);
        SelectionContext.setSkipOldDetalleOnce(true);
        Navigation.loadInMain("/inscripcion-menu-view.fxml","Inscripciones");
    }

    //Baja del club
    @FXML
    private void onDarBajaSocio() {
        if (socio == null) { warn("No hay socio cargado."); return; }
        if (socio.getEstado() == EstadoSocio.INACTIVO) { info("El socio se encuentra INACTIVO."); return; }

        var conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Desea dar de baja al socio " + socio.getNombreCompleto() + " del club?",
                ButtonType.YES, ButtonType.NO);

        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    SocioDao socioDao = new SocioDaoImpl();
                    boolean ok = socioDao.eliminar(socio.getId()); // baja lógica
                    if (ok) {
                        socio.setEstado(EstadoSocio.INACTIVO);
                        socio.setFechaBaja(LocalDate.now());
                        refrescarBotoneraPorEstado();
                        refrescarHeaderYSaldos();
                        info("Socio dado de baja con éxito.");
                    } else {
                        error("No fue posible dar de baja al socio.");
                    }
                } catch (Exception e) {
                    error("Error al dar de baja:\n" + e.getMessage());
                }
            }
        });
    }

    //Baja de actividades
    @FXML
    private void onDarBaja() {
        var sel = tblInscripciones.getSelectionModel().getSelectedItem();
        if (sel == null) {
            info("Seleccione una actividad.");
            return;
        }

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
                    boolean ok = inscDao.darDeBaja(sel.getId(), LocalDate.now());
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
        var dialog = new Dialog<ButtonType>();
        var dp = new DatePicker(LocalDate.now());
        dialog.setTitle("Apto médico");
        dialog.setHeaderText("Ingrese fecha de emisión (vigencia: 1 año)");
        dialog.getDialogPane().setContent(dp);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                var emision = dp.getValue();
                if (emision == null) { error("Elige una fecha"); return; }
                var venc = emision.plusYears(1);
                try {
                    aptoDao.upsertApto(socio.getId(), emision, venc);
                    info("Apto registrado con éxito. Vence el " + venc.format(DMY));
                    cargarAptoMedico(socio);
                } catch (Exception e) {
                    error("No fue posible registrar el apto médico:\n" + e.getMessage());
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
        if (socio.getEstado() == EstadoSocio.ACTIVO) { info("El socio ya se encuentra ACTIVO."); return; }

        var conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Desea reactivar al socio " + socio.getNombreCompleto() + "?",
                ButtonType.YES, ButtonType.NO);

        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    SocioDao socioDao = new SocioDaoImpl();
                    boolean ok = socioDao.reactivarSocio(socio.getId());
                    if (!ok) { error("No fue posible reactivar al socio."); return; }

                    socio.setEstado(EstadoSocio.ACTIVO);
                    refrescarBotoneraPorEstado();

                    try {
                        var parametrosDao = new ParametrosDaoImpl();
                        var cuota = parametrosDao.getDecimal("CUOTA_MENSUAL_CLUB")
                                .orElse(java.math.BigDecimal.ZERO);
                        if (cuota.signum() > 0) {
                            var mcDao   = new MovimientoCuentaDaoImpl();
                            var periodo = YearMonth.now();
                            boolean yaCobreEsteMes = mcDao.existeCargoMensual(
                                    socio.getId(), "Cuota Social", periodo);
                            if (!yaCobreEsteMes) {
                                var cuentaDao = new CuentaDaoImpl();
                                cuentaDao.registrarDebitoAltaClub(socio.getId(), cuota);
                            }
                        }
                    } catch (Exception ex) {
                        warn("Socio reactivado. Cuenta no actualizada: " + ex.getMessage());
                    }

                    try {
                        mcDao.generarCargosMensuales(socio.getId(), YearMonth.now());
                    } catch (Exception ignore) {}

                    // refrescar header/tabla
                    try {
                        var cuentaDao = new CuentaDaoImpl();
                        var saldoClub = cuentaDao.saldoCuotaClub(socio.getId());
                        var inscripciones = inscDao.listarPorSocio(socio.getId());
                        var saldoActiv = java.math.BigDecimal.ZERO;
                        for (var i : inscripciones) {
                            boolean a = i.getFechaBaja()==null && (i.getEstado()==null || "ACTIVA".equals(i.getEstado().name()));
                            if (!a) continue;
                            saldoActiv = saldoActiv.add(cuentaDao.saldoPorInscripcion(i.getId()));
                        }
                        var saldoTotal = saldoClub.add(saldoActiv);
                        lblEstadoSaldo.setText(
                                (socio.getEstado()==null? "" : socio.getEstado().name()) +
                                        " | Club: " + money.format(saldoClub) +
                                        " | Actividades: " + money.format(saldoActiv) +
                                        " | Total: " + money.format(saldoTotal)
                        );
                        cargarInscripciones();
                    } catch (Exception ignore) {}
                    refrescarHeaderYSaldos();
                    info("Socio reactivado con éxito.");
                } catch (Exception e) {
                    error("Error al reactivar:\n" + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void onVolver() {
        EntryPoint ep = SelectionContext.consumeEntryPoint();
        switch (ep) {
            case LISTA -> Navigation.loadInMainReplace("/socios-list-view.fxml", "Socios");
            default    -> Navigation.loadInMainReplace("/home-view.fxml", "Inicio");
        }
    }
}
