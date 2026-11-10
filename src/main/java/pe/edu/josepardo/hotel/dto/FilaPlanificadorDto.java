package pe.edu.josepardo.hotel.dto;

import java.util.List;

public record FilaPlanificadorDto(
        Long id,
        String numero,
        String nombre,
        String categoria,
        String estado,
        List<TramoDto> tramos) {
}
