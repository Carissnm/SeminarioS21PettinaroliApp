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
import java.util.Locale;
import java.util.Optional;

public class InscripcionFormController {

    // ====== UI (fx:id deben existir en el FXML) ======
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

    // ====== Init ======
    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Socios");

        // Campo DNI listo para tipear
        if (txtSocioDni != null) {
            txtSocioDni.setEditable(true);
            txtSocioDni.setDisable(false);
        }

        // Fecha por defecto = hoy
        if (dpFechaAlta != null && dpFechaAlta.getValue() == null) {
            dpFechaAlta.setValue(LocalDate.now());
        }

        // Precio de alta es informativo: viene de la actividad
        if (txtPrecioAlta != null) {
            txtPrecioAlta.setEditable(false);
        }

        // Cargar actividades ACTIVAS
        cargarActividades();

        // Si vengo del detalle, tomar el socio del contexto
        var socioCtx = SelectionContext.getSocioActual();
        if (socioCtx != null) {
            this.socioSel = socioCtx;
            if (txtSocioDni != null) txtSocioDni.setText(socioSel.getDni());
            pintarSocioSeleccionado();
        } else {
            pintarSocioSeleccionado();
        }
    }

    private void cargarActividades() {
        try {
            List<Actividad> actList = actividadDao.listarActivas();
            cbActividad.getItems().setAll(actList);

            // Render bonito: nombre en lista/botón
            cbActividad.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Actividad a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a == null ? "" : a.getNombre());
                }
            });
            cbActividad.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Actividad a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a == null ? "" : a.getNombre());
                }
            });

        } catch (Exception e) {
            error("No se pudieron cargar las actividades:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // ====== Búsqueda de socio ======

    private void setSocioSeleccionado(Socio s) {
        this.socioSel = s;
        if (txtSocioDni != null) txtSocioDni.setText(s.getDni());
        pintarSocioSeleccionado();
    }
    /** ENTER dentro del TextField del DNI */
    @FXML
    private void onBuscarSocioPorDni() {
        String dni = txtSocioDni.getText() == null ? "" : txtSocioDni.getText().trim();
        if (dni.isEmpty()) { warn("Ingresá un DNI (o prefijo) para buscar."); return; }

        try {
            var hallados = socioDao.buscarPorDni(dni);
            if (hallados.isEmpty()) {
                socioSel = null; pintarSocioSeleccionado();
                warn("No se encontró socio con ese DNI/prefijo.");
                return;
            }

            // 1) Match exacto
            var exacto = hallados.stream().filter(s -> dni.equalsIgnoreCase(s.getDni())).findFirst();
            if (exacto.isPresent()) { setSocioSeleccionado(exacto.get()); return; }

            // 2) Si hay 1 solo, tomarlo
            if (hallados.size() == 1) { setSocioSeleccionado(hallados.get(0)); return; }

            // 3) Varios -> ChoiceDialog
            var opciones = hallados.stream()
                    .map(s -> s.getDni() + " - " + s.getApellido() + ", " + s.getNombre())
                    .toList();

            var dlg = new ChoiceDialog<>(opciones.get(0), opciones);
            dlg.setTitle("Seleccionar socio");
            dlg.setHeaderText("Se encontraron varios socios");
            dlg.setContentText("Elegí uno:");
            dlg.showAndWait().ifPresent(sel -> {
                // <-- ACÁ definimos dniElegido
                String dniElegido = sel.split(" - ", 2)[0];
                hallados.stream()
                        .filter(s -> s.getDni().equals(dniElegido))
                        .findFirst()
                        .ifPresent(this::setSocioSeleccionado);
            });

        } catch (Exception e) {
            error("Error buscando socio:\n" + e.getMessage());
            e.printStackTrace();
        }
    }
    private void actualizarHabilitados() {
        boolean ok = (socioSel != null && cbActividad != null && cbActividad.getValue() != null);
        if (btnGuardar != null) btnGuardar.setDisable(!ok);
    }

    /** Botón “Buscar…” (ir a pantalla dedicada de búsqueda) */
    @FXML
    private void onBuscarSocio() {
        Navigation.loadInMain("/socios-buscar-view.fxml", "Socios");
    }

    private void buscarSocioPorDni() {
        String dni = txtSocioDni.getText() == null ? "" : txtSocioDni.getText().trim();
        if (dni.isEmpty()) { warn("Ingresá un DNI para buscar."); return; }

        try {
            List<Socio> hallados = socioDao.buscarPorDni(dni);
            if (hallados.isEmpty()) { warn("No se encontró socio con ese DNI/prefijo."); return; }

            // Preferir match exacto; si no hay, usar único resultado; si hay varios, pedir búsqueda avanzada
            Optional<Socio> exacto = hallados.stream()
                    .filter(s -> dni.equalsIgnoreCase(s.getDni()))
                    .findFirst();

            if (exacto.isPresent()) {
                socioSel = exacto.get();
            } else if (hallados.size() == 1) {
                socioSel = hallados.get(0);
            } else {
                warn("Se encontraron varios socios para ese prefijo. Usá el botón Buscar…");
                return;
            }

            // Mostrar el DNI confirmado (si querés, podrías mostrar apellido/nombre en otro campo)
            txtSocioDni.setText(socioSel.getDni());

        } catch (Exception e) {
            error("Error buscando socio:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

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


    // ====== Actividad seleccionada → precio_default al campo ======
    @FXML
    private void onActividadChange() {
        Actividad a = cbActividad.getValue();
        if (a == null || a.getPrecioDefault() == null) {
            txtPrecioAlta.setText("");
        } else {
            // Mostrar precio tal cual (podrías formatear si preferís)
            txtPrecioAlta.setText(a.getPrecioDefault().toPlainString());
        }
    }

    // ====== Guardar inscripción ======
    @FXML
    private void onGuardarInscripcion() {
        try {
            // 1️⃣ Validar DNI del socio
            String dni = txtSocioDni.getText() == null ? "" : txtSocioDni.getText().trim();
            if (dni.isEmpty()) {
                warn("Ingresá un DNI de socio válido.");
                return;
            }

            if (socioSel == null) { warn("Seleccioná o buscá un socio."); return; }
            if (socioSel.getEstado() != null && "INACTIVO".equals(socioSel.getEstado().name())) {
                warn("El socio está INACTIVO. No se puede inscribir.");
                return;
            }

            // 2️⃣ Buscar el socio en base de datos
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

            // 3️⃣ Validar actividad
            Actividad act = cbActividad.getValue();
            if (act == null) {
                warn("Seleccioná una actividad.");
                return;
            }

            LocalDate fecha = dpFechaAlta != null && dpFechaAlta.getValue() != null
                    ? dpFechaAlta.getValue() : LocalDate.now();

            BigDecimal precio = act.getPrecioDefault();
            if (precio == null || precio.signum() <= 0) {
                warn("La actividad no tiene precio configurado.");
                return;
            }

            // 4️⃣ Intentar inscribir
            inscripcionDao.inscribir(socioSel.getId(), act.getId(), precio, "Inscripción a " + act.getNombre());

            info("Inscripción realizada con éxito.");
            Navigation.loadInMain("/inscripcion-menu-view.fxml", "Socios");

        } catch (IllegalStateException ex) {
            error(ex.getMessage());
        } catch (Exception e) {
            error("No se pudo inscribir:\n" + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private void onVolver() {
        Navigation.backOr("/home-view.fxml", "Socios");
    }

    @FXML
    private void onCancelar() {
        Navigation.loadInMain("/inscripcion-menu-view.fxml", "Socios");
    }

    // ====== Helpers ======
    private static void info(String m){ new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
    private static void warn(String m){ new Alert(Alert.AlertType.WARNING, m).showAndWait(); }
    private static void error(String m){ new Alert(Alert.AlertType.ERROR, m).showAndWait(); }
}
