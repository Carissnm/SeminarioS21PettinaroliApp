package ar.edu.csp.sistemadegestioncspgui.dao;

import ar.edu.csp.sistemadegestioncspgui.db.DataSourceFactory;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdministradorDao {

    private static final String SQL =
            "SELECT id, password_hash, estado FROM administrador WHERE email = ?";

    /**
     * @return id del admin si (email + password) son válidos y estado = ACTIVO; si no, null.
     */
    public Long validar(String email, String passwordPlano) throws Exception {
        try (Connection cn = DataSourceFactory.get().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                String hash = rs.getString("password_hash");
                String estado = rs.getString("estado");
                if (!"ACTIVO".equalsIgnoreCase(estado)) return null;

                boolean ok = BCrypt.checkpw(passwordPlano, hash);
                return ok ? rs.getLong("id") : null;
            }
        }
    }

    // Utilidad opcional para generar hashes (para poblar la BD)
    public static String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }
}
