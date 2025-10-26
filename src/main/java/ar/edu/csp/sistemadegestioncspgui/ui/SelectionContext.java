package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.model.Socio;

//Contexto de selección simple Global para la Interfaz del Usuario
//Actúa como memoria temporal entre las pantallas para poder pasar el socio seleccionado
//sin tener que encadenar parámetros por cada navegación
//Guarda un único Socio estático (SocioActual) accesible desde cualquier controlador.
//Se settea antes de navegar y se lee en la pantalla de destino. Cuando ya no se necesita se limpia explícitamente.
public final class SelectionContext {
    //Estado global: último socio seleccionado en la Interfaz de Usuario
    private static Socio socioActual;
    //Utilidad, no instanciable.
    private SelectionContext() {}
    //Guarda el socio para que pueda ser recuperado en la siguiente vista
    public static void setSocioActual(Socio s) {
        socioActual = s;
    }
    //Devuelve el socio seleccionado actualmente (puede ser null)
    public static Socio getSocioActual() {
        return socioActual;
    }

    //Limpia el contexto y deja de haber un socio seleccionado.
    public static void clear() {
        socioActual = null;
    }
}
