package pe.edu.josepardo.hotel.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ControlTraficoPrueba {

    private static final long MINUTO = 60_000L;

    private final AtomicLong reloj = new AtomicLong(1_000_000L);
    private final ControlTrafico control = new ControlTrafico(3, 10, 3, reloj::get);

    @Test
    void permiteHastaElLimiteYLuegoLimita() {
        for (int i = 0; i < 5; i++) {
            assertEquals(ControlTrafico.Decision.PERMITIR, control.evaluar("1.1.1.1", 5));
        }
        assertEquals(ControlTrafico.Decision.LIMITAR, control.evaluar("1.1.1.1", 5));
    }

    @Test
    void reiniciaElConteoEnLaVentanaSiguiente() {
        for (int i = 0; i < 6; i++) {
            control.evaluar("1.1.1.1", 5);
        }
        reloj.addAndGet(MINUTO + 1);
        assertEquals(ControlTrafico.Decision.PERMITIR, control.evaluar("1.1.1.1", 5));
    }

    @Test
    void bloqueaTrasExcesosRepetidosYLiberaAlTerminarElCastigo() {
        for (int ventana = 0; ventana < 3; ventana++) {
            for (int i = 0; i < 7; i++) {
                control.evaluar("2.2.2.2", 5);
            }
            if (ventana < 2) {
                reloj.addAndGet(MINUTO + 1);
            }
        }
        assertEquals(ControlTrafico.Decision.BLOQUEAR, control.evaluar("2.2.2.2", 5));
        assertEquals(1, control.ipsBloqueadas());
        reloj.addAndGet(10 * MINUTO + 1);
        assertEquals(ControlTrafico.Decision.PERMITIR, control.evaluar("2.2.2.2", 5));
    }

    @Test
    void unaIpNoAfectaAOtra() {
        for (int i = 0; i < 10; i++) {
            control.evaluar("3.3.3.3", 5);
        }
        assertEquals(ControlTrafico.Decision.PERMITIR, control.evaluar("4.4.4.4", 5));
    }

    @Test
    void acotaLaMemoriaAnteMuchasIpsDistintas() {
        control.evaluar("10.0.0.1", 5);
        control.evaluar("10.0.0.2", 5);
        control.evaluar("10.0.0.3", 5);
        assertEquals(ControlTrafico.Decision.LIMITAR, control.evaluar("10.0.0.4", 5));
    }

    @Test
    void agrupaIpv6PorPrefijo() {
        assertEquals(
                ControlTrafico.normalizarIp("2001:db8:1:2:aaaa::1"),
                ControlTrafico.normalizarIp("2001:db8:1:2:bbbb::9"));
        assertEquals("8.8.8.8", ControlTrafico.normalizarIp("8.8.8.8"));
    }
}
