package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.ActividadDao;
import ar.edu.csp.sistemadegestioncspgui.dao.ActividadDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.model.Actividad;
import ar.edu.csp.sistemadegestioncspgui.model.EstadoActividad;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
//Controlador que carga los estados posibles en el ComboBox y settea el render con toLabel()
//Si viene de una actividad externa copia sus datos para la edición
//Valida el nombre y el precio setteando activa por default
//Persiste llamando a ActividadDao para crear si id=null o modificar si el id existe.
public class ActividadFormController {
    //Campos del formulario inyectados de FXML
    @FXML private TextField txtNombre, txtDescripcion, txtPrecio;
    @FXML private ComboBox<EstadoActividad> cbEstado;

    //Acceso a los Datos de la actividad
    private final ActividadDao actividadDao = new ActividadDaoImpl();

    //Estado interno
    private Actividad actividad; // copia editable que se asegura con ensureActividad()
    private boolean ok;

    @FXML
    private void initialize() {
        //Carga de los estados ene l combo y renderización con etiquetas
        cbEstado.getItems().setAll(EstadoActividad.values());
        cbEstado.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(EstadoActividad item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item==null ? "" : item.toLabel());
            }
        });
        cbEstado.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(EstadoActividad item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item==null ? "" : item.toLabel());
            }
        });
        //En el caso en el que nadie cargó una actividad aún se crea una por defecto.
        ensureActividad();
        //Estado por defecto como Activa si el administrador no la elige.
        if (cbEstado.getValue() == null) cbEstado.setValue(EstadoActividad.ACTIVA);
    }

    // Garantiza qeu haya un objeto Actividad editable con valores por defecto.
    private void ensureActividad() {
        if (this.actividad == null) {
            this.actividad = new Actividad();
            this.actividad.setEstado(EstadoActividad.ACTIVA);
            this.actividad.setPrecioDefault(BigDecimal.ZERO);
        }
    }

    //Carga datos para la edición. Se usa una copia para no tocar el objeto externo de manera directa.
    public void setActividad(Actividad a) {
        this.actividad = (a==null? new Actividad() : copy(a));
        if (a != null) {
            txtNombre.setText(a.getNombre());
            txtDescripcion.setText(a.getDescripcion());
            txtPrecio.setText(a.getPrecioDefault()==null? "" : a.getPrecioDefault().toPlainString());
            cbEstado.setValue(a.getEstado());
        } else {
            cbEstado.setValue(EstadoActividad.ACTIVA);
        }
    }

    //Creación de una copia preventiva para evitar daños colaterales sobre la instancia original.
    private Actividad copy(Actividad a) {
        Actividad x = new Actividad();
        x.setId(a.getId());
        x.setNombre(a.getNombre());
        x.setDescripcion(a.getDescripcion());
        x.setEstado(a.getEstado());
        x.setPrecioDefault(a.getPrecioDefault());
        x.setCreadoEn(a.getCreadoEn());
        x.setActualizadoEn(a.getActualizadoEn());
        return x;
    }

    //Guardado (de creación en caso de Alta de una actividad o modificación de una actividad preexistente) tras la validación de los inputs.
    @FXML private void onGuardar(javafx.event.ActionEvent err) {
        try {
            ensureActividad();
            //Validaciones mínimas.
            if (txtNombre.getText()==null || txtNombre.getText().trim().isEmpty()) {
                warn("El nombre es obligatorio"); return;
            }
            BigDecimal precio;
            try {
                String raw = (txtPrecio.getText()==null? "" : txtPrecio.getText()).trim().replace(",", ".");
                precio = raw.isEmpty()? BigDecimal.ZERO : new BigDecimal(raw);
                if (precio.signum() < 0) { warn("El precio no puede ser negativo"); return; }
            } catch (NumberFormatException nfe) { warn("Precio inválido"); return; }

            //Modelado de los datos finales.
            actividad.setNombre(txtNombre.getText().trim());
            actividad.setDescripcion(txtDescripcion.getText()==null? "" : txtDescripcion.getText().trim());
            actividad.setPrecioDefault(precio);
            actividad.setEstado(cbEstado.getValue()==null? EstadoActividad.ACTIVA : cbEstado.getValue());

            // Persistencia
            if (actividad.getId() == null) {
                long id = actividadDao.crear(actividad);
                actividad.setId(id);
            } else {
                actividadDao.actualizar(actividad);
            }

            ok = true;

            var f = NumberFormat.getCurrencyInstance(new Locale("es","AR"));
            new Alert(Alert.AlertType.INFORMATION,
                    "Actividad guardada con éxito:\n" +
                            "• " + actividad.getNombre() + "\n" +
                            "• Precio: " + f.format(actividad.getPrecioDefault()))
                    .showAndWait();

            // Se cierra si es diálogo o se navega al menú de actividades cuadno está embebido.
            closeIfDialogOrNavigateBack();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "No fue posible guardar:\n" + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    //Cancelación del alta o la modificación de una actividad.
    @FXML private void onCancelar(javafx.event.ActionEvent err) {
        ok = false;
        closeIfDialogOrNavigateBack();
    }

    //Alerta de validación
    private void warn(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    //Al estar embebido este metodo navega al menú actividades
    //si corriera como diálogo modal se cerraría el cuadro de diálogo.
    private void closeIfDialogOrNavigateBack() {
        Window w = txtNombre.getScene() != null ? txtNombre.getScene().getWindow() : null;
        if (w instanceof Stage s && s.getOwner() != null) {
            s.close();
        } else {
            Navigation.loadInMain("/actividades-menu-view.fxml", "Actividades");
        }
    }

    //Para cuadro de diálogo modal.
    public boolean isOk() { return ok; }
    public Actividad getResultado() { return actividad; }
}
