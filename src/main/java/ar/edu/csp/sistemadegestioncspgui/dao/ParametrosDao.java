package ar.edu.csp.sistemadegestioncspgui.dao;

import java.math.BigDecimal;
import java.util.Optional;

// Interfaz para leer los parámetros globales de la base de datos

public interface ParametrosDao {
    Optional<BigDecimal> getDecimal(String clave) throws Exception;
    Optional<String>     getTexto(String clave)   throws Exception;
}
