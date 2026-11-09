package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReservaResumenDto(
        String codigo,
        String cliente,
        String dni,
        String correo,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        int noches,
        int cantidadHuespedes,
        String habitaciones,
        BigDecimal total,
        String estado,
        String estadoEtiqueta,
        String metodoPago,
        String metodoPagoEtiqueta,
        LocalDateTime fechaPago,
        LocalDateTime creadoEn,
        LocalDateTime expiraEn,
        List<String> estadosSiguientes) {

    public ReservaResumenDto conEstadosSiguientes(List<String> siguientes) {
        return new ReservaResumenDto(codigo, cliente, dni, correo, fechaEntrada, fechaSalida, noches, cantidadHuespedes,
                habitaciones, total, estado, estadoEtiqueta, metodoPago, metodoPagoEtiqueta, fechaPago, creadoEn,
                expiraEn, siguientes);
    }
}
