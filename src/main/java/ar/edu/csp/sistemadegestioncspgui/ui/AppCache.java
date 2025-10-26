package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.scene.image.Image;

//Caché simple en memoria para recursos de la aplicación en la carga del logo del club.
public final class AppCache {
    private static Image LOGO; //Instancia cacheada del logo
    private AppCache() {} //utilidad no instanciable

    public static Image getLogo(String url) {
        if (LOGO == null) {
            //Crea la imagen con carga asíncrona para no bloquear el hilo de la Interfaz de Usuario
            LOGO = new Image(url, true);
        }
        return LOGO; //Siempre devuelve la misma instancia cacheada.
    }
}