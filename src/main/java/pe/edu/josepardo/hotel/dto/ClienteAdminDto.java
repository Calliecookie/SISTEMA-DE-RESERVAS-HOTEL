package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteAdminDto(
        Long id,
        Long usuarioId,
        String nombres,
        String apellidos,
        String correo,
        String dni,
        String telefono,
        String direccion,
        boolean dniVerificado,
        String origenDatos,
        boolean activo,
        long cantidadReservas,
        BigDecimal totalPagado,
        LocalDateTime creadoEn) {
}
