package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudRecuperacionAdmin(
        @NotBlank(message = "Ingresa el correo del administrador.") @Email(message = "Ingresa un correo válido.") @Size(max = 150, message = "El correo es demasiado largo.") String correo,
        @NotBlank(message = "Ingresa la clave de recuperación.") @Size(max = 200, message = "La clave es demasiado larga.") String clave,
        @NotBlank(message = "Ingresa una contraseña.") @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{10,72}$", message = "La contraseña del administrador debe tener entre 10 y 72 caracteres, con letras y números.") String contrasena,
        @NotBlank(message = "Confirma tu contraseña.") String confirmacion) {
}
