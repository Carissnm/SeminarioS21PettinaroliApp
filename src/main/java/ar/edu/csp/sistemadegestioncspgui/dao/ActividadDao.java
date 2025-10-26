package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.model.Actividad;
import ar.edu.csp.sistemadegestioncspgui.model.EstadoActividad;

import java.util.List;
import java.util.Optional;
// Interfaz para la gestión de las actividades.
public interface ActividadDao {
    // Listar todas las actividades
    List<Actividad> listarTodas() throws Exception;
    // Listar solo las actividades en estado activo.
    List<Actividad> listarActivas() throws Exception;
    // Búsqueda de actividad por id
    Optional<Actividad> buscarPorId(long id) throws Exception;

    // Permite la creación de un objeto de tipo Actividad.
    long crear(Actividad a) throws Exception;                 // devuelve id generado
    // Permite la modificación de los datos de una actividad existente previamente.
    boolean actualizar(Actividad a) throws Exception;         // devuelve true si afectó filas
    // Permite cambiar el estado de una actividad
    boolean cambiarEstado(long id, EstadoActividad est) throws Exception;
    // Permite actualizar/modificar el precio de la cuota de una actividad
    boolean actualizarPrecio(long id, java.math.BigDecimal nuevoPrecio) throws Exception;
}
