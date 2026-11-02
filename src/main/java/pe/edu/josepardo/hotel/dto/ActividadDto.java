package pe.edu.josepardo.hotel.dto;

import java.time.LocalDateTime;

public record ActividadDto(
        String accion,
        String entidad,
        String detalle,
        String usuario,
        LocalDateTime creadoEn) {
}
