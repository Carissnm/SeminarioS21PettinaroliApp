package ar.edu.csp.sistemadegestioncspgui.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MovimientoCuenta {
    private Long id;
    private Long cuentaId;
    private LocalDate fecha;
    private String tipo;          // "PAGO" / "CARGO" / etc
    private String descripcion;   // antes 'concepto'
    private BigDecimal importe;   // + pago / - cargo
    private String referenciaExt; // opcional
    private Long inscripcionId;   // link al cargo por actividad

    public MovimientoCuenta() {
    }

    public MovimientoCuenta(Long cuentaId, LocalDate fecha, String tipo, String descripcion, BigDecimal importe, String referenciaExt, Long inscripcionId) {
        this.cuentaId = cuentaId;
        this.fecha = fecha;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.importe = importe;
        this.referenciaExt = referenciaExt;
        this.inscripcionId = inscripcionId;
    }

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
