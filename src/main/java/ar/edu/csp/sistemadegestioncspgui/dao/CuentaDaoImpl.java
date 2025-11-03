package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.model.MovimientoCuenta;

import java.math.BigDecimal;

public class CuentaDaoImpl implements CuentaDao {

    //Pool de conexiones de la base de datos centralizado
    private final javax.sql.DataSource ds = ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory.get();

    //Creación de la cuenta del socio si la misma no existe.
    private static final String SQL_ENSURE =
            "INSERT INTO cuenta (socio_id) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM cuenta WHERE socio_id=?)";

    //Obtención del id de la cuenta del socio.
    private static final String SQL_ID =
            "SELECT id FROM cuenta WHERE socio_id=?";

    // Suma todos los importes de los movimientos para la obtención del saldo actual
    // del socio.
    private static final String SQL_SALDO = """
        SELECT COALESCE(SUM(m.importe),0) AS saldo
        FROM cuenta c LEFT JOIN movimiento_cuenta m ON m.cuenta_id=c.id
        WHERE c.socio_id=?""";

    //Listado de los movimientos del socio, del más reciente al más antiguo
    private static final String SQL_LIST = """
        SELECT m.id, m.cuenta_id, m.fecha, m.tipo, m.descripcion, m.importe,
               m.referencia_ext, m.inscripcion_id
        FROM movimiento_cuenta m
        JOIN cuenta c ON c.id = m.cuenta_id
        WHERE c.socio_id = ?
        ORDER BY m.fecha DESC, m.id DESC
        """;


    //Inserción de un movimiento genérico para ser utilizado desde registrarMovimiento
    private static final String SQL_INSERT_MOV = """
        INSERT INTO movimiento_cuenta
        (cuenta_id, fecha, tipo, descripcion, importe, referencia_ext, inscripcion_id)
        VALUES (?,?,?,?,?,?,?)
        """;

    // Permite acceder al saldo por inscripción
    private static final String SQL_SALDO_X_INSC = """
    SELECT COALESCE(SUM(importe),0) AS saldo
    FROM movimiento_cuenta
    WHERE inscripcion_id = ?
""";

