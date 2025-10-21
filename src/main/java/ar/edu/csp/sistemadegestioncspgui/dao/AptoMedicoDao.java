package ar.edu.csp.sistemadegestioncspgui.dao;

import java.time.LocalDate;
import java.util.Optional;

public interface AptoMedicoDao {
    /** Inserta un registro de apto: emision y vencimiento (1 año o el que pases). */
    void upsertApto(long socioId, LocalDate fechaEmision, LocalDate fechaVenc) throws Exception;

    /** Última fecha de vencimiento registrada para el socio (si existe). */
    Optional<LocalDate> ultimoVencimiento(long socioId) throws Exception;

    /** true si existe un apto con fecha_vencimiento >= hoy. */
    boolean tieneAptoVigente(long socioId) throws Exception;
}
