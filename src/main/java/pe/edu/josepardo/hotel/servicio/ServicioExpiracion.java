package pe.edu.josepardo.hotel.servicio;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.josepardo.hotel.modelo.Actor;
import pe.edu.josepardo.hotel.modelo.EstadoPago;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.repositorio.RepositorioPago;
import pe.edu.josepardo.hotel.repositorio.RepositorioReserva;

@Service
public class ServicioExpiracion {

    private final RepositorioReserva repositorioReserva;
    private final RepositorioPago repositorioPago;
    private final ServicioFlujoReserva servicioFlujo;
    private final TransactionTemplate transaccion;

    public ServicioExpiracion(
            RepositorioReserva repositorioReserva,
            RepositorioPago repositorioPago,
            ServicioFlujoReserva servicioFlujo,
            PlatformTransactionManager gestorTransacciones) {
        this.repositorioReserva = repositorioReserva;
        this.repositorioPago = repositorioPago;
        this.servicioFlujo = servicioFlujo;
        this.transaccion = new TransactionTemplate(gestorTransacciones);
    }

    public int expirarVencidas() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Long> ids = repositorioReserva.buscarIdsVencidas(EstadoReserva.PENDIENTE, ahora);
        int expiradas = 0;
        for (Long id : ids) {
            Boolean resultado = transaccion.execute(estado -> expirar(id, ahora));
            if (Boolean.TRUE.equals(resultado)) {
                expiradas++;
            }
        }
        return expiradas;
    }

    private boolean expirar(Long id, LocalDateTime ahora) {
        return repositorioReserva.bloquearPorId(id)
                .filter(reserva -> reserva.getEstado() == EstadoReserva.PENDIENTE)
                .filter(reserva -> reserva.getExpiraEn() != null && !reserva.getExpiraEn().isAfter(ahora))
                .map(reserva -> {
                    servicioFlujo.aplicar(reserva, EstadoReserva.EXPIRADO, Actor.SISTEMA, null, "Plazo de pago vencido");
                    repositorioPago.listarPorReserva(id).stream()
                            .filter(pago -> pago.getEstado() == EstadoPago.DECLARADO || pago.getEstado() == EstadoPago.INICIADO)
                            .forEach(pago -> {
                                pago.setEstado(EstadoPago.RECHAZADO);
                                pago.setMotivoRechazo("Reserva expirada");
                            });
                    return true;
                })
                .orElse(false);
    }
}
