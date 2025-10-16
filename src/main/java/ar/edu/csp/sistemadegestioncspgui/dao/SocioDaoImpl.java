package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory;
import ar.edu.csp.sistemadegestioncspgui.model.EstadoSocio;
import ar.edu.csp.sistemadegestioncspgui.model.Socio;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SocioDaoImpl implements SocioDao {

    private final DataSource ds = DataSourceFactory.get();

    // --- SQL base ---
    private static final String SELECT_BASE = """
        SELECT id, dni, nombre, apellido, fecha_nac, domicilio, email,
               telefono, estado, fecha_alta, fecha_baja
          FROM socio
        """;

    private static final String SELECT_ALL            = SELECT_BASE + " ORDER BY apellido, nombre";
    private static final String SELECT_BY_ID          = SELECT_BASE + " WHERE id = ?";
    private static final String SELECT_BY_DNI_PREFIX  = SELECT_BASE + " WHERE dni LIKE ? ORDER BY apellido, nombre";

    // INSERT omite estado/fecha_alta para usar defaults de la BD
    private static final String INSERT_SQL = """
        INSERT INTO socio (dni, nombre, apellido, fecha_nac, domicilio, email, telefono, fecha_baja)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String UPDATE_SQL = """
        UPDATE socio
           SET dni = ?, nombre = ?, apellido = ?, fecha_nac = ?, domicilio = ?, email = ?, telefono = ?, 
               estado = ?, fecha_baja = ?
         WHERE id = ?
        """;

    private static LocalDate toLocal(Date d) { return d == null ? null : d.toLocalDate(); }
    private static Date toDate(LocalDate d)  { return d == null ? null : Date.valueOf(d); }

    private void validarRequeridos(Socio s) {
        if (s == null) throw new IllegalArgumentException("Socio null");
        if (s.getDni() == null || s.getDni().isBlank())
            throw new IllegalArgumentException("El DNI es obligatorio.");
        if (s.getNombre() == null || s.getNombre().isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio.");
        if (s.getApellido() == null || s.getApellido().isBlank())
            throw new IllegalArgumentException("El apellido es obligatorio.");
    }

    private Socio mapRow(ResultSet rs) throws SQLException {
        Socio s = new Socio();
        s.setId(rs.getLong("id"));
        s.setDni(rs.getString("dni"));
        s.setNombre(rs.getString("nombre"));
        s.setApellido(rs.getString("apellido"));
        s.setFechaNac(toLocal(rs.getDate("fecha_nac")));
        s.setDomicilio(rs.getString("domicilio"));
        s.setEmail(rs.getString("email"));
        s.setTelefono(rs.getString("telefono"));
        s.setEstado(EstadoSocio.fromDb(rs.getString("estado")));
        s.setFechaAlta(toLocal(rs.getDate("fecha_alta")));
        s.setFechaBaja(toLocal(rs.getDate("fecha_baja")));
        return s;
    }

    // ========================== Impl de la interfaz ==========================

    @Override
    public List<Socio> listarTodos() throws SQLException {
        try (Connection cn = ds.getConnection();
             PreparedStatement st = cn.prepareStatement(SELECT_ALL);
             ResultSet rs = st.executeQuery()) {
            List<Socio> out = new ArrayList<>();
            while (rs.next()) out.add(mapRow(rs));
            return out;
        }
    }

    @Override
    public List<Socio> buscarPorDni(String dniPrefix) throws SQLException {
        String pattern = (dniPrefix == null || dniPrefix.isBlank())
                ? "%"
                : dniPrefix.replaceAll("\\D", "") + "%";
        try (Connection cn = ds.getConnection();
             PreparedStatement st = cn.prepareStatement(SELECT_BY_DNI_PREFIX)) {
            st.setString(1, pattern);
            try (ResultSet rs = st.executeQuery()) {
                List<Socio> out = new ArrayList<>();
                while (rs.next()) out.add(mapRow(rs));
                return out;
            }
        }
    }

    @Override
    public Optional<Socio> buscarPorId(long id) throws SQLException {
        try (Connection cn = ds.getConnection();
             PreparedStatement st = cn.prepareStatement(SELECT_BY_ID)) {
            st.setLong(1, id);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public long crear(Socio s) throws SQLException {
        validarRequeridos(s);

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            int i = 1;
            ps.setString(i++, s.getDni());
            ps.setString(i++, s.getNombre());
            ps.setString(i++, s.getApellido());
            ps.setDate(i++, toDate(s.getFechaNac()));
            ps.setString(i++, s.getDomicilio());
            ps.setString(i++, s.getEmail());
            ps.setString(i++, s.getTelefono());
            ps.setDate(i++, toDate(s.getFechaBaja())); // puede ser null -> queda null

            int affected = ps.executeUpdate();
            if (affected == 0) throw new SQLException("No se insertó socio");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    s.setId(id);
                    return id;
                } else {
                    throw new SQLException("No se obtuvo ID generado");
                }
            }
        }

        /* Si preferís setear estado/fecha_alta manualmente, reemplazá por:
        String sql = "INSERT INTO socio (dni,nombre,apellido,fecha_nac,domicilio,email,telefono,estado,fecha_alta,fecha_baja) VALUES (?,?,?,?,?,?,?,?,?,?)";
        ps.setString(8, (s.getEstado() == null ? EstadoSocio.ACTIVO : s.getEstado()).toDb());
        ps.setDate(9, toDate(s.getFechaAlta() != null ? s.getFechaAlta() : LocalDate.now()));
        ps.setDate(10, toDate(s.getFechaBaja()));
        */
    }

    @Override
    public boolean actualizar(Socio s) throws SQLException {
        if (s == null || s.getId() == null) throw new IllegalArgumentException("Id requerido");
        validarRequeridos(s);

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_SQL)) {

            int i = 1;
            ps.setString(i++, s.getDni());
            ps.setString(i++, s.getNombre());
            ps.setString(i++, s.getApellido());
            ps.setDate(i++, toDate(s.getFechaNac()));
            ps.setString(i++, s.getDomicilio());
            ps.setString(i++, s.getEmail());
            ps.setString(i++, s.getTelefono());
            ps.setString(i++, (s.getEstado() == null ? EstadoSocio.ACTIVO : s.getEstado()).toDb());
            ps.setDate(i++, toDate(s.getFechaBaja()));
            ps.setLong(i, s.getId());

            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean eliminar(long id) throws SQLException {
        try (Connection cn = ds.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM socio WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
        // Si hay FKs, preferí baja lógica:
        // UPDATE socio SET estado='INACTIVO', fecha_baja = CURRENT_DATE WHERE id=?
    }
}
