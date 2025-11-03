package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.model.Actividad;
import ar.edu.csp.sistemadegestioncspgui.model.Inscripcion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
// Interfaz con el contrato de acceso a los datos para la entidad Inscripción
// Una inscripción es activa cuando su estado es Activa y su fecha de baja es null.
// No debe existir más de una inscripción activa del mismo socio a una misma actividad.
// La baja es lógica, es decir que no se borra sino que se setea a estado Baja con una fecha de baja.
// No debe permitirse una inscripción si el socio está como Inactivo
// No puede darse de baja una inscripción si existe saldo negativo asociado.

public interface InscripcionDao {
    List<Inscripcion> listarPorSocio(long socioId) throws Exception;

    Optional<Inscripcion> buscarActiva(long socioId, long actividadId) throws Exception;

    long inscribir(long socioId, long actividadId, BigDecimal precioUsado, String observacion) throws Exception;

    boolean darDeBaja(long inscripcionId, java.time.LocalDate fechaBaja) throws Exception;

    List<Actividad> listarActividadesVigentesPorSocio(long socioId) throws Exception;
}