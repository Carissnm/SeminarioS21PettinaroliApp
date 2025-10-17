package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.model.MovimientoCuenta;

import java.math.BigDecimal;

public class CuentaDaoImpl implements CuentaDao {
    private final javax.sql.DataSource ds = ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory.get();
    private static final String SQL_ENSURE =
            "INSERT INTO cuenta (socio_id) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM cuenta WHERE socio_id=?)";
    private static final String SQL_ID =
            "SELECT id FROM cuenta WHERE socio_id=?";
    private static final String SQL_SALDO = """
        SELECT COALESCE(SUM(m.importe),0) AS saldo
        FROM cuenta c LEFT JOIN movimiento_cuenta m ON m.cuenta_id=c.id
        WHERE c.socio_id=?""";
    private static final String SQL_LIST = """
        SELECT m.id, m.cuenta_id, m.fecha, m.tipo, m.descripcion, m.importe,
               m.referencia_ext, m.inscripcion_id
        FROM movimiento_cuenta m
        JOIN cuenta c ON c.id = m.cuenta_id
        WHERE c.socio_id = ?
        ORDER BY m.fecha DESC, m.id DESC
        """;

    private static final String SQL_INSERT_MOV = """
        INSERT INTO movimiento_cuenta
        (cuenta_id, fecha, tipo, descripcion, importe, referencia_ext, inscripcion_id)
        VALUES (?,?,?,?,?,?,?)
        """;

    @Override public long ensureCuenta(long socioId) throws Exception {
        try (var cn = ds.getConnection()) {
            cn.setAutoCommit(false);
            try (var ins = cn.prepareStatement(SQL_ENSURE)) {
                ins.setLong(1, socioId); ins.setLong(2, socioId); ins.executeUpdate();
            }
            long id;
            try (var ps = cn.prepareStatement(SQL_ID)) {
                ps.setLong(1, socioId); try (var rs = ps.executeQuery()) { rs.next(); id = rs.getLong(1); }
            }
            cn.commit(); cn.setAutoCommit(true); return id;
        }
    }

    @Override public java.math.BigDecimal saldo(long socioId) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SQL_SALDO)) {
            ps.setLong(1, socioId); try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : java.math.BigDecimal.ZERO;
            }
        }
    }

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

    @Override
    public void registrarPago(long socioId, BigDecimal importe, String descripcion) throws Exception {
        if (importe == null || importe.signum() <= 0) throw new IllegalArgumentException("Importe de pago inválido");
        registrarMovimiento(socioId, java.time.LocalDate.now(), "PAGO",
                (descripcion==null? "Pago" : descripcion), importe, null, null);
    }
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

    @Override
    public void registrarCargoActividad(long socioId, BigDecimal importe, Long inscripcionId, String descripcion) throws Exception {
        if (importe == null || importe.signum() <= 0)
            throw new IllegalArgumentException("Importe de cargo inválido");
        registrarMovimiento(
                socioId,
                java.time.LocalDate.now(),
                "CARGO",
                (descripcion == null ? "Cargo actividad" : descripcion),
                importe.negate(), // cargo resta
                null,
                inscripcionId
        );
    }



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

    private long ensureCuentaTx(java.sql.Connection cn, long socioId) throws Exception {
        try (var ins = cn.prepareStatement(SQL_ENSURE)) { ins.setLong(1, socioId); ins.setLong(2, socioId); ins.executeUpdate(); }
        try (var ps = cn.prepareStatement(SQL_ID)) { ps.setLong(1, socioId); try (var rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); } }
    }
}
