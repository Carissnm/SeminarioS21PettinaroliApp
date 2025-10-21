package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.AptoMedicoDao;
import ar.edu.csp.sistemadegestioncspgui.dao.AptoMedicoDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.dao.SocioDao;
import ar.edu.csp.sistemadegestioncspgui.dao.SocioDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.model.EstadoSocio;
import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Window;

public class SocioFormController {

    // fx:id del FXML
    @FXML private TextField txtDni, txtApellido, txtNombre, txtEmail, txtTelefono, txtDomicilio;
    @FXML private DatePicker dpFechaNac, dpFechaBaja;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar, btnCancelar;
    @FXML private CheckBox chkApto;
    @FXML private DatePicker dpAptoEmision;

    private final AptoMedicoDao aptoDao = new AptoMedicoDaoImpl();
    private final SocioDao socioDao = new SocioDaoImpl();
    private Socio socio = new Socio();   // estado en edición
    private boolean saved = false;

    // --- Inicialización: tomar socio del contexto y refrescar desde DB ---
    @FXML
    public void initialize() {
        try {
            // Si hay socio en el contexto, es edición; si no, alta
            Socio ctx = SelectionContext.getSocioActual();

            if (ctx != null && ctx.getId() != null) {
                // Refrescar desde DB para traer datos actuales
                socio = socioDao.buscarPorId(ctx.getId())
                        .orElse(ctx); // por las dudas, si no está, usamos el del contexto
                Navigation.setSectionTitle("Editar socio");
            } else {
                socio = new Socio();
                Navigation.setSectionTitle("Alta de socio");
            }

            // Si no hay estado, por defecto ACTIVO en alta
            if (socio.getEstado() == null) {
                chkActivo.setSelected(true);
                socio.setEstado(EstadoSocio.ACTIVO);
            }

            if (dpAptoEmision != null) dpAptoEmision.setValue(java.time.LocalDate.now());

            // Poblar UI
            fillFormFromModel();

        } catch (Exception e) {
            error("No se pudo cargar los datos del socio:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Botones ---
    @FXML
    private void onGuardar() {
        try {
            dumpFormToModel(); // UI -> modelo

            if (socio.getId() == null) {
                long id = socioDao.crear(socio);
                socio.setId(id);
            } else {
                socioDao.actualizar(socio);
            }
            saved = true;

            if (chkApto != null && chkApto.isSelected()) {
                var emision = (dpAptoEmision.getValue() == null ? java.time.LocalDate.now() : dpAptoEmision.getValue());
                var venc = emision.plusYears(1);
                try {
                    aptoDao.upsertApto(socio.getId(), emision, venc);
                } catch (Exception ex) {
                    error("El socio se guardó, pero no pude registrar el apto médico:\n" + ex.getMessage());
                }
            }


            // Volver al menú principal de Socios con mensaje de éxito
            info("Los datos del socio se guardaron correctamente.");
            Navigation.loadInMain("/socios-menu-view.fxml", "Socios");

        } catch (Exception e) {
            error("No se pudo guardar:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onCancelar() {
        saved = false;
        Navigation.backOr("/socios-menu-view.fxml", "Socios");
    }

    // --- Helpers: modelo -> UI ---
    private void fillFormFromModel() {
        txtDni.setText(nv(socio.getDni()));
        txtApellido.setText(nv(socio.getApellido()));
        txtNombre.setText(nv(socio.getNombre()));
        txtEmail.setText(nv(socio.getEmail()));
        txtTelefono.setText(nv(socio.getTelefono()));
        txtDomicilio.setText(nv(socio.getDomicilio()));
        dpFechaNac.setValue(socio.getFechaNac());
        dpFechaBaja.setValue(socio.getFechaBaja());
        if (chkApto != null) chkApto.setSelected(false);
        if (dpAptoEmision != null) dpAptoEmision.setValue(java.time.LocalDate.now());
        chkActivo.setSelected(socio.getEstado() == null || socio.getEstado() == EstadoSocio.ACTIVO);
    }

    // --- Helpers: UI -> modelo ---
    private void dumpFormToModel() {
        socio.setDni(req(txtDni.getText(), "El DNI es obligatorio."));
        socio.setApellido(req(txtApellido.getText(), "El apellido es obligatorio."));
        socio.setNombre(req(txtNombre.getText(), "El nombre es obligatorio."));
        socio.setEmail(nt(txtEmail.getText()));
        socio.setTelefono(nt(txtTelefono.getText()));
        socio.setDomicilio(nt(txtDomicilio.getText()));
        socio.setFechaNac(dpFechaNac.getValue());
        socio.setFechaBaja(dpFechaBaja.getValue());
        socio.setEstado(chkActivo.isSelected() ? EstadoSocio.ACTIVO : EstadoSocio.INACTIVO);
    }

    // --- API opcional para llamados externos ---
    public void setSocio(Socio s) {
        this.socio = (s != null ? s : new Socio());
        fillFormFromModel();
    }
    public Socio getSocio() { return socio; }
    public boolean isSaved() { return saved; }

    // --- Utiles ---
    private static String nv(String s) { return s == null ? "" : s; }
    private static String nt(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private static String req(String s, String msg) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(msg);
        return s.trim();
    }
    private static void info(String m){ new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
    private static void error(String m){ new Alert(Alert.AlertType.ERROR, m).showAndWait(); }
}
