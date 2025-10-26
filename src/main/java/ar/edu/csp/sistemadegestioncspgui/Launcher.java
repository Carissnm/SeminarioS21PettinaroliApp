package ar.edu.csp.sistemadegestioncspgui;

import ar.edu.csp.sistemadegestioncspgui.dao.AdministradorDao;
import javafx.application.Application;

/* Punto de entrada de la aplicación:
Soporta dos modos:
 1) Modo Utilidad (línea de comandos):
 si se pasa el argumento --print-hash imprime por stdout el hash BCrypt de esa clave y termina sin levantar JavaFX
 lo cual es útil y fue de hecho utilizado para generar contraseñas en la base.
 2) Modo normal (GUI JavaFX):
 Si no se pasa --print-hash lanza la aplicación JavaFX

 */
public class Launcher {
    public static void main(String[] args) {
        String passArg = null;
        for (String a : args) {
            //busca un argumento de la forma --print-hash=...
            if (a.startsWith("--print-hash=")) {
                passArg = a.substring("--print-hash=".length());
                break;
            }
        }
        if (passArg != null) {
            // Modo utilidad para imprimir el hash BCrypt y salir
            System.out.println(AdministradorDao.hashPassword(passArg));
            return; //no levanta JavaFX
        }

        // MODO NORMAL (aplicación JavaFX para la aplicación de escritorio del trabajo práctico)
        Application.launch(SistemaDeGestionCSPgui.class, args);
    }
}
