package ar.edu.csp.sistemadegestioncspgui.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Inscripcion {
    private Long id;
    private Long socioId;
    private Long actividadId;
    private BigDecimal precioAlta;
    private EstadoInscripcion estado;
    private LocalDate fechaAlta, fechaBaja;

    // Campos “de vista” para UI
    private String actividadNombre;
    private BigDecimal cuotaMensual; // tomamos de actividad.precio_default

    public Inscripcion() {
    }

    public Inscripcion(Long socioId, Long actividadId, EstadoInscripcion estado, LocalDate fechaAlta, LocalDate fechaBaja, String actividadNombre, BigDecimal cuotaMensual) {
        this.socioId = socioId;
        this.actividadId = actividadId;
        this.estado = estado;
        this.fechaAlta = fechaAlta;
        this.fechaBaja = fechaBaja;
        this.actividadNombre = actividadNombre;
        this.cuotaMensual = cuotaMensual;
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

    public Long getActividadId() {
        return actividadId;
    }

    public void setActividadId(Long actividadId) {
        this.actividadId = actividadId;
    }

    public BigDecimal getPrecioAlta() {
        return precioAlta;
    }

    public void setPrecioAlta(BigDecimal precioAlta) {
        this.precioAlta = precioAlta;
    }

    public EstadoInscripcion getEstado() {
        return estado;
    }

    public void setEstado(EstadoInscripcion estado) {
        this.estado = estado;
    }

    public LocalDate getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(LocalDate fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    public LocalDate getFechaBaja() {
        return fechaBaja;
    }

    public void setFechaBaja(LocalDate fechaBaja) {
        this.fechaBaja = fechaBaja;
    }

    public String getActividadNombre() {
        return actividadNombre;
    }

    public void setActividadNombre(String actividadNombre) {
        this.actividadNombre = actividadNombre;
    }

    public BigDecimal getCuotaMensual() {
        return cuotaMensual;
    }

    public void setCuotaMensual(BigDecimal cuotaMensual) {
        this.cuotaMensual = cuotaMensual;
    }
}
