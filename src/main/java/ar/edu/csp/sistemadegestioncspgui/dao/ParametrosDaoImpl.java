package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class ParametrosDaoImpl implements ParametrosDao {
    //Pool de conexiones para obtener conexiones JDBC
    private final DataSource ds = DataSourceFactory.get();

    //Consulta genérica para traer las columnas de valor_num y valor_text
// ParametrosDaoImpl.java

    private static final String SQL_ANY = """
    SELECT valor_num
      FROM parametros_globales
     WHERE UPPER(clave) = UPPER(?)
    """;

    private Optional<BigDecimal> getNumeroCI(String clave) throws SQLException {
        try (var cn = ds.getConnection();
             var ps = cn.prepareStatement(SQL_ANY)) {
            ps.setString(1, clave);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getBigDecimal(1)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<BigDecimal> getDecimal(String clave) throws Exception {
        for (String k : new String[]{clave, "CUOTA_MENSUAL_CLUB", "cuota_club"}) {
            var val = getNumeroCI(k);
            if (val.isPresent()) return val;
        }
        return Optional.empty();
    }


    @Override
    public Optional<String> getTexto(String clave) throws Exception {
        //Devuelve el valor texto de la clave si es que existe y no es null y
        //como en el caso anterior si no devuelve Optional.empty().
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SQL_ANY)) {
            ps.setString(1, clave);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.ofNullable(rs.getString("valor_text"));
            }
        }
    }

    public Optional<BigDecimal> getNumero(String clave) {
        //Intenta leer valor numérico y frente a cualquier error de conexión
        //o de la base de datos devuelve Optional.empty().
        try (var cn = ds.getConnection();
             var ps = cn.prepareStatement("SELECT valor_num FROM parametros_globales WHERE clave=?")) {
            ps.setString(1, clave);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getBigDecimal(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

}
