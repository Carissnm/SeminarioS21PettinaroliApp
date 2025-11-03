module ar.edu.csp.sistemadegestioncspgui {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // JDBC / pool
    requires java.sql;
    requires com.zaxxer.hikari;

    // BCrypt (si te da error este nombre, probá: requires org.mindrot.jbcrypt;)
    requires jbcrypt;
    requires java.desktop;

    // Exportá el paquete donde está tu Application (SistemaDeGestionCSPgui)
    exports ar.edu.csp.sistemadegestioncspgui;

    // Abrí solo los controllers para FXML (reflexión del FXMLLoader)
    opens ar.edu.csp.sistemadegestioncspgui.ui to javafx.fxml;
    opens ar.edu.csp.sistemadegestioncspgui.model to javafx.base;
}
