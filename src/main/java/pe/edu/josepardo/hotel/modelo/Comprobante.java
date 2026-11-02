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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "comprobantes")
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @Column(nullable = false, length = 40)
    private String numero;

    @Column(name = "ruta_archivo", nullable = false, length = 255)
    private String rutaArchivo;

    @Column(name = "estado_pago", nullable = false, length = 30)
    private String estadoPago;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "correo_destino", nullable = false, length = 150)
    private String correoDestino;

    @Enumerated(EnumType.STRING)
    @Column(name = "correo_estado", nullable = false, length = 15)
    private EstadoEnvioCorreo correoEstado = EstadoEnvioCorreo.PENDIENTE;

    @Column(name = "correo_detalle", length = 300)
    private String correoDetalle;

    @Column(name = "correo_fecha")
    private LocalDateTime correoFecha;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getCorreoDestino() {
        return correoDestino;
    }

    public void setCorreoDestino(String correoDestino) {
        this.correoDestino = correoDestino;
    }

    public EstadoEnvioCorreo getCorreoEstado() {
        return correoEstado;
    }

    public void setCorreoEstado(EstadoEnvioCorreo correoEstado) {
        this.correoEstado = correoEstado;
    }

    public String getCorreoDetalle() {
        return correoDetalle;
    }

    public void setCorreoDetalle(String correoDetalle) {
        this.correoDetalle = correoDetalle;
    }

    public LocalDateTime getCorreoFecha() {
        return correoFecha;
    }

    public void setCorreoFecha(LocalDateTime correoFecha) {
        this.correoFecha = correoFecha;
    }
}
