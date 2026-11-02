package pe.edu.josepardo.hotel.servicio;

import java.time.LocalDate;

public record FiltroReporte(LocalDate desde, LocalDate hasta, String busqueda, Long clienteId) {
}
