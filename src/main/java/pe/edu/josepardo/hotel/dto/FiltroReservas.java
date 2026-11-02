package pe.edu.josepardo.hotel.dto;

import java.time.LocalDate;

public record FiltroReservas(
        String busqueda,
        String estado,
        String metodoPago,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        Integer pagina,
        Integer tamano) {
}
