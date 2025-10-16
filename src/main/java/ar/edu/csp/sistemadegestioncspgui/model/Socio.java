package ar.edu.csp.sistemadegestioncspgui.model;

import java.time.LocalDate;
import java.util.Objects;

public class Socio {
    private Long id;
    private String dni;
    private String nombre;
    private String apellido;
    private LocalDate fechaNac;
    private String domicilio;
    private String email;
    private String telefono;
    private EstadoSocio estado = EstadoSocio.ACTIVO; // default seguro
    private LocalDate fechaAlta;
    private LocalDate fechaBaja;

    // --- Constructores ---
    public Socio() {}

    /** Para crear un socio nuevo (sin id todavía). */
    public Socio(String dni, String nombre, String apellido,
                 LocalDate fechaNac, String domicilio, String email,
                 String telefono, EstadoSocio estado, LocalDate fechaAlta, LocalDate fechaBaja) {
        this.dni = dni;
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaNac = fechaNac;
        this.domicilio = domicilio;
        this.email = email;
        this.telefono = telefono;
        this.estado = (estado != null ? estado : EstadoSocio.ACTIVO);
        this.fechaAlta = fechaAlta;
        this.fechaBaja = fechaBaja;
    }

    /** Constructor completo (incluye id). */
    public Socio(Long id, String dni, String nombre, String apellido,
                 LocalDate fechaNac, String domicilio, String email,
                 String telefono, EstadoSocio estado, LocalDate fechaAlta, LocalDate fechaBaja) {
        this(dni, nombre, apellido, fechaNac, domicilio, email, telefono, estado, fechaAlta, fechaBaja);
        this.id = id;
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public LocalDate getFechaNac() { return fechaNac; }
    public void setFechaNac(LocalDate fechaNac) { this.fechaNac = fechaNac; }

    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public EstadoSocio getEstado() { return estado; }
    public void setEstado(EstadoSocio estado) { this.estado = (estado != null ? estado : EstadoSocio.ACTIVO); }

    public LocalDate getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDate fechaAlta) { this.fechaAlta = fechaAlta; }

    public LocalDate getFechaBaja() { return fechaBaja; }
    public void setFechaBaja(LocalDate fechaBaja) { this.fechaBaja = fechaBaja; }

    // --- Helpers de compatibilidad/UX ---
    public boolean isActivo() { return estado == EstadoSocio.ACTIVO; }
    public void setActivo(boolean activo) { this.estado = activo ? EstadoSocio.ACTIVO : EstadoSocio.INACTIVO; }
    public String getNombreCompleto() { return (apellido == null ? "" : apellido) + ", " + (nombre == null ? "" : nombre); }

    @Override
    public String toString() {
        return String.format("Socio{id=%s, dni=%s, nombre=%s, apellido=%s, estado=%s}",
                id, dni, nombre, apellido, estado);
    }

    // Opcional: identidad por id si existe; si no, por dni.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Socio)) return false;
        Socio socio = (Socio) o;
        if (id != null && socio.id != null) return Objects.equals(id, socio.id);
        return Objects.equals(dni, socio.dni);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : Objects.hash(dni);
    }
}
