package ar.edu.csp.sistemadegestioncspgui;

import ar.edu.csp.sistemadegestioncspgui.dao.AdministradorDao;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        String passArg = null;
        for (String a : args) {
            if (a.startsWith("--print-hash=")) {
                passArg = a.substring("--print-hash=".length());
                break;
            }
        }
        if (passArg != null) {
            // Modo utilidad (sin levantar JavaFX)
            System.out.println(AdministradorDao.hashPassword(passArg));
            return;
        }

        // Modo normal (aplicación JavaFX)
        Application.launch(SistemaDeGestionCSPgui.class, args);
    }
}
