package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

//Controlador del menú de Socios para la pantalla de navegación inicial de la aplicación
//Se encuentra asociado a un fxml

//Los métodos se anotan con @FXML, son handlers que se llaman desde el FXML a través de onAction
//o inyectados como campos (lbl...)

//Se utiliza el helper Navigation para cambiar de vista dentro del área principal de la
//aplicación a través de loadInMain, y para manejar la pila de navegación.

//SelectionContext se utiliza como almacén global para pasar el socio actual entre pantallas
//sin tener que usar parámetros explícitos.

public class SociosMenuController {

    @FXML private Label lblTitulo; // inyección del label definido en el FXML

    @FXML
    public void initialize() {
        //Metodo del ciclo de vida de JavaFX: se ejecuta automáticamente
        //justo después de cargar el FXML y haber hecho las inyecciones @FXML
        //Se setea el título de la sección en una barra global.
        Navigation.setSectionTitle("Socios");
    }

    // ======= Acciones =======

    @FXML
    private void irAltaSocio() {
        //Navega al formulario de alta del socio.
        SelectionContext.setSocioActual(null); // null indica una alta nueva
        //Carga la vista del formulario dentro del contenedor principal (main)
        //y actualiza el título del área a "Socios".
        Navigation.loadInMain("/socio-form-view.fxml", "Socios"); // ✅ embebido en main
    }

    @FXML
    private void irListarSocios() {
        // Abre la pantalla que lista todos lso socios.
        Navigation.loadInMain("/socios-list-view.fxml", "Socios");
    }

    @FXML private void irBuscarSocio()  {
        //Abre la pantalla específica de búsqueda/filtrado de socios.
        Navigation.loadInMain("/socios-buscar-view.fxml", "Socios");
    }

    @FXML
    private void irInscripciones() {
        SelectionContext.setSocioActual(null);
        SelectionContext.setReturnToHomeAfterInscripcion(true);
        SelectionContext.setReturnToSocioDetalle(false);
        //Abre un submenú relacionado a las inscripciones de socios.
        Navigation.loadInMainReset("/inscripcion-menu-view.fxml", "Inscripciones");
    }

    @FXML
    private void irPagos() {
        Navigation.loadInMainReset("/pagos-form-view.fxml", "Pagos");
    }

    @FXML
    private void volver() {
        //Permite volver a la vista anterior usando la pila de navegación interna.
        Navigation.loadInMainReset("/home-view.fxml", "Inicio");
    }
}
