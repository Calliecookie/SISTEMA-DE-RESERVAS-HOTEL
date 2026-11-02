package pe.edu.josepardo.hotel.dto;

public record EstadoIntegracionesDto(
        boolean paypalCredenciales,
        String paypalEntorno,
        String paypalMoneda,
        boolean paypalTipoCambio,
        boolean yapeConfigurado,
        boolean factilizaConfigurada,
        boolean correoHabilitado) {
}
