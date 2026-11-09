package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudCambioEstado(
        @NotBlank(message = "Selecciona el nuevo estado.") String estado,
        @Size(max = 300, message = "El motivo no puede superar 300 caracteres.") String motivo,
        String metodo,
        @Size(max = 40, message = "El número de operación es demasiado largo.") String numeroOperacion) {
}
