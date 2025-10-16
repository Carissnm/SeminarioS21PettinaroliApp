package ar.edu.csp.sistemadegestioncspgui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.net.URL;

public class SistemaDeGestionCSPgui extends Application {

    @Override
    public void start(Stage stage) {
        // Manejo global de excepciones no capturadas (thread UI)
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> showFatal(e));

        try {
            // 1) Localizar recurso
            URL fxml = getClass().getResource("/login-view.fxml");
            if (fxml == null) {
                throw new IllegalStateException("Recurso /login-view.fxml no encontrado en classpath (¿falta en src/main/resources?).");
            }

            // 2) Cargar FXML
            FXMLLoader loader = new FXMLLoader(fxml);
            Scene scene = new Scene(loader.load(), 420, 240);

            // 3) (Opcional) estilos e ícono
            // scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
            // stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));

            // 4) Mostrar ventana
            stage.setTitle("Login - Club Social Potencia");
            stage.setScene(scene);
            stage.setMinWidth(380);
            stage.setMinHeight(220);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            showFatal(e);
        }
    }

    private void showFatal(Throwable e) {
        e.printStackTrace();
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Error crítico");
            a.setHeaderText("La aplicación no pudo iniciarse");
            a.setContentText(e.getMessage() == null ? e.toString() : e.getMessage());
            a.showAndWait();
            Platform.exit();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
