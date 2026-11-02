package pe.edu.josepardo.hotel.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formato {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formato() {
    }

    public static String moneda(BigDecimal valor) {
        return "S/ " + decimal(valor);
    }

    public static String decimal(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato.format(valor == null ? BigDecimal.ZERO : valor);
    }

    public static String fecha(LocalDate valor) {
        return valor == null ? "" : valor.format(FECHA);
    }

    public static String fechaHora(LocalDateTime valor) {
        return valor == null ? "" : valor.format(FECHA_HORA);
    }

    public static String porcentaje(BigDecimal valor) {
        return decimal(valor) + " %";
    }
}
