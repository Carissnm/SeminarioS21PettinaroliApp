package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;

public class InscripcionFormController {

    @FXML private TextField txtSocio;
    @FXML private ComboBox<String> cmbActividad; // cambiá a tu tipo Actividad si ya lo tenés
    @FXML private DatePicker dpFechaAlta;
    @FXML private TextField txtPrecioAlta;

    private final ObservableList<String> actividades = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        Navigation.setSectionTitle("Socios"); // o "Inscripciones"
        dpFechaAlta.setValue(LocalDate.now());

        // TODO: reemplazar con datos reales
        actividades.setAll("Fútbol", "Natación", "Tenis");
        cmbActividad.setItems(actividades);

        Socio socio = SelectionContext.getSocioActual();
        if (socio != null) {
            txtSocio.setText(socio.getApellido() + ", " + socio.getNombre() + " (ID " + socio.getId() + ")");
        }
    }

    @FXML private void buscarSocio() { Navigation.loadInMain("ui/SociosList.fxml", "Socios"); }

    @FXML
    private void guardar() {
        String actividad = cmbActividad.getValue();
        LocalDate fechaAlta = dpFechaAlta.getValue();
        if (txtSocio.getText().isBlank() || actividad == null || fechaAlta == null) {
            new Alert(Alert.AlertType.WARNING, "Completá los campos requeridos.", ButtonType.OK).showAndWait();
            return;
        }
        // TODO: persistir inscripción
        new Alert(Alert.AlertType.INFORMATION, "Inscripción guardada.", ButtonType.OK).showAndWait();
        Navigation.back();
    }

    @FXML private void cancelar() { Navigation.back(); }
}
