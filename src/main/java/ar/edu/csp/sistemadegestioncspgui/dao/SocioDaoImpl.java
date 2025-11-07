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
/*
* Todas las lecturas traen, además de los campos del socio, un saldo calculado con una subconsulta que
* suma los movimientos de todas las cuentas del socio.
* Las bajas se manejan de como Bajas Lógicas, es decir, no se borran sino que se cambia el estado del socio de
* Activo a Inactivo y se guarda la fecha de baja.
* */
public class SocioDaoImpl implements SocioDao {
    //Pool de conexiones en un Datasource centralizado.
    private final DataSource ds = DataSourceFactory.get();

    // --- BASE SQL ---
    // Trae al socio y al saldo con una subconsulta que suma los movimientos de todas sus cuentas:
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
    //Lista ordenada por apellido y nombre:
    private static final String SELECT_ALL = SELECT_BASE + " ORDER BY s.apellido, s.nombre";
    private static final String SELECT_BY_ID = """
    SELECT s.id, s.dni, s.apellido, s.nombre, s.email, s.telefono, s.domicilio,
           s.fecha_nac, s.fecha_alta, s.fecha_baja, s.estado,
           COALESCE((
               SELECT SUM(mc.importe)
                 FROM cuenta c
                 JOIN movimiento_cuenta mc ON mc.cuenta_id = c.id
                WHERE c.socio_id = s.id
           ), 0) AS saldo
      FROM socio s
     WHERE s.id = ?
""";

    //Búsqueda de socio por prefijo de dni:
    private static final String SELECT_BY_DNI_PREFIX = SELECT_BASE + " WHERE s.dni LIKE ? ORDER BY s.apellido, s.nombre";

