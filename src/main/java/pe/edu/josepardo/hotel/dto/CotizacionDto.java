package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CotizacionDto(
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        int noches,
        List<LineaCotizacionDto> lineas,
        ResumenImportesDto resumen,
        BigDecimal promedioPorNoche,
        int nochesParaDescuento,
        BigDecimal porcentajeDescuentoDisponible,
        BigDecimal ahorroPotencial,
        boolean todasDisponibles) {
}
