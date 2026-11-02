package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudCategoria(
        @NotBlank(message = "Ingresa el nombre de la categoría.") @Size(max = 80, message = "El nombre no puede superar 80 caracteres.") String nombre,
        @Size(max = 300, message = "La descripción no puede superar 300 caracteres.") String descripcion) {
}
