package pe.edu.josepardo.hotel.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import pe.edu.josepardo.hotel.modelo.Rol;

class ReglasSeguridadPrueba {

    @Test
    void ignoraReenvioCuandoElOrigenNoEsProxyConfiable() {
        assertEquals("200.1.1.1", ResolutorIpCliente.resolver("200.1.1.1", "1.2.3.4", Set.of("10.0.0.1")));
    }

    @Test
    void tomaLaUltimaIpNoConfiableTrasUnProxy() {
        assertEquals("190.5.5.5", ResolutorIpCliente.resolver("10.0.0.1", "6.6.6.6, 190.5.5.5, 10.0.0.2", Set.of("10.0.0.1", "10.0.0.2")));
    }

    @Test
    void noPermiteRedireccionesAOtrosSitios() {
        assertEquals("/mi-cuenta", ManejadorAccesoExitoso.resolverDestino("https://malicioso.pe", Rol.CLIENTE, ""));
        assertEquals("/mi-cuenta", ManejadorAccesoExitoso.resolverDestino("//malicioso.pe", Rol.CLIENTE, ""));
        assertEquals("/carrito?paso=2", ManejadorAccesoExitoso.resolverDestino("/carrito?paso=2", Rol.CLIENTE, ""));
        assertEquals("/administracion", ManejadorAccesoExitoso.resolverDestino("/carrito", Rol.ADMINISTRADOR, ""));
    }

    @Test
    void limitadorBloqueaAlSuperarElMaximoYSeReiniciaConElTiempo() {
        AtomicLong reloj = new AtomicLong(0);
        LimitadorSolicitudes limitador = new LimitadorSolicitudes(reloj::get);
        assertTrue(limitador.permitir("clave", 2, Duration.ofMinutes(1)));
        assertTrue(limitador.permitir("clave", 2, Duration.ofMinutes(1)));
        assertFalse(limitador.permitir("clave", 2, Duration.ofMinutes(1)));
        reloj.addAndGet(61_000);
        assertTrue(limitador.permitir("clave", 2, Duration.ofMinutes(1)));
    }
}
