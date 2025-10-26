package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdministradorDao {

    // Consulta mínima para el login, busca por mail y trae el id, el hash de la contraseña y el estado del administrador.
    // La verificación de la contraseña se realiza con BCrypt en Java.
    private static final String SQL =
            "SELECT id, password_hash, estado FROM administrador WHERE email = ?";

    // El metodo retorna el id del admin si tanto el mail como el password del administrador
    // son válidos y posee estado Activo. De lo contrario devuelve null.
    public Long validar(String email, String passwordPlano) throws Exception {
        try (Connection cn = DataSourceFactory.get().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL)) {
            ps.setString(1, email);
            // Se ejecuta la consulta y se procesa el resultado, si no existe fila
            // el mail no existe.
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                // Se recuperan hash y estado desde la base de datos.
                String hash = rs.getString("password_hash");
                String estado = rs.getString("estado");

                //Si el administrador no figura como activo no se permite el login.
                if (!"ACTIVO".equalsIgnoreCase(estado)) return null;

                //Se verifica la contraseña comparando el password plano vs el hash almacenado.
                boolean ok = BCrypt.checkpw(passwordPlano, hash);
                return ok ? rs.getLong("id") : null; // Si coinciden devuelve el id del administrador y si no null y no se permite el login.
            }
        }
    }

    // Metodo empleado para generar hashes BCrypt (en este caso se utiliza para poblar la base de datos)
    public static String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }
}
