package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SolicitudItemCarrito(
        @NotNull(message = "Selecciona una habitación.") Long habitacionId,
        @NotNull(message = "Indica la cantidad de huéspedes.") @Min(value = 1, message = "Debe haber al menos un huésped.") @Max(value = 10, message = "La cantidad de huéspedes no es válida.") Integer huespedes,
        LocalDate fechaEntrada,
        LocalDate fechaSalida) {
}
