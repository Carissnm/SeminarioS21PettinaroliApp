package ar.edu.csp.sistemadegestioncspgui.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.Properties;

public final class DataSourceFactory {

    private static HikariDataSource ds;
    private DataSourceFactory() {}

    public static synchronized DataSource get() {
        if (ds == null) {
            Properties p = load("db.properties"); // src/main/resources
            HikariConfig c = new HikariConfig();

            c.setJdbcUrl(p.getProperty("db.url"));
            c.setUsername(p.getProperty("db.user"));
            c.setPassword(p.getProperty("db.password"));

            // Pool sizing
            c.setMaximumPoolSize(Integer.parseInt(p.getProperty("db.pool.maxSize", "5")));
            c.setMinimumIdle(Integer.parseInt(p.getProperty("db.pool.minIdle", "1")));

            // Timeouts / health
            c.setConnectionTimeout(Long.parseLong(p.getProperty("db.pool.connectionTimeoutMs", "10000"))); // esperar por una conexión
            c.setValidationTimeout(Long.parseLong(p.getProperty("db.pool.validationTimeoutMs", "3000")));   // validar conexión
            c.setIdleTimeout(Long.parseLong(p.getProperty("db.pool.idleTimeoutMs", "120000")));            // cerrar ociosas
            c.setMaxLifetime(Long.parseLong(p.getProperty("db.pool.maxLifetimeMs", "1500000")));           // reciclar antes de wait_timeout
            c.setKeepaliveTime(Long.parseLong(p.getProperty("db.pool.keepaliveMs", "60000")));             // evita que se “duerman”

            // Validación (opción A: isValid de JDBC4; opción B: query)
            if (Boolean.parseBoolean(p.getProperty("db.pool.useTestQuery", "true"))) {
                c.setConnectionTestQuery(p.getProperty("db.pool.testQuery", "SELECT 1"));
            }

            // Driver explícito (opcional, MySQL 8)
            c.setDriverClassName(p.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));

            ds = new HikariDataSource(c);
        }
        return ds;
    }

    private static Properties load(String file) {
        try (InputStream in = DataSourceFactory.class.getClassLoader().getResourceAsStream(file)) {
            if (in == null) throw new IllegalStateException("No se encontró " + file);
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (Exception e) {
            throw new RuntimeException("Error cargando " + file, e);
        }
    }
}
