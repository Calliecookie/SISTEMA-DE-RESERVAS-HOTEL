package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReservaDetalleDto(
        String codigo,
        String estado,
        String estadoEtiqueta,
        String cliente,
        String dni,
        String correo,
        String telefono,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        int noches,
        int cantidadHuespedes,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal impuesto,
        BigDecimal total,
        String metodoPago,
        String metodoPagoEtiqueta,
        LocalDateTime fechaPago,
        LocalDateTime creadoEn,
        LocalDateTime expiraEn,
        String observaciones,
        List<DetalleLineaDto> lineas,
        List<PagoDto> pagos,
        List<ComprobanteDto> comprobantes,
        List<HistorialDto> historial,
        List<String> accionesPermitidas) {
}
