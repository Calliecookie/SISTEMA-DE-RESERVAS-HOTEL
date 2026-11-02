package pe.edu.josepardo.hotel.dto;

import java.time.LocalDateTime;

public record AuditoriaDto(
        Long id,
        String usuario,
        String accion,
        String entidad,
        String entidadId,
        String detalle,
        String direccionIp,
        LocalDateTime creadoEn) {
}
