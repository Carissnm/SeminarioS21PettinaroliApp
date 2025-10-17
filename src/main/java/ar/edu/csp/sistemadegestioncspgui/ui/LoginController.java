package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.AdministradorDao;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
// Para loguearse utilizar las siguientes credenciales: Usuario: admin@csp.local, Contraseña: admin123
public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMsg;
    @FXML private Button btnIngresar; // marcá fx:id="btnIngresar" en el FXML

    private final AdministradorDao adminDao = new AdministradorDao();

    @FXML
    private void initialize() {
        // Enter en password => ingresar
        txtPassword.setOnAction(e -> onIngresar());
        // Enter en email => foco a password
        txtEmail.setOnAction(e -> txtPassword.requestFocus());
        lblMsg.setText("");
    }

    @FXML
    private void onIngresar() {
        lblMsg.setText("");
        final String email = (txtEmail.getText() == null ? "" : txtEmail.getText().trim().toLowerCase());
        final String pass  = (txtPassword.getText() == null ? "" : txtPassword.getText());

        // Validación básica
        if (email.isEmpty()) {
            lblMsg.setText("Ingresá tu email.");
            txtEmail.requestFocus();
            return;
        }
        if (pass.isEmpty()) {
            lblMsg.setText("Ingresá tu contraseña.");
            txtPassword.requestFocus();
            return;
        }

        // Evita doble click
        boolean prev = btnIngresar.isDisable();
        btnIngresar.setDisable(true);
        try {
            Long adminId = adminDao.validar(email, pass);

            if (adminId != null) {
                // 1) cerrar login
                Stage stageLogin = (Stage) txtEmail.getScene().getWindow();
                stageLogin.close();

                // 2) abrir principal
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/main-view.fxml"));
                Scene scene = new Scene(loader.load(), 900, 600);
                // Opcional: scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());

                Stage stageMain = new Stage();
                stageMain.setTitle("Sistema de Gestión - CSP");
                stageMain.setScene(scene);
                stageMain.centerOnScreen();
                // Opcional: stageMain.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
                stageMain.show();

            } else {
                lblMsg.setText("Credenciales inválidas o usuario inactivo.");
                txtPassword.clear();
                txtEmail.requestFocus();
            }
        } catch (Exception ex) {
            // Para el usuario: mensaje simple
            lblMsg.setText("Ocurrió un error al iniciar sesión.");
            // Para debugging: alerta modal (en producción podés loguearlo y ocultar esto)
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Error");
            a.setHeaderText("No fue posible iniciar sesión");
            a.setContentText(ex.getMessage() == null ? ex.toString() : ex.getMessage());
            a.showAndWait();
            ex.printStackTrace();
        } finally {
            btnIngresar.setDisable(prev);
        }
    }
}
