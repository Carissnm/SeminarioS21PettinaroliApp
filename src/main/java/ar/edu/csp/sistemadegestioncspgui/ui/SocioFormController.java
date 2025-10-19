package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.SocioDao;
import ar.edu.csp.sistemadegestioncspgui.dao.SocioDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.model.EstadoSocio;
import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;

public class SocioFormController {

    // fx:id EXACTOS del FXML
    @FXML private TextField txtDni, txtApellido, txtNombre, txtEmail, txtTelefono, txtDomicilio;
    @FXML private DatePicker dpFechaNac, dpFechaBaja;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar, btnCancelar;

    private final SocioDao socioDao = new SocioDaoImpl();
    private Socio socio = new Socio();   // por defecto “alta”
    private boolean saved = false;

    // --- API para el caller (SocioForm.showDialog) ---
    public void setSocio(Socio s) {
        this.socio = (s != null ? s : new Socio());
        fillFormFromModel();
    }
    public Socio getSocio() { return socio; }
    public boolean isSaved() { return saved; }

    @FXML
    public void initialize() {
        // valores por defecto en ALTA
        chkActivo.setSelected(true);
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

            // ✅ Mostrar éxito y volver al MENÚ de Socios
            ButtonType CONTINUAR = new ButtonType("Continuar", ButtonBar.ButtonData.OK_DONE);
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "El socio se guardó con éxito.", CONTINUAR);
            ok.setHeaderText(null);
            ok.showAndWait();

            navigateToSociosMenu();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "No se pudo guardar:\n" + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    private void onCancelar() {
        saved = false;
        // ❗ Sin cerrar ventana principal: simplemente volvemos al MENÚ Socios
        navigateToSociosMenu();
    }

    private void navigateToSociosMenu() {
        Navigation.loadInMain("/socios-menu-view.fxml", "Socios");
    }

    /** Cierra SOLO si es un diálogo modal; si no, navega sin cerrar la ventana principal. */
    private void closeIfDialogOrNavigateBack() {
        Window w = btnCancelar.getScene().getWindow();
        if (w instanceof Stage s && s.getOwner() != null) {
            // Es un diálogo modal -> cerrar
            s.close();
        } else {
            // Está embebido en el main -> NO cerrar la app; volver al menú/lista
            Navigation.loadInMain("/socios-menu-view.fxml", "Socios");
        }
    }

    // --- Helper: modelo -> UI
    private void fillFormFromModel() {
        txtDni.setText(nv(socio.getDni()));
        txtApellido.setText(nv(socio.getApellido()));
        txtNombre.setText(nv(socio.getNombre()));
        txtEmail.setText(nv(socio.getEmail()));
        txtTelefono.setText(nv(socio.getTelefono()));
        txtDomicilio.setText(nv(socio.getDomicilio()));
        dpFechaNac.setValue(socio.getFechaNac());
        dpFechaBaja.setValue(socio.getFechaBaja());
        chkActivo.setSelected(socio.getEstado() == null || socio.getEstado() == EstadoSocio.ACTIVO);
    }

    // --- Helper: UI -> modelo
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

    private static String nv(String s) { return s == null ? "" : s; }
    private static String nt(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private static String req(String s, String msg) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(msg);
        return s.trim();
    }
}
