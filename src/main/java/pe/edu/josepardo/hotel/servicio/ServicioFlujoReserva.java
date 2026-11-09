package pe.edu.josepardo.hotel.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Actor;
import pe.edu.josepardo.hotel.modelo.EstadoPago;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.modelo.HistorialEstadoReserva;
import pe.edu.josepardo.hotel.modelo.Pago;
import pe.edu.josepardo.hotel.modelo.Reserva;
import pe.edu.josepardo.hotel.repositorio.RepositorioHistorialEstado;
import pe.edu.josepardo.hotel.repositorio.RepositorioPago;
import pe.edu.josepardo.hotel.repositorio.RepositorioReserva;
import pe.edu.josepardo.hotel.repositorio.RepositorioUsuario;
import pe.edu.josepardo.hotel.seguridad.UsuarioAutenticado;

@Service
public class ServicioFlujoReserva {

    private final RepositorioReserva repositorioReserva;
    private final RepositorioHistorialEstado repositorioHistorial;
    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioPago repositorioPago;
    private final MaquinaEstadosReserva maquina;
    private final ServicioAuditoria auditoria;
    private final ApplicationEventPublisher publicador;
    private final ServicioEstadoHabitacion estadoHabitacion;

    public ServicioFlujoReserva(
            RepositorioReserva repositorioReserva,
            RepositorioHistorialEstado repositorioHistorial,
            RepositorioUsuario repositorioUsuario,
            RepositorioPago repositorioPago,
            MaquinaEstadosReserva maquina,
            ServicioAuditoria auditoria,
            ApplicationEventPublisher publicador,
            ServicioEstadoHabitacion estadoHabitacion) {
        this.repositorioReserva = repositorioReserva;
        this.repositorioHistorial = repositorioHistorial;
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioPago = repositorioPago;
        this.maquina = maquina;
        this.auditoria = auditoria;
        this.publicador = publicador;
        this.estadoHabitacion = estadoHabitacion;
    }

    public void registrarInicio(Reserva reserva, Actor actor, Long usuarioId) {
        guardarHistorial(reserva, null, reserva.getEstado(), actor, usuarioId, "Reserva registrada");
    }

    public void aplicar(Reserva reserva, EstadoReserva destino, Actor actor, Long usuarioId, String motivo) {
        EstadoReserva origen = reserva.getEstado();
        if (!maquina.permite(origen, destino, actor)) {
            throw ExcepcionNegocio.conflicto("TRANSICION_NO_PERMITIDA",
                    "No se puede pasar de " + origen.getEtiqueta() + " a " + destino.getEtiqueta() + ".");
        }
        reserva.setEstado(destino);
        guardarHistorial(reserva, origen, destino, actor, usuarioId, motivo);
        estadoHabitacion.recalcular(reserva.getDetalles().stream().map(detalle -> detalle.getHabitacion().getId()).toList());
    }

    @Transactional
    public void confirmar(String codigo, UsuarioAutenticado usuario) {
        Reserva reserva = bloquear(codigo);
        aplicar(reserva, EstadoReserva.CONFIRMADO, usuario.getActor(), usuario.getId(), "Reserva confirmada");
        auditar(usuario, "CONFIRMAR_RESERVA", reserva, null);
    }

    @Transactional
    public void registrarIngreso(String codigo, UsuarioAutenticado usuario) {
        Reserva reserva = bloquear(codigo);
        if (reserva.getEstado() == EstadoReserva.CONFIRMADO && reserva.getFechaEntrada().isAfter(LocalDate.now())) {
            throw ExcepcionNegocio.conflicto("INGRESO_ANTICIPADO", "El ingreso solo se registra desde la fecha de entrada.");
        }
        aplicar(reserva, EstadoReserva.ENTREGADO, usuario.getActor(), usuario.getId(), "Ingreso del huésped");
        auditar(usuario, "REGISTRAR_INGRESO", reserva, null);
    }

    @Transactional
    public void registrarSalida(String codigo, UsuarioAutenticado usuario) {
        Reserva reserva = bloquear(codigo);
        aplicar(reserva, EstadoReserva.FINALIZADO, usuario.getActor(), usuario.getId(), "Salida del huésped");
        auditar(usuario, "REGISTRAR_SALIDA", reserva, null);
    }

    @Transactional
    public void cancelarPorCliente(String codigo, Long clienteUsuarioId, String motivo) {
        Reserva reserva = bloquear(codigo);
        if (!reserva.getCliente().getUsuario().getId().equals(clienteUsuarioId)) {
            throw ExcepcionNegocio.noEncontrado("La reserva no existe.");
        }
        aplicar(reserva, EstadoReserva.CANCELADO, Actor.CLIENTE, clienteUsuarioId, motivo);
        cerrarPagos(reserva, motivo, clienteUsuarioId);
    }

    @Transactional
    public void cancelarPorPersonal(String codigo, UsuarioAutenticado usuario, String motivo) {
        Reserva reserva = bloquear(codigo);
        aplicar(reserva, EstadoReserva.CANCELADO, usuario.getActor(), usuario.getId(), motivo);
        cerrarPagos(reserva, motivo, usuario.getId());
        auditar(usuario, "CANCELAR_RESERVA", reserva, motivo);
        publicador.publishEvent(new EventoComprobanteRequerido(reserva.getId()));
    }

    private void cerrarPagos(Reserva reserva, String motivo, Long usuarioId) {
        List<Pago> pagos = repositorioPago.listarPorReserva(reserva.getId());
        for (Pago pago : pagos) {
            if (pago.getEstado() == EstadoPago.CONFIRMADO) {
                pago.setEstado(EstadoPago.REEMBOLSADO);
                pago.setMotivoRechazo(recortar(motivo));
            } else if (pago.getEstado() == EstadoPago.DECLARADO || pago.getEstado() == EstadoPago.INICIADO) {
                pago.setEstado(EstadoPago.RECHAZADO);
                pago.setMotivoRechazo(recortar(motivo));
            }
        }
    }

    private String recortar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() > 300 ? texto.substring(0, 300) : texto;
    }

    private Reserva bloquear(String codigo) {
        return repositorioReserva.bloquearPorCodigo(codigo)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("La reserva no existe."));
    }

    private void auditar(UsuarioAutenticado usuario, String accion, Reserva reserva, String detalle) {
        auditoria.registrar(usuario.getId(), accion, "reservas", reserva.getCodigo(), detalle);
    }

    private void guardarHistorial(
            Reserva reserva, EstadoReserva origen, EstadoReserva destino, Actor actor, Long usuarioId, String motivo) {
        HistorialEstadoReserva historial = new HistorialEstadoReserva();
        historial.setReserva(reserva);
        historial.setEstadoAnterior(origen);
        historial.setEstadoNuevo(destino);
        historial.setActor(actor);
        if (usuarioId != null) {
            historial.setUsuario(repositorioUsuario.getReferenceById(usuarioId));
        }
        historial.setMotivo(recortar(motivo));
        historial.setCreadoEn(LocalDateTime.now());
        repositorioHistorial.save(historial);
    }
}
