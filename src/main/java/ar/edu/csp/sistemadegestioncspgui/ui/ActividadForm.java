package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.model.Actividad;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public class ActividadForm {
    public static Optional<Actividad> showDialog(Actividad actividad) {
        try {
            var loader = new FXMLLoader(ActividadForm.class.getResource("/actividad-form-view.fxml"));
            Parent root = loader.load();
            var ctl = (ActividadFormController) loader.getController();
            ctl.setActividad(actividad);

            Stage st = new Stage();
            st.setTitle(actividad == null ? "Nueva actividad" : "Editar actividad");
            st.initModality(Modality.APPLICATION_MODAL);
            st.setScene(new Scene(root));
            st.showAndWait();

            return ctl.isOk() ? Optional.of(ctl.getResultado()) : Optional.empty();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
