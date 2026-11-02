package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservaResumenDto(
        String codigo,
        String cliente,
        String dni,
        String correo,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        int noches,
        int cantidadHuespedes,
        String habitaciones,
        BigDecimal total,
        String estado,
        String estadoEtiqueta,
        String metodoPago,
        String metodoPagoEtiqueta,
        LocalDateTime fechaPago,
        LocalDateTime creadoEn,
        LocalDateTime expiraEn) {
}
