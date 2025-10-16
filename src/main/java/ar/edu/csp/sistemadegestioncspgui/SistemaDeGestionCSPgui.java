package ar.edu.csp.sistemadegestioncspgui;

import ar.edu.csp.sistemadegestioncspgui.dao.AdministradorDao;
import ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SistemaDeGestionCSPgui extends Application {
    @Override
    public void start(Stage stage) throws Exception {

        //Carga de la UI
        var url = getClass().getResource("/login-view.fxml");
        var scene = new javafx.scene.Scene(new javafx.fxml.FXMLLoader(url).load(), 420, 240);
        stage.setTitle("Login - Club Social Potencia");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

    }


    public static void main(String[] args) {
        launch(args);
    }
}
