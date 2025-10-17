// Actividad.java
package ar.edu.csp.sistemadegestioncspgui.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Actividad {
    private Long id;
    private String nombre;
    private String descripcion;
    private EstadoActividad estado;
    private BigDecimal precioDefault;
    private LocalDateTime creadoEn, actualizadoEn;

    public Actividad() {
    }

    public Actividad(String nombre, String descripcion, EstadoActividad estado, BigDecimal precioDefault) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
        this.precioDefault = precioDefault;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoActividad getEstado() {
        return estado;
    }

    public void setEstado(EstadoActividad estado) {
        this.estado = estado;
    }

    public BigDecimal getPrecioDefault() {
        return precioDefault;
    }

    public void setPrecioDefault(BigDecimal precioDefault) {
        this.precioDefault = precioDefault;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(LocalDateTime actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }

    @Override public String toString() { return nombre; } // para ComboBox
}
