package pe.edu.josepardo.hotel.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraImportesPrueba {

    private final CalculadoraImportes calculadora = new CalculadoraImportes();
    private final CalculadoraImportes.Parametros parametros =
            new CalculadoraImportes.Parametros(new BigDecimal("18.00"), 5, new BigDecimal("5.00"));

    @Test
    void calculaSubtotalImpuestoYTotal() {
        CalculadoraImportes.Resumen resumen = calculadora.calcular(List.of(new BigDecimal("220.00")), 3, parametros);
        assertEquals(new BigDecimal("660.00"), resumen.subtotal());
        assertEquals(new BigDecimal("0.00"), resumen.descuento());
        assertEquals(new BigDecimal("118.80"), resumen.impuesto());
        assertEquals(new BigDecimal("778.80"), resumen.total());
    }

    @Test
    void aplicaDescuentoDesdeLasNochesMinimas() {
        CalculadoraImportes.Resumen resumen = calculadora.calcular(List.of(new BigDecimal("100.00")), 5, parametros);
        assertEquals(new BigDecimal("500.00"), resumen.subtotal());
        assertEquals(new BigDecimal("25.00"), resumen.descuento());
        assertEquals(new BigDecimal("85.50"), resumen.impuesto());
        assertEquals(new BigDecimal("560.50"), resumen.total());
    }

    @Test
    void noAplicaDescuentoPorDebajoDelMinimo() {
        CalculadoraImportes.Resumen resumen = calculadora.calcular(List.of(new BigDecimal("100.00")), 4, parametros);
        assertEquals(new BigDecimal("0.00"), resumen.descuento());
    }

    @Test
    void lasLineasSumanExactamenteLosTotales() {
        CalculadoraImportes.Resumen resumen = calculadora.calcular(
                List.of(new BigDecimal("99.99"), new BigDecimal("133.33"), new BigDecimal("77.77")), 7, parametros);
        BigDecimal descuento = resumen.lineas().stream().map(CalculadoraImportes.Linea::descuento).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal impuesto = resumen.lineas().stream().map(CalculadoraImportes.Linea::impuesto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = resumen.lineas().stream().map(CalculadoraImportes.Linea::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, resumen.descuento().compareTo(descuento));
        assertEquals(0, resumen.impuesto().compareTo(impuesto));
        assertEquals(0, resumen.total().compareTo(total));
    }

    @Test
    void rechazaEstanciaSinNoches() {
        assertThrows(IllegalArgumentException.class,
                () -> calculadora.calcular(List.of(new BigDecimal("100.00")), 0, parametros));
    }
}
