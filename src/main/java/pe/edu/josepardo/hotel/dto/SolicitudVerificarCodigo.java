package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudVerificarCodigo(
        @NotBlank(message = "Ingresa tu correo electrónico.") @Email(message = "Ingresa un correo válido.") @Size(max = 150, message = "El correo es demasiado largo.") String correo,
        @NotBlank(message = "Ingresa el código.") @Pattern(regexp = "^\\d{6}$", message = "El código tiene 6 dígitos.") String codigo) {
}
