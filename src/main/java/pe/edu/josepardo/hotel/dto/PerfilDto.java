package pe.edu.josepardo.hotel.dto;

public record PerfilDto(
        String nombres,
        String apellidos,
        String correo,
        String dni,
        String telefono,
        String direccion,
        boolean dniVerificado) {
}
