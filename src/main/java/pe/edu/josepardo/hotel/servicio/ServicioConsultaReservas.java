package pe.edu.josepardo.hotel.servicio;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.ComprobanteDto;
import pe.edu.josepardo.hotel.dto.DetalleLineaDto;
import pe.edu.josepardo.hotel.dto.FiltroReservas;
import pe.edu.josepardo.hotel.dto.HistorialDto;
import pe.edu.josepardo.hotel.dto.Pagina;
import pe.edu.josepardo.hotel.dto.PagoDto;
import pe.edu.josepardo.hotel.dto.ReservaDetalleDto;
import pe.edu.josepardo.hotel.dto.ReservaResumenDto;
import pe.edu.josepardo.hotel.dto.VentasDto;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Actor;
import pe.edu.josepardo.hotel.modelo.Cliente;
import pe.edu.josepardo.hotel.modelo.EstadoPago;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.modelo.MetodoPago;
import pe.edu.josepardo.hotel.modelo.Reserva;
import pe.edu.josepardo.hotel.repositorio.ConsultaReservas;
import pe.edu.josepardo.hotel.repositorio.RepositorioCliente;
import pe.edu.josepardo.hotel.repositorio.RepositorioComprobante;
import pe.edu.josepardo.hotel.repositorio.RepositorioDetalleReserva;
import pe.edu.josepardo.hotel.repositorio.RepositorioHistorialEstado;
import pe.edu.josepardo.hotel.repositorio.RepositorioPago;
import pe.edu.josepardo.hotel.repositorio.RepositorioReserva;
import pe.edu.josepardo.hotel.seguridad.UsuarioAutenticado;

@Service
public class ServicioConsultaReservas {

    private static final int TAMANO_POR_DEFECTO = 10;
    private static final int TAMANO_MAXIMO = 100;

    private final ConsultaReservas consulta;
    private final RepositorioReserva repositorioReserva;
    private final RepositorioDetalleReserva repositorioDetalle;
    private final RepositorioPago repositorioPago;
    private final RepositorioComprobante repositorioComprobante;
    private final RepositorioHistorialEstado repositorioHistorial;
    private final RepositorioCliente repositorioCliente;
    private final MaquinaEstadosReserva maquina;

    public ServicioConsultaReservas(
            ConsultaReservas consulta,
            RepositorioReserva repositorioReserva,
            RepositorioDetalleReserva repositorioDetalle,
            RepositorioPago repositorioPago,
            RepositorioComprobante repositorioComprobante,
            RepositorioHistorialEstado repositorioHistorial,
            RepositorioCliente repositorioCliente,
            MaquinaEstadosReserva maquina) {
        this.consulta = consulta;
        this.repositorioReserva = repositorioReserva;
        this.repositorioDetalle = repositorioDetalle;
        this.repositorioPago = repositorioPago;
        this.repositorioComprobante = repositorioComprobante;
        this.repositorioHistorial = repositorioHistorial;
        this.repositorioCliente = repositorioCliente;
        this.maquina = maquina;
    }

    @Transactional(readOnly = true)
    public Pagina<ReservaResumenDto> listarDeCliente(Long usuarioId, Integer pagina, Integer tamano) {
        Cliente cliente = repositorioCliente.buscarPorUsuarioId(usuarioId)
                .orElseThrow(() -> ExcepcionNegocio.prohibido("Tu cuenta no tiene un perfil de cliente."));
        FiltroReservas filtro = new FiltroReservas(null, null, null, null, null, pagina, tamano);
        return consulta.listar(filtro, cliente.getId(), pagina(pagina), tamano(tamano));
    }

    @Transactional(readOnly = true)
    public Pagina<ReservaResumenDto> listar(FiltroReservas filtro) {
        validar(filtro);
        return consulta.listar(filtro, null, pagina(filtro.pagina()), tamano(filtro.tamano()));
    }

    @Transactional(readOnly = true)
    public VentasDto listarVentas(FiltroReservas filtro) {
        validar(filtro);
        return consulta.listarVentas(filtro, pagina(filtro.pagina()), tamano(filtro.tamano()));
    }

