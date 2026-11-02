package pe.edu.josepardo.hotel.dto;

import java.time.LocalDateTime;

public record UsuarioDto(
        Long id,
        String nombres,
        String apellidos,
        String correo,
        String rol,
        String rolEtiqueta,
        boolean activo,
        LocalDateTime ultimoAcceso,
        LocalDateTime creadoEn) {
}
