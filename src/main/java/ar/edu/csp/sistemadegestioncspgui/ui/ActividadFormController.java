package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.model.Actividad;
import ar.edu.csp.sistemadegestioncspgui.model.EstadoActividad;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;

public class ActividadFormController {
    @FXML private TextField txtNombre, txtDescripcion, txtPrecio;
    @FXML private ComboBox<EstadoActividad> cbEstado;

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
    }

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
        if (txtNombre.getText().trim().isEmpty()) { warn("El nombre es obligatorio"); return; }
        BigDecimal precio;
        try {
            String raw = txtPrecio.getText().trim().replace(",", "."); // simple parser
            precio = raw.isEmpty()? BigDecimal.ZERO : new BigDecimal(raw);
            if (precio.signum() < 0) { warn("El precio no puede ser negativo"); return; }
        } catch (NumberFormatException nfe) { warn("Precio inválido"); return; }

        actividad.setNombre(txtNombre.getText().trim());
        actividad.setDescripcion(txtDescripcion.getText().trim());
        actividad.setPrecioDefault(precio);
        actividad.setEstado(cbEstado.getValue()==null? EstadoActividad.ACTIVA : cbEstado.getValue());

        ok = true;
        close();
    }

    @FXML private void onCancelar() { ok = false; close(); }

    private void warn(String msg) { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }
    private void close() { ((Stage) txtNombre.getScene().getWindow()).close(); }

    public boolean isOk() { return ok; }
    public Actividad getResultado() { return actividad; }
}
