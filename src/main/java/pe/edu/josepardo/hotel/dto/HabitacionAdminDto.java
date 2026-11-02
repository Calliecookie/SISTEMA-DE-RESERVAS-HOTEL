package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record HabitacionAdminDto(
        Long id,
        String numero,
        String nombre,
        Long categoriaId,
        String categoria,
        String descripcion,
        int capacidad,
        BigDecimal precioNoche,
        String estadoOperativo,
        String estadoEtiqueta,
        String imagen,
        boolean activo,
        List<Long> caracteristicaIds,
        List<CaracteristicaDto> caracteristicas,
        long reservasAsociadas,
        LocalDateTime creadoEn) {
}
