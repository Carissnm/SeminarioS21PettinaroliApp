package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.model.MovimientoCuenta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovimientoCuentaDao {

    Long insertar(MovimientoCuenta m);

    /** Registra un PAGO (CREDITO) a la cuenta del socio. */
    Long registrarPago(Long socioId, BigDecimal importe, String descripcion, LocalDate fecha);

    /** Registra un CARGO (DEBITO) a la cuenta del socio (por si lo necesitás). */
    Long registrarCargo(Long socioId, BigDecimal importe, String descripcion, LocalDate fecha, Long inscripcionId);

    /** Saldo por socio (suma créditos – débitos). */
    BigDecimal obtenerSaldoPorSocio(Long socioId);

    /** Últimos movimientos del socio. */
    List<MovimientoCuenta> listarPorSocio(Long socioId, int limit);

    /** Helper: devuelve id de cuenta del socio, creándola si no existe. */
    Long ensureCuentaParaSocio(Long socioId);
}
