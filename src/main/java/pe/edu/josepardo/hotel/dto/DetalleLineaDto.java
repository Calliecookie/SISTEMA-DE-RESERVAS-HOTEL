package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;

public record DetalleLineaDto(
        String numero,
        String nombre,
        String categoria,
        String imagen,
        int huespedes,
        BigDecimal precioNoche,
        int noches,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal impuesto,
        BigDecimal total) {
}
