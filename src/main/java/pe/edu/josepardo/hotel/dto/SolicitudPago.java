package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudPago(
        @NotBlank(message = "Selecciona un método de pago.") String metodo,
        @Size(max = 40, message = "El número de operación es demasiado largo.") String numeroOperacion) {
}
