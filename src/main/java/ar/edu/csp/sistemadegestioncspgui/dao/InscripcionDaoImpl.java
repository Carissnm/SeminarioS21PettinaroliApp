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
    //Pool de conexiones de la base de datos centralizado
    private final DataSource ds = DataSourceFactory.get();
    //DAO auxiliar para registrar cargos/débitos asociados a la inscripción
    private final CuentaDao cuentaDao = new CuentaDaoImpl();
    //DAO auxiliar para obtener datos de la actividad
    private final ActividadDao actDao  = new ActividadDaoImpl();

    // Permite traer datos de inscripcion + nombre y precio_default de actividad (para tus campos de vista)
    private static final String SEL_BASE = """
        SELECT i.id, i.socio_id, i.actividad_id, i.precio_alta, i.estado,
               i.fecha_alta, i.fecha_baja,
               a.nombre AS actividad_nombre, a.precio_default AS actividad_precio
          FROM inscripcion i
          JOIN actividad a ON a.id = i.actividad_id
        """;

    // Permite listar las inscripciones de un socio, de las más recientes a las más antiguas
    private static final String SEL_POR_SOCIO = SEL_BASE + " WHERE i.socio_id=? ORDER BY i.fecha_alta DESC, i.id DESC";

    // Busca una inscripción Activa específica
    private static final String SEL_ACTIVA = SEL_BASE + " WHERE i.socio_id=? AND i.actividad_id=? AND (i.estado='ACTIVA' OR i.estado IS NULL) AND i.fecha_baja IS NULL";

    // Permite dar de alta la inscripción, fijando la fecha de alta la del día de la fecha
    // en la que se realiza el alta y el estado como Activa.
    private static final String INS_SQL = """
        INSERT INTO inscripcion (socio_id, actividad_id, precio_alta, fecha_alta, estado)
        VALUES (?, ?, ?, CURRENT_DATE, 'ACTIVA')
        """;

    // Baja lógica. Se settea la fecha de baja con la fecha del mismo día de la baja y el estado para a Baja.
    private static final String BAJA_SQL = """
        UPDATE inscripcion SET fecha_baja=?, estado='BAJA' WHERE id=?
        """;

    /* El metodo map transforma la fila del resultset en un objeto Inscripción.
    Convierte el estado String a Enum
    */
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

        // Campos de vista para la interfaz de usuario
        i.setActividadNombre(rs.getString("actividad_nombre"));
        i.setCuotaMensual(rs.getBigDecimal("actividad_precio"));

        return i;
    }

    // Este metodo devuelve todas las inscripciones del socio, tanto activas
    // como inactivas, desde las más recientes a las más antiguas.
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


    // Este metodo retorna la inscripcion activa de un socio en el caso en el que exista
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

    // Metodo que suma los importes de la tabla movimiento_cuenta asociados a la inscripción.
    // Un saldo negativo implica deuda y por ende no permite dar de baja al socio de esa actividad.
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
        if (precioUsado == null || precioUsado.signum() <= 0)  // Primero se verifica que no exista inscripción previa activa para la misma actividad
            // y que el precioUsado sea mayor que cero.
            throw new IllegalArgumentException("Precio inválido");
        if (buscarActiva(socioId, actividadId).isPresent())
            throw new IllegalStateException("El socio ya está inscripto en esta actividad");

        long inscId;

        try (var cn = ds.getConnection()) {
            boolean origAuto = cn.getAutoCommit();
            try {
                //Si el socio está inactivo no es posible inscribirlo a ninguna actividad
                if (!socioActivo(socioId, cn))
                    throw new IllegalStateException("El socio está INACTIVO. No se puede inscribir.");

                // Se chequea que exista apto médico vigente para poder continuar con la inscripción
                if (!aptoVigente(socioId, cn))
                    throw new IllegalStateException("El socio no posee apto médico vigente.");

                cn.setAutoCommit(false);
                // Alta de inscripción con fecha actual
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
                //Si se realiza con éxito se confirma la transacción
                cn.commit();
            } catch (Exception ex) {
                //Si hay un error en las reglas o en el insert se vuelve atrás.
                try { cn.rollback(); } catch (Exception ignore) {}
                throw ex;
            } finally {
                try {
                    //Se restaura el autocommit original
                    cn.setAutoCommit(origAuto);
                } catch (Exception ignore) {}
            }
        }

        //Se registra el cargo por la inscripción en al cuenta del socio.
        //Si existe algún fallo no se revierte el alta de la inscripción.
        var act = actDao.buscarPorId(actividadId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad inexistente"));
        String desc = (observacion == null || observacion.isBlank())
                ? "Inscripción a " + act.getNombre()
                : observacion;
        cuentaDao.registrarCargoActividad(socioId, precioUsado, inscId, desc);

        return inscId;
    }

    // El metodo verifica que el socio figure con estado Activo, precondición para el alta de la inscripción.
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
                // Si existe deuda asociada a esa inscripción se bloquea la baja
                var saldo = saldoInscripcion(inscripcionId, cn);
                if (saldo.compareTo(java.math.BigDecimal.ZERO) < 0) {
                    throw new IllegalStateException("No se puede dar de baja: hay deuda pendiente en esta actividad.");
                }

                try (var ps = cn.prepareStatement(BAJA_SQL)) { //Se ejecuta la baja lógica
                    ps.setDate(1, java.sql.Date.valueOf(fechaBaja == null ? java.time.LocalDate.now() : fechaBaja));
                    ps.setLong(2, inscripcionId);
                    boolean ok = ps.executeUpdate() > 0;
                    cn.commit();
                    return ok; // Si no existe deuda se gestiona la baja cambiando el estado a Baja y se marca la fecha actual
                    //como fecha de baja.
                }

            } catch (Exception ex) {
                try {
                    cn.rollback();
                } catch (Exception ignore) {}
                throw ex;
            } finally {
                try {
                    cn.setAutoCommit(true);
                } catch (Exception ignore) {}
            }
        }
    }

    // Este metodo evalúa si existe un apto médico vigente para el socio, es decir, si tiene una fecha de vencimiento
    // mayor a la del día de la fecha en la que se quiere realizar la inscripción.
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
