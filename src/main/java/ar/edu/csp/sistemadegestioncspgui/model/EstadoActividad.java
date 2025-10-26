package ar.edu.csp.sistemadegestioncspgui.model;
//Enum de estado de una Actividad del sistema. Se utiliza para representar si la actividad
// está disponible para la inscripción (ACTIVA) o no (INACTIVA).
// Un socio no puede inscribirse a una actividad que figura como inactiva.
public enum EstadoActividad {
        ACTIVA, INACTIVA;

        // Convierte el texto de la base de datos al enum.
        // por defecto devuelve ACTIVA.
        public static EstadoActividad fromDb(String s) {
            return "INACTIVA".equalsIgnoreCase(s) ? INACTIVA : ACTIVA; // default ACTIVA
        }
        //Convierte el enum al texto que se guarda en la base de datos
        public String toDb() {
            return this == ACTIVA ? "ACTIVA" : "INACTIVA";
        }

        // Etiqueta para ser legible para la UI
        public String toLabel() {
            return this == ACTIVA ? "Activa" : "Inactiva";
        }
}
