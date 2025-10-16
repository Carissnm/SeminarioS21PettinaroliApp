package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.AdministradorDao;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
// Para las pruebas utilizar mail: admin@csp.local, password: admin123
public class LoginController {
    @FXML private TextField txtEmail;       // <-- igual al FXML
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMsg;

    private final AdministradorDao adminDao = new AdministradorDao();

    @FXML
    private void onIngresar() {
        String email = txtEmail.getText().trim();
        String pass  = txtPassword.getText();

        try {
            Long adminId = adminDao.validar(email, pass);

            if (adminId != null) {

                // 1) cerrar la ventana de login
                Stage stageLogin = (Stage) txtEmail.getScene().getWindow();
                stageLogin.close();

                // 2) abrir la ventana principal
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/main-view.fxml"));
                Scene scene = new Scene(loader.load(), 900, 600);

                Stage stageMain = new Stage();
                stageMain.setTitle("Sistema de Gestión - CSP");
                stageMain.setScene(scene);
                stageMain.centerOnScreen();
                stageMain.show();
                // ---------------------------------------------------
            } else {
                lblMsg.setText("Credenciales inválidas o usuario inactivo");
                txtPassword.clear();
                txtEmail.requestFocus();
            }
        } catch (Exception e) {
            lblMsg.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
