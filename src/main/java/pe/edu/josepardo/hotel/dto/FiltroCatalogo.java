package pe.edu.josepardo.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FiltroCatalogo(
        String busqueda,
        Long categoriaId,
        BigDecimal precioMinimo,
        BigDecimal precioMaximo,
        Integer capacidad,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        Boolean soloDisponibles,
        String orden,
        Integer pagina,
        Integer tamano) {
}