    //Este metodo garantiza la existencia de la cuenta del socio y devuelve su id
    @Override public long ensureCuenta(long socioId) throws Exception {
        try (var cn = ds.getConnection()) {
            cn.setAutoCommit(false);
            try (var ins = cn.prepareStatement(SQL_ENSURE)) {
                ins.setLong(1, socioId);
                ins.setLong(2, socioId);
                ins.executeUpdate(); // si ya existe no se inserta nada.
            }
            long id;
            try (var ps = cn.prepareStatement(SQL_ID)) {
                ps.setLong(1, socioId);
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    id = rs.getLong(1);
                }
            }
            cn.commit(); cn.setAutoCommit(true); return id;
        }
    }

    // Suma de los importes de todos los movimientos del socio.
    @Override public java.math.BigDecimal saldo(long socioId) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SQL_SALDO)) {
            ps.setLong(1, socioId); try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : java.math.BigDecimal.ZERO;
            }
        }
    }

    // Devuelve el extracto del socio (del más reciente al más antiguo)
    // Mapea las columnas usadas en la Interfaz Gráfica de Usuario.
    @Override
    public java.util.List<MovimientoCuenta> listarMovimientos(long socioId) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SQL_LIST)) {
            ps.setLong(1, socioId);
            try (var rs = ps.executeQuery()) {
                var out = new java.util.ArrayList<MovimientoCuenta>();
                while (rs.next()) {
                    var m = new MovimientoCuenta();
                    m.setId(rs.getLong("id"));
                    m.setCuentaId(rs.getLong("cuenta_id"));
                    var d = rs.getDate("fecha"); m.setFecha(d==null?null:d.toLocalDate());
                    m.setDescripcion(rs.getString("descripcion"));
                    m.setImporte(rs.getBigDecimal("importe"));
                    out.add(m);
                }
                return out;
            }
        }
    }

    //Metodo para registrar créditos. Valida que el importe sea positivo.
    @Override
    public void registrarPago(long socioId, BigDecimal importe, String descripcion) throws Exception {
        if (importe == null || importe.signum() <= 0) throw new IllegalArgumentException("Importe de pago inválido");
        registrarMovimiento(socioId, java.time.LocalDate.now(), "PAGO",
                (descripcion==null? "Pago" : descripcion), importe, null, null);
    }

    // Metodo para registrar debitos a la cuenta del socio por cuota de club / de actividades
    @Override
    public void registrarCargo(long socioId, BigDecimal importe, String descripcion) throws Exception {
        if (importe == null || importe.signum() <= 0)
            throw new IllegalArgumentException("Importe de cargo inválido");
        registrarMovimiento(
                socioId,
                java.time.LocalDate.now(),
                "CARGO",
                (descripcion == null ? "Cargo" : descripcion),
                importe.negate(), // cargo resta
                null,
                null
        );
    }

    // Creación de la cuenta si la misma no existe, con inscripción de un movimiento
    // como deuda. Permite vincular el movimiento a una inscripción para trazabilidad
    public void registrarCargoActividad(long socioId,
                                        java.math.BigDecimal precio,
                                        Long inscripcionId,
                                        String referenciaExt) throws Exception {
        long cuentaId = ensureCuenta(socioId); // crea si no existe

        try (var cn = ds.getConnection()) {
            cn.setAutoCommit(false);
            try (var ps = cn.prepareStatement("""
            INSERT INTO movimiento_cuenta (cuenta_id, tipo, descripcion, importe, inscripcion_id)
            VALUES (?, 'INSCRIPCION_ACTIVIDAD', ?, ?, ?)
        """)) {
                ps.setLong(1, cuentaId);
                ps.setString(2, (referenciaExt == null || referenciaExt.isBlank())
                        ? "Inscripción de actividad" : referenciaExt);
                ps.setBigDecimal(3, precio.negate());   // invierte el signo de la deuda a negativo
                if (inscripcionId == null) ps.setNull(4, java.sql.Types.BIGINT);
                else ps.setLong(4, inscripcionId);
                ps.executeUpdate();
            }
            cn.commit();
            cn.setAutoCommit(true);
        }
    }

    // El metodo registra un débito por alta del club al socio recién creado.
    public void registrarDebitoAltaClub(long socioId, BigDecimal importe) throws Exception {
        long cuentaId = ensureCuenta(socioId);
        try (var cn = ds.getConnection();
             var ps = cn.prepareStatement("""
            INSERT INTO movimiento_cuenta (cuenta_id, tipo, descripcion, importe)
            VALUES (?, 'ALTA_SOCIO_CUOTA_CLUB', 'Cuota de alta del club', ?)
         """)) {
            ps.setLong(1, cuentaId);
            ps.setBigDecimal(2, importe.negate()); // invierte el signo de la deuda a negativo
            ps.executeUpdate();
        }
    }


    // El metodo registrarMovimiendo garantiza la cuenta dentro de la misma transacción
    private void registrarMovimiento(long socioId,
                                     java.time.LocalDate fecha,
                                     String tipo,
                                     String descripcion,
                                     BigDecimal importe,
                                     String referenciaExt,
                                     Long inscripcionId) throws Exception {
        try (var cn = ds.getConnection()) {
            cn.setAutoCommit(false);
            long cuentaId = ensureCuentaTx(cn, socioId);
            try (var ps = cn.prepareStatement(SQL_INSERT_MOV)) {
                ps.setLong(1, cuentaId);
                ps.setDate(2, java.sql.Date.valueOf(fecha));
                ps.setString(3, tipo);
                ps.setString(4, descripcion);
                ps.setBigDecimal(5, importe);
                ps.setString(6, referenciaExt);
                if (inscripcionId == null) ps.setNull(7, java.sql.Types.BIGINT); else ps.setLong(7, inscripcionId);
                ps.executeUpdate();
            }
            cn.commit();
            cn.setAutoCommit(true);
        }
    }

    // Reutiliza la conexión recibida dentro de la transacción. Se evita así abrir/cerrar conexiones durante
    // una operación compuesta.
    private long ensureCuentaTx(java.sql.Connection cn, long socioId) throws Exception {
        try (var ins = cn.prepareStatement(SQL_ENSURE)) {
            ins.setLong(1, socioId);
            ins.setLong(2, socioId);
            ins.executeUpdate();
        }
        try (var ps = cn.prepareStatement(SQL_ID)) {
            ps.setLong(1, socioId);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    @Override
    public java.math.BigDecimal saldoPorInscripcion(long inscripcionId) throws Exception {
        try (var cn = ds.getConnection();
             var ps = cn.prepareStatement(SQL_SALDO_X_INSC)) {
            ps.setLong(1, inscripcionId);
            try (var rs = ps.executeQuery()) {
                return rs.next()? rs.getBigDecimal(1) : java.math.BigDecimal.ZERO;
            }
        }
    }

    @Override
    public void registrarPagoActividad(long socioId, long inscripcionId,
                                       java.math.BigDecimal importe, String descripcion) throws Exception {
        if (importe == null || importe.signum() <= 0)
            throw new IllegalArgumentException("Importe de pago inválido");

        try (var cn = ds.getConnection()) {
            cn.setAutoCommit(false);
            try {
                long cuentaId = ensureCuentaTx(cn, socioId); // ya lo tenés implementado

                try (var ps = cn.prepareStatement(SQL_INSERT_MOV)) { // ya lo tenés declarado arriba
                    ps.setLong(1, cuentaId);
                    ps.setDate(2, java.sql.Date.valueOf(java.time.LocalDate.now()));
                    ps.setString(3, "PAGO"); // tu convención: pagos positivos
                    ps.setString(4, (descripcion == null || descripcion.isBlank()) ? "Pago actividad" : descripcion);
                    ps.setBigDecimal(5, importe);   // PAGO = positivo
                    ps.setString(6, null);          // referencia_ext opcional
                    ps.setLong(7, inscripcionId);   // <<< clave: se imputa a la actividad
                    ps.executeUpdate();
                }

                cn.commit();
            } catch (Exception ex) {
                try { cn.rollback(); } catch (Exception ignore) {}
                throw ex;
            } finally {
                try { cn.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }
    }
}
