package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXML;

//Controlador del menú principal de la sección Actividades
public class ActividadesMenuController {

    @FXML
    public void initialize() {
        //Muestra "Actividades" como título de la sección en el layout principal
        Navigation.setSectionTitle("Actividades");
    }

    @FXML private void irAltaActividad() {
        //Navega al formulario de alta / modificación de actividades cargado en el contenedor central
        Navigation.loadInMain("/actividad-form-view.fxml", "Actividades");
    }

    @FXML private void irListarActiv() {
        //Navega a la pantalla del listado de actividades
        Navigation.loadInMain("/actividades-list-view.fxml", "Actividades");
    }
    @FXML private void volver() {
        //Permite regresar al menú de inicio
        Navigation.loadInMain("/home-view.fxml", "Inicio");
    }
}
