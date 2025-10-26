package ar.edu.csp.sistemadegestioncspgui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.net.URL;

// Clase que extiende de Application, entry point de JavaFX, que levanta la ventana
//de login.
public class SistemaDeGestionCSPgui extends Application {

    @Override
    public void start(Stage stage) {
        // Inicialmente se configura un handler global para excepciones no capturadas del hilo de la Interfaz de Usuario
        // permite mostrar un mensaje legible al administrador y cerrar de manera controlada.
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> showFatal(e));
        try {
            // 1) Localiza el FXML del Login desde el classpath.
            URL fxml = getClass().getResource("/login-view.fxml");
            if (fxml == null) {
                // Error descriptivo en el caso en el que el recurso no esté.
                throw new IllegalStateException("Recurso /login-view.fxml no encontrado en classpath (¿falta en src/main/resources?).");
            }

            // 2) Carga de la FXML con FXMLLoader con un tamaño inicial
            FXMLLoader loader = new FXMLLoader(fxml);
            Scene scene = new Scene(loader.load(), 420, 240);
            // 3) Configuración y visualización de la ventana principal de Login
            stage.setTitle("Login - Club Social Potencia");
            stage.setScene(scene);
            stage.setMinWidth(380); //Tamaño mínimo para evitar layouts rotos.
            stage.setMinHeight(220);
            stage.centerOnScreen(); // centrado de la ventana en la pantalla
            stage.show(); // muestra la ventana.

        } catch (Exception e) {
            showFatal(e); // cualquier excepción en la carga/arranque se maneja como error crítico.
        }
    }

    //El metodo muestra un diálogo de error crítico y finaliza la aplicación.
    // Por seguridad se ejecuta en el hilo de la Interfaz de Usuacio con Platform.runLater.
    private void showFatal(Throwable e) {
        e.printStackTrace(); // log en consola para debugging
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Error crítico");
            a.setHeaderText("La aplicación no pudo iniciarse");
            a.setContentText(e.getMessage() == null ? e.toString() : e.getMessage());
            a.showAndWait();
            Platform.exit(); // terminación de la aplicación.
        });
    }

    public static void main(String[] args) {
        launch(args); //delegado std para lanzar JavaFX.
    }
}
