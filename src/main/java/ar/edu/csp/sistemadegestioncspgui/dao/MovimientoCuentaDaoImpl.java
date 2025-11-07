package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory;
import ar.edu.csp.sistemadegestioncspgui.model.Actividad;
import ar.edu.csp.sistemadegestioncspgui.model.MovimientoCuenta;
import java.util.Set;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MovimientoCuentaDaoImpl implements MovimientoCuentaDao {

    private static final Set<String> TIPOS_VALIDOS = Set.of(
            "ALTA_SOCIO_CUOTA_CLUB",
            "PAGO",
            "INSCRIPCION_ACTIVIDAD",
            "AJUSTE_DEBITO",
            "AJUSTE_CREDITO",
            "BAJA_REINTEGRO"
    );

    final String sql = """
    SELECT COALESCE(SUM(CASE
              WHEN mc.tipo IN ('PAGO','AJUSTE_CREDITO','BAJA_REINTEGRO') THEN mc.importe    -- créditos (+)
              WHEN mc.tipo IN ('ALTA_SOCIO_CUOTA_CLUB','INSCRIPCION_ACTIVIDAD','AJUSTE_DEBITO') THEN -mc.importe -- débitos (-)
              ELSE 0 END), 0) AS saldo
    FROM cuenta c
    LEFT JOIN movimiento_cuenta mc ON mc.cuenta_id = c.id
    WHERE c.socio_id = ?
""";


    private static void validarTipoEnum(String tipo) {
        if (tipo == null || !TIPOS_VALIDOS.contains(tipo)) {
            throw new IllegalArgumentException(
                    "Tipo de movimiento inválido '" + tipo + "'. Debe ser uno de: " + TIPOS_VALIDOS
            );
        }
    }

    @Override
    public Long insertar(MovimientoCuenta m) {

        validarTipoEnum(m.getTipo());

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
            String msg = String.format(
                    "Error al insertar movimiento (sqlState=%s, errorCode=%d): %s",
                    e.getSQLState(), e.getErrorCode(), e.getMessage()
            );
            throw new RuntimeException(msg, e);
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
                "PAGO",
                (descripcion != null && !descripcion.isBlank()) ? descripcion : "Pago",
                importe,
                null,
                null
        );
        return insertar(m);
    }



    @Override
    public Long registrarCargo(Long socioId, BigDecimal importe, String descripcion, LocalDate fecha, Long inscripcionId) {
        Long cuentaId = ensureCuentaParaSocio(socioId);
        MovimientoCuenta m = new MovimientoCuenta(
                cuentaId,
                fecha != null ? fecha : LocalDate.now(),
                "AJUSTE_DEBITO",
                (descripcion != null && !descripcion.isBlank()) ? descripcion : "Cargo",
                importe,
                null,
                inscripcionId
        );
        return insertar(m);
    }


    public Long registrarCargo(Long socioId, BigDecimal importe, String descripcion,
                               LocalDate fecha, Long inscripcionId, String tipoEnum) {
        if (importe == null || importe.signum() <= 0) {
            throw new IllegalArgumentException("El importe del cargo debe ser positivo.");
        }
        if (tipoEnum == null || tipoEnum.isBlank()) {
            throw new IllegalArgumentException("Debe indicarse un tipo de movimiento válido (ENUM).");
        }
        Long cuentaId = ensureCuentaParaSocio(socioId);
        MovimientoCuenta m = new MovimientoCuenta(
                cuentaId,
                fecha != null ? fecha : LocalDate.now(),
                tipoEnum,
                (descripcion != null && !descripcion.isBlank()) ? descripcion : "Cargo",
                importe,
                null,
                inscripcionId
        );
        return insertar(m);
    }

    private void registrarCargoMensual(Long socioId, BigDecimal importe, String descripcion,
                                       LocalDate fecha, String tipoEnum, Long inscripcionId) {
        registrarCargo(socioId, importe, descripcion, fecha, inscripcionId, tipoEnum);
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

    @Override
    public boolean existeCargoMensual(Long socioId, String concepto, YearMonth periodo) {
        final String sql = """
        SELECT 1
          FROM cuenta c
          JOIN movimiento_cuenta mc ON mc.cuenta_id = c.id
         WHERE c.socio_id = ?
           AND mc.descripcion LIKE ?
           AND mc.fecha BETWEEN ? AND ?
         LIMIT 1
    """;

        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();

        String like = concepto + " " + String.format("%02d/%d", periodo.getMonthValue(), periodo.getYear()) + "%";

        try (Connection con = DataSourceFactory.get().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, socioId);
            ps.setString(2, like);
            ps.setDate(3, Date.valueOf(desde));
            ps.setDate(4, Date.valueOf(hasta));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error verificando cargo mensual", e);
        }
    }



    @Override
    public void generarCargosMensuales(Long socioId, YearMonth periodo) {
        LocalDate fechaCargo = periodo.atEndOfMonth();

        var parametrosDao = new ParametrosDaoImpl();
        var inscDao = new InscripcionDaoImpl();

        // --- 1) Cuota Social ---
        BigDecimal cuotaSocial;
        try {
            cuotaSocial = parametrosDao.getDecimal("CUOTA_SOCIAL").orElse(BigDecimal.ZERO);
        } catch (Exception e) {
            cuotaSocial = BigDecimal.ZERO; // no frenamos todo si falla la param
        }

        if (cuotaSocial.signum() > 0) {
            String concepto = "Cuota Social";
            if (!existeCargoMensual(socioId, concepto, periodo)) {
                registrarCargoMensual(
                        socioId,
                        cuotaSocial,
                        concepto + " " + String.format("%02d/%d", periodo.getMonthValue(), periodo.getYear()),
                        fechaCargo,
                        "ALTA_SOCIO_CUOTA_CLUB",   // <<< ENUM correcto
                        null                       // inscripcion_id = null (cargo del club)
                );
            }
        }

        // --- 2) Actividades vigentes ---
        List<Actividad> actividades;
        try {
            actividades = inscDao.listarActividadesVigentesPorSocio(socioId);
        } catch (Exception e) {
            actividades = Collections.emptyList();
        }

        for (var act : actividades) {
            BigDecimal precio = act.getPrecioDefault();
            if (precio == null || precio.signum() <= 0) continue;

            String concepto = "Cuota Actividad " + act.getNombre();
            if (!existeCargoMensual(socioId, concepto, periodo)) {
                registrarCargoMensual(
                        socioId,
                        precio,
                        concepto + " " + String.format("%02d/%d", periodo.getMonthValue(), periodo.getYear()),
                        fechaCargo,
                        "INSCRIPCION_ACTIVIDAD",   // <<< ENUM correcto
                        null // si tenés el inscripcion_id de esa actividad, pásalo acá; si no, dejalo null
                );
            }
        }
    }

}

