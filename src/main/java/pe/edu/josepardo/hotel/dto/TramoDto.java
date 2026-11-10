package pe.edu.josepardo.hotel.dto;

import java.time.LocalDate;

public record TramoDto(
        String codigo,
        String cliente,
        String estado,
        String estadoEtiqueta,
        LocalDate entrada,
        LocalDate salida,
        int inicio,
        int noches,
        boolean continuaAntes,
        boolean continuaDespues) {
}
