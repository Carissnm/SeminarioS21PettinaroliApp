package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.model.Socio;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

public class SocioForm {

    /** Abre el diálogo sin owner (cae en APPLICATION_MODAL). */
    public static Optional<Socio> showDialog(Socio socio) {
        return showDialog(null, socio);
    }

    /** Abre el diálogo con owner (recomendado). */
    public static Optional<Socio> showDialog(Window owner, Socio socio) {
        try {
            FXMLLoader loader = new FXMLLoader(SocioForm.class.getResource("/socio-form-view.fxml"));
            Parent root = loader.load();

            Object ctl = loader.getController();
            if (!(ctl instanceof SocioFormController controller)) {
                throw new IllegalStateException("El controller de /socio-form-view.fxml no es SocioFormController");
            }

            controller.setSocio(socio);

            Stage st = new Stage();
            st.setTitle(socio == null ? "Nuevo socio" : "Editar socio");
            st.setScene(new Scene(root));
            st.setResizable(false);

            if (owner != null) {
                st.initOwner(owner);
                st.initModality(Modality.WINDOW_MODAL);
            } else {
                st.initModality(Modality.APPLICATION_MODAL);
            }

            st.sizeToScene();
            if (owner == null) st.centerOnScreen();

            st.showAndWait();

            return controller.isOk() ? Optional.ofNullable(controller.getResultado()) : Optional.empty();

        } catch (Exception e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Error");
            a.setHeaderText("No se pudo abrir el formulario de Socio");
            a.setContentText(e.getMessage() == null ? e.toString() : e.getMessage());
            a.showAndWait();
            return Optional.empty();
        }
    }
}
