package pe.edu.josepardo.hotel.servicio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import pe.edu.josepardo.hotel.modelo.Actor;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;

class MaquinaEstadosReservaPrueba {

    private final MaquinaEstadosReserva maquina = new MaquinaEstadosReserva();

    @Test
    void permiteElFlujoCompleto() {
        assertTrue(maquina.permite(EstadoReserva.PENDIENTE, EstadoReserva.PAGADO, Actor.TRABAJADOR));
        assertTrue(maquina.permite(EstadoReserva.PAGADO, EstadoReserva.CONFIRMADO, Actor.TRABAJADOR));
        assertTrue(maquina.permite(EstadoReserva.CONFIRMADO, EstadoReserva.ENTREGADO, Actor.TRABAJADOR));
        assertTrue(maquina.permite(EstadoReserva.ENTREGADO, EstadoReserva.FINALIZADO, Actor.ADMINISTRADOR));
    }

    @Test
    void elClienteSoloCancelaPendientes() {
        assertTrue(maquina.permite(EstadoReserva.PENDIENTE, EstadoReserva.CANCELADO, Actor.CLIENTE));
        assertFalse(maquina.permite(EstadoReserva.PAGADO, EstadoReserva.CANCELADO, Actor.CLIENTE));
        assertFalse(maquina.permite(EstadoReserva.PENDIENTE, EstadoReserva.PAGADO, Actor.CLIENTE));
    }

    @Test
    void soloElSistemaExpira() {
        assertTrue(maquina.permite(EstadoReserva.PENDIENTE, EstadoReserva.EXPIRADO, Actor.SISTEMA));
        assertFalse(maquina.permite(EstadoReserva.PENDIENTE, EstadoReserva.EXPIRADO, Actor.ADMINISTRADOR));
    }

    @Test
    void soloElAdministradorCancelaConfirmadas() {
        assertTrue(maquina.permite(EstadoReserva.CONFIRMADO, EstadoReserva.CANCELADO, Actor.ADMINISTRADOR));
        assertFalse(maquina.permite(EstadoReserva.CONFIRMADO, EstadoReserva.CANCELADO, Actor.TRABAJADOR));
    }

    @Test
    void losEstadosFinalesNoTienenSalida() {
        for (EstadoReserva terminal : new EstadoReserva[] {EstadoReserva.FINALIZADO, EstadoReserva.CANCELADO, EstadoReserva.EXPIRADO}) {
            for (Actor actor : Actor.values()) {
                assertTrue(maquina.destinos(terminal, actor).isEmpty());
            }
        }
    }

    @Test
    void noPermiteSaltarEstados() {
        assertFalse(maquina.permite(EstadoReserva.PENDIENTE, EstadoReserva.ENTREGADO, Actor.ADMINISTRADOR));
        assertFalse(maquina.permite(EstadoReserva.PAGADO, EstadoReserva.FINALIZADO, Actor.ADMINISTRADOR));
    }
}
