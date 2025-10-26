// Actividad.java
package ar.edu.csp.sistemadegestioncspgui.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
// Modelo que representa una actividad dentro del club. Se utiliza tanto para la UI como en servicios y DAO's
public class Actividad {
    private Long id; // primary key autogenerada en la base de datos
    private String nombre;
    private String descripcion;
    private EstadoActividad estado; // Estado lógico de la actividad
    private BigDecimal precioDefault; // arancel base de la actividad
    private LocalDateTime creadoEn, actualizadoEn; // marcas de tiempo para indicar cuándo se crea un registro y la última vez que fue modificado.

    //Constructor vacío para frameworks, mapeos y JavaFX
    public Actividad() {
    }

    // Constructor para crear instancias en memoria previo a la persistencia
    public Actividad(String nombre, String descripcion, EstadoActividad estado, BigDecimal precioDefault) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
        this.precioDefault = precioDefault;
    }

    // ====== GETTERS Y SETTERS ====== //

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

    // Representación de texto para la UI
    @Override public String toString() { return nombre; }
}
