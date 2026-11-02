package pe.edu.josepardo.hotel.dto;

public record DisponibilidadDto(
        boolean disponible,
        int noches,
        String mensaje,
        ResumenImportesDto importes) {
}
