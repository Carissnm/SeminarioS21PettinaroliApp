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
    public static Optional<Socio> open(Window owner, Socio socio) {
        try {
            FXMLLoader l = new FXMLLoader(SocioForm.class.getResource("/socio-form-view.fxml"));
            Parent root = l.load();
            SocioFormController controller = l.getController();
            controller.setSocio(socio);

            Stage st = new Stage();
            st.setTitle(socio == null ? "Alta de socio" : "Editar socio");
            if (owner != null) st.initOwner(owner);
            st.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            st.setScene(new javafx.scene.Scene(root));
            st.setResizable(false);
            st.showAndWait();

            return controller.isSaved() ? Optional.of(controller.getSocio()) : Optional.empty();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario:\n" + e.getMessage()).showAndWait();
            e.printStackTrace();
            return Optional.empty();
        }
    }

}
