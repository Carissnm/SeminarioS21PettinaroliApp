package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

//Controlador de la pantalla "Home/Inicio"
//Configura un ImageView para mostrar el logo del club obteniendo la imagen a través de un caché de aplicación
//busca cargar de manera más rápida el logo del club.
public class HomeController {
    @FXML private ImageView imgLogo;

    //URL pública del logo
    private static final String LOGO_URL = "https://i.ibb.co/kgKh9qks/logoCSP.png"; //se apunta a un recurso remoto donde se encuentra el archivo de la imagen del logo del club

    @FXML
    public void initialize() {
        //Se mantienen las proporciones dle logo y se fija el ancho
        imgLogo.setPreserveRatio(true);
        imgLogo.setFitWidth(400);
        // Se solicita la imagen al caché de la aplicación con carga asíncrona si no está en la caché.
        Image logo = AppCache.getLogo(LOGO_URL);
        // una vez que se termina de cargar settea la imagen en el imageview
        logo.progressProperty().addListener((obs, o, n) -> {
            if (logo.isError()) {
                logo.getException().printStackTrace();
            } else if (n.doubleValue() >= 1.0) {
                imgLogo.setImage(logo);
            }
        });
        // si el logo ya estaba cacheado/cargado, se muestra de inmediato
        if (logo.getProgress() >= 1.0 && !logo.isError()) {
            imgLogo.setImage(logo);
        }
    }

    //Helper para mostrar funciones aún no .
    private void mostrarEnDesarrollo(String nombre) {
        new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION,
                "La funcionalidad '" + nombre + "' todavía no está disponible.\nSerá incorporada en la próxima versión.",
                javafx.scene.control.ButtonType.OK
        ).showAndWait();
    }



}
