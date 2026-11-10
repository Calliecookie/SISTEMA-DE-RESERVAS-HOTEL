package pe.edu.josepardo.hotel.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record MapaHabitacionesDto(
        LocalDate fecha,
        LocalDateTime actualizadoEn,
        List<PisoDto> pisos,
        Map<String, Long> resumen,
        int ocupacionPorcentaje) {
}
