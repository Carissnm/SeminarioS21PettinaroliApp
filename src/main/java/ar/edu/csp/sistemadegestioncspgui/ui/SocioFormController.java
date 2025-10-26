package ar.edu.csp.sistemadegestioncspgui.ui;
import ar.edu.csp.sistemadegestioncspgui.dao.AptoMedicoDao;
import ar.edu.csp.sistemadegestioncspgui.dao.AptoMedicoDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.dao.SocioDao;
import ar.edu.csp.sistemadegestioncspgui.dao.SocioDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.model.EstadoSocio;
import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.fxml.FXML;
import javafx.scene.control.*;

// Controlador del formulario del socio, tanto para Edición de un socio existente como para el Alta del socio nuevo
public class SocioFormController extends BaseController {
    // Inyección de controles
    @FXML private TextField txtDni, txtApellido, txtNombre, txtEmail, txtTelefono, txtDomicilio;
    @FXML private DatePicker dpFechaNac, dpFechaBaja;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar, btnCancelar;
    @FXML private CheckBox chkApto;
    @FXML private DatePicker dpAptoEmision;
    // Objetos DAO que encapsulan la lógica de acceso a la base de datos.
    // Se usan para crear/actualizar socios y registrar aptos médicos.
    private final AptoMedicoDao aptoDao = new AptoMedicoDaoImpl();
    private final SocioDao socioDao = new SocioDaoImpl();
    // Instancia de socio para vincularlo con la Interfaz de Usuario
    private Socio socio = new Socio();
    private boolean saved = false;

    // CICLO DE VIDA
    // Inicialización del formulario
    @FXML
    public void initialize() {
        try {
            // Si hay socio en el contexto, es modificación de datos del socio existente ; de lo contrario es un alta
            Socio ctx = SelectionContext.getSocioActual();

            if (ctx != null && ctx.getId() != null) {
                // Se refresca desde la base de datos para traer datos actuales
                socio = socioDao.buscarPorId(ctx.getId())
                        .orElse(ctx);
                Navigation.setSectionTitle("Editar socio");
            } else {
                socio = new Socio();
                Navigation.setSectionTitle("Alta de socio");
            }

            // Si el estado es null se considera por default Activo
            if (socio.getEstado() == null) {
                chkActivo.setSelected(true);
                socio.setEstado(EstadoSocio.ACTIVO);
            }

            // Si no se aclara una fecha de emisión del apto médico se establece
            //por defecto la fecha en la que se registra la entrega del apto.
            if (dpAptoEmision != null) dpAptoEmision.setValue(java.time.LocalDate.now());

            // Este metodo lleva los datos del modelo a la Interfaz de Usuario
            fillFormFromModel();

        } catch (Exception e) {
            error("No se pudo cargar los datos del socio:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // ======= Acciones =======
    @FXML
    private void onGuardar() {
        try {
            // Toma los datos de la Interfaz de Usuario y los valida en el modelo
            dumpFormToModel();
            // Para la persistencia, si no tiene Id lo crea y si existe lo actualiza
            if (socio.getId() == null) {
                long id = socioDao.crear(socio);
                socio.setId(id); // settea la primary key generada
            } else {
                socioDao.actualizar(socio);
            }
            saved = true;
            // Si el administrador marcó que se entrega un apto se hace un upsert del apto médico
            if (chkApto != null && chkApto.isSelected()) {
                var emision = (dpAptoEmision.getValue() == null ? java.time.LocalDate.now() : dpAptoEmision.getValue());
                var venc = emision.plusYears(1); // para estipular una vigencia de un año desde la fecha registrada de emisión
                try {
                    aptoDao.upsertApto(socio.getId(), emision, venc);
                } catch (Exception ex) {
                    //Si falla la carga del apto médico aún así se continúa con el proceso de alta/edición, no se revierte.
                    error("El socio fue registrado, pero no fue posible registrar el apto médico:\n" + ex.getMessage());
                }
            }
            // Se vuelve al menú principal de Socios con mensaje de éxito
            info("Los datos del socio fueron guardados correctamente.");
            Navigation.loadInMain("/socios-menu-view.fxml", "Socios");
        } catch (Exception e) {
            error("No fue posible guardar:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

        @FXML
    private void onCancelar() {
        // Permite salir del formulario sin guardar cambios
        saved = false;
        Navigation.backOr("/socios-menu-view.fxml", "Socios");
    }

    // SINCRONIZACIÓN ENTRE EL MODELO Y LA INTERFAZ DE USUARIO
    private void fillFormFromModel() {
        txtDni.setText(nv(socio.getDni()));
        txtApellido.setText(nv(socio.getApellido()));
        txtNombre.setText(nv(socio.getNombre()));
        txtEmail.setText(nv(socio.getEmail()));
        txtTelefono.setText(nv(socio.getTelefono()));
        txtDomicilio.setText(nv(socio.getDomicilio()));
        dpFechaNac.setValue(socio.getFechaNac());
        dpFechaBaja.setValue(socio.getFechaBaja());
        if (chkApto != null) chkApto.setSelected(false); // por defecto no se registra apto médico
        if (dpAptoEmision != null) dpAptoEmision.setValue(java.time.LocalDate.now());
        chkActivo.setSelected(socio.getEstado() == null || socio.getEstado() == EstadoSocio.ACTIVO);
    }

    // Envío de toda la información del formulario para la carga/modificación de la base de datos.
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

    // Metodo pcional para llamados externos
    public void setSocio(Socio s) {
        this.socio = (s != null ? s : new Socio());
        fillFormFromModel();
    }
    public Socio getSocio() { return socio; }
    public boolean isSaved() { return saved; }

    // Utilidades
    private static String nt(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    //Lanza un IllegalArgumentException si viene vacío.
    private static String req(String s, String msg) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(msg);
        return s.trim();
    }
}
