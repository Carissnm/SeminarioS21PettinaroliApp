package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory;
import ar.edu.csp.sistemadegestioncspgui.model.EstadoSocio;
import ar.edu.csp.sistemadegestioncspgui.model.Socio;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SocioDaoImpl implements SocioDao {

    private final DataSource ds = DataSourceFactory.get();

    // --- BASE SQL ---
    private static final String SELECT_BASE = """
    SELECT 
        s.id, s.dni, s.nombre, s.apellido, s.fecha_nac, s.domicilio, s.email,
        s.telefono, s.estado, s.fecha_alta, s.fecha_baja,
        (
          SELECT COALESCE(SUM(m.importe), 0)
          FROM cuenta c
          LEFT JOIN movimiento_cuenta m ON m.cuenta_id = c.id
          WHERE c.socio_id = s.id
        ) AS saldo
    FROM socio s
    """;

    private static final String SELECT_ALL           = SELECT_BASE + " ORDER BY s.apellido, s.nombre";
    private static final String SELECT_BY_ID         = SELECT_BASE + " WHERE s.id = ?";
    private static final String SELECT_BY_DNI_PREFIX = SELECT_BASE + " WHERE s.dni LIKE ? ORDER BY s.apellido, s.nombre";


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

    private static final String SQL_SALDO_SOCIO = """
    SELECT COALESCE(SUM(m.importe), 0) AS saldo
    FROM cuenta c
    LEFT JOIN movimiento_cuenta m ON m.cuenta_id = c.id
    WHERE c.socio_id = ?
    """;

    private static final String SQL_BAJA_LOGICA_SOCIO = """
    UPDATE socio
       SET estado = 'INACTIVO',
           fecha_baja = CURRENT_DATE
     WHERE id = ?
    """;

    private static final String SQL_BAJA_INSCRIPCIONES_ACTIVAS = """
    UPDATE inscripcion
       SET estado = 'BAJA',
           fecha_baja = CURRENT_DATE
     WHERE socio_id = ?
       AND estado = 'ACTIVA'
       AND fecha_baja IS NULL
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
        s.setSaldo(rs.getBigDecimal("saldo"));
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
        try (Connection cn = ds.getConnection()) {
            cn.setAutoCommit(false);
            try {
                // 1) Regla de negocio: si tiene deuda -> no permitir
                BigDecimal saldo = calcularSaldo(cn, id);
                if (saldo.compareTo(BigDecimal.ZERO) < 0) {
                    cn.rollback();
                    throw new SQLException("El socio tiene deuda pendiente (saldo: " + saldo + "). No se puede dar de baja.");
                }

                // 2) Dar de baja inscripciones ACTIVAS (para que no se siga cobrando)
                try (PreparedStatement ps = cn.prepareStatement(SQL_BAJA_INSCRIPCIONES_ACTIVAS)) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }

                // 3) Baja lógica del socio (no borrar por FKs: aptos, etc.)
                int updated;
                try (PreparedStatement ps = cn.prepareStatement(SQL_BAJA_LOGICA_SOCIO)) {
                    ps.setLong(1, id);
                    updated = ps.executeUpdate();
                }

                cn.commit();
                return updated > 0;

            } catch (SQLException ex) {
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    private BigDecimal calcularSaldo(Connection cn, long socioId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_SALDO_SOCIO)) {
            ps.setLong(1, socioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("saldo");
                return BigDecimal.ZERO;
            }
        }
    }
}
