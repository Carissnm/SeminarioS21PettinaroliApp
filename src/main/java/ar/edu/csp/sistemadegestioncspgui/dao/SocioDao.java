package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.model.EstadoSocio;
import ar.edu.csp.sistemadegestioncspgui.model.Socio;

import java.util.List;
import java.util.Optional;
/*
DAO para acceder y gestionar los datos de Socio.
*/
public interface SocioDao {
    //Listado de socios registrados en el club
    List<Socio> listarTodos() throws Exception;
    //Búsqueda de socios por dni
    List<Socio> buscarPorDni(String dni) throws Exception;     // búsqueda parcial o exacta
    //Búsqueda de socios por id
    Optional<Socio> buscarPorId(long id) throws Exception;

    //Creación de un nuevo socio.
    long crear(Socio socio) throws Exception; // devuelve id generado
    //Permite la actualización de los datos del socio existente.
    boolean actualizar(Socio socio) throws Exception;
    //Permite la eliminación del socio por id, en una baja lógica (el socio queda figurando como inactivo)
    boolean eliminar(long id) throws Exception;

    boolean reactivarSocio(long socioId) throws Exception;
    boolean actualizarEstado(Long socioId, EstadoSocio nuevo) throws Exception;
}
