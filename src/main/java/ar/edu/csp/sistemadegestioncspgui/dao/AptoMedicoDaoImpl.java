package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;

public class AptoMedicoDaoImpl implements AptoMedicoDao {

    //Pool de conexiones de la base de datos centralizado
    private final DataSource ds = DataSourceFactory.get();

    // Inserción de un nuevo apto médico en un socio ya existente.
    private static final String INSERT_SQL = """
        INSERT INTO apto_medico (socio_id, fecha_emision, fecha_vencimiento, observaciones)
        VALUES (?, ?, ?, ?)
    """;

    // Actualización de fechas y observaciones del apto médico preexistente.
    private static final String UPSERT_SQL = """
    INSERT INTO apto_medico (socio_id, fecha_emision, fecha_vencimiento, observaciones)
    VALUES (?, ?, ?, ?)
    ON DUPLICATE KEY UPDATE
        fecha_emision = VALUES(fecha_emision),
        fecha_vencimiento = VALUES(fecha_vencimiento),
        observaciones = VALUES(observaciones)
    """;

    // Permite traer el último vencimiento mayor registrado para el socio.
    private static final String SELECT_ULT_VENC = """
        SELECT fecha_vencimiento
          FROM apto_medico
         WHERE socio_id = ?
         ORDER BY fecha_vencimiento DESC
         LIMIT 1
    """;

    // Chequea si el socio tiene algún apto cuyo vencimiento sea la fecha actual o posterior.
    private static final String SELECT_VIGENTE = """
        SELECT 1
          FROM apto_medico
         WHERE socio_id = ?
           AND fecha_vencimiento >= CURRENT_DATE
         LIMIT 1
    """;

    @Override
    public void upsertApto(long socioId, LocalDate fechaEmision, LocalDate fechaVenc) throws Exception {
        // Metodo que inserta o actualiza el apto médico del socio.
        try (var cn = ds.getConnection();
             var ps = cn.prepareStatement(UPSERT_SQL)) {
            ps.setLong(1, socioId);
            ps.setDate(2, java.sql.Date.valueOf(fechaEmision));
            ps.setDate(3, java.sql.Date.valueOf(fechaVenc));
            ps.setString(4, null);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<LocalDate> ultimoVencimiento(long socioId) throws Exception {
        // Devuelve el mayor vencimiento registrado para el socio si existe.
        // Si no existe devuelve Optional.empty().
        try (var cn = ds.getConnection();
             var ps = cn.prepareStatement(SELECT_ULT_VENC)) {
            ps.setLong(1, socioId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return Optional.ofNullable(rs.getDate(1)).map(Date::toLocalDate);
                return Optional.empty();
            }
        }
    }

    @Override
    public boolean tieneAptoVigente(long socioId) throws Exception {
        //Devuevle true si existe al menos un apto médico cuyo vencimiento sea mayor o igual al de hoy
        try (var cn = ds.getConnection();
             var ps = cn.prepareStatement(SELECT_VIGENTE)) {
            ps.setLong(1, socioId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
