package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.model.Actividad;
import ar.edu.csp.sistemadegestioncspgui.model.EstadoActividad;

import java.util.List;
import java.util.Optional;

public interface ActividadDao {
    // lecturas
    List<Actividad> listarTodas() throws Exception;
    List<Actividad> listarActivas() throws Exception;
    Optional<Actividad> buscarPorId(long id) throws Exception;

    // escrituras
    long crear(Actividad a) throws Exception;                 // devuelve id generado
    boolean actualizar(Actividad a) throws Exception;         // true si afectó filas
    boolean cambiarEstado(long id, EstadoActividad est) throws Exception;
    boolean actualizarPrecio(long id, java.math.BigDecimal nuevoPrecio) throws Exception;
}
