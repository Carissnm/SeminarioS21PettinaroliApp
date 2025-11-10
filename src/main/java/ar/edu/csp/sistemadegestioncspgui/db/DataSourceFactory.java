package ar.edu.csp.sistemadegestioncspgui.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.Properties;

public final class DataSourceFactory {
    //DataSource del pool Hikari, único para toda la aplicación.
    private static HikariDataSource ds;
    private DataSourceFactory() {} // Clase utilitaria, no se instancia

    public static synchronized DataSource get() { // Punto de acceso único al DataSource
        //Inicializa una sola vez y reutiliza la misma instancia
        if (ds == null) {
            Properties p = load("db.properties"); // Lee la configuración desde resources
            HikariConfig c = new HikariConfig(); // se almacena un objeto de configuración del pool en la variable c

            // Conexión con la base de datos
            c.setJdbcUrl(p.getProperty("db.url")); // cadena JDBC
            c.setUsername(p.getProperty("db.user")); // usuario de la base de datos
            c.setPassword(p.getProperty("db.password")); // contraseña de la base de datos
            // Tamaño del pool
            // Máximo de conexiones simultáneas
            c.setMaximumPoolSize(Integer.parseInt(p.getProperty("db.pool.maxSize", "5")));
            // Mínimo de conexiones ociosas que se mantienen preparadas.
            c.setMinimumIdle(Integer.parseInt(p.getProperty("db.pool.minIdle", "1")));
            // Timeouts / health del pool
            // Tiempo máximo a esperar una conexión libre antes de lanzar excepción
            c.setConnectionTimeout(Long.parseLong(p.getProperty("db.pool.connectionTimeoutMs", "10000"))); // esperar por una conexión
            // Tiempo límite para validar una conexión (health check)
            c.setValidationTimeout(Long.parseLong(p.getProperty("db.pool.validationTimeoutMs", "3000")));   // validar conexión
            // Cierra conexiones que llevan ociosas este tiempo (libera recursos)
            c.setIdleTimeout(Long.parseLong(p.getProperty("db.pool.idleTimeoutMs", "120000")));            // cerrar ociosas
            // Vida máxima de una conexión en el pool (se recicla antes de wait_timeout del server)
            c.setMaxLifetime(Long.parseLong(p.getProperty("db.pool.maxLifetimeMs", "1500000")));           // reciclar antes de wait_timeout
            // Keepalive para despertar conexiones inactivas y evitar cierre por el servidor
            c.setKeepaliveTime(Long.parseLong(p.getProperty("db.pool.keepaliveMs", "60000")));             // evita que se “duerman”
            // Validación a través de una query para testear conexiones.
            if (Boolean.parseBoolean(p.getProperty("db.pool.useTestQuery", "true"))) {
                c.setConnectionTestQuery(p.getProperty("db.pool.testQuery", "SELECT 1"));
            }
            // Declaración del driver JDBC
            c.setDriverClassName(p.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
            // Se crea el DataSource del pool con la configuración previa.
            ds = new HikariDataSource(c);
        }
        return ds; // Siempre devuelve la misma instancia compartida
    }
    // Este metodo carga un archivo .properties del classpath y lo retorna como tipo Properties
    private static Properties load(String file) {
        try (InputStream in = DataSourceFactory.class.getClassLoader().getResourceAsStream(file)) {
            if (in == null) throw new IllegalStateException("No se encontró " + file);
            Properties p = new Properties();
            p.load(in); // parseo de pares clave=valor
            return p;
        } catch (Exception e) {
            //Si se da una falla en la carga de la configuración se aborta con un RuntimeException.
            throw new RuntimeException("Error cargando " + file, e);
        }
    }
}