    @Transactional(readOnly = true)
    public ReservaDetalleDto detalle(String codigo, UsuarioAutenticado usuario) {
        Reserva reserva = repositorioReserva.buscarPorCodigo(codigo)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("La reserva no existe."));
        if (!usuario.esPersonal() && !reserva.getCliente().getUsuario().getId().equals(usuario.getId())) {
            throw ExcepcionNegocio.noEncontrado("La reserva no existe.");
        }
        Long id = reserva.getId();
        List<DetalleLineaDto> lineas = repositorioDetalle.listarPorReserva(id).stream()
                .map(detalle -> new DetalleLineaDto(
                        detalle.getHabitacion().getNumero(),
                        detalle.getHabitacion().getNombre(),
                        detalle.getHabitacion().getCategoria().getNombre(),
                        detalle.getHabitacion().getImagenPrincipal(),
                        detalle.getCantidadHuespedes(),
                        detalle.getPrecioNoche(),
                        detalle.getNoches(),
                        detalle.getSubtotal(),
                        detalle.getDescuento(),
                        detalle.getImpuesto(),
                        detalle.getTotal()))
                .toList();
        List<PagoDto> pagos = repositorioPago.listarPorReserva(id).stream()
                .map(pago -> new PagoDto(
                        pago.getId(),
                        pago.getMetodo().name(),
                        pago.getMetodo().getEtiqueta(),
                        pago.getMonto(),
                        pago.getNumeroOperacion(),
                        pago.getMonedaExterna(),
                        pago.getMontoExterno(),
                        pago.getEstado().name(),
                        pago.getEstado().getEtiqueta(),
                        pago.getFechaPago(),
                        pago.getCreadoEn(),
                        pago.getMotivoRechazo(),
                        pago.getConfirmadoPor() == null ? null : pago.getConfirmadoPor().getNombreCompleto()))
                .toList();
        List<ComprobanteDto> comprobantes = repositorioComprobante.listarPorReserva(id).stream()
                .map(comprobante -> new ComprobanteDto(
                        comprobante.getNumero(),
                        comprobante.getEstadoPago(),
                        comprobante.getFechaEmision(),
                        comprobante.getCorreoDestino(),
                        comprobante.getCorreoEstado().name(),
                        comprobante.getCorreoEstado().getEtiqueta(),
                        comprobante.getCorreoDetalle(),
                        comprobante.getCorreoFecha(),
                        "/comprobantes/" + comprobante.getNumero()))
                .toList();
        List<HistorialDto> historial = repositorioHistorial.listarPorReserva(id).stream()
                .map(item -> new HistorialDto(
                        item.getEstadoAnterior() == null ? null : item.getEstadoAnterior().getEtiqueta(),
                        item.getEstadoNuevo().name(),
                        item.getEstadoNuevo().getEtiqueta(),
                        item.getActor().getEtiqueta(),
                        item.getUsuario() == null ? null : item.getUsuario().getNombreCompleto(),
                        item.getMotivo(),
                        item.getCreadoEn()))
                .toList();
        MetodoPago metodo = reserva.getMetodoPago();
        return new ReservaDetalleDto(
                reserva.getCodigo(),
                reserva.getEstado().name(),
                reserva.getEstado().getEtiqueta(),
                reserva.getCliente().getUsuario().getNombreCompleto(),
                reserva.getCliente().getDni(),
                reserva.getCliente().getUsuario().getCorreo(),
                reserva.getCliente().getTelefono(),
                reserva.getFechaEntrada(),
                reserva.getFechaSalida(),
                reserva.getNoches(),
                reserva.getCantidadHuespedes(),
                reserva.getSubtotal(),
                reserva.getDescuento(),
                reserva.getImpuesto(),
                reserva.getTotal(),
                metodo == null ? null : metodo.name(),
                metodo == null ? null : metodo.getEtiqueta(),
                reserva.getFechaPago(),
                reserva.getCreadoEn(),
                reserva.getExpiraEn(),
                reserva.getObservaciones(),
                lineas,
                pagos,
                comprobantes,
                historial,
                acciones(reserva, usuario.getActor()));
    }

    private List<String> acciones(Reserva reserva, Actor actor) {
        List<String> acciones = new ArrayList<>();
        for (EstadoReserva destino : maquina.destinos(reserva.getEstado(), actor)) {
            acciones.add(switch (destino) {
                case PAGADO -> "CONFIRMAR_PAGO";
                case CONFIRMADO -> "CONFIRMAR";
                case ENTREGADO -> "INGRESO";
                case FINALIZADO -> "SALIDA";
                case CANCELADO -> "CANCELAR";
                default -> destino.name();
            });
        }
        if (actor != Actor.CLIENTE
                && repositorioPago.buscarVigente(reserva.getId(), EstadoPago.DECLARADO).isPresent()
                && reserva.getEstado() == EstadoReserva.PENDIENTE) {
            acciones.add("RECHAZAR_PAGO");
        }
        return acciones;
    }

    private void validar(FiltroReservas filtro) {
        if (filtro.estado() != null && !filtro.estado().isBlank()) {
            try {
                EstadoReserva.valueOf(filtro.estado());
            } catch (IllegalArgumentException excepcion) {
                throw ExcepcionNegocio.solicitudInvalida("El estado indicado no es válido.");
            }
        }
        if (filtro.metodoPago() != null && !filtro.metodoPago().isBlank()) {
            try {
                MetodoPago.valueOf(filtro.metodoPago());
            } catch (IllegalArgumentException excepcion) {
                throw ExcepcionNegocio.solicitudInvalida("El método de pago indicado no es válido.");
            }
        }
        if (filtro.fechaDesde() != null && filtro.fechaHasta() != null && filtro.fechaDesde().isAfter(filtro.fechaHasta())) {
            throw ExcepcionNegocio.solicitudInvalida("La fecha inicial no puede superar a la final.");
        }
    }

    private int pagina(Integer valor) {
        return valor == null ? 0 : Math.max(valor, 0);
    }

    private int tamano(Integer valor) {
        return valor == null ? TAMANO_POR_DEFECTO : Math.min(Math.max(valor, 1), TAMANO_MAXIMO);
    }
}
