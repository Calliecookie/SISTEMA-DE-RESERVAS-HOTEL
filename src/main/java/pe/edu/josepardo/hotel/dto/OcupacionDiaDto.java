package pe.edu.josepardo.hotel.dto;

import java.time.LocalDate;

public record OcupacionDiaDto(LocalDate fecha, int ocupadas, int porcentaje) {
}
