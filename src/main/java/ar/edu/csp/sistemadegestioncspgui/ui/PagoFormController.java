package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.*;

import ar.edu.csp.sistemadegestioncspgui.dao.*;
import ar.edu.csp.sistemadegestioncspgui.model.Actividad;
import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.text.NumberFormat;
import java.util.Locale;
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
    @FXML private HBox boxBusqueda;

    private static final NumberFormat ARS = NumberFormat.getCurrencyInstance(new Locale("es","AR"));

    private String money(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        return ARS.format(v);
    }

    private String signed(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        return (v.signum() < 0 ? "-" : "") + ARS.format(v.abs());
    }

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
                cbActividad.getSelectionModel().clearSelection();
                inscripcionIdSel = null;
                if (lblDeudaAct != null) lblDeudaAct.setText("");
            }
            recalcularImporteYEtiquetas(); // <<< SIEMPRE recalcular
            autocompletarDescripcion();
        });

        // Al elegir actividad → cargar deuda de esa actividad y sugerir importe/descripción
        cbActividad.getSelectionModel().selectedItemProperty().addListener((obs, oldA, newA) -> {
            // Sólo importa en modo actividad; recalcula deuda/importe aunque ya hubiera valor previo
            recalcularImporteYEtiquetas(); // <<< SIEMPRE recalcular
            autocompletarDescripcion();
        });
    }

    @Override
    public void onShow() {
        boolean desdeDetalle = SelectionContext.getReturnToSocioDetalle(); // true si viene desde detalle

        if (!desdeDetalle) {
            // === CASO 1: desde MENÚ ===
            SelectionContext.setSocioActual(null);
            socioSel = null;

            if (boxBusqueda != null) {
                boxBusqueda.setVisible(true);
                boxBusqueda.setManaged(true);
            }

            if (txtDni != null) txtDni.clear();
            if (lblSocio != null) lblSocio.setText("");
            if (lblSaldo != null) lblSaldo.setText("");
        } else {
            // === CASO 2: desde DETALLE ===
            socioSel = SelectionContext.getSocioActual();

            if (boxBusqueda != null) {
                boxBusqueda.setVisible(false);
                boxBusqueda.setManaged(false);
            }

            if (socioSel != null && lblSocio != null) {
                lblSocio.setText(socioSel.getNombreCompleto() + " (" + socioSel.getDni() + ")");
                try {
                    var saldoTotal = cuentaDao.saldo(socioSel.getId());
                    if (lblSaldo != null) lblSaldo.setText("Saldo total: " + signed(saldoTotal));
                } catch (Exception ignore) {}
            }
        }

        if (txtImporte != null) txtImporte.clear();
        if (txtDescripcion != null) txtDescripcion.clear();
        if (lblDeudaClub != null) lblDeudaClub.setText("");
        if (lblDeudaAct != null) lblDeudaAct.setText("");

        if (rbClub != null) rbClub.setSelected(true);
        if (cbActividad != null) {
            cbActividad.getItems().clear();
            cbActividad.setDisable(true);
        }

        // Si ya hay socio seleccionado
        if (socioSel != null) {
            cargarActividadesDeSocio();
            sugerirDeudaClub();
            recalcularImporteYEtiquetas();
            autocompletarDescripcion();
        }
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
            saldoClubActual = cuentaDao.saldoCuotaClub(socioSel.getId()); // +crédito / −deuda
            var deuda = saldoClubActual.signum() < 0 ? saldoClubActual.negate() : BigDecimal.ZERO;

            if (lblDeudaClub != null) {
                if (deuda.signum() > 0) {
                    lblDeudaClub.setText("Deuda club: " + money(deuda));
                    lblDeudaClub.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                } else if (saldoClubActual.signum() > 0) {
                    lblDeudaClub.setText("Saldo a favor club: " + money(saldoClubActual));
                    lblDeudaClub.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    lblDeudaClub.setText("Sin deuda club");
                    lblDeudaClub.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                }
            }

            // Autocompletar importe solo si hay deuda y el campo está vacío
            if (txtImporte != null && (txtImporte.getText() == null || txtImporte.getText().isBlank())) {
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
            lblSaldo.setText(signed(saldo));

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
                if (a == null) { warn("Seleccione una actividad para pagar.");
                    return;
                }

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
                                    (saldoClubActual.signum() > 0 ? "(saldo a favor: " + money(saldoClubActual) + ")" : "") +
                                    ".\n¿Registrar de todos modos este pago (sumará crédito)?",
                            ButtonType.YES, ButtonType.NO
                    );
                    var r = conf.showAndWait();
                    if (r.isEmpty() || r.get() == ButtonType.NO) return;
                }
                cuentaDao.registrarPago(socioSel.getId(), importe, desc);
            }

            // 4) Actualiza saaldo en memoria y deja el socio en contexto
            try {
                var nuevoSaldo = cuentaDao.saldo(socioSel.getId());
                socioSel.setSaldo(nuevoSaldo);
                SelectionContext.setSocioActual(socioSel);
            } catch (Exception ignore) {}

            info("Pago registrado correctamente.");

            // 5) Navega según origen
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

    private void recalcularImporteYEtiquetas() {
        if (socioSel == null) {
            if (txtImporte != null) txtImporte.clear();
            if (lblDeudaAct != null) lblDeudaAct.setText("");
            if (lblDeudaClub != null) lblDeudaClub.setText("");
            return;
        }

        try {
            if (rbClub.isSelected()) {
                // --- Cuota de club ---
                var saldoClub = cuentaDao.saldoCuotaClub(socioSel.getId()); // +crédito / −deuda
                saldoClubActual = saldoClub; // mantener sincronizado para onGuardar
                var deudaClub = saldoClub.signum() < 0 ? saldoClub.negate() : BigDecimal.ZERO;

                // Etiqueta
                if (lblDeudaClub != null) {
                    if (deudaClub.signum() > 0) {
                        lblDeudaClub.setText("Deuda club: " + money(deudaClub));
                        lblDeudaClub.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if (saldoClub.signum() > 0) {
                        lblDeudaClub.setText("Saldo a favor club: " + money(saldoClub));
                        lblDeudaClub.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else {
                        lblDeudaClub.setText("Sin deuda club");
                        lblDeudaClub.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    }
                }


                txtImporte.setText(deudaClub.toPlainString());
                txtImporte.setEditable(false);
                inscripcionIdSel = null; // club no usa inscripción
                return;
            }

            if (rbActividad.isSelected()) {
                var a = cbActividad.getValue();
                if (a == null) {
                    if (lblDeudaAct != null) lblDeudaAct.setText("");
                    txtImporte.setText("0");
                    txtImporte.setEditable(false);
                    inscripcionIdSel = null;
                    return;
                }

                // Buscar la inscripción activa para esa actividad
                var inscOpt = inscDao.buscarActiva(socioSel.getId(), a.getId());
                if (inscOpt.isEmpty()) {
                    if (lblDeudaAct != null) lblDeudaAct.setText("No tiene inscripción activa");
                    txtImporte.setText("0");
                    txtImporte.setEditable(false);
                    inscripcionIdSel = null;
                    return;
                }

                inscripcionIdSel = inscOpt.get().getId();

                var saldoAct = cuentaDao.saldoPorInscripcion(inscripcionIdSel);
                var deudaAct = saldoAct.signum() < 0 ? saldoAct.negate() : BigDecimal.ZERO;

                if (lblDeudaAct != null) {
                    if (deudaAct.signum() > 0) {
                        lblDeudaAct.setText("Deuda actividad: " + money(deudaAct));
                        lblDeudaAct.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if (saldoAct.signum() > 0) {
                        lblDeudaAct.setText("Saldo a favor actividad: " + money(saldoAct));
                        lblDeudaAct.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else {
                        lblDeudaAct.setText("Sin deuda actividad");
                        lblDeudaAct.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    }
                }


                txtImporte.setText(deudaAct.toPlainString());
                txtImporte.setEditable(false);


                if (txtDescripcion.getText() == null || txtDescripcion.getText().isBlank()
                        || txtDescripcion.getText().startsWith("Pago actividad:")) {
                    txtDescripcion.setText("Pago actividad: " + a.getNombre());
                }
                return;
            }

            // fallback
            txtImporte.clear();
        } catch (Exception e) {
            txtImporte.setText("0");
            txtImporte.setEditable(false);
        }
    }


    @FXML private void onCancelar() {
        volverSegunOrigen();
    }
    @FXML private void onVolver()   {
        volverSegunOrigen();
    }
}
