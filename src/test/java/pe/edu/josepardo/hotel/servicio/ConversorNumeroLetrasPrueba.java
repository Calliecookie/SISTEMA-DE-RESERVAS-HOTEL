package pe.edu.josepardo.hotel.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ConversorNumeroLetrasPrueba {

    @Test
    void convierteNumerosConLasReglasDelCastellano() {
        assertEquals("CERO", ConversorNumeroLetras.enLetras(0));
        assertEquals("QUINCE", ConversorNumeroLetras.enLetras(15));
        assertEquals("VEINTIDÓS", ConversorNumeroLetras.enLetras(22));
        assertEquals("TREINTA Y UNO", ConversorNumeroLetras.enLetras(31));
        assertEquals("CIEN", ConversorNumeroLetras.enLetras(100));
        assertEquals("CIENTO UNO", ConversorNumeroLetras.enLetras(101));
        assertEquals("QUINIENTOS", ConversorNumeroLetras.enLetras(500));
        assertEquals("MIL", ConversorNumeroLetras.enLetras(1000));
        assertEquals("VEINTIÚN MIL", ConversorNumeroLetras.enLetras(21_000));
        assertEquals("CIENTO UN MIL DOSCIENTOS", ConversorNumeroLetras.enLetras(101_200));
        assertEquals("UN MILLÓN", ConversorNumeroLetras.enLetras(1_000_000));
        assertEquals("DOS MILLONES TRESCIENTOS MIL CUARENTA Y CINCO", ConversorNumeroLetras.enLetras(2_300_045));
    }

    @Test
    void escribeElImporteDeLaBoletaEnSoles() {
        assertEquals("SON: MIL DOSCIENTOS CINCUENTA CON 50/100 SOLES",
                ConversorNumeroLetras.importeEnSoles(new BigDecimal("1250.50")));
        assertEquals("SON: CUATROCIENTOS SETENTA Y DOS CON 00/100 SOLES",
                ConversorNumeroLetras.importeEnSoles(new BigDecimal("472")));
        assertEquals("SON: UNO CON 05/100 SOL",
                ConversorNumeroLetras.importeEnSoles(new BigDecimal("1.049")));
        assertEquals("SON: CERO CON 99/100 SOLES",
                ConversorNumeroLetras.importeEnSoles(new BigDecimal("0.99")));
        assertThrows(IllegalArgumentException.class,
                () -> ConversorNumeroLetras.importeEnSoles(new BigDecimal("-1")));
    }
}
