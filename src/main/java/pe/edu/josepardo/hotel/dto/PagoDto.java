package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoDto(
        Long id,
        String metodo,
        String metodoEtiqueta,
        BigDecimal monto,
        String numeroOperacion,
        String monedaExterna,
        java.math.BigDecimal montoExterno,
        String estado,
        String estadoEtiqueta,
        LocalDateTime fechaPago,
        LocalDateTime creadoEn,
        String motivoRechazo,
        String confirmadoPor) {
}
