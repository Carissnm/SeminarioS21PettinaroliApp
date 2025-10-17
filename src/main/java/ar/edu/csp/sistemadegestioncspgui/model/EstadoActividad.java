package ar.edu.csp.sistemadegestioncspgui.model;

public enum EstadoActividad {
        ACTIVA, INACTIVA;

        public static EstadoActividad fromDb(String s) {
            return "INACTIVA".equalsIgnoreCase(s) ? INACTIVA : ACTIVA; // default ACTIVA
        }
        public String toDb() { return this == ACTIVA ? "ACTIVA" : "INACTIVA"; }
        public String toLabel() { return this == ACTIVA ? "Activa" : "Inactiva"; }
}
