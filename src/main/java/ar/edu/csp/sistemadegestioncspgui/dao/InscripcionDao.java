package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.model.Inscripcion;

public interface InscripcionDao {
    java.util.List<Inscripcion> listarPorSocio(long socioId) throws Exception;
    long inscribir(long socioId, long actividadId) throws Exception; // genera cargo de la cuota actual
    boolean baja(long inscripcionId) throws Exception;
}
