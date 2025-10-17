package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.model.MovimientoCuenta;

import java.math.BigDecimal;

public interface CuentaDao {
    long ensureCuenta(long socioId) throws Exception;

    java.math.BigDecimal saldo(long socioId) throws Exception;

    java.util.List<MovimientoCuenta> listarMovimientos(long socioId) throws Exception;

    // MOVIMIENTOS
    void registrarPago(long socioId, BigDecimal importe, String descripcion) throws Exception;

    void registrarCargo(long socioId, BigDecimal importe, String descripcion) throws Exception;

    void registrarCargoActividad(long socioId, BigDecimal importe, Long inscripcionId, String descripcion) throws Exception;
}
