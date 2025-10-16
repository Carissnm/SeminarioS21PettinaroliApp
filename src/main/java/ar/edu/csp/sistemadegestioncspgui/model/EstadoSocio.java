package ar.edu.csp.sistemadegestioncspgui.model;

public enum EstadoSocio {
    ACTIVO, INACTIVO;

    public static EstadoSocio fromDb(String s) {
        return "ACTIVO".equalsIgnoreCase(s) ? ACTIVO : INACTIVO;
    }

    public String toDb() {
        return this == ACTIVO ? "ACTIVO" : "INACTIVO";
    }

    public String toLabel() {
        return this == ACTIVO ? "Activo" : "Inactivo";
    }
}
