package pe.edu.josepardo.hotel.dto;

import java.time.LocalDateTime;

public record HistorialDto(
        String estadoAnterior,
        String estadoNuevo,
        String estadoEtiqueta,
        String actor,
        String usuario,
        String motivo,
        LocalDateTime creadoEn) {
}
