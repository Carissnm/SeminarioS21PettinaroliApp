package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.model.Inscripcion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InscripcionDao {
    List<Inscripcion> listarPorSocio(long socioId) throws Exception;

    Optional<Inscripcion> buscarActiva(long socioId, long actividadId) throws Exception;

    long inscribir(long socioId, long actividadId, BigDecimal precioUsado, String observacion) throws Exception;

    boolean darDeBaja(long inscripcionId, java.time.LocalDate fechaBaja) throws Exception;
}