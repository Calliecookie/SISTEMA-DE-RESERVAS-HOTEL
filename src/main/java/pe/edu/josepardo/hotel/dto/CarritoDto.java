package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CarritoDto(
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        int noches,
        List<ItemCarritoDto> items,
        int cantidadItems,
        int totalHuespedes,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal porcentajeDescuento,
        BigDecimal impuesto,
        BigDecimal porcentajeImpuesto,
        BigDecimal total,
        boolean listo,
        List<String> avisos) {
}
