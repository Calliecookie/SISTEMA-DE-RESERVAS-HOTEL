package pe.edu.josepardo.hotel.excepcion;

import org.springframework.http.HttpStatus;

public class ExcepcionNegocio extends RuntimeException {

    private final HttpStatus estado;
    private final String codigo;

    public ExcepcionNegocio(HttpStatus estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public static ExcepcionNegocio solicitudInvalida(String mensaje) {
        return new ExcepcionNegocio(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", mensaje);
    }

    public static ExcepcionNegocio noEncontrado(String mensaje) {
        return new ExcepcionNegocio(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", mensaje);
    }

    public static ExcepcionNegocio conflicto(String codigo, String mensaje) {
        return new ExcepcionNegocio(HttpStatus.CONFLICT, codigo, mensaje);
    }

    public static ExcepcionNegocio prohibido(String mensaje) {
        return new ExcepcionNegocio(HttpStatus.FORBIDDEN, "PROHIBIDO", mensaje);
    }

    public static ExcepcionNegocio demasiadasSolicitudes() {
        return new ExcepcionNegocio(
                HttpStatus.TOO_MANY_REQUESTS,
                "DEMASIADAS_SOLICITUDES",
                "Realizaste demasiados intentos. Espera unos minutos y vuelve a intentarlo.");
    }

    public static ExcepcionNegocio servicioNoDisponible(String mensaje) {
        return new ExcepcionNegocio(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE", mensaje);
    }

    public String getMensajeSeguro() {
        return getMessage();
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public String getCodigo() {
        return codigo;
    }
}
