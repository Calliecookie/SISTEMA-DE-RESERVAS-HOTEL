package pe.edu.josepardo.hotel.dto;

import java.time.LocalDate;

public record DiaCalendarioDto(LocalDate fecha, boolean disponible) {
}
