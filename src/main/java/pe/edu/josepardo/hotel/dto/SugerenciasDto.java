package pe.edu.josepardo.hotel.dto;

import java.time.LocalDate;
import java.util.List;

public record SugerenciasDto(
        boolean disponible,
        int noches,
        LocalDate proximaEntrada,
        LocalDate proximaSalida,
        List<AlternativaDto> alternativas) {
}
