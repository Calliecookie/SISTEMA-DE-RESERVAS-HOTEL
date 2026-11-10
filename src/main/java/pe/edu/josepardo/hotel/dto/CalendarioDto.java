package pe.edu.josepardo.hotel.dto;

import java.time.LocalDate;
import java.util.List;

public record CalendarioDto(
        Long habitacionId,
        LocalDate desde,
        boolean enServicio,
        int diasDisponibles,
        List<DiaCalendarioDto> dias) {
}
