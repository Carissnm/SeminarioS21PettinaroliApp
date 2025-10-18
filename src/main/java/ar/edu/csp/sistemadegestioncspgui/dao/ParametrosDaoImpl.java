package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class ParametrosDaoImpl implements ParametrosDao {
    private final DataSource ds = DataSourceFactory.get();

    private static final String SQL = """
        SELECT valor_num, valor_text
          FROM parametros_globales
         WHERE clave = ?
        """;

    @Override
    public Optional<BigDecimal> getDecimal(String clave) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SQL)) {
            ps.setString(1, clave);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                var num = rs.getBigDecimal("valor_num");
                return Optional.ofNullable(num);
            }
        }
    }

    @Override
    public Optional<String> getTexto(String clave) throws Exception {
        try (var cn = ds.getConnection(); var ps = cn.prepareStatement(SQL)) {
            ps.setString(1, clave);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.ofNullable(rs.getString("valor_text"));
            }
        }
    }
}
