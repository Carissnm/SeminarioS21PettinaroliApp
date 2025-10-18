package ar.edu.csp.sistemadegestioncspgui.ui;

import ar.edu.csp.sistemadegestioncspgui.model.Socio;

public final class SelectionContext {
    private static Socio socioActual;

    private SelectionContext() {}

    public static void setSocioActual(Socio s) { socioActual = s; }
    public static Socio getSocioActual() { return socioActual; }
    public static void clear() { socioActual = null; }
}
