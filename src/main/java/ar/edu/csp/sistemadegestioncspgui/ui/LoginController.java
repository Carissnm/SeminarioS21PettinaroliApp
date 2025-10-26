package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.dao.AdministradorDao;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
// IMPORTANTE (solo para pruebas locales):
// Usuario: admin@csp.local
// Contraseña: admin123
public class LoginController {

    //Inyección de controles del FXML
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMsg;
    @FXML private Button btnIngresar;

    //Acceso a datos para la validación del mail y la contraseña
    private final AdministradorDao adminDao = new AdministradorDao();

    @FXML
    private void initialize() {
        // Ingresar al presionar Enter
        txtPassword.setOnAction(e -> onIngresar());
        // Al presionar Enter en el mail dirige al label siguiente para ingresar la Contraseña
        txtEmail.setOnAction(e -> txtPassword.requestFocus());
        // Limpia mensajes al iniciar
        lblMsg.setText("");
    }

    @FXML
    private void onIngresar() {
        // Se limpia el mensaje anterior
        lblMsg.setText("");
        //Normalización de entradas
        final String email = (txtEmail.getText() == null ? "" : txtEmail.getText().trim().toLowerCase());
        final String pass  = (txtPassword.getText() == null ? "" : txtPassword.getText());

        // Validación básica de los campos requeridos
        if (email.isEmpty()) {
            lblMsg.setText("Ingrese su email.");
            txtEmail.requestFocus();
            return;
        }
        if (pass.isEmpty()) {
            lblMsg.setText("Ingrese su contraseña.");
            txtPassword.requestFocus();
            return;
        }

        // Permite evitar el doble click/múltiples envíos mientras se realiza la validación
        boolean prev = btnIngresar.isDisable();
        btnIngresar.setDisable(true);
        try {
            //Consulta al DAO: devuelve el id si tanto el email como la contraseña son válidas y el
            //adminisrtador tiene como estado activo.
            Long adminId = adminDao.validar(email, pass);

            if (adminId != null) {
                //Si el login es exitoso:
                // 1) Se cierra la ventana de Login.
                Stage stageLogin = (Stage) txtEmail.getScene().getWindow();
                stageLogin.close();

                // 2) Se carga y abre la ventana principal.
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/main-view.fxml"));
                Scene scene = new Scene(loader.load(), 900, 600);
                Stage stageMain = new Stage();
                stageMain.setTitle("Sistema de Gestión - CSP");
                stageMain.setScene(scene);
                stageMain.centerOnScreen();
                stageMain.show();

            } else {
                //Si el login falla:
                lblMsg.setText("Credenciales inválidas o usuario inactivo.");
                txtPassword.clear();
                txtEmail.requestFocus();
            }
        } catch (Exception ex) {
            // Mensaje genérico para el administrador
            lblMsg.setText("Ocurrió un error al iniciar sesión.");
            // Alerta modal para debugging
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Error");
            a.setHeaderText("No fue posible iniciar sesión");
            a.setContentText(ex.getMessage() == null ? ex.toString() : ex.getMessage());
            a.showAndWait();
            ex.printStackTrace();
        } finally {
            //Restaura el estado del botón y queda nuevamente habilitado
            btnIngresar.setDisable(prev);
        }
    }
}
