package pe.edu.josepardo.hotel.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraImportes {

    private static final BigDecimal CIEN = new BigDecimal("100");

    public record Parametros(BigDecimal porcentajeImpuesto, int nochesMinimasDescuento, BigDecimal porcentajeDescuento) {
    }

    public record Linea(
            BigDecimal precioNoche, int noches, BigDecimal subtotal, BigDecimal descuento, BigDecimal impuesto, BigDecimal total) {
    }

    public record Resumen(
            int noches,
            BigDecimal subtotal,
            BigDecimal descuento,
            BigDecimal porcentajeDescuento,
            BigDecimal impuesto,
            BigDecimal porcentajeImpuesto,
            BigDecimal total,
            List<Linea> lineas) {
    }

    public Resumen calcular(List<BigDecimal> preciosNoche, int noches, Parametros parametros) {
        if (noches < 1) {
            throw new IllegalArgumentException("La estancia debe tener al menos una noche");
        }
        List<BigDecimal> subtotales = preciosNoche.stream()
                .map(precio -> redondear(precio.multiply(BigDecimal.valueOf(noches))))
                .toList();
        BigDecimal subtotal = sumar(subtotales);
        boolean aplicaDescuento = parametros.nochesMinimasDescuento() > 0
                && noches >= parametros.nochesMinimasDescuento()
                && parametros.porcentajeDescuento().signum() > 0;
        BigDecimal porcentajeDescuento = aplicaDescuento ? parametros.porcentajeDescuento() : BigDecimal.ZERO;
        BigDecimal descuento = redondear(subtotal.multiply(porcentajeDescuento).divide(CIEN));
        BigDecimal baseImponible = subtotal.subtract(descuento);
        BigDecimal impuesto = redondear(baseImponible.multiply(parametros.porcentajeImpuesto()).divide(CIEN));
        BigDecimal total = baseImponible.add(impuesto);

        List<BigDecimal> descuentos = distribuir(descuento, subtotales, subtotal);
        List<BigDecimal> impuestos = distribuir(impuesto, subtotales, subtotal);
        List<Linea> lineas = new ArrayList<>();
        for (int i = 0; i < preciosNoche.size(); i++) {
            BigDecimal lineaSubtotal = subtotales.get(i);
            lineas.add(new Linea(
                    preciosNoche.get(i),
                    noches,
                    lineaSubtotal,
                    descuentos.get(i),
                    impuestos.get(i),
                    lineaSubtotal.subtract(descuentos.get(i)).add(impuestos.get(i))));
        }
        return new Resumen(
                noches, subtotal, descuento, porcentajeDescuento, impuesto, parametros.porcentajeImpuesto(), total, lineas);
    }

    private List<BigDecimal> distribuir(BigDecimal monto, List<BigDecimal> pesos, BigDecimal pesoTotal) {
        List<BigDecimal> partes = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO.setScale(2);
        for (int i = 0; i < pesos.size(); i++) {
            BigDecimal parte;
            if (i == pesos.size() - 1) {
                parte = monto.subtract(acumulado);
            } else if (pesoTotal.signum() == 0) {
                parte = BigDecimal.ZERO.setScale(2);
            } else {
                parte = monto.multiply(pesos.get(i)).divide(pesoTotal, 2, RoundingMode.HALF_UP);
            }
            partes.add(parte);
            acumulado = acumulado.add(parte);
        }
        return partes;
    }

    private BigDecimal sumar(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    private BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
