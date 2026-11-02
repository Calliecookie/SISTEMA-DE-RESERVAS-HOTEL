package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SolicitudConsultaDni(
        @NotBlank(message = "Ingresa tu DNI.") @Pattern(regexp = "^\\d{8}$", message = "El DNI debe tener exactamente 8 dígitos.") String dni) {
}
