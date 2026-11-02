package pe.edu.josepardo.hotel.servicio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;

@Component
public class ValidadorEstancia {

    public static final int NOCHES_MAXIMAS = 30;

    public int validar(LocalDate fechaEntrada, LocalDate fechaSalida, LocalDate hoy) {
        if (fechaEntrada == null || fechaSalida == null) {
            throw ExcepcionNegocio.solicitudInvalida("Selecciona las fechas de entrada y salida.");
        }
        if (fechaEntrada.isBefore(hoy)) {
            throw ExcepcionNegocio.solicitudInvalida("La fecha de entrada no puede ser anterior a hoy.");
        }
        if (!fechaSalida.isAfter(fechaEntrada)) {
            throw ExcepcionNegocio.solicitudInvalida("La fecha de salida debe ser posterior a la de entrada.");
        }
        int noches = (int) ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);
        if (noches > NOCHES_MAXIMAS) {
            throw ExcepcionNegocio.solicitudInvalida("La estancia máxima es de " + NOCHES_MAXIMAS + " noches.");
        }
        return noches;
    }
}
