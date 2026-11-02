package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudUsuario(
        @NotBlank(message = "Ingresa los nombres.") @Size(max = 100, message = "Los nombres son demasiado largos.") String nombres,
        @NotBlank(message = "Ingresa los apellidos.") @Size(max = 120, message = "Los apellidos son demasiado largos.") String apellidos,
        @NotBlank(message = "Ingresa el correo electrónico.") @Email(message = "Ingresa un correo válido.") @Size(max = 150, message = "El correo es demasiado largo.") String correo,
        @NotBlank(message = "Selecciona el rol.") String rol,
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$", message = "La contraseña debe tener entre 8 y 72 caracteres, con letras y números.") String contrasena) {
}
