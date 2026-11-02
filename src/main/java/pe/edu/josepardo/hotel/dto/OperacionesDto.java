package pe.edu.josepardo.hotel.dto;

import java.util.List;

public record OperacionesDto(
        List<ReservaResumenDto> llegadasHoy,
        List<ReservaResumenDto> salidasHoy,
        List<ReservaResumenDto> pagosPorVerificar,
        List<ReservaResumenDto> porConfirmar,
        long huespedesAlojados,
        long habitacionesOcupadas) {
}
