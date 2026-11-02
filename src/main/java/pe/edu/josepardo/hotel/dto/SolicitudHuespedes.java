package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SolicitudHuespedes(
        @NotNull(message = "Indica la cantidad de huéspedes.") @Min(value = 1, message = "Debe haber al menos un huésped.") @Max(value = 10, message = "La cantidad de huéspedes no es válida.") Integer huespedes) {
}
