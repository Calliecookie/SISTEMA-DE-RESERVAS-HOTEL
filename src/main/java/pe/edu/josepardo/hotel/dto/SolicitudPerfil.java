package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudPerfil(
        @Pattern(regexp = "^$|^9\\d{8}$", message = "El celular debe tener 9 dígitos y empezar con 9.") String telefono,
        @Size(max = 200, message = "La dirección es demasiado larga.") String direccion) {
}
