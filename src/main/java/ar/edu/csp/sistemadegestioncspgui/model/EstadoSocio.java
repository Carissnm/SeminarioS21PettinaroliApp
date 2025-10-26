package ar.edu.csp.sistemadegestioncspgui.model;
// Enum que representa el estado de un socio en el sistema.
// Permite controlar si es posible realizar operaciones como inscripciones, pagos, entre otros.
public enum EstadoSocio {
    ACTIVO, INACTIVO;

    public static EstadoSocio fromDb(String s) {
        //Convierte el valor del texto que viene de la base de datos al enum correspondiente.
        return "ACTIVO".equalsIgnoreCase(s) ? ACTIVO : INACTIVO;
    }

    //Convierte el enum en texto para guardarlo en la base de datos
    public String toDb() {
        return this == ACTIVO ? "ACTIVO" : "INACTIVO";
    }

    // Devuelve una etiqueta para mostrar en la UI.
    public String toLabel() {
        return this == ACTIVO ? "Activo" : "Inactivo";
    }
}
