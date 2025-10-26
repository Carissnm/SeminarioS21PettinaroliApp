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

    //Pool de conexiones de la base de datos centralizado
    private final DataSource ds = DataSourceFactory.get();

    // Permite traer todos los campos relevantes de una actividad.
    private static final String SELECT_BASE = """
        SELECT id, nombre, descripcion, estado, precio_default, creado_en, actualizado_en
        FROM actividad
        """;

    // Permite seleccionar todas las actividades y listarlas ordenadas por nombre.
    private static final String SELECT_TODAS = SELECT_BASE + " ORDER BY nombre";
    // Selecciona y muestra solo las actividades con estado Activa, ordenadas por nombre.
    private static final String SELECT_ACTIVAS = SELECT_BASE + " WHERE estado = 'ACTIVA' OR estado IS NULL ORDER BY nombre";
    // Permite una búsqueda puntual por id.
    private static final String SELECT_BY_ID = SELECT_BASE + " WHERE id = ?";
    // Permite insertar una nueva actividad.
    private static final String INSERT_SQL = """
        INSERT INTO actividad (nombre, descripcion, estado, precio_default, creado_en, actualizado_en)
        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;
    // Actualiza todos los campos editables.
    private static final String UPDATE_SQL = """
        UPDATE actividad
           SET nombre = ?, descripcion = ?, estado = ?, precio_default = ?, actualizado_en = CURRENT_TIMESTAMP
         WHERE id = ?
        """;
    // Permite cambiar el estado.
    private static final String UPDATE_ESTADO = """
        UPDATE actividad SET estado = ?, actualizado_en = CURRENT_TIMESTAMP WHERE id = ?
        """;
    // Permite cambiar el precio
    private static final String UPDATE_PRECIO = """
        UPDATE actividad SET precio_default = ?, actualizado_en = CURRENT_TIMESTAMP WHERE id = ?
        """;

    // A través de un mapeo se transforma la fila de la tabla de la base de datos
    // en un objeto det ipo Actividad.
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

    // Devuelve toda slas actividades, tanto activas como inactivas, ordenadas por nombre.
    @Override
    public List<Actividad> listarTodas() throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SELECT_TODAS); var rs = ps.executeQuery()) {
            List<Actividad> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        }
    }

    // Devuelve una lista con las actividades activas, ordenadas por nombre.
    @Override
    public List<Actividad> listarActivas() throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SELECT_ACTIVAS); var rs = ps.executeQuery()) {
            List<Actividad> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        }
    }

    //Permite buscar actividad por su id.
    @Override
    public Optional<Actividad> buscarPorId(long id) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    // Este metodo permite la creación de una nuev aactividad
    // y su inserción en la base de datos.
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

    // Metodo para la actualización/modificación de los datos de una actividad.
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

    // Este metodo permite modificar el estado de una actividad. Se toma un estado Activo por defecto
    // si el estado ingresa como null.
    @Override
    public boolean cambiarEstado(long id, EstadoActividad est) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(UPDATE_ESTADO)) {
            ps.setString(1, (est == null ? EstadoActividad.ACTIVA : est).toDb());
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Permite actualizar el precio por default de una actividad.
    @Override
    public boolean actualizarPrecio(long id, java.math.BigDecimal nuevoPrecio) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(UPDATE_PRECIO)) {
            ps.setBigDecimal(1, nuevoPrecio);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        }
    }
}
