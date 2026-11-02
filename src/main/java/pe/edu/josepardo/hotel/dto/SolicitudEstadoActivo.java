package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.NotNull;

public record SolicitudEstadoActivo(
        @NotNull(message = "Indica el estado.") Boolean activo) {
}
