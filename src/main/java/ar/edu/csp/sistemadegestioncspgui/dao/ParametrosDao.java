package ar.edu.csp.sistemadegestioncspgui.dao;

import java.math.BigDecimal;
import java.util.Optional;

public interface ParametrosDao {
    Optional<BigDecimal> getDecimal(String clave) throws Exception;
    Optional<String>     getTexto(String clave)   throws Exception;
}
