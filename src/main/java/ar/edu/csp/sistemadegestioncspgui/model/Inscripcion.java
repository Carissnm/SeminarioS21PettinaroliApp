package ar.edu.csp.sistemadegestioncspgui.model;

import java.math.BigDecimal;
import java.time.LocalDate;
// Modelo que representa la inscripción de un socio a una actividad.
// Representa el estado y los datos que usan la Interfaz de Usuario y los servicios.
public class Inscripcion {
    private Long id; // Primary Key autogenerada por la Base de Datos.
    private Long socioId; // Foreign Key al socio que se inscribe.
    private Long actividadId; // Foreign Key a la actividad elegida por el socio para la inscripción.
    private BigDecimal precioAlta; // precio cobrado al dar de alta la inscripción
    private EstadoInscripcion estado; // estado lógico de la inscripción
    private LocalDate fechaAlta, fechaBaja;
    private String actividadNombre;
    private BigDecimal cuotaMensual;


    //Constructor vacío para ser utilizado por frameworks, mapeos y JavaFX
    public Inscripcion() {
    }

    // Constructor para instanciar objetos para la Interfaz de Usuarios
    public Inscripcion(Long socioId, Long actividadId, EstadoInscripcion estado, LocalDate fechaAlta, LocalDate fechaBaja, String actividadNombre, BigDecimal cuotaMensual) {
        this.socioId = socioId;
        this.actividadId = actividadId;
        this.estado = estado;
        this.fechaAlta = fechaAlta;
        this.fechaBaja = fechaBaja;
        this.actividadNombre = actividadNombre;
        this.cuotaMensual = cuotaMensual;
    }

    // ====== GETTERS Y SETTERS ====== //
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

    // Representación del objeto como texto para la Interfaz de Usuario
    @Override
    public String toString() {
        return "Inscripcion{" +
                "precioAlta=" + precioAlta +
                ", fechaAlta=" + fechaAlta +
                ", actividadNombre='" + actividadNombre + '\'' +
                ", cuotaMensual=" + cuotaMensual +
                '}';
    }
}
