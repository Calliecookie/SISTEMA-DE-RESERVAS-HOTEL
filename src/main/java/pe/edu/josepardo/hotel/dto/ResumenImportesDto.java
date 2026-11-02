package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;

public record ResumenImportesDto(
        int noches,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal porcentajeDescuento,
        BigDecimal impuesto,
        BigDecimal porcentajeImpuesto,
        BigDecimal total) {
}
