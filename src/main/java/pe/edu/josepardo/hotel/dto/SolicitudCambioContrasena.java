package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SolicitudCambioContrasena(
        @NotBlank(message = "Ingresa tu contraseña actual.") String actual,
        @NotBlank(message = "Ingresa una contraseña.") @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$", message = "La contraseña debe tener entre 8 y 72 caracteres, con letras y números.") String nueva,
        @NotBlank(message = "Confirma tu contraseña.") String confirmacion) {
}
