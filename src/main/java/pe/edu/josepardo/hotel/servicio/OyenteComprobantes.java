package pe.edu.josepardo.hotel.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OyenteComprobantes {

    private static final Logger REGISTRO = LoggerFactory.getLogger(OyenteComprobantes.class);

    private final ServicioComprobante servicioComprobante;
    private final ServicioCorreo servicioCorreo;

    public OyenteComprobantes(ServicioComprobante servicioComprobante, ServicioCorreo servicioCorreo) {
        this.servicioComprobante = servicioComprobante;
        this.servicioCorreo = servicioCorreo;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCambiarEstado(EventoEstadoReservaCambiado evento) {
        try {
            servicioCorreo.notificarCambioEstado(evento.reservaId(), evento.estadoEtiqueta(), evento.motivo());
        } catch (RuntimeException excepcion) {
            REGISTRO.error("No se pudo avisar el cambio de estado de la reserva {}", evento.reservaId(), excepcion);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmarse(EventoComprobanteRequerido evento) {
        try {
            servicioComprobante.emitirYEnviar(evento.reservaId());
        } catch (RuntimeException excepcion) {
            REGISTRO.error("No se pudo emitir el comprobante de la reserva {}", evento.reservaId(), excepcion);
        }
    }
}
