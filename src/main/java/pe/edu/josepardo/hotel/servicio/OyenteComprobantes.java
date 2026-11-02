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

    public OyenteComprobantes(ServicioComprobante servicioComprobante) {
        this.servicioComprobante = servicioComprobante;
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
