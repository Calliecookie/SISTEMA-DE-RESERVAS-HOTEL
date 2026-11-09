package pe.edu.josepardo.hotel.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Convierte un importe en soles al texto que se imprime al pie de la boleta,
 * por ejemplo 1250.50 → "SON: MIL DOSCIENTOS CINCUENTA CON 50/100 SOLES".
 */
public final class ConversorNumeroLetras {

    private static final String[] UNIDADES = {
        "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
        "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
        "VEINTE", "VEINTIUNO", "VEINTIDÓS", "VEINTITRÉS", "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS",
        "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"
    };
    private static final String[] DECENAS = {
        "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };
    private static final String[] CENTENAS = {
        "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS", "SEISCIENTOS",
        "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    private ConversorNumeroLetras() {
    }

    public static String importeEnSoles(BigDecimal importe) {
        if (importe == null) {
            importe = BigDecimal.ZERO;
        }
        if (importe.signum() < 0) {
            throw new IllegalArgumentException("El importe no puede ser negativo");
        }
        BigDecimal redondeado = importe.setScale(2, RoundingMode.HALF_UP);
        long entero = redondeado.longValue();
        int centimos = redondeado.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        String moneda = entero == 1 ? "SOL" : "SOLES";
        return "SON: " + enLetras(entero) + " CON " + String.format("%02d", centimos) + "/100 " + moneda;
    }

    public static String enLetras(long numero) {
        if (numero < 0 || numero > 999_999_999_999L) {
            throw new IllegalArgumentException("Número fuera de rango: " + numero);
        }
        if (numero == 0) {
            return "CERO";
        }
        StringBuilder texto = new StringBuilder();
        long millones = numero / 1_000_000;
        long resto = numero % 1_000_000;
        if (millones > 0) {
            texto.append(millones == 1 ? "UN MILLÓN" : apocopar(miles(millones)) + " MILLONES");
        }
        if (resto > 0) {
            if (!texto.isEmpty()) {
                texto.append(' ');
            }
            texto.append(miles(resto));
        }
        return texto.toString();
    }

    private static String miles(long numero) {
        long miles = numero / 1000;
        int resto = (int) (numero % 1000);
        StringBuilder texto = new StringBuilder();
        if (miles > 0) {
            texto.append(miles == 1 ? "MIL" : apocopar(centenas((int) miles)) + " MIL");
        }
        if (resto > 0) {
            if (!texto.isEmpty()) {
                texto.append(' ');
            }
            texto.append(centenas(resto));
        }
        return texto.toString();
    }

    private static String centenas(int numero) {
        if (numero == 100) {
            return "CIEN";
        }
        int centena = numero / 100;
        int resto = numero % 100;
        StringBuilder texto = new StringBuilder(CENTENAS[centena]);
        if (resto > 0) {
            if (!texto.isEmpty()) {
                texto.append(' ');
            }
            texto.append(decenas(resto));
        }
        return texto.toString();
    }

    private static String decenas(int numero) {
        if (numero < 30) {
            return UNIDADES[numero];
        }
        int unidad = numero % 10;
        return DECENAS[numero / 10] + (unidad > 0 ? " Y " + UNIDADES[unidad] : "");
    }

    /** "VEINTIUNO MIL" → "VEINTIÚN MIL", "TREINTA Y UNO MIL" → "TREINTA Y UN MIL". */
    private static String apocopar(String texto) {
        if (texto.endsWith("VEINTIUNO")) {
            return texto.substring(0, texto.length() - "VEINTIUNO".length()) + "VEINTIÚN";
        }
        if (texto.endsWith("UNO")) {
            return texto.substring(0, texto.length() - 1);
        }
        return texto;
    }
}
