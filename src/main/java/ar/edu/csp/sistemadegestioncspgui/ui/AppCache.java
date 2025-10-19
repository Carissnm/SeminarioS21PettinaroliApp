package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.scene.image.Image;

public final class AppCache {
    private static Image LOGO;

    private AppCache() {}

    public static Image getLogo(String url) {
        if (LOGO == null) {
            // true = backgroundLoading (no bloquea la UI)
            LOGO = new Image(url, true);
        }
        return LOGO;
    }
}