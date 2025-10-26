package ar.edu.csp.sistemadegestioncspgui.model;
//Enum de estado de una Inscripción del sistema a una actividad. Se utiliza para representar el estado de una Inscripción.
// Al dar de baja una inscripción la misma pasa de estado Activa a Inactiva.

public enum EstadoInscripcion {
    ACTIVA, INACTIVA;

    //Convierte el valor de la base de datos de tipo String al Enum.
    // En caso en que el texto no corresponda a "INACTIVA" devuelve por default ACTIVA
    public static EstadoInscripcion fromDb(String s){
        return "INACTIVA".equalsIgnoreCase(s) ? INACTIVA : ACTIVA;
    }
    //Convierte el enum al texto que luego persiste en la base de datos
    public String toDb(){
        return this==ACTIVA ? "ACTIVA" : "INACTIVA";
    }
    // Etiqueta para mostrar en UI
    public String toLabel(){
        return this==ACTIVA ? "Activa" : "Inactiva";
    }
}
