package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudRegistro(
        @NotBlank(message = "Ingresa tu DNI.") @Pattern(regexp = "^\\d{8}$", message = "El DNI debe tener exactamente 8 dígitos.") String dni,
        @NotBlank(message = "Ingresa tu correo electrónico.") @Email(message = "Ingresa un correo válido.") @Size(max = 150, message = "El correo es demasiado largo.") String correo,
        @NotBlank(message = "Ingresa una contraseña.") @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$", message = "La contraseña debe tener entre 8 y 72 caracteres, con letras y números.") String contrasena,
        @NotBlank(message = "Confirma tu contraseña.") String confirmacion,
        @Size(max = 100, message = "Los nombres son demasiado largos.") String nombres,
        @Size(max = 120, message = "Los apellidos son demasiado largos.") String apellidos) {
}
