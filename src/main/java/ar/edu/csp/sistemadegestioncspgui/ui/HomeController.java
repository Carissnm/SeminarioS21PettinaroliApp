package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class HomeController {
    @FXML private ImageView imgLogo;

    private static final String LOGO_URL = "https://i.ibb.co/kgKh9qks/logoCSP.png";

    @FXML
    public void initialize() {
        imgLogo.setPreserveRatio(true);
        imgLogo.setFitWidth(400);



        Image logo = AppCache.getLogo(LOGO_URL);
        // cuando termine de cargar en background, el ImageView ya lo tiene; si querés:
        logo.progressProperty().addListener((obs, o, n) -> {
            if (logo.isError()) {
                // log opcional: logo.getException().printStackTrace();
            } else if (n.doubleValue() >= 1.0) {
                imgLogo.setImage(logo);
            }
        });
        // si el logo ya estaba cacheado/cargado, se verá al toque:
        if (logo.getProgress() >= 1.0 && !logo.isError()) {
            imgLogo.setImage(logo);
        }
    }
}
