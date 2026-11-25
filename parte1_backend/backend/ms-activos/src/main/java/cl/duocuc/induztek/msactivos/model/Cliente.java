package cl.duocuc.induztek.msactivos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String rut;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(nullable = false, length = 80)
    private String giro;

    @Column(name = "contacto_nombre", nullable = false, length = 120)
    private String contactoNombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(nullable = false, length = 200)
    private String direccion = "";

    @Column(name = "creado_por")
    private Long creadoPor;

    @Column(name = "creado_en", nullable = false)
    private Long creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Long actualizadoEn;

    @PrePersist
    void alCrear() {
        creadoEn = System.currentTimeMillis();
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getGiro() { return giro; }
    public void setGiro(String giro) { this.giro = giro; }
    public String getContactoNombre() { return contactoNombre; }
    public void setContactoNombre(String contactoNombre) { this.contactoNombre = contactoNombre; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public Long getCreadoPor() { return creadoPor; }
    public void setCreadoPor(Long creadoPor) { this.creadoPor = creadoPor; }
    public Long getCreadoEn() { return creadoEn; }
    public Long getActualizadoEn() { return actualizadoEn; }
}
