package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.model.MovimientoCuenta;

import java.math.BigDecimal;

public interface CuentaDao {

    //El metodo ensureCuenta asegura que el socio tenga una cuenta creada y devuelve su id
    //Si la cuenta ya existe devuelve el id existente, y si no existe la crea con un saldo inicial 0 y devuelve el nuevo id
    long ensureCuenta(long socioId) throws Exception;

    //Para devolver el saldo del socio.
    java.math.BigDecimal saldo(long socioId) throws Exception;

    //Este metodo tiene como fin poder listar los movimientos de la cuena del socio
    java.util.List<MovimientoCuenta> listarMovimientos(long socioId) throws Exception;

    // El fin del metodo es registrar un pago del socio
    void registrarPago(long socioId, BigDecimal importe, String descripcion) throws Exception;

    // El fin del metodo es registrar un cargo a la cuenta del socio
    void registrarCargo(long socioId, BigDecimal importe, String descripcion) throws Exception;

    // El metodo sirve para registrar un cargo devenido de la inscripción del socio en una actividad
    void registrarCargoActividad(long socioId, BigDecimal importe, Long inscripcionId, String descripcion) throws Exception;

    // Deuda/saldo asociado a una inscripción (actividad)
    java.math.BigDecimal saldoPorInscripcion(long inscripcionId) throws Exception;

    // Deuda/saldo
    java.math.BigDecimal saldoCuotaClub(long socioId) throws Exception;

    // Registrar un pago imputado a una inscripción (disminuye deuda de esa actividad)
    void registrarPagoActividad(long socioId, long inscripcionId, java.math.BigDecimal importe, String descripcion) throws Exception;

}
