package ar.edu.csp.sistemadegestioncspgui.dao;

import java.time.LocalDate;
import java.util.Optional;

public interface AptoMedicoDao {
    // Metodo para insertar un registro de apto con fecha de emision y fecha de venc a un año.
    void upsertApto(long socioId, LocalDate fechaEmision, LocalDate fechaVnc) throws Exception;

    // Permite calcular la útima fecha de vencimiento registrada para el socio si existe.
    Optional<LocalDate> ultimoVencimiento(long socioId) throws Exception;

    // Sirve para verificar si el socio cuenta con apto médico vigente.
    boolean tieneAptoVigente(long socioId) throws Exception;
}
