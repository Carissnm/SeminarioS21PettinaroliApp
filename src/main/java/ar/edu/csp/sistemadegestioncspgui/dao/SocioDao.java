package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.model.Socio;

import java.util.List;
import java.util.Optional;

public interface SocioDao {
    List<Socio> listarTodos() throws Exception;
    List<Socio> buscarPorDni(String dni) throws Exception;     // búsqueda parcial o exacta
    Optional<Socio> buscarPorId(long id) throws Exception;

    long crear(Socio socio) throws Exception;                  // devuelve id generado
    boolean actualizar(Socio socio) throws Exception;          // true si afectó filas
    boolean eliminar(long id) throws Exception;                // true si afectó filas
}
