package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.dao.ActividadDaoImpl;
import ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory;
import ar.edu.csp.sistemadegestioncspgui.model.Actividad;
import ar.edu.csp.sistemadegestioncspgui.model.EstadoActividad;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ActividadDaoImpl implements ActividadDao {

    private final DataSource ds = DataSourceFactory.get();

    private static final String SELECT_BASE = """
        SELECT id, nombre, descripcion, estado, precio_default, creado_en, actualizado_en
        FROM actividad
        """;
    private static final String SELECT_TODAS   = SELECT_BASE + " ORDER BY nombre";
    private static final String SELECT_ACTIVAS = SELECT_BASE + " WHERE estado = 'ACTIVA' OR estado IS NULL ORDER BY nombre";
    private static final String SELECT_BY_ID   = SELECT_BASE + " WHERE id = ?";

    private static final String INSERT_SQL = """
        INSERT INTO actividad (nombre, descripcion, estado, precio_default, creado_en, actualizado_en)
        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;
    private static final String UPDATE_SQL = """
        UPDATE actividad
           SET nombre = ?, descripcion = ?, estado = ?, precio_default = ?, actualizado_en = CURRENT_TIMESTAMP
         WHERE id = ?
        """;
    private static final String UPDATE_ESTADO = """
        UPDATE actividad SET estado = ?, actualizado_en = CURRENT_TIMESTAMP WHERE id = ?
        """;
    private static final String UPDATE_PRECIO = """
        UPDATE actividad SET precio_default = ?, actualizado_en = CURRENT_TIMESTAMP WHERE id = ?
        """;


    private Actividad map(ResultSet rs) throws SQLException {
        var a = new Actividad();
        a.setId(rs.getLong("id"));
        a.setNombre(rs.getString("nombre"));
        a.setDescripcion(rs.getString("descripcion"));
        a.setEstado(EstadoActividad.fromDb(rs.getString("estado")));
        a.setPrecioDefault(rs.getBigDecimal("precio_default"));
        var c = rs.getTimestamp("creado_en");
        var u = rs.getTimestamp("actualizado_en");
        a.setCreadoEn(c == null ? null : c.toLocalDateTime());
        a.setActualizadoEn(u == null ? null : u.toLocalDateTime());
        return a;
    }

    // ---------- lecturas ----------
    @Override
    public List<Actividad> listarTodas() throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SELECT_TODAS); var rs = ps.executeQuery()) {
            List<Actividad> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        }
    }

    @Override
    public List<Actividad> listarActivas() throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SELECT_ACTIVAS); var rs = ps.executeQuery()) {
            List<Actividad> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        }
    }

    @Override
    public Optional<Actividad> buscarPorId(long id) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    // ---------- escrituras ----------
    @Override
    public long crear(Actividad a) throws Exception {
        if (a.getEstado() == null) a.setEstado(EstadoActividad.ACTIVA);
        try (var cn = ds.getConnection();
             var ps = cn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getNombre());
            ps.setString(2, a.getDescripcion());
            ps.setString(3, a.getEstado().toDb());
            ps.setBigDecimal(4, a.getPrecioDefault());
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) a.setId(keys.getLong(1));
            }
            return a.getId();
        }
    }

    @Override
    public boolean actualizar(Actividad a) throws Exception {
        if (a.getId() == null) throw new IllegalArgumentException("Actividad sin id");
        if (a.getEstado() == null) a.setEstado(EstadoActividad.ACTIVA);
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(UPDATE_SQL)) {
            ps.setString(1, a.getNombre());
            ps.setString(2, a.getDescripcion());
            ps.setString(3, a.getEstado().toDb());
            ps.setBigDecimal(4, a.getPrecioDefault());
            ps.setLong(5, a.getId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean cambiarEstado(long id, EstadoActividad est) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(UPDATE_ESTADO)) {
            ps.setString(1, (est == null ? EstadoActividad.ACTIVA : est).toDb());
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean actualizarPrecio(long id, java.math.BigDecimal nuevoPrecio) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(UPDATE_PRECIO)) {
            ps.setBigDecimal(1, nuevoPrecio);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        }
    }
}
