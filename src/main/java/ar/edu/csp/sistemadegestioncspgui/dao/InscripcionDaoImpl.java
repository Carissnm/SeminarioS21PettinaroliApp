package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory;
import ar.edu.csp.sistemadegestioncspgui.model.EstadoInscripcion;
import ar.edu.csp.sistemadegestioncspgui.model.Inscripcion;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InscripcionDaoImpl implements InscripcionDao {
    private final DataSource ds = DataSourceFactory.get();
    private final CuentaDao cuentaDao = new CuentaDaoImpl();
    private final ActividadDao actDao  = new ActividadDaoImpl();

    // Trae datos de inscripcion + nombre y precio_default de actividad (para tus campos de vista)
    private static final String SEL_BASE = """
        SELECT i.id, i.socio_id, i.actividad_id, i.precio_alta, i.estado,
               i.fecha_alta, i.fecha_baja,
               a.nombre AS actividad_nombre, a.precio_default AS actividad_precio
          FROM inscripcion i
          JOIN actividad a ON a.id = i.actividad_id
        """;
    private static final String SEL_POR_SOCIO = SEL_BASE + " WHERE i.socio_id=? ORDER BY i.fecha_alta DESC, i.id DESC";
    private static final String SEL_ACTIVA    = SEL_BASE + " WHERE i.socio_id=? AND i.actividad_id=? AND (i.estado='ACTIVA' OR i.estado IS NULL) AND i.fecha_baja IS NULL";

    private static final String INS_SQL = """
        INSERT INTO inscripcion (socio_id, actividad_id, precio_alta, fecha_alta, estado)
        VALUES (?, ?, ?, CURRENT_DATE, 'ACTIVA')
        """;

    private static final String BAJA_SQL = """
        UPDATE inscripcion SET fecha_baja=?, estado='BAJA' WHERE id=?
        """;

    private Inscripcion map(ResultSet rs) throws SQLException {
        var i = new Inscripcion();
        i.setId(rs.getLong("id"));
        i.setSocioId(rs.getLong("socio_id"));
        i.setActividadId(rs.getLong("actividad_id"));
        i.setPrecioAlta(rs.getBigDecimal("precio_alta"));
        i.setEstado(EstadoInscripcion.fromDb(rs.getString("estado")));
        var fa = rs.getDate("fecha_alta");
        var fb = rs.getDate("fecha_baja");
        i.setFechaAlta(fa == null ? null : fa.toLocalDate());
        i.setFechaBaja(fb == null ? null : fb.toLocalDate());

        // Campos de vista que pediste:
        i.setActividadNombre(rs.getString("actividad_nombre"));
        i.setCuotaMensual(rs.getBigDecimal("actividad_precio"));

        return i;
    }

    @Override
    public List<Inscripcion> listarPorSocio(long socioId) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SEL_POR_SOCIO)) {
            ps.setLong(1, socioId);
            try (var rs = ps.executeQuery()) {
                List<Inscripcion> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    @Override
    public Optional<Inscripcion> buscarActiva(long socioId, long actividadId) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SEL_ACTIVA)) {
            ps.setLong(1, socioId);
            ps.setLong(2, actividadId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private java.math.BigDecimal saldoInscripcion(long inscripcionId, java.sql.Connection cn) throws Exception {
        try (var ps = cn.prepareStatement("""
        SELECT COALESCE(SUM(importe),0) AS saldo
          FROM movimiento_cuenta
         WHERE inscripcion_id = ?
    """)) {
            ps.setLong(1, inscripcionId);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("saldo");
            }
        }
    }

    @Override
    public long inscribir(long socioId, long actividadId, BigDecimal precioUsado, String observacion) throws Exception {
        if (precioUsado == null || precioUsado.signum() <= 0)
            throw new IllegalArgumentException("Precio inválido");
        if (buscarActiva(socioId, actividadId).isPresent())
            throw new IllegalStateException("El socio ya está inscripto en esta actividad");

        long inscId;

        try (var cn = ds.getConnection()) {
            boolean origAuto = cn.getAutoCommit();
            try {
                // ⛔️ Nuevo: no permitir si el socio está INACTIVO
                if (!socioActivo(socioId, cn))
                    throw new IllegalStateException("El socio está INACTIVO. No se puede inscribir.");

                // ✅ Chequeo de apto vigente
                if (!aptoVigente(socioId, cn))
                    throw new IllegalStateException("El socio no posee apto médico vigente.");

                cn.setAutoCommit(false);

                try (var ps = cn.prepareStatement("""
                INSERT INTO inscripcion (socio_id, actividad_id, precio_alta, fecha_alta, estado)
                VALUES (?, ?, ?, CURRENT_DATE, 'ACTIVA')
            """, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, socioId);
                    ps.setLong(2, actividadId);
                    ps.setBigDecimal(3, precioUsado);
                    ps.executeUpdate();
                    try (var keys = ps.getGeneratedKeys()) {
                        if (keys.next()) inscId = keys.getLong(1);
                        else throw new SQLException("No se generó ID de inscripción");
                    }
                }

                cn.commit();
            } catch (Exception ex) {
                try { cn.rollback(); } catch (Exception ignore) {}
                throw ex;
            } finally {
                try { cn.setAutoCommit(origAuto); } catch (Exception ignore) {}
            }
        }

        var act = actDao.buscarPorId(actividadId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad inexistente"));
        String desc = (observacion == null || observacion.isBlank())
                ? "Inscripción a " + act.getNombre()
                : observacion;
        cuentaDao.registrarCargoActividad(socioId, precioUsado, inscId, desc);

        return inscId;
    }

    // Socio debe estar ACTIVO y sin fecha_baja
    private boolean socioActivo(long socioId, Connection cn) throws Exception {
        try (var ps = cn.prepareStatement("""
        SELECT 1
          FROM socio
         WHERE id = ?
           AND (estado = 'ACTIVO' OR estado IS NULL)
           AND (fecha_baja IS NULL)
         LIMIT 1
    """)) {
            ps.setLong(1, socioId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean darDeBaja(long inscripcionId, LocalDate fechaBaja) throws Exception {
        try (var cn = ds.getConnection()) {
            cn.setAutoCommit(false);
            try {
                // ⛔️ Bloqueo si hay deuda asociada a esa inscripción
                var saldo = saldoInscripcion(inscripcionId, cn);
                if (saldo.compareTo(java.math.BigDecimal.ZERO) < 0) {
                    throw new IllegalStateException("No se puede dar de baja: hay deuda pendiente en esta actividad.");
                }

                try (var ps = cn.prepareStatement(BAJA_SQL)) {
                    ps.setDate(1, java.sql.Date.valueOf(fechaBaja == null ? java.time.LocalDate.now() : fechaBaja));
                    ps.setLong(2, inscripcionId);
                    boolean ok = ps.executeUpdate() > 0;
                    cn.commit();
                    return ok;
                }

            } catch (Exception ex) {
                try { cn.rollback(); } catch (Exception ignore) {}
                throw ex;
            } finally {
                try { cn.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }
    }

    // === Helper: apto médico vigente (fecha_vencimiento >= hoy) ===
    private boolean aptoVigente(long socioId, Connection cn) throws Exception {
        try (var ps = cn.prepareStatement("""
        SELECT 1
          FROM apto_medico
         WHERE socio_id = ?
           AND fecha_vencimiento >= CURRENT_DATE
         LIMIT 1
    """)) {
            ps.setLong(1, socioId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
