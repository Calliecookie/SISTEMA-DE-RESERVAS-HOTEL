package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardDto(
        long totalHabitaciones,
        long habitacionesDisponibles,
        long habitacionesOcupadas,
        long reservasPendientes,
        long reservasConfirmadas,
        BigDecimal ingresosAcumulados,
        BigDecimal ingresosMes,
        List<PuntoSerie> ingresosPorMes,
        List<ReservaResumenDto> reservasRecientes,
        List<ActividadDto> actividadReciente) {
}
