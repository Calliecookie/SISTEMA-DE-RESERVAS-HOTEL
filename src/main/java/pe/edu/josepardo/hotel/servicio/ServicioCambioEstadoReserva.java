package pe.edu.josepardo.hotel.servicio;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.SolicitudCambioEstado;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.repositorio.RepositorioReserva;
import pe.edu.josepardo.hotel.seguridad.UsuarioAutenticado;

@Service
public class ServicioCambioEstadoReserva {

    private final ServicioPago servicioPago;
    private final ServicioFlujoReserva servicioFlujo;
    private final RepositorioReserva repositorioReserva;
    private final ApplicationEventPublisher publicador;

    public ServicioCambioEstadoReserva(
            ServicioPago servicioPago,
            ServicioFlujoReserva servicioFlujo,
            RepositorioReserva repositorioReserva,
            ApplicationEventPublisher publicador) {
        this.servicioPago = servicioPago;
        this.servicioFlujo = servicioFlujo;
        this.repositorioReserva = repositorioReserva;
        this.publicador = publicador;
    }

    @Transactional
    public EstadoReserva cambiar(String codigo, SolicitudCambioEstado solicitud, UsuarioAutenticado usuario) {
        EstadoReserva destino;
        try {
            destino = EstadoReserva.valueOf(solicitud.estado());
        } catch (IllegalArgumentException excepcion) {
            throw ExcepcionNegocio.solicitudInvalida("El estado indicado no es válido.");
        }
        String motivo = solicitud.motivo() == null ? "" : solicitud.motivo().trim();
        switch (destino) {
            case PAGADO -> servicioPago.confirmarPago(codigo, usuario, solicitud.metodo(), solicitud.numeroOperacion());
            case CONFIRMADO -> servicioFlujo.confirmar(codigo, usuario);
            case ENTREGADO -> servicioFlujo.registrarIngreso(codigo, usuario);
            case FINALIZADO -> servicioFlujo.registrarSalida(codigo, usuario);
            case CANCELADO -> {
                if (motivo.isEmpty()) {
                    throw ExcepcionNegocio.solicitudInvalida("Indica el motivo de la cancelación.");
                }
                servicioFlujo.cancelarPorPersonal(codigo, usuario, motivo);
            }
            default -> throw ExcepcionNegocio.solicitudInvalida(
                    "El estado " + destino.getEtiqueta() + " lo asigna el sistema.");
        }
        if (destino != EstadoReserva.PAGADO && destino != EstadoReserva.CANCELADO) {
            repositorioReserva.buscarPorCodigo(codigo).ifPresent(reserva -> publicador.publishEvent(
                    new EventoEstadoReservaCambiado(reserva.getId(), destino.getEtiqueta(), motivo.isEmpty() ? null : motivo)));
        }
        return destino;
    }
}