    // Lista de todos los socios, tanto activos como inactivos con fecha de alta y saldo:
    private static final String SELECT_TODOS = """
    SELECT s.id, s.dni, s.apellido, s.nombre, s.email, s.telefono, s.domicilio,
           s.fecha_nac, s.fecha_alta, s.fecha_baja, s.estado,
           COALESCE((
               SELECT SUM(mc.importe)
                 FROM cuenta c
                 JOIN movimiento_cuenta mc ON mc.cuenta_id = c.id
                WHERE c.socio_id = s.id
           ), 0) AS saldo
      FROM socio s
     ORDER BY s.apellido, s.nombre
    """;
    // INSERT, utiliza defaults de la base de datos para el estado y la fecha de alta:
    private static final String INSERT_SQL = """
        INSERT INTO socio (dni, nombre, apellido, fecha_nac, domicilio, email, telefono, fecha_baja)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    //Update completo del socio que incluye el estado y la fecha de alta:
    private static final String UPDATE_SQL = """
        UPDATE socio
           SET dni = ?, nombre = ?, apellido = ?, fecha_nac = ?, domicilio = ?, email = ?, telefono = ?, 
               estado = ?, fecha_baja = ?
         WHERE id = ?
        """;

    //Búsqueda por prefijo de dni:
    private static final String BUSCAR_X_DNI = """
    SELECT s.id, s.dni, s.apellido, s.nombre, s.email, s.telefono, s.domicilio,
           s.fecha_nac, s.fecha_alta, s.fecha_baja, s.estado,
           COALESCE((
               SELECT SUM(mc.importe)
                 FROM cuenta c
                 JOIN movimiento_cuenta mc ON mc.cuenta_id = c.id
                WHERE c.socio_id = s.id
           ), 0) AS saldo
      FROM socio s
     WHERE s.dni LIKE CONCAT(?, '%')
     ORDER BY s.dni
    """;

    // Consulta de saldo del socio
    private static final String SQL_SALDO_SOCIO = """
    SELECT COALESCE(SUM(m.importe), 0) AS saldo
    FROM cuenta c
    LEFT JOIN movimiento_cuenta m ON m.cuenta_id = c.id
    WHERE c.socio_id = ?
    """;

    //El socio no se borra por foreing keys sino que se marca como Inactivo con fecha de baja del día en el que se
    // realiza la baja:
    private static final String SQL_BAJA_LOGICA_SOCIO = """
    UPDATE socio
       SET estado = 'INACTIVO',
           fecha_baja = CURRENT_DATE
     WHERE id = ?
    """;

    //Baja lógica de inscripciones activas del socio para que no sigan
    //cobrándose las cuotas.
    private static final String SQL_BAJA_INSCRIPCIONES_ACTIVAS = """
    UPDATE inscripcion
       SET estado = 'BAJA',
           fecha_baja = CURRENT_DATE
     WHERE socio_id = ?
       AND estado = 'ACTIVA'
       AND fecha_baja IS NULL
    """;


    // Helpers para conversión de tipso de fecha entre JDBC y java.time
    private static LocalDate toLocal(Date d) {
        return d == null ? null : d.toLocalDate();
    }

    private static Date toDate(LocalDate d)  {
        return d == null ? null : Date.valueOf(d);
    }

    // Metodo para validación de campos obligatorios del socio en el ingreso de datos
    private void validarRequeridos(Socio s) {
        if (s == null) throw new IllegalArgumentException("Socio null");
        if (s.getDni() == null || s.getDni().isBlank())
            throw new IllegalArgumentException("El DNI es obligatorio.");
        if (s.getNombre() == null || s.getNombre().isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio.");
        if (s.getApellido() == null || s.getApellido().isBlank())
            throw new IllegalArgumentException("El apellido es obligatorio.");
    }

    // ========================== Mapeo ==========================
    // Con este metodo se mapea la fila del ResultSet a objeto socio
    // Se construye un Socio desde la fila actual del ResultSet.
    private Socio mapRow(ResultSet rs) throws SQLException {
        Socio s = new Socio();
        s.setId(rs.getLong("id"));
        s.setDni(rs.getString("dni"));
        s.setNombre(rs.getString("nombre"));
        s.setApellido(rs.getString("apellido"));
        s.setFechaNac(toLocal(rs.getDate("fecha_nac")));
        s.setFechaAlta(toLocal(rs.getDate("fecha_alta")));
        s.setFechaBaja(toLocal(rs.getDate("fecha_baja")));
        s.setDomicilio(rs.getString("domicilio"));
        s.setEmail(rs.getString("email"));
        s.setTelefono(rs.getString("telefono"));
        s.setEstado(EstadoSocio.fromDb(rs.getString("estado"))); // conversión de String a enum
        s.setSaldo(rs.getBigDecimal("saldo"));
        return s;
    }

    // ========================== Implementación de la interfaz ==========================

    @Override
    public List<Socio> listarTodos() throws SQLException {
        //Se listan todos lso socios con su saldo calculado para las pantallas de listado/administración.
        try (Connection cn = ds.getConnection();
             PreparedStatement st = cn.prepareStatement(SELECT_ALL);
             ResultSet rs = st.executeQuery()) {
            List<Socio> out = new ArrayList<>();
            while (rs.next()) out.add(mapRow(rs));
            return out;
        }
    }

    @Override
    public List<Socio> buscarPorDni(String dni) throws SQLException {
        // Búsqueda por prefijo de dni. Si el parámetro es null se interpreta como "".
        try (var cn = ds.getConnection();
             var ps = cn.prepareStatement(BUSCAR_X_DNI)) {
            ps.setString(1, dni == null ? "" : dni.trim());
            try (var rs = ps.executeQuery()) {
                List<Socio> out = new ArrayList<>();
                while (rs.next()) out.add(mapRow(rs));
                return out;
            }
        }
    }

    @Override
    public Optional<Socio> buscarPorId(long id) throws SQLException {
        //Recupera un socio por su id y devuelve Optional.empty() si no existe.
        //Incluye el cálculo de saldo apra ese socio.
        //No se usa por el momento en la aplicación pero queda por si eventualmente se decide buscar por id.
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
        /* Se inserta un nuevo socio y se devuelve el id generado
         Primero se validan los campos requeridos, luego se ejecuta un insert para que la base
         de datos le asigne default al estado/fecha de alta. A continuación se lee la primary key generada
         y se registra un débito inicial de alta del club */
        long id; // se guarda el id para su uso a posteriori
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
            ps.setDate(i++, toDate(s.getFechaBaja()));
            int affected = ps.executeUpdate();
            if (affected == 0) throw new SQLException("No se insertó socio");
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getLong(1); //se guarda la pk autogenerada
                    s.setId(id); // se refleja en el objeto
                } else {
                    throw new SQLException("No se obtuvo ID generado");
                }
            }
            // === Cargo inicial por alta ===
            try {
                var params = new ParametrosDaoImpl();
                var cuentaDao = new CuentaDaoImpl();
                var monto = params.getNumero("CUOTA_INICIAL_CLUB")
                        .or(() -> params.getNumero("CUOTA_MENSUAL_CLUB"))
                        .orElse(BigDecimal.ZERO);
                if (monto.signum() > 0) {
                    cuentaDao.registrarDebitoAltaClub(id, monto);
                }
            } catch (Exception e) {
                e.printStackTrace(); // Si falla el cargo se prioriza el alta del socio y no se revierte.
            }
            return id;
        }
    }

    @Override
    public boolean actualizar(Socio s) throws SQLException {
        //Actualización de los datos del socio por ID. Se aplica ACTIVO por defecto.
        //Si se actualiza al menos una fila devuelve true, y devuelve false si el id no existe.
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
            //En caso de no haber un estado especificado se usa ACTIVO para evitar errores en la base de datos.
            ps.setString(i++, (s.getEstado() == null ? EstadoSocio.ACTIVO : s.getEstado()).toDb());
            ps.setDate(i++, toDate(s.getFechaBaja()));
            ps.setLong(i, s.getId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean eliminar(long id) throws SQLException {
        /* Baja lógica en la que primero se verifica si hay deuda.
        * Si el saldo es negativo se aborta la acción.
        * De lo contrario se dan de baja las inscripciones activas del socio */
        try (Connection cn = ds.getConnection()) {
            cn.setAutoCommit(false);
            try {
                // 1) No se permite la baja si existe saldo negativo
                BigDecimal saldo = calcularSaldo(cn, id);
                if (saldo.compareTo(BigDecimal.ZERO) < 0) {
                    cn.rollback();
                    throw new SQLException("El socio tiene deuda pendiente (saldo: " + saldo + "). No se puede dar de baja.");
                }
                // 2) Se dan de baja inscripciones ACTIVAS (para que no se siga cobrando)
                try (PreparedStatement ps = cn.prepareStatement(SQL_BAJA_INSCRIPCIONES_ACTIVAS)) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
                // 3) Se realiza la Baja lógica del socio cambiando su estado a Inactivo
                int updated;
                try (PreparedStatement ps = cn.prepareStatement(SQL_BAJA_LOGICA_SOCIO)) {
                    ps.setLong(1, id);
                    updated = ps.executeUpdate();
                }
                cn.commit();
                return updated > 0;
            } catch (SQLException ex) {
                // Frente a cualquier error se revierte todo lo previo.
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true); //Siempre se restaura autocommit
            }
        }
    }
    //Se calcula el saldo reutilizando la misma conexión. Si no hay filas devuelve 0
    private BigDecimal calcularSaldo(Connection cn, long socioId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SQL_SALDO_SOCIO)) {
            ps.setLong(1, socioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("saldo");
                return BigDecimal.ZERO;
            }
        }
    }

    @Override
    public boolean reactivarSocio(long socioId) throws Exception {
        final String sql = """
        UPDATE socio
           SET estado = 'ACTIVO',
               fecha_baja = NULL
         WHERE id = ?
    """;
        try (var con = ds.getConnection();
             var ps  = con.prepareStatement(sql)) {
            ps.setLong(1, socioId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean actualizarEstado(Long socioId, EstadoSocio nuevo) throws Exception {
        if (nuevo == EstadoSocio.ACTIVO) {
            return reactivarSocio(socioId); // delega y limpia fecha_baja
        }
        final String sql = "UPDATE socio SET estado = ? WHERE id = ?";
        try (var con = ds.getConnection();
             var ps  = con.prepareStatement(sql)) {
            ps.setString(1, nuevo.name());
            ps.setLong(2, socioId);
            return ps.executeUpdate() > 0;
        }
    }



}
