package ar.edu.csp.sistemadegestioncspgui.ui;

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
    @FXML private RadioButton rbClub;
    @FXML private RadioButton rbActividad;
    @FXML private ComboBox<Actividad> cbActividad;
    @FXML private TextField txtImporte;
    @FXML private TextField txtDescripcion;

    private final ToggleGroup tgTipo = new ToggleGroup();

    private final SocioDao socioDao = new SocioDaoImpl();
    private final InscripcionDao inscDao = new InscripcionDaoImpl();
    private final CuentaDao cuentaDao = new CuentaDaoImpl();

    private Socio socioSel;
    private Long inscripcionIdSel = null;

    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Pagos");

        // ToggleGroup para tipo de pago
        rbClub.setToggleGroup(tgTipo);
        rbActividad.setToggleGroup(tgTipo);
        rbClub.setSelected(true);
        cbActividad.setDisable(true);

        tgTipo.selectedToggleProperty().addListener((obs, a, b) -> {
            boolean act = rbActividad.isSelected();
            cbActividad.setDisable(!act);
            if (!act) cbActividad.getSelectionModel().clearSelection();
            autocompletarDescripcion();
        });
        cbActividad.getSelectionModel().selectedItemProperty().addListener((obs, oldA, newA) -> {
            inscripcionIdSel = null;
            if (rbActividad.isSelected() && socioSel != null && newA != null) {
                try {
                    var inscOpt = inscDao.buscarActiva(socioSel.getId(), newA.getId());
                    if (inscOpt.isPresent()) {
                        inscripcionIdSel = inscOpt.get().getId();

                        var saldo = cuentaDao.saldoPorInscripcion(inscripcionIdSel); // cargos(-) + pagos(+)
                        var deuda = saldo.signum() < 0 ? saldo.negate() : java.math.BigDecimal.ZERO;

                        // sugerir importe si el campo está vacío
                        if (txtImporte.getText() == null || txtImporte.getText().isBlank()) {
                            txtImporte.setText(deuda.compareTo(java.math.BigDecimal.ZERO) > 0 ? deuda.toPlainString() : "");
                        }

                        if (lblDeudaAct != null) lblDeudaAct.setText("$" + deuda.toPlainString());

                        // autodescripción
                        if (txtDescripcion.getText() == null || txtDescripcion.getText().isBlank()
                                || txtDescripcion.getText().startsWith("Pago actividad:")) {
                            txtDescripcion.setText("Pago actividad: " + newA.getNombre());
                        }
                    } else {
                        // no hay inscripción activa → limpiar sugerencias
                        if (txtImporte.getText() == null || txtImporte.getText().isBlank()) {
                            txtImporte.clear();
                        }
                        if (lblDeudaAct != null) lblDeudaAct.setText("");
                    }
                } catch (Exception e) {
                    // opcional: log / warn suave
                }
            } else {
                if (lblDeudaAct != null) lblDeudaAct.setText("");
            }
        });
    }

    @Override
    public void onShow() {
        var s = SelectionContext.getSocioActual();
        if (s != null) {
            this.socioSel = s;
            if (txtDni != null) txtDni.setText(s.getDni());
            if (lblSocio != null) lblSocio.setText(s.getNombreCompleto() + " (" + s.getDni() + ")");
        }
        if (txtDni != null) txtDni.clear();
        if (lblSocio != null) lblSocio.setText("");
        if (lblSaldo != null) lblSaldo.setText("");
        if (rbClub != null) rbClub.setSelected(true);
        if (cbActividad != null) {
            cbActividad.getItems().clear();
            cbActividad.setDisable(true);
        }
        if (txtImporte != null) txtImporte.clear();
        if (txtDescripcion != null) txtDescripcion.clear();
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

            // saldo y actividades vigentes
            var saldo = cuentaDao.saldo(socioSel.getId());
            lblSaldo.setText("$" + saldo.toPlainString());
            cbActividad.getItems().setAll(inscDao.listarActividadesVigentesPorSocio(socioSel.getId()));

            if (rbActividad.isSelected() && cbActividad.getValue() != null) {
                // fuerza a que el listener corra para la actividad actual
                cbActividad.getSelectionModel().select(cbActividad.getValue());
            }
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

    private void autocompletarDescripcion() {
        if (txtDescripcion == null) return;
        if (rbActividad.isSelected()) {
            var a = cbActividad.getValue();
            if (a != null && (txtDescripcion.getText() == null || txtDescripcion.getText().isBlank())) {
                txtDescripcion.setText("Pago actividad: " + a.getNombre());
            }
        } else {
            if (txtDescripcion.getText() == null || txtDescripcion.getText().isBlank()) {
                txtDescripcion.setText("Pago cuota del club");
            }
        }
    }

    @FXML
    private void onGuardar() {
        if (socioSel == null) { warn("Busque y seleccione un socio."); return; }

        // Parseo robusto de importe (admite coma o punto)
        BigDecimal importe;
        try {
            String raw = (txtImporte.getText() == null ? "" : txtImporte.getText().trim()).replace(",", ".");
            importe = new BigDecimal(raw);
        } catch (Exception ex) {
            warn("Importe inválido.");
            return;
        }
        if (importe.signum() <= 0) { warn("El importe debe ser positivo."); return; }

        // Descripción por defecto según tipo
        String desc = txtDescripcion.getText();
        try {
            if (rbActividad.isSelected()) {
                var a = cbActividad.getValue();
                if (a == null) { warn("Seleccione una actividad para pagar."); return; }
                if (inscripcionIdSel == null) {
                    var inscOpt = inscDao.buscarActiva(socioSel.getId(), a.getId());
                    if (inscOpt.isEmpty()) {
                        warn("El socio no tiene inscripción activa en esa actividad.");
                        return;
                    }
                    inscripcionIdSel = inscOpt.get().getId();
                }
                cuentaDao.registrarPagoActividad(socioSel.getId(), inscripcionIdSel, importe, desc);
            } else {
                // pago general: cuota del club
                cuentaDao.registrarPago(socioSel.getId(), importe, desc);
            }

            info("Pago registrado correctamente.");
            Navigation.loadInMainReplace("/home-view.fxml", "Inicio");

        } catch (Exception e) {
            error("No fue posible registrar el pago:\n" + e.getMessage());
        }

        try {
            cuentaDao.registrarPago(socioSel.getId(), importe, desc);
            info("Pago registrado correctamente.");
            Navigation.loadInMainReplace("/home-view.fxml", "Inicio");
        } catch (Exception e) {
            error("No fue posible registrar el pago:\n" + e.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        Navigation.backOr("/home-view.fxml", "Inicio");
    }

    @FXML
    private void onVolver() {
        Navigation.backOr("/home-view.fxml", "Inicio");
    }
}
