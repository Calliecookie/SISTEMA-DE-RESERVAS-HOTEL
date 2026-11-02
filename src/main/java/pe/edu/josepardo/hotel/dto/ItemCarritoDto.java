package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;

public record ItemCarritoDto(
        Long habitacionId,
        String numero,
        String nombre,
        String categoria,
        String imagen,
        int capacidad,
        int huespedes,
        BigDecimal precioNoche,
        BigDecimal subtotal,
        boolean disponible,
        String aviso) {
}
