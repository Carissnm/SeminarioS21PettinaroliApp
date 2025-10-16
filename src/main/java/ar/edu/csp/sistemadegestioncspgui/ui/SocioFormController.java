package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public class SocioFormController {
    @FXML private TextField txtDni, txtApellido, txtNombre, txtEmail, txtTelefono, txtDomicilio;
    @FXML private DatePicker dpFechaNac, dpFechaBaja;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar, btnCancelar; // asegurate de setear fx:id en el FXML

    private Socio socio; // copia editable
    private boolean ok;

    @FXML
    private void initialize() {
        // Enter -> guardar | Esc -> cancelar
        txtNombre.getScene(); // (el Scene puede ser null en initialize; por eso usamos setOnKeyPressed en root en onShown si querés)
        // Bind básico: cuando cambia "Activo", habilita/deshabilita fecha de baja
        chkActivo.selectedProperty().addListener((obs, was, isSel) -> {
            dpFechaBaja.setDisable(isSel);
            if (isSel) dpFechaBaja.setValue(null);
        });

        // Handlers de teclado sobre controles principales
        setEnterEsc(txtDni);
        setEnterEsc(txtApellido);
        setEnterEsc(txtNombre);
        setEnterEsc(txtEmail);
        setEnterEsc(txtTelefono);
        setEnterEsc(txtDomicilio);
        dpFechaNac.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) onGuardar(); else if (e.getCode() == KeyCode.ESCAPE) onCancelar(); });
        dpFechaBaja.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) onGuardar(); else if (e.getCode() == KeyCode.ESCAPE) onCancelar(); });
    }

    private void setEnterEsc(Control c) {
        c.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onGuardar();
            else if (e.getCode() == KeyCode.ESCAPE) onCancelar();
        });
    }

    public void setSocio(Socio s) {
        socio = (s == null) ? new Socio() : copy(s);
        if (s != null) {
            txtDni.setText(s.getDni());
            txtApellido.setText(s.getApellido());
            txtNombre.setText(s.getNombre());
            txtEmail.setText(s.getEmail());
            txtTelefono.setText(s.getTelefono());
            txtDomicilio.setText(s.getDomicilio());
            dpFechaNac.setValue(s.getFechaNac());
            dpFechaBaja.setValue(s.getFechaBaja());
            chkActivo.setSelected(s.isActivo()); // <-- enum -> boolean
        } else {
            chkActivo.setSelected(true);
            dpFechaBaja.setDisable(true);
        }
    }

    private Socio copy(Socio s) {
        return new Socio(
                s.getId(), s.getDni(), s.getNombre(), s.getApellido(),
                s.getFechaNac(), s.getDomicilio(), s.getEmail(), s.getTelefono(),
                s.getEstado(), s.getFechaAlta(), s.getFechaBaja()
        );
    }

    @FXML
    private void onGuardar() {
        String dni = (txtDni.getText() == null ? "" : txtDni.getText().replaceAll("\\D", ""));
        if (dni.isBlank()) { warn("El DNI es obligatorio."); txtDni.requestFocus(); return; }
        if (txtApellido.getText() == null || txtApellido.getText().isBlank()) { warn("El apellido es obligatorio."); txtApellido.requestFocus(); return; }
        if (txtNombre.getText() == null || txtNombre.getText().isBlank()) { warn("El nombre es obligatorio."); txtNombre.requestFocus(); return; }

        // Si está activo, no debe tener fecha de baja
        if (chkActivo.isSelected() && dpFechaBaja.getValue() != null) {
            warn("Un socio ACTIVO no puede tener fecha de baja. Quitala o marcá como inactivo.");
            dpFechaBaja.requestFocus();
            return;
        }

        socio.setDni(dni);
        socio.setApellido(txtApellido.getText().trim());
        socio.setNombre(txtNombre.getText().trim());
        socio.setEmail((txtEmail.getText() == null) ? null : txtEmail.getText().trim().toLowerCase());
        socio.setTelefono((txtTelefono.getText() == null) ? null : txtTelefono.getText().trim());
        socio.setDomicilio((txtDomicilio.getText() == null) ? null : txtDomicilio.getText().trim());
        socio.setFechaNac(dpFechaNac.getValue());
        socio.setFechaBaja(chkActivo.isSelected() ? null : dpFechaBaja.getValue());
        socio.setActivo(chkActivo.isSelected()); // <-- enum via helper

        ok = true;
        close();
    }

    @FXML private void onCancelar() { ok = false; close(); }

    private void warn(String msg) { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }
    private void close() { ((Stage) txtDni.getScene().getWindow()).close(); }

    public boolean isOk() { return ok; }
    public Socio getResultado() { return socio; }
}
