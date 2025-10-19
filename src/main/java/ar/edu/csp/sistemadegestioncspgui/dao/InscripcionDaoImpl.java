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
        UPDATE inscripcion SET fecha_baja=?, estado='INACTIVA' WHERE id=?
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

    @Override
    public long inscribir(long socioId, long actividadId, BigDecimal precioUsado, String observacion) throws Exception {
        if (precioUsado == null || precioUsado.signum() <= 0) {
            throw new IllegalArgumentException("Precio inválido");
        }
        if (buscarActiva(socioId, actividadId).isPresent()) {
            throw new IllegalStateException("El socio ya está inscripto en esta actividad");
        }

        long inscId;
        try (var cn = ds.getConnection()) {
            // ✅ Chequeo de apto médico vigente ANTES del INSERT
            if (!aptoVigente(socioId, cn)) {
                throw new IllegalStateException("El socio no posee apto médico vigente.");
            }

            cn.setAutoCommit(false);

            try (var ps = cn.prepareStatement(INS_SQL, Statement.RETURN_GENERATED_KEYS)) {
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
            cn.setAutoCommit(true);
        }

        // Cargo en cuenta (negativo) vinculado a la inscripción (fuera de la tx anterior, como ya lo tenías)
        var act = actDao.buscarPorId(actividadId).orElseThrow(() -> new IllegalArgumentException("Actividad inexistente"));
        String desc = (observacion == null || observacion.isBlank())
                ? "Inscripción a " + act.getNombre()
                : observacion;
        cuentaDao.registrarCargoActividad(socioId, precioUsado, inscId, desc);

        return inscId;
    }

    @Override
    public boolean darDeBaja(long inscripcionId, LocalDate fechaBaja) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(BAJA_SQL)) {
            ps.setDate(1, java.sql.Date.valueOf(fechaBaja == null ? LocalDate.now() : fechaBaja));
            ps.setLong(2, inscripcionId);
            return ps.executeUpdate() > 0;
        }
    }

    // === Helper: apto médico vigente (fecha_vencimiento >= hoy) ===
    private boolean aptoVigente(long socioId, Connection cn) throws Exception {
        try (var ps = cn.prepareStatement("""
            SELECT 1
              FROM apto_medico
             WHERE socio_id = ?
               AND fecha_vencimiento >= CURRENT_DATE()
             LIMIT 1
        """)) {
            ps.setLong(1, socioId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
