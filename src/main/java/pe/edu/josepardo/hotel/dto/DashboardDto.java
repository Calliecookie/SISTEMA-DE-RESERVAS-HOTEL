package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
        List<ActividadDto> actividadReciente,
        long ventasEfectuadas,
        long ventasEfectuadasMes,
        BigDecimal ingresosMesAnterior,
        BigDecimal ticketPromedioMes,
        int ocupacionHoy,
        BigDecimal tarifaPromedio,
        BigDecimal revpar,
        long pagosPorVerificar,
        long llegadasHoy,
        long salidasHoy,
        List<OcupacionDiaDto> ocupacionProxima,
        Map<String, Long> estadosHabitaciones) {
}
