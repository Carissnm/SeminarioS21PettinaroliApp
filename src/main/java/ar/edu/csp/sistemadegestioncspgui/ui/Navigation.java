package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

// Utilidad centralizada para poder manejar la navegación entre vistas embebidas en FXML
// dentro de un contenedor principal y un título de sección.
// funciona cargando un fxml dentro del contenedor principal de la aplicación sin abrir nuevas ventanas.

public final class Navigation {
    //Contenedor principal donde se inyectan las vistas
    private static StackPane container;
    //Label donde se muestra el título de la sección actual
    private static Label sectionLabel;
    //Historial de vistas para poder "volver" (LIFO)
    private static final Deque<Node> history = new ArrayDeque<>();

    //Clase utilitaria no instanciable
    private Navigation() {}

    //El metodo init inicializa el sistemad e navegación con el contenedor principal y el label de sección.
    public static void init(StackPane root, Label lbl) {
        container = root;
        sectionLabel = lbl;
    }

    //Cambia el texto del título de sección si hay un label configurado.
    public static void setSectionTitle(String title) {
        if (sectionLabel != null) sectionLabel.setText(title);
    }

    //Carga un FXML dentro del contenedor principal y actualiza el título de la sección.
    //Apila la vista anterior en un "historial" para poder regresar.
    public static void loadInMain(String fxmlPath, String sectionTitle) {
        if (container == null) throw new IllegalStateException("Navigation no inicializado");
        //Normalización de la ruta: exige que empiece con "/" para el ClassLoader.getResource.
        String normalized = fxmlPath.startsWith("/") ? fxmlPath : "/" + fxmlPath;
        //Busca el recurso FXML en el classpath
        var url = Navigation.class.getResource(normalized);
        if (url == null) {
            throw new RuntimeException("FXML no encontrado en classpath: " + normalized +
                    "\nVerifica el nombre del archivo y que esté en src/main/resources (o su subcarpeta).");
        }
        try {
            //Carga el árbol de nodos definido por el FXML
            Node view = FXMLLoader.load(url);
            // Si ya hay una vista mostrada la guarda en el historial antes de reemplazarla.
            if (!container.getChildren().isEmpty()) history.push(container.getChildren().get(0));
            //Reemplaza la vista visible por la nueva
            container.getChildren().setAll(view);
            //Actualiza el título de la sección
            setSectionTitle(sectionTitle);
        } catch (IOException e) {
            throw new RuntimeException("No fue posible cargar " + normalized, e);
        }
    }

    //El metodo back() permite volver a la vista anterior cuando existe el historial
    public static void back() {
        if (!history.isEmpty() && container != null) {
            container.getChildren().setAll(history.pop());
        }
    }

    //El metodo backOr intenta volver. En caso de no haber historial carga la vista fallback y settea su título.
    public static void backOr(String fallbackFxml, String sectionTitle) {
        if (container != null && !history.isEmpty()) {
            container.getChildren().setAll(history.pop());
            setSectionTitle(sectionTitle);
        } else {
            loadInMain(fallbackFxml, sectionTitle);
        }
    }

}
