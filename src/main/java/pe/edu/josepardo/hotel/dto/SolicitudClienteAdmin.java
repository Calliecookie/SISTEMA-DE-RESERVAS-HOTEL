package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudClienteAdmin(
        @NotBlank(message = "Ingresa los nombres.") @Size(max = 100, message = "Los nombres son demasiado largos.") String nombres,
        @NotBlank(message = "Ingresa los apellidos.") @Size(max = 120, message = "Los apellidos son demasiado largos.") String apellidos,
        @Pattern(regexp = "^$|^9\\d{8}$", message = "El celular debe tener 9 dígitos y empezar con 9.") String telefono,
        @Size(max = 200, message = "La dirección es demasiado larga.") String direccion) {
}
