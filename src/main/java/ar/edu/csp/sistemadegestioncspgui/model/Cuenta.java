package ar.edu.csp.sistemadegestioncspgui.model;

public class Cuenta {
    private Long id;
    private Long socioId;

    public Cuenta(Long id, Long socioId) {
        this.id = id;
        this.socioId = socioId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSocioId() {
        return socioId;
    }

    public void setSocioId(Long socioId) {
        this.socioId = socioId;
    }

    @Override
    public String toString() {
        return "Cuenta{" +
                "socioId=" + socioId +
                '}';
    }
}
