package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservaCreadaDto(
        String codigo,
        BigDecimal total,
        LocalDateTime expiraEn) {
}
