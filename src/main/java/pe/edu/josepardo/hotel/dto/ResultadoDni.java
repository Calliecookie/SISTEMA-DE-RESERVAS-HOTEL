package pe.edu.josepardo.hotel.dto;

public record ResultadoDni(
        String dni,
        String nombres,
        String apellidos,
        boolean verificado,
        boolean permiteManual,
        String mensaje) {
}
