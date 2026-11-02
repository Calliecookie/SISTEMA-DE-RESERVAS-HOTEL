package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;

public record VentasDto(
        Pagina<ReservaResumenDto> pagina,
        long cantidadOperaciones,
        BigDecimal ingresosConfirmados,
        BigDecimal importePendiente) {
}
