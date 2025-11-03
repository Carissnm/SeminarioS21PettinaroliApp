package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory;
import ar.edu.csp.sistemadegestioncspgui.model.MovimientoCuenta;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MovimientoCuentaDaoImpl implements MovimientoCuentaDao {

    @Override
    public Long insertar(MovimientoCuenta m) {
        final String sql = """
            INSERT INTO movimiento_cuenta (cuenta_id, fecha, tipo, descripcion, importe, referencia_ext, inscripcion_id)
            VALUES (?,?,?,?,?,?,?)
        """;
        try (Connection con = DataSourceFactory.get().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, m.getCuentaId());
            ps.setDate(2, Date.valueOf(m.getFecha() != null ? m.getFecha() : LocalDate.now()));
            ps.setString(3, m.getTipo()); // "CREDITO" o "DEBITO"
            ps.setString(4, m.getDescripcion());
            ps.setBigDecimal(5, m.getImporte());
            ps.setString(6, m.getReferenciaExt());
            if (m.getInscripcionId() != null) ps.setLong(7, m.getInscripcionId()); else ps.setNull(7, Types.BIGINT);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar movimiento", e);
        }
    }

    @Override
    public Long registrarPago(Long socioId, BigDecimal importe, String descripcion, LocalDate fecha) {
        if (importe == null || importe.signum() <= 0) {
            throw new IllegalArgumentException("El importe del pago debe ser positivo.");
        }
        Long cuentaId = ensureCuentaParaSocio(socioId);
        MovimientoCuenta m = new MovimientoCuenta(
                cuentaId,
                fecha != null ? fecha : LocalDate.now(),
                "CREDITO",
                (descripcion != null && !descripcion.isBlank()) ? descripcion : "Pago",
                importe,
                null,
                null
        );
        return insertar(m);
    }

    @Override
    public Long registrarCargo(Long socioId, BigDecimal importe, String descripcion, LocalDate fecha, Long inscripcionId) {
        if (importe == null || importe.signum() <= 0) {
            throw new IllegalArgumentException("El importe del cargo debe ser positivo.");
        }
        Long cuentaId = ensureCuentaParaSocio(socioId);
        MovimientoCuenta m = new MovimientoCuenta(
                cuentaId,
                fecha != null ? fecha : LocalDate.now(),
                "DEBITO",
                (descripcion != null && !descripcion.isBlank()) ? descripcion : "Cargo",
                importe,
                null,
                inscripcionId
        );
        return insertar(m);
    }

    @Override
    public BigDecimal obtenerSaldoPorSocio(Long socioId) {
        final String sql = """
            SELECT COALESCE(SUM(CASE
                      WHEN mc.tipo='CREDITO' THEN mc.importe
                      WHEN mc.tipo='DEBITO'  THEN -mc.importe
                      ELSE 0 END), 0) AS saldo
            FROM cuenta c
            LEFT JOIN movimiento_cuenta mc ON mc.cuenta_id = c.id
            WHERE c.socio_id = ?
        """;
        try (Connection con = DataSourceFactory.get().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, socioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("saldo");
                return BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo saldo del socio", e);
        }
    }

    @Override
    public List<MovimientoCuenta> listarPorSocio(Long socioId, int limit) {
        final String sql = """
            SELECT mc.*
            FROM cuenta c
            JOIN movimiento_cuenta mc ON mc.cuenta_id = c.id
            WHERE c.socio_id = ?
            ORDER BY mc.fecha DESC, mc.id DESC
            LIMIT ?
        """;
        List<MovimientoCuenta> out = new ArrayList<>();
        try (Connection con = DataSourceFactory.get().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, socioId);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovimientoCuenta m = new MovimientoCuenta();
                    m.setId(rs.getLong("id"));
                    m.setCuentaId(rs.getLong("cuenta_id"));
                    m.setFecha(rs.getDate("fecha").toLocalDate());
                    m.setTipo(rs.getString("tipo"));
                    m.setDescripcion(rs.getString("descripcion"));
                    m.setImporte(rs.getBigDecimal("importe"));
                    m.setReferenciaExt(rs.getString("referencia_ext"));
                    long insc = rs.getLong("inscripcion_id");
                    m.setInscripcionId(rs.wasNull() ? null : insc);
                    out.add(m);
                }
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando movimientos", e);
        }
    }

    @Override
    public Long ensureCuentaParaSocio(Long socioId) {
        final String sel = "SELECT id FROM cuenta WHERE socio_id = ?";
        final String ins = "INSERT INTO cuenta (socio_id) VALUES (?)";
        try (Connection con = DataSourceFactory.get().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(sel)) {
                ps.setLong(1, socioId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
            try (PreparedStatement ps = con.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, socioId);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
            throw new RuntimeException("No se pudo crear/obtener la cuenta para el socio " + socioId);
        } catch (SQLException e) {
            throw new RuntimeException("Error al asegurar cuenta de socio", e);
        }
    }
}
