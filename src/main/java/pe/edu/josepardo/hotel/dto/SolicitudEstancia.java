package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SolicitudEstancia(
        @NotNull(message = "Selecciona la fecha de entrada.") LocalDate fechaEntrada,
        @NotNull(message = "Selecciona la fecha de salida.") LocalDate fechaSalida) {
}
