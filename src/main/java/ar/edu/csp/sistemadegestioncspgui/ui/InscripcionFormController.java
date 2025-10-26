package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.ActividadDao;
import ar.edu.csp.sistemadegestioncspgui.dao.ActividadDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.dao.InscripcionDao;
import ar.edu.csp.sistemadegestioncspgui.dao.InscripcionDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.dao.SocioDao;
import ar.edu.csp.sistemadegestioncspgui.dao.SocioDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.model.Actividad;
import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

//Controlador del formulario de alta para la inscripción de un socio a una actividad.
//Permite ingresar/buscar un socio por DNI, cargar y listar las actividades activas
//Mostrar la fecha de alta (del día de la inscripción por defecto) y el precio de alta de la actividad
//Valida reglas básicas antes de inscribir y ejecuta la inscripción además de registar el cargo en la cuenta del socio.
public class InscripcionFormController extends BaseController {

    // Inyección desde FXML
    @FXML private TextField txtSocioDni;
    @FXML private Label lblSocioSel;
    @FXML private ComboBox<Actividad> cbActividad;
    @FXML private DatePicker dpFechaAlta;
    @FXML private TextField txtPrecioAlta;
    @FXML private Button btnGuardar;

    // ====== DAOs ======
    private final SocioDao socioDao = new SocioDaoImpl();
    private final ActividadDao actividadDao = new ActividadDaoImpl();
    private final InscripcionDao inscripcionDao = new InscripcionDaoImpl();

    // ====== Estado ======
    private Socio socioSel;  // socio seleccionado/encontrado

    // ====== Inicialización ======
    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Socios");

        // Campo DNI listo para tipear
        if (txtSocioDni != null) {
            txtSocioDni.setEditable(true);
            txtSocioDni.setDisable(false);
        }

        // Fecha por defecto = fecha en la que se está realizando la inscripción
        if (dpFechaAlta != null && dpFechaAlta.getValue() == null) {
            dpFechaAlta.setValue(LocalDate.now());
        }

        // El precio de alta de la actividad es informativo y no se puede editar desde este campo.
        if (txtPrecioAlta != null) {
            txtPrecioAlta.setEditable(false);
        }

        // Carga de actividades ACTIVAS
        cargarActividades();

