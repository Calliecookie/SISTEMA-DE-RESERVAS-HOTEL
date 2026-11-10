package pe.edu.josepardo.hotel.dto;

import java.time.LocalDate;

public record PuertaDto(
        Long id,
        String numero,
        String nombre,
        String categoria,
        int capacidad,
        String estado,
        String situacion,
        String situacionEtiqueta,
        String codigoReserva,
        String huesped,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        boolean llegaHoy,
        boolean saleHoy) {
}
