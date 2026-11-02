package pe.edu.josepardo.hotel.dto;

public record CategoriaDto(
        Long id,
        String nombre,
        String descripcion,
        boolean activo,
        long cantidadHabitaciones) {
}
