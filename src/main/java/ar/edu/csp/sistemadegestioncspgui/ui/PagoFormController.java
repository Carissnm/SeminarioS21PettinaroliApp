package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.*;

import ar.edu.csp.sistemadegestioncspgui.dao.*;
import ar.edu.csp.sistemadegestioncspgui.model.Actividad;
import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class PagoFormController extends BaseController implements ViewOnShow {

    @FXML private TextField txtDni;
    @FXML private Label lblSocio;
    @FXML private Label lblSaldo;
    @FXML private Label lblDeudaAct;
    @FXML private Label lblDeudaClub;
    @FXML private RadioButton rbClub;
    @FXML private RadioButton rbActividad;
    @FXML private ComboBox<Actividad> cbActividad;
    @FXML private TextField txtImporte;
    @FXML private TextField txtDescripcion;

    @FXML private ToggleGroup tgTipo;

    private final SocioDao socioDao = new SocioDaoImpl();
    private final InscripcionDao inscDao = new InscripcionDaoImpl();
    private final CuentaDao cuentaDao = new CuentaDaoImpl();
    private final ActividadDao actividadDao = new ActividadDaoImpl();

    private Socio socioSel;
    private Long inscripcionIdSel;

    // Guardamos el saldo de club para validaciones al guardar
    private BigDecimal saldoClubActual = BigDecimal.ZERO;

    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Pagos");

        // Toggle group
        rbClub.setToggleGroup(tgTipo);
        rbActividad.setToggleGroup(tgTipo);
        rbClub.setSelected(true);
        cbActividad.setDisable(true);

        // Cambiar entre club / actividad
        tgTipo.selectedToggleProperty().addListener((obs, a, b) -> {
            boolean esActividad = rbActividad.isSelected();
            cbActividad.setDisable(!esActividad);
            if (!esActividad) {
                // Modo club
                cbActividad.getSelectionModel().clearSelection();
                inscripcionIdSel = null;
                if (lblDeudaAct != null) lblDeudaAct.setText("");
                // limpiar importe ANTES de sugerir (evita arrastrar valores anteriores)
                if (txtImporte != null && txtImporte.getText() != null && !txtImporte.getText().isBlank()) {
                    txtImporte.clear();
                }
                sugerirDeudaClub();       // calcula y sugiere importe de club sólo si hay deuda
            }
            autocompletarDescripcion();
        });

        // Al elegir actividad → cargar deuda de esa actividad y sugerir importe/descripción
        cbActividad.getSelectionModel().selectedItemProperty().addListener((obs, oldA, newA) -> {
            inscripcionIdSel = null;
            if (rbActividad.isSelected() && socioSel != null && newA != null) {
                try {
                    var inscOpt = inscDao.buscarActiva(socioSel.getId(), newA.getId());
                    if (inscOpt.isPresent()) {
                        inscripcionIdSel = inscOpt.get().getId();

                        var saldo = cuentaDao.saldoPorInscripcion(inscripcionIdSel); // cargos(-)+pagos(+)
                        var deuda = saldo.signum() < 0 ? saldo.negate() : BigDecimal.ZERO;

                        if (lblDeudaAct != null) lblDeudaAct.setText("$" + deuda.toPlainString());
                        if ((txtImporte.getText() == null || txtImporte.getText().isBlank()) && deuda.signum() > 0) {
                            txtImporte.setText(deuda.toPlainString());
                        }
                        if (txtDescripcion.getText() == null || txtDescripcion.getText().isBlank()
                                || txtDescripcion.getText().startsWith("Pago actividad:")) {
                            txtDescripcion.setText("Pago actividad: " + newA.getNombre());
                        }
                    } else {
                        if (lblDeudaAct != null) lblDeudaAct.setText("");
                        if (txtImporte.getText() == null || txtImporte.getText().isBlank()) txtImporte.clear();
                    }
                } catch (Exception ignore) {}
            } else {
                if (lblDeudaAct != null) lblDeudaAct.setText("");
            }
        });
    }

    @Override
    public void onShow() {
        // 1) tomar socio del contexto (desde Detalle → SelectionContext.setSocioActual(socio))
        var s = SelectionContext.getSocioActual();
        if (s != null) {
            socioSel = s;
            if (lblSocio != null) lblSocio.setText(s.getNombreCompleto() + " (" + s.getDni() + ")");
            // saldo total como referencia
            try {
                var saldoTotal = cuentaDao.saldo(s.getId());
                if (lblSaldo != null) lblSaldo.setText("Saldo total: $" + saldoTotal.toPlainString());
            } catch (Exception ignore) {}
        }

        // 2) limpiar campos y set por defecto (ANTES de sugerir)
        if (txtDni != null) txtDni.clear();
        if (txtImporte != null) txtImporte.clear();
        if (txtDescripcion != null) txtDescripcion.clear();
        if (lblDeudaClub != null) lblDeudaClub.setText("");
        if (lblDeudaAct != null) lblDeudaAct.setText("");
        if (rbClub != null) rbClub.setSelected(true);
        if (cbActividad != null) {
            cbActividad.getItems().clear();
            cbActividad.setDisable(true);
        }

        // 3) cargar actividades activas del socio (combo)
        cargarActividadesDeSocio();

        // 4) sugerir deuda de club (después de limpiar)
        sugerirDeudaClub();

        // 5) autodescripción base
        autocompletarDescripcion();
    }

    private void cargarActividadesDeSocio() {
        if (socioSel == null || cbActividad == null) return;
        try {
            var inscs = inscDao.listarPorSocio(socioSel.getId());
            var activas = inscs.stream()
                    .filter(i -> i.getFechaBaja() == null && (i.getEstado()==null || "ACTIVA".equals(i.getEstado().name())))
                    .toList();

            // armamos objetos Actividad mínimos con (id, nombre, precioDefault) para el combo
            var acts = new java.util.ArrayList<Actividad>();
            for (var i : activas) {
                var a = new Actividad();
                a.setId(i.getActividadId());
                a.setNombre(i.getActividadNombre());
                a.setPrecioDefault(i.getCuotaMensual()); // opcional para mostrar
                acts.add(a);
            }
            cbActividad.getItems().setAll(acts);

            // renderiza por nombre
            cbActividad.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Actividad a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a==null ? "" : a.getNombre());
                }
            });
            cbActividad.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Actividad a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a==null ? "" : a.getNombre());
                }
            });
        } catch (Exception e) {
            cbActividad.getItems().clear();
        }
    }

    private void sugerirDeudaClub() {
        if (socioSel == null) return;
        try {
            saldoClubActual = cuentaDao.saldoCuotaClub(socioSel.getId()); // sólo club (inscripcion_id IS NULL)
            var deuda = saldoClubActual.signum() < 0 ? saldoClubActual.negate() : BigDecimal.ZERO;

            if (lblDeudaClub != null) {
                if (deuda.signum() > 0) {
                    lblDeudaClub.setText("Deuda: $" + deuda.toPlainString());
                    lblDeudaClub.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                } else {
                    String msg = saldoClubActual.signum() > 0
                            ? "Sin deuda (saldo a favor: $" + saldoClubActual.toPlainString() + ")"
                            : "Sin deuda";
                    lblDeudaClub.setText(msg);
                    lblDeudaClub.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                }
            }

            // Sólo autocompletar importe si hay deuda y el campo está vacío
            if ((txtImporte.getText() == null || txtImporte.getText().isBlank())) {
                if (deuda.signum() > 0) {
                    txtImporte.setText(deuda.toPlainString());
                } else {
                    txtImporte.clear();
                }
            }
        } catch (Exception ignore) {}
    }

    private void autocompletarDescripcion() {
        if (txtDescripcion == null) return;
        if (rbClub.isSelected()) {
            if (txtDescripcion.getText()==null || txtDescripcion.getText().isBlank()
                    || txtDescripcion.getText().startsWith("Pago actividad:")) {
                txtDescripcion.setText("Pago cuota de club");
            }
        } else {
            var a = cbActividad.getValue();
            if (a != null) {
                txtDescripcion.setText("Pago actividad: " + a.getNombre());
            } else if (txtDescripcion.getText()==null || txtDescripcion.getText().isBlank()) {
                txtDescripcion.setText("Pago actividad");
            }
        }
    }

    // Buscar socio por DNI/prefijo con elección cuando hay varios
    @FXML
    private void onBuscarSocio() {
        String dni = txtDni.getText() == null ? "" : txtDni.getText().trim();
        if (dni.isEmpty()) {
            warn("Ingrese un DNI (o prefijo) para buscar.");
            return;
        }

        try {
            var hallados = socioDao.buscarPorDni(dni);

            if (hallados.isEmpty()) {
                warn("No se encontró socio con ese DNI/prefijo.");
                return;
            }

            Optional<Socio> exacto = hallados.stream()
                    .filter(s -> dni.equalsIgnoreCase(s.getDni()))
                    .findFirst();
            socioSel = exacto.orElse(hallados.size() == 1 ? hallados.get(0) : elegirSocio(hallados));
            if (socioSel == null) return;

            lblSocio.setText(socioSel.getNombreCompleto() + " (" + socioSel.getDni() + ")");
            txtDni.setText(socioSel.getDni());

            var saldo = cuentaDao.saldo(socioSel.getId());
            lblSaldo.setText("$" + saldo.toPlainString());

            cargarActividadesDeSocio();   // repuebla combo
            if (rbActividad.isSelected() && cbActividad.getValue() != null) {
                cbActividad.getSelectionModel().select(cbActividad.getValue());
            }

            // al cambiar de socio, recalculá deuda club
            if (txtImporte != null) txtImporte.clear();
            sugerirDeudaClub();

            autocompletarDescripcion();

        } catch (Exception e) {
            error("Error al buscar socio:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private Socio elegirSocio(List<Socio> lista) {
        var opciones = lista.stream().map(s -> s.getDni() + " - " + s.getApellido() + ", " + s.getNombre()).toList();
        var dlg = new ChoiceDialog<>(opciones.get(0), opciones);
        dlg.setTitle("Seleccionar socio");
        dlg.setHeaderText("Se encontraron varios socios");
        dlg.setContentText("Elija uno:");
        var sel = dlg.showAndWait().orElse(null);
        if (sel == null) return null;
        String dniElegido = sel.split(" - ", 2)[0];
        return lista.stream().filter(s -> s.getDni().equals(dniElegido)).findFirst().orElse(null);
    }

    @FXML
    private void onGuardar() {
        if (socioSel == null) {
            warn("Busque y seleccione un socio.");
            return;
        }

        // 1) Parseo del importe
        BigDecimal importe;
        try {
            String raw = (txtImporte.getText() == null ? "" : txtImporte.getText().trim()).replace(",", ".");
            importe = new BigDecimal(raw);
        } catch (Exception ex) {
            warn("Importe inválido.");
            return;
        }
        if (importe.signum() <= 0) {
            warn("El importe debe ser positivo.");
            return;
        }

        // 2) Descripción (por defecto según tipo)
        String desc = (txtDescripcion.getText() == null || txtDescripcion.getText().isBlank())
                ? (rbActividad.isSelected() ? "Pago actividad" : "Pago cuota de club")
                : txtDescripcion.getText().trim();

        try {
            // 3) Registrar pago (club vs actividad)
            if (rbActividad.isSelected()) {
                var a = cbActividad.getValue();
                if (a == null) { warn("Seleccione una actividad para pagar."); return; }

                Long inscId = inscripcionIdSel;
                if (inscId == null) {
                    var inscOpt = inscDao.buscarActiva(socioSel.getId(), a.getId());
                    if (inscOpt.isEmpty()) {
                        warn("El socio no tiene inscripción activa en esa actividad.");
                        return;
                    }
                    inscId = inscOpt.get().getId();
                }
                cuentaDao.registrarPagoActividad(socioSel.getId(), inscId, importe, desc);

            } else {
                // ---- Cuota del club ----
                // Si no hay deuda (saldoClubActual >= 0), pedir confirmación:
                if (saldoClubActual != null && saldoClubActual.signum() >= 0) {
                    var conf = new Alert(
                            Alert.AlertType.CONFIRMATION,
                            "El socio no tiene deuda de club " +
                                    (saldoClubActual.signum() > 0 ? "(saldo a favor: $" + saldoClubActual.toPlainString() + ")" : "") +
                                    ".\n¿Registrar de todos modos este pago (sumará crédito)?",
                            ButtonType.YES, ButtonType.NO
                    );
                    var r = conf.showAndWait();
                    if (r.isEmpty() || r.get() == ButtonType.NO) return;
                }
                cuentaDao.registrarPago(socioSel.getId(), importe, desc);
            }

            // 4) Actualizo saldo en memoria y dejo el socio en contexto
            try {
                var nuevoSaldo = cuentaDao.saldo(socioSel.getId());
                socioSel.setSaldo(nuevoSaldo);
                SelectionContext.setSocioActual(socioSel);
            } catch (Exception ignore) {}

            info("Pago registrado correctamente.");

            // 5) Navegar según origen
            boolean volverADetalle = SelectionContext.getReturnToSocioDetalle();
            SelectionContext.setReturnToSocioDetalle(false); // limpiar flag

            if (volverADetalle) {
                SelectionContext.setSkipOldDetalleOnce(true);
                Navigation.loadInMainReplace("/socio-detalle-view.fxml", "Socios");
            } else {
                Navigation.loadInMainReplace("/home-view.fxml", "Inicio");
            }

        } catch (Exception e) {
            error("No fue posible registrar el pago:\n" + e.getMessage());
        }
    }

    // --- Volver / Cancelar respetando el origen ---
    private void volverSegunOrigen() {
        boolean volverADetalle = SelectionContext.getReturnToSocioDetalle();
        SelectionContext.setReturnToSocioDetalle(false); // limpiar flag
        if (volverADetalle) {
            Navigation.backOr("/socio-detalle-view.fxml", "Socios");
        } else {
            Navigation.backOr("/home-view.fxml", "Inicio");
        }
    }

    @FXML private void onCancelar() { volverSegunOrigen(); }
    @FXML private void onVolver()   { volverSegunOrigen(); }
}
