package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.model.EstadoInscripcion;
import ar.edu.csp.sistemadegestioncspgui.model.Inscripcion;

// ar.edu.csp.sistemadegestioncspgui.dao.impl.InscripcionDaoImpl
public class InscripcionDaoImpl implements InscripcionDao {
    private final javax.sql.DataSource ds = ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory.get();
    private final CuentaDao cuentaDao = new CuentaDaoImpl();

    private static final String SQL_LIST = """
        SELECT i.id, i.socio_id, i.actividad_id, i.estado, i.fecha_alta, i.fecha_baja,
               a.nombre AS actividadNombre, a.cuota_mensual AS cuotaMensual
        FROM inscripcion i
        JOIN actividad a ON a.id = i.actividad_id
        WHERE i.socio_id = ?
        ORDER BY i.estado DESC, a.nombre
        """;

    private static final String SQL_INSERT = """
        INSERT INTO inscripcion (socio_id, actividad_id, estado, fecha_alta) 
        VALUES (?, ?, 'ACTIVA', CURRENT_DATE)
        """;

    private static final String SQL_BAJA = """
        UPDATE inscripcion 
           SET estado='BAJA', fecha_baja=CURRENT_DATE 
         WHERE id=? AND estado='ACTIVA'
        """;

    private static final String SQL_CUOTA = "SELECT cuota_mensual FROM actividad WHERE id=?";

    @Override public java.util.List<Inscripcion> listarPorSocio(long socioId) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SQL_LIST)) {
            ps.setLong(1, socioId);
            try (var rs = ps.executeQuery()) {
                var out = new java.util.ArrayList<Inscripcion>();
                while (rs.next()) {
                    var i = new Inscripcion();
                    i.setId(rs.getLong("id"));
                    i.setSocioId(rs.getLong("socio_id"));
                    i.setActividadId(rs.getLong("actividad_id"));
                    var est = rs.getString("estado");
                    i.setEstado(est==null? EstadoInscripcion.ACTIVA:EstadoInscripcion.valueOf(est));
                    var fa = rs.getDate("fecha_alta");  i.setFechaAlta(fa==null?null:fa.toLocalDate());
                    var fb = rs.getDate("fecha_baja");  i.setFechaBaja(fb==null?null:fb.toLocalDate());
                    i.setActividadNombre(rs.getString("actividadNombre"));
                    i.setCuotaMensual(rs.getBigDecimal("cuotaMensual"));
                    out.add(i);
                }
                return out;
            }
        }
    }

    @Override public long inscribir(long socioId, long actividadId) throws Exception {
        try (var cn = ds.getConnection()) {
            cn.setAutoCommit(false);
            long newId;
            // 1) crear inscripción
            try (var ps = cn.prepareStatement(SQL_INSERT, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, socioId);
                ps.setLong(2, actividadId);
                ps.executeUpdate();
                try (var keys = ps.getGeneratedKeys()) { keys.next(); newId = keys.getLong(1); }
            }
            // 2) generar cargo de la cuota vigente
            java.math.BigDecimal cuota = java.math.BigDecimal.ZERO;
            try (var ps = cn.prepareStatement(SQL_CUOTA)) {
                ps.setLong(1, actividadId);
                try (var rs = ps.executeQuery()) { if (rs.next()) cuota = rs.getBigDecimal(1); }
            }
            cn.commit(); cn.setAutoCommit(true);

            if (cuota != null && cuota.signum() > 0) {
                new CuentaDaoImpl().registrarCargo(socioId, cuota, "Inscripción a actividad ID " + actividadId);
            }
            return newId;
        }
    }

    @Override public boolean baja(long inscripcionId) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SQL_BAJA)) {
            ps.setLong(1, inscripcionId);
            return ps.executeUpdate() > 0;
        }
    }
}
