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
            Properties p = load("db.properties"); // se lee desde src/main/resources
            HikariConfig c = new HikariConfig();
            c.setJdbcUrl(p.getProperty("db.url"));
            c.setUsername(p.getProperty("db.user"));
            c.setPassword(p.getProperty("db.password"));
            c.setMaximumPoolSize(Integer.parseInt(p.getProperty("db.pool.maxSize","10")));
            c.setMinimumIdle(Integer.parseInt(p.getProperty("db.pool.minIdle","2")));
            c.setConnectionTimeout(Long.parseLong(p.getProperty("db.pool.timeoutMs","30000")));
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