        // Si se viene desde el detalle del socio se toma el socio del contexto
        var socioCtx = SelectionContext.getSocioActual();
        if (socioCtx != null) {
            this.socioSel = socioCtx;
            if (txtSocioDni != null) txtSocioDni.setText(socioSel.getDni());
            pintarSocioSeleccionado();
        } else {
            pintarSocioSeleccionado(); //se limpia si no hay socio
        }
    }
    //Carga de actividades con estado Activa y configuración del render del ComboBox para mostrar solo el nombre.
    private void cargarActividades() {
        try {
            List<Actividad> actList = actividadDao.listarActivas();
            cbActividad.getItems().setAll(actList);

            // Render de los items del combo
            cbActividad.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Actividad a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a == null ? "" : a.getNombre());
                }
            });
            // Render del botón (elemento seleccionado) del combo
            cbActividad.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Actividad a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a == null ? "" : a.getNombre());
                }
            });

        } catch (Exception e) {
            error("No fue posible cargar las actividades:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // ====== Búsqueda de socio ======

    //Setter de selección y reflejo en la Interfaz de Usuario
    private void setSocioSeleccionado(Socio s) {
        this.socioSel = s;
        if (txtSocioDni != null) txtSocioDni.setText(s.getDni());
        pintarSocioSeleccionado();
    }
    //El metodo onBuscarSocioPorDni() ejecuta la búsqueda parcial por prefijo del dni de un socio.
    @FXML
    private void onBuscarSocioPorDni() {
        String dni = txtSocioDni.getText() == null ? "" : txtSocioDni.getText().trim();
        if (dni.isEmpty()) { warn("Ingrese un DNI (o prefijo) para buscar."); return; }

        try {
            var hallados = socioDao.buscarPorDni(dni);
            if (hallados.isEmpty()) {
                socioSel = null; pintarSocioSeleccionado();
                warn("No se encontró socio con ese DNI/prefijo.");
                return;
            }

            // 1) Coincidencia exacta
            var exacto = hallados.stream().filter(s -> dni.equalsIgnoreCase(s.getDni())).findFirst();
            if (exacto.isPresent()) {
                setSocioSeleccionado(exacto.get());
                return;
            }

            // 2) Si hay un único resultado se toma
            if (hallados.size() == 1) {
                setSocioSeleccionado(hallados.get(0));
                return;
            }

            // 3) Si existen varias opciones en la lista se muestra un ChoiceDialog para elegir
            var opciones = hallados.stream()
                    .map(s -> s.getDni() + " - " + s.getApellido() + ", " + s.getNombre())
                    .toList();

            var dlg = new ChoiceDialog<>(opciones.get(0), opciones);
            dlg.setTitle("Seleccionar socio");
            dlg.setHeaderText("Se encontraron varios socios");
            dlg.setContentText("Elija uno:");
            dlg.showAndWait().ifPresent(sel -> {
                // Dni elegido antes de " - "
                String dniElegido = sel.split(" - ", 2)[0];
                hallados.stream()
                        .filter(s -> s.getDni().equals(dniElegido))
                        .findFirst()
                        .ifPresent(this::setSocioSeleccionado);
            });

        } catch (Exception e) {
            error("Error en la búsqueda del socio:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // Habilita/Deshabilita el botón Guardar en función de que haya socio y actividad seleccionados
    private void actualizarHabilitados() {
        boolean ok = (socioSel != null && cbActividad != null && cbActividad.getValue() != null);
        if (btnGuardar != null) btnGuardar.setDisable(!ok);
    }


    //Búsqueda de socio por dni
    private void buscarSocioPorDni() {
        String dni = txtSocioDni.getText() == null ? "" : txtSocioDni.getText().trim();
        if (dni.isEmpty()) { warn("Ingrese un DNI para buscar."); return; }

        try {
            List<Socio> hallados = socioDao.buscarPorDni(dni);
            if (hallados.isEmpty()) {
                warn("No se encontró ningún socio con ese DNI/prefijo.");
                return;
            }
            Optional<Socio> exacto = hallados.stream()
                    .filter(s -> dni.equalsIgnoreCase(s.getDni()))
                    .findFirst();

            if (exacto.isPresent()) {
                socioSel = exacto.get();
            } else if (hallados.size() == 1) {
                socioSel = hallados.get(0);
            } else {
                warn("Se encontraron varios socios para ese prefijo. Utilice el botón Buscar…");
                return;
            }

            // Se muestra el DNI confirmado
            txtSocioDni.setText(socioSel.getDni());

        } catch (Exception e) {
            error("Error en la búsqueda del socio:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // Este metodo muestra un resumen del socio seleccionado
    private void pintarSocioSeleccionado() {
        if (lblSocioSel == null) return;
        if (socioSel == null) {
            lblSocioSel.setText("");
            lblSocioSel.setTooltip(null);
            return;
        }
        String texto = String.format("%s (%s) – %s  |  Saldo: %s",
                socioSel.getNombreCompleto(),
                socioSel.getDni(),
                socioSel.getEstado() == null ? "" : socioSel.getEstado().name(),
                socioSel.getSaldo() == null ? "$0" : socioSel.getSaldo().toPlainString());
        lblSocioSel.setText(texto);

        var tip = new Tooltip(texto);
        lblSocioSel.setTooltip(tip);
    }


    @FXML
    private void onActividadChange() {
        Actividad a = cbActividad.getValue();
        if (a == null || a.getPrecioDefault() == null) {
            txtPrecioAlta.setText("");
        } else {
            // Se muestra el precio como figura en la base de datos
            txtPrecioAlta.setText(a.getPrecioDefault().toPlainString());
        }
    }

    // Metodo para registrar la inscripción de un socio a una determinada actividad.
    @FXML
    private void onGuardarInscripcion() {
        try {
            // Validación del DNI del socio
            String dni = txtSocioDni.getText() == null ? "" : txtSocioDni.getText().trim();
            if (dni.isEmpty()) {
                warn("Ingrese un DNI de socio válido.");
                return;
            }

            if (socioSel == null) {
                warn("Seleccione o busque un socio."); return;
            }

            if (socioSel.getEstado() != null && "INACTIVO".equals(socioSel.getEstado().name())) {
                warn("El socio se encuentra INACTIVO. No es posible su inscripción.");
                return;
            }

            // Búsqueda del socio en la base de datos
            List<Socio> hallados = socioDao.buscarPorDni(dni);
            if (hallados.isEmpty()) {
                warn("No existe ningún socio con ese DNI.");
                return;
            }

            // Si hay varios con mismo prefijo → tomar exacto o pedir selección
            Optional<Socio> exacto = hallados.stream()
                    .filter(s -> dni.equalsIgnoreCase(s.getDni()))
                    .findFirst();

            if (exacto.isPresent()) {
                socioSel = exacto.get();
            } else if (hallados.size() == 1) {
                socioSel = hallados.get(0);
            } else {
                warn("Se encontraron varios socios con ese prefijo. Usá el botón 'Buscar…'.");
                return;
            }

            // Validación de la actividad
            Actividad act = cbActividad.getValue();
            if (act == null) {
                warn("Seleccione una actividad.");
                return;
            }

            LocalDate fecha = dpFechaAlta != null && dpFechaAlta.getValue() != null
                    ? dpFechaAlta.getValue() : LocalDate.now();

            BigDecimal precio = act.getPrecioDefault();
            if (precio == null || precio.signum() <= 0) {
                warn("La actividad no posee precio configurado.");
                return;
            }

            // Inscripción. El DAO valida el apto vigente y el estado del socio.
            inscripcionDao.inscribir(socioSel.getId(), act.getId(), precio, "Inscripción a " + act.getNombre());

            info("Inscripción realizada con éxito.");
            Navigation.loadInMain("/inscripcion-menu-view.fxml", "Socios");

        } catch (IllegalStateException ex) {
            //Frente a reglas de negocio fallidas
            error(ex.getMessage());
        } catch (Exception e) {
            error("No fue posible realizar la inscripción:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    //Para regresar a la pantalla anterior o a Home si no hay historial
    @FXML
    private void onVolver() {
        Navigation.backOr("/home-view.fxml", "Socios");
    }

    //Permite cancelar el flujo y volver al menú de inscripciones.
    @FXML
    private void onCancelar() {
        Navigation.loadInMain("/inscripcion-menu-view.fxml", "Socios");
    }
}
