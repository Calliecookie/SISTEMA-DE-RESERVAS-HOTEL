package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudMotivo(
        @NotBlank(message = "Indica el motivo.") @Size(max = 300, message = "El motivo no puede superar 300 caracteres.") String motivo) {
}
