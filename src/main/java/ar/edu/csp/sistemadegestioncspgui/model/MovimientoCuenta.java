package ar.edu.csp.sistemadegestioncspgui.model;

import java.math.BigDecimal;
import java.time.LocalDate;

// La clase MovimientoCuenta representa la cuenta corriente de un socio
public class MovimientoCuenta {
    private Long id; // Primary Key del movimiento de la cuenta, autogenerada por la base de datos
    private Long cuentaId; // Foreign Key del id de la cuenta asociada al movimiento.
    private LocalDate fecha;
    private String tipo;
    private String descripcion;
    private BigDecimal importe;   // Positivo para pagos y negativos para cargos
    private String referenciaExt; //
    private Long inscripcionId;   // traza de cargos ligado sa una inscripción.

    //Constructor vacío para su empleo en frameworks, mapeos y JavaFX
    public MovimientoCuenta() {
    }

    //Constructor para instanciar objetos de tipo MovimientoCuenta
    public MovimientoCuenta(Long cuentaId, LocalDate fecha, String tipo, String descripcion, BigDecimal importe, String referenciaExt, Long inscripcionId) {
        this.cuentaId = cuentaId;
        this.fecha = fecha;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.importe = importe;
        this.referenciaExt = referenciaExt;
        this.inscripcionId = inscripcionId;
    }

    // ====== GETTERS Y SETTERS ====== //
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public String getReferenciaExt() {
        return referenciaExt;
    }

    public void setReferenciaExt(String referenciaExt) {
        this.referenciaExt = referenciaExt;
    }

    public Long getInscripcionId() {
        return inscripcionId;
    }

    public void setInscripcionId(Long inscripcionId) {
        this.inscripcionId = inscripcionId;
    }
}
