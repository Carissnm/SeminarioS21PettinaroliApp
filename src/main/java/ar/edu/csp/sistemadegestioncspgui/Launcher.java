package ar.edu.csp.sistemadegestioncspgui;

import ar.edu.csp.sistemadegestioncspgui.dao.AdministradorDao;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        Application.launch(SistemaDeGestionCSPgui.class, args);

        String hash = AdministradorDao.hashPassword("admin123"); // <-- tu contraseña
        System.out.println(hash); // copiá el resultado que imprime
    }
}
