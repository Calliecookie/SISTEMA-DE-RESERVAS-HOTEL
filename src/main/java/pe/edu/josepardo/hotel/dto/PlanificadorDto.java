package pe.edu.josepardo.hotel.dto;

import java.time.LocalDate;
import java.util.List;

public record PlanificadorDto(
        LocalDate desde,
        List<LocalDate> dias,
        List<FilaPlanificadorDto> habitaciones,
        List<OcupacionDiaDto> ocupacion) {
}
