package pe.edu.josepardo.hotel.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;

class ValidadorEstanciaPrueba {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 1);

    private final ValidadorEstancia validador = new ValidadorEstancia();

    @Test
    void cuentaNochesSinIncluirElDiaDeSalida() {
        assertEquals(1, validador.validar(HOY, HOY.plusDays(1), HOY));
        assertEquals(3, validador.validar(HOY.plusDays(2), HOY.plusDays(5), HOY));
    }

    @Test
    void cuentaBienAlCambiarDeMesYAnio() {
        assertEquals(4, validador.validar(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 3), HOY));
    }

    @Test
    void rechazaEntradaEnElPasado() {
        assertThrows(ExcepcionNegocio.class, () -> validador.validar(HOY.minusDays(1), HOY.plusDays(1), HOY));
    }

    @Test
    void rechazaSalidaIgualOAnterior() {
        assertThrows(ExcepcionNegocio.class, () -> validador.validar(HOY, HOY, HOY));
        assertThrows(ExcepcionNegocio.class, () -> validador.validar(HOY.plusDays(3), HOY.plusDays(2), HOY));
    }

    @Test
    void rechazaEstanciaMayorAlMaximo() {
        assertEquals(30, validador.validar(HOY, HOY.plusDays(30), HOY));
        assertThrows(ExcepcionNegocio.class, () -> validador.validar(HOY, HOY.plusDays(31), HOY));
    }

    @Test
    void rechazaFechasVacias() {
        assertThrows(ExcepcionNegocio.class, () -> validador.validar(null, HOY, HOY));
        assertThrows(ExcepcionNegocio.class, () -> validador.validar(HOY, null, HOY));
    }
}
