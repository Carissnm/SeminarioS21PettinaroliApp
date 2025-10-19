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
    public static Optional<Socio> showDialog(Window owner, Socio socio) throws Exception {
        FXMLLoader loader = new FXMLLoader(SocioForm.class.getResource("/socio-form-view.fxml"));
        Parent root = loader.load();

        Object ctl = loader.getController();
        if (!(ctl instanceof SocioFormController controller)) {
            throw new IllegalStateException("El controller de /socio-form-view.fxml no es SocioFormController");
        }

        // Prefill (edición) o alta si socio == null
        controller.setSocio(socio);

        Stage st = new Stage();
        st.setTitle(socio == null ? "Alta de socio" : "Editar socio");
        st.initOwner(owner);
        st.initModality(Modality.APPLICATION_MODAL);
        st.setScene(new Scene(root));
        st.setResizable(false);
        st.showAndWait();

        return controller.isSaved() ? Optional.of(controller.getSocio()) : Optional.empty();
    }


}
