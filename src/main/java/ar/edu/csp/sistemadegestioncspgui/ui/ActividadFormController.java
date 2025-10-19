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

public class ActividadFormController {
    @FXML private TextField txtNombre, txtDescripcion, txtPrecio;
    @FXML private ComboBox<EstadoActividad> cbEstado;

    private final ActividadDao actividadDao = new ActividadDaoImpl();

    private Actividad actividad; // copia editable
    private boolean ok;

    @FXML
    private void initialize() {
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

        ensureActividad();
        if (cbEstado.getValue() == null) cbEstado.setValue(EstadoActividad.ACTIVA);
    }

    /** Si nadie llamó setActividad(...) (alta embebida), aseguramos un objeto nuevo. */
    private void ensureActividad() {
        if (this.actividad == null) {
            this.actividad = new Actividad();
            this.actividad.setEstado(EstadoActividad.ACTIVA);
            this.actividad.setPrecioDefault(BigDecimal.ZERO);
        }
    }

    /** Para edición: el caller puede pasar la actividad a editar. */
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

    @FXML private void onGuardar() {
        try {
            ensureActividad();

            if (txtNombre.getText()==null || txtNombre.getText().trim().isEmpty()) {
                warn("El nombre es obligatorio"); return;
            }
            BigDecimal precio;
            try {
                String raw = (txtPrecio.getText()==null? "" : txtPrecio.getText()).trim().replace(",", ".");
                precio = raw.isEmpty()? BigDecimal.ZERO : new BigDecimal(raw);
                if (precio.signum() < 0) { warn("El precio no puede ser negativo"); return; }
            } catch (NumberFormatException nfe) { warn("Precio inválido"); return; }

            actividad.setNombre(txtNombre.getText().trim());
            actividad.setDescripcion(txtDescripcion.getText()==null? "" : txtDescripcion.getText().trim());
            actividad.setPrecioDefault(precio);
            actividad.setEstado(cbEstado.getValue()==null? EstadoActividad.ACTIVA : cbEstado.getValue());

            // 🔸 PERSISTENCIA REAL
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

            // Volvemos al menú de actividades (embebido)
            closeIfDialogOrNavigateBack();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "No se pudo guardar:\n" + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    @FXML private void onCancelar() {
        ok = false;
        closeIfDialogOrNavigateBack();
    }

    private void warn(String msg) { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }

    /** Cierra solo si es diálogo; si no, vuelve al menú Actividades en la ventana principal. */
    private void closeIfDialogOrNavigateBack() {
        Window w = txtNombre.getScene() != null ? txtNombre.getScene().getWindow() : null;
        if (w instanceof Stage s && s.getOwner() != null) {
            s.close();
        } else {
            Navigation.loadInMain("/actividades-menu-view.fxml", "Actividades");
        }
    }

    public boolean isOk() { return ok; }
    public Actividad getResultado() { return actividad; }
}
