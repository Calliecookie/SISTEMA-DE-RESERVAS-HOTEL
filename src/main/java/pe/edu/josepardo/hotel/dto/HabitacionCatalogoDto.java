package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;
import java.util.List;

public record HabitacionCatalogoDto(
        Long id,
        String numero,
        String nombre,
        Long categoriaId,
        String categoria,
        String descripcion,
        int capacidad,
        BigDecimal precioNoche,
        String imagen,
        String estado,
        String estadoEtiqueta,
        boolean disponible,
        List<CaracteristicaDto> caracteristicas) {
}
