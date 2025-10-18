package ar.edu.csp.sistemadegestioncspgui.model;

public enum EstadoInscripcion {
    ACTIVA, INACTIVA;
    public static EstadoInscripcion fromDb(String s){ return "INACTIVA".equalsIgnoreCase(s) ? INACTIVA : ACTIVA; }
    public String toDb(){ return this==ACTIVA ? "ACTIVA" : "INACTIVA"; }
    public String toLabel(){ return this==ACTIVA ? "Activa" : "Inactiva"; }
}
