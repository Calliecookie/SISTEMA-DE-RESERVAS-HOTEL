package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;
import java.util.List;

public record AlternativaDto(
        HabitacionCatalogoDto habitacion,
        int afinidad,
        List<String> motivos,
        BigDecimal diferenciaPrecio) {
}
