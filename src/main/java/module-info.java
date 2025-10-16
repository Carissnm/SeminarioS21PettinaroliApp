module ar.edu.csp.sistemadegestioncspgui {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // JDBC / pool
    requires java.sql;
    requires com.zaxxer.hikari;

    // BCrypt
    requires jbcrypt;

    // 1) Exporta el paquete donde está la clase Application
    exports ar.edu.csp.sistemadegestioncspgui to javafx.graphics;

    // 2) Abre los paquetes que cargan FXML (reflexión de FXMLLoader)
    opens ar.edu.csp.sistemadegestioncspgui.ui to javafx.fxml;

    // 3) (opcional) si usás bindings de propiedades en tablas
    opens ar.edu.csp.sistemadegestioncspgui.model to javafx.base;
}
