package pe.edu.josepardo.hotel.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "clientes")
public class Cliente extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 8)
    private String dni;

    @Column(length = 15)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    @Column(name = "dni_verificado", nullable = false)
    private boolean dniVerificado;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_datos", nullable = false, length = 15)
    private OrigenDatosCliente origenDatos = OrigenDatosCliente.MANUAL;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public boolean isDniVerificado() {
        return dniVerificado;
    }

    public void setDniVerificado(boolean dniVerificado) {
        this.dniVerificado = dniVerificado;
    }

    public OrigenDatosCliente getOrigenDatos() {
        return origenDatos;
    }

    public void setOrigenDatos(OrigenDatosCliente origenDatos) {
        this.origenDatos = origenDatos;
    }
}
