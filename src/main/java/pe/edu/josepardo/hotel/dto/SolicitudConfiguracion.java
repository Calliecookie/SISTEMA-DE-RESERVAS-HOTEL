package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record SolicitudConfiguracion(
        @NotNull(message = "Envía los valores.") Map<String, String> valores) {
}
