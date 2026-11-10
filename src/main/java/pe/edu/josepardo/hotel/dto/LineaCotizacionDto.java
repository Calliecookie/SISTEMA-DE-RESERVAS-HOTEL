package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;

public record LineaCotizacionDto(
        HabitacionCatalogoDto habitacion,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal impuesto,
        BigDecimal total) {
}
