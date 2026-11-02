package pe.edu.josepardo.hotel.dto;

import java.time.LocalDateTime;

public record ComprobanteDto(
        String numero,
        String estadoPago,
        LocalDateTime fechaEmision,
        String correoDestino,
        String correoEstado,
        String correoEstadoEtiqueta,
        String correoDetalle,
        LocalDateTime correoFecha,
        String url) {
}
