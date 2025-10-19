package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class SocioDetalleController {

    @FXML private TextField txtId;
    @FXML private TextField txtApellido;
    @FXML private TextField txtNombre;
    @FXML private TextField txtDocumento;
    @FXML private TextField txtEstado;

    private Socio socio;

    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Socios");
        Socio s = SelectionContext.getSocioActual();
        if (s != null) setSocio(s);
    }

    public void setSocio(Socio socio) {
        this.socio = socio;
        txtId.setText(socio.getId() != null ? socio.getId().toString() : "");
        txtApellido.setText(ns(socio.getApellido()));
        txtNombre.setText(ns(socio.getNombre()));
        txtDocumento.setText(ns(socio.getDni()));
        txtEstado.setText(socio.getEstado() != null ? socio.getEstado().name() : "");
    }

    @FXML private void editar() { SelectionContext.setSocioActual(socio); Navigation.loadInMain("/socio-form-view.fxml", "Socios"); }
    @FXML private void inscribir() { SelectionContext.setSocioActual(socio); Navigation.loadInMain("/inscripcion-menu-view.fxml", "Socios"); }
    @FXML private void verInscripciones() { SelectionContext.setSocioActual(socio); Navigation.loadInMain("/inscripcion-menu-view.fxml", "Socios"); }
    @FXML private void registrarPago() { SelectionContext.setSocioActual(socio); Navigation.loadInMain("ui/PagoForm.fxml", "Socios"); }
    @FXML private void volver() { Navigation.back(); }

    private static String ns(String s) { return s == null ? "" : s; }
}
