package pe.edu.josepardo.hotel.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.josepardo.hotel.configuracion.PropiedadesHotel;
import pe.edu.josepardo.hotel.dto.InicioPayPalDto;
import pe.edu.josepardo.hotel.dto.MetodoPagoDisponibleDto;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Actor;
import pe.edu.josepardo.hotel.modelo.EstadoPago;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.modelo.MetodoPago;
import pe.edu.josepardo.hotel.modelo.Pago;
import pe.edu.josepardo.hotel.modelo.Reserva;
import pe.edu.josepardo.hotel.repositorio.RepositorioPago;
import pe.edu.josepardo.hotel.repositorio.RepositorioReserva;
import pe.edu.josepardo.hotel.repositorio.RepositorioUsuario;
import pe.edu.josepardo.hotel.seguridad.UsuarioAutenticado;

@Service
public class ServicioPago {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ServicioPago.class);
    private static final int MINUTOS_PAGO_PAYPAL = 30;

    private record DatosCobro(Long reservaId, BigDecimal totalSoles, BigDecimal montoExterno, BigDecimal tipoCambio) {
    }

    private record DatosCaptura(Long pagoId, String ordenId, BigDecimal montoEsperado, String moneda) {
    }

    private final RepositorioReserva repositorioReserva;
    private final RepositorioPago repositorioPago;
    private final RepositorioUsuario repositorioUsuario;
    private final ServicioFlujoReserva servicioFlujo;
    private final ServicioConfiguracion configuracion;
    private final ServicioAuditoria auditoria;
    private final ClientePayPal clientePayPal;
    private final ApplicationEventPublisher publicador;
    private final PropiedadesHotel propiedades;
    private final TransactionTemplate transaccion;

    public ServicioPago(
            RepositorioReserva repositorioReserva,
            RepositorioPago repositorioPago,
            RepositorioUsuario repositorioUsuario,
            ServicioFlujoReserva servicioFlujo,
            ServicioConfiguracion configuracion,
            ServicioAuditoria auditoria,
            ClientePayPal clientePayPal,
            ApplicationEventPublisher publicador,
            PropiedadesHotel propiedades,
            PlatformTransactionManager gestorTransacciones) {
        this.repositorioReserva = repositorioReserva;
        this.repositorioPago = repositorioPago;
        this.repositorioUsuario = repositorioUsuario;
        this.servicioFlujo = servicioFlujo;
        this.configuracion = configuracion;
        this.auditoria = auditoria;
        this.clientePayPal = clientePayPal;
        this.publicador = publicador;
        this.propiedades = propiedades;
        this.transaccion = new TransactionTemplate(gestorTransacciones);
    }

    public boolean payPalDisponible() {
        return propiedades.paypal().tieneCredenciales() && tipoCambio() != null;
    }

    public boolean yapeDisponible() {
        return !configuracion.texto(ServicioConfiguracion.YAPE_NUMERO).isBlank()
                && !configuracion.texto(ServicioConfiguracion.YAPE_TITULAR).isBlank();
    }

    @Transactional(readOnly = true)
    public List<MetodoPagoDisponibleDto> metodosDisponibles(String codigo, Long clienteUsuarioId) {
        Reserva reserva = cargarPropia(codigo, clienteUsuarioId);
        List<MetodoPagoDisponibleDto> metodos = new ArrayList<>();
        metodos.add(new MetodoPagoDisponibleDto(
                MetodoPago.EFECTIVO.name(), MetodoPago.EFECTIVO.getEtiqueta(),
                "Paga en recepción dentro del plazo de tu reserva.", false, false, null, null, null, null, null));
        if (yapeDisponible()) {
            metodos.add(new MetodoPagoDisponibleDto(
                    MetodoPago.YAPE.name(), MetodoPago.YAPE.getEtiqueta(),
                    "Yapea el total y registra tu número de operación. Verificaremos el pago.",
                    true, false,
                    configuracion.texto(ServicioConfiguracion.YAPE_NUMERO),
                    configuracion.texto(ServicioConfiguracion.YAPE_TITULAR),
                    configuracion.texto(ServicioConfiguracion.YAPE_QR),
                    null, null));
        }
        if (payPalDisponible()) {
            metodos.add(new MetodoPagoDisponibleDto(
                    MetodoPago.PAYPAL.name(), MetodoPago.PAYPAL.getEtiqueta(),
                    "Paga en línea con tu cuenta PayPal. El cobro se realiza en " + propiedades.paypal().moneda() + ".",
                    false, true, null, null, null,
                    Formato.decimal(convertir(reserva.getTotal(), tipoCambio())),
                    propiedades.paypal().moneda()));
        }
        return metodos;
    }

    @Transactional
    public void declararPago(String codigo, Long clienteUsuarioId, String metodoTexto, String numeroOperacion) {
        MetodoPago metodo = leerMetodo(metodoTexto);
        if (metodo == MetodoPago.PAYPAL) {
            throw ExcepcionNegocio.solicitudInvalida("PayPal se paga desde el botón de pago en línea.");
        }
        if (metodo == MetodoPago.YAPE && !yapeDisponible()) {
            throw ExcepcionNegocio.conflicto("METODO_NO_DISPONIBLE", "Yape no está disponible en este momento.");
        }
        String numero = numeroOperacion == null ? "" : numeroOperacion.trim();
        if (metodo.requiereNumeroOperacion() && !numero.matches("^[0-9A-Za-z-]{4,40}$")) {
            throw ExcepcionNegocio.solicitudInvalida("Ingresa el número de operación de Yape.");
        }
        Reserva reserva = bloquearPropia(codigo, clienteUsuarioId);
        exigirPendienteVigente(reserva);
        if (metodo == MetodoPago.YAPE && repositorioPago.existeOperacionVigente(metodo, numero)) {
            throw ExcepcionNegocio.conflicto("OPERACION_REPETIDA", "Ese número de operación ya fue registrado.");
        }
        cerrarIntentosAbiertos(reserva.getId(), "Reemplazado por un nuevo pago");

        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setMetodo(metodo);
        pago.setMonto(reserva.getTotal());
        pago.setNumeroOperacion(numero.isEmpty() ? null : numero);
        pago.setEstado(EstadoPago.DECLARADO);
        pago.setRegistradoPor(repositorioUsuario.getReferenceById(clienteUsuarioId));
        repositorioPago.save(pago);

        reserva.setMetodoPago(metodo);
        extenderPlazo(reserva, configuracion.entero(ServicioConfiguracion.HORAS_VERIFICACION, 12) * 60);
    }

    public InicioPayPalDto iniciarPayPal(String codigo, Long clienteUsuarioId) {
        if (!payPalDisponible()) {
            throw ExcepcionNegocio.conflicto("METODO_NO_DISPONIBLE", "PayPal no está disponible en este momento.");
        }
        BigDecimal cambio = tipoCambio();
        DatosCobro cobro = transaccion.execute(estado -> {
            Reserva reserva = bloquearPropia(codigo, clienteUsuarioId);
            exigirPendienteVigente(reserva);
            cerrarIntentosAbiertos(reserva.getId(), "Reemplazado por un nuevo intento");
            return new DatosCobro(reserva.getId(), reserva.getTotal(), convertir(reserva.getTotal(), cambio), cambio);
        });
        String base = propiedades.urlPublica();
        String moneda = propiedades.paypal().moneda();
        ClientePayPal.OrdenCreada orden;
        try {
            orden = clientePayPal.crearOrden(
                    codigo,
                    cobro.montoExterno(),
                    moneda,
                    base + "/pagos/paypal/retorno/" + codigo,
                    base + "/carrito?reserva=" + codigo + "&paso=4&pago=cancelado",
                    "orden-" + codigo + "-" + System.currentTimeMillis());
        } catch (ClientePayPal.ErrorPayPal excepcion) {
            throw ExcepcionNegocio.servicioNoDisponible("No pudimos conectar con PayPal. Intenta nuevamente o usa otro método.");
        }
        transaccion.executeWithoutResult(estado -> {
            Reserva reserva = bloquearPropia(codigo, clienteUsuarioId);
            Pago pago = new Pago();
            pago.setReserva(reserva);
            pago.setMetodo(MetodoPago.PAYPAL);
            pago.setMonto(cobro.totalSoles());
            pago.setReferenciaExterna(orden.id());
            pago.setMonedaExterna(moneda);
            pago.setMontoExterno(cobro.montoExterno());
            pago.setTipoCambio(cobro.tipoCambio());
            pago.setEstado(EstadoPago.INICIADO);
            pago.setRegistradoPor(repositorioUsuario.getReferenceById(clienteUsuarioId));
            repositorioPago.save(pago);
            extenderPlazo(reserva, MINUTOS_PAGO_PAYPAL);
        });
        return new InicioPayPalDto(orden.urlAprobacion());
    }

    public boolean completarPayPal(String codigo, Long clienteUsuarioId) {
        DatosCaptura datos = transaccion.execute(estado -> {
            Reserva reserva = bloquearPropia(codigo, clienteUsuarioId);
            if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
                return null;
            }
            return repositorioPago.buscarVigente(reserva.getId(), EstadoPago.INICIADO)
                    .map(pago -> new DatosCaptura(pago.getId(), pago.getReferenciaExterna(), pago.getMontoExterno(), pago.getMonedaExterna()))
                    .orElse(null);
        });
        if (datos == null) {
            return repositorioPago.buscarVigentePorCodigo(codigo, EstadoPago.CONFIRMADO).isPresent();
        }
        ClientePayPal.CapturaOrden captura;
        try {
            captura = clientePayPal.capturarOrden(datos.ordenId(), "captura-" + datos.pagoId());
        } catch (ClientePayPal.ErrorPayPal excepcion) {
            REGISTRO.warn("No se pudo capturar la orden {}: {}", datos.ordenId(), excepcion.getMessage());
            return false;
        }
        boolean completa = "COMPLETED".equals(captura.estado())
                && captura.monto().compareTo(datos.montoEsperado()) == 0
                && datos.moneda().equals(captura.moneda());
        if (!completa) {
            REGISTRO.warn("Captura no válida para la orden {}: estado {}", datos.ordenId(), captura.estado());
            return false;
        }
        return Boolean.TRUE.equals(transaccion.execute(estado -> aplicarCaptura(codigo, clienteUsuarioId, datos.pagoId(), captura)));
    }

    private boolean aplicarCaptura(String codigo, Long clienteUsuarioId, Long pagoId, ClientePayPal.CapturaOrden captura) {
        Reserva reserva = bloquearPropia(codigo, clienteUsuarioId);
        Pago pago = repositorioPago.findById(pagoId).orElseThrow();
        pago.setNumeroOperacion(captura.captureId());
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            pago.setEstado(EstadoPago.DECLARADO);
            pago.setMotivoRechazo("Cobro recibido con la reserva en estado " + reserva.getEstado().getEtiqueta() + ". Revisar reembolso.");
            REGISTRO.error("Captura {} recibida para la reserva {} en estado {}", captura.captureId(), codigo, reserva.getEstado());
            return false;
        }
        LocalDateTime ahora = LocalDateTime.now();
        pago.setEstado(EstadoPago.CONFIRMADO);
        pago.setFechaPago(ahora);
        reserva.setMetodoPago(MetodoPago.PAYPAL);
        reserva.setFechaPago(ahora);
        reserva.setExpiraEn(null);
        servicioFlujo.aplicar(reserva, EstadoReserva.PAGADO, Actor.SISTEMA, null, "Pago capturado en PayPal");
        publicador.publishEvent(new EventoComprobanteRequerido(reserva.getId()));
        return true;
    }

    @Transactional
    public void confirmarPago(String codigo, UsuarioAutenticado usuario, String metodoTexto, String numeroOperacion) {
        Reserva reserva = bloquear(codigo);
        exigirPendienteVigente(reserva);
        Pago pago = repositorioPago.buscarVigente(reserva.getId(), EstadoPago.DECLARADO).orElse(null);
        if (pago == null) {
            MetodoPago metodo = leerMetodo(metodoTexto);
            if (metodo == MetodoPago.PAYPAL) {
                throw ExcepcionNegocio.solicitudInvalida("Los pagos con PayPal se confirman automáticamente.");
            }
            String numero = numeroOperacion == null ? "" : numeroOperacion.trim();
            if (metodo.requiereNumeroOperacion() && numero.isEmpty()) {
                throw ExcepcionNegocio.solicitudInvalida("Ingresa el número de operación.");
            }
            pago = new Pago();
            pago.setReserva(reserva);
            pago.setMetodo(metodo);
            pago.setMonto(reserva.getTotal());
            pago.setNumeroOperacion(numero.isEmpty() ? null : numero);
            pago.setRegistradoPor(repositorioUsuario.getReferenceById(usuario.getId()));
        }
        LocalDateTime ahora = LocalDateTime.now();
        pago.setEstado(EstadoPago.CONFIRMADO);
        pago.setConfirmadoPor(repositorioUsuario.getReferenceById(usuario.getId()));
        pago.setFechaPago(ahora);
        repositorioPago.save(pago);

        reserva.setMetodoPago(pago.getMetodo());
        reserva.setFechaPago(ahora);
        reserva.setExpiraEn(null);
        servicioFlujo.aplicar(reserva, EstadoReserva.PAGADO, usuario.getActor(), usuario.getId(), "Pago confirmado");
        auditoria.registrar(usuario.getId(), "CONFIRMAR_PAGO", "reservas", codigo, pago.getMetodo().getEtiqueta());
        publicador.publishEvent(new EventoComprobanteRequerido(reserva.getId()));
    }

    @Transactional
    public void rechazarPago(String codigo, UsuarioAutenticado usuario, String motivo) {
        Reserva reserva = bloquear(codigo);
        Pago pago = repositorioPago.buscarVigente(reserva.getId(), EstadoPago.DECLARADO)
                .orElseThrow(() -> ExcepcionNegocio.conflicto("SIN_PAGO", "La reserva no tiene un pago por verificar."));
        pago.setEstado(EstadoPago.RECHAZADO);
        pago.setMotivoRechazo(motivo);
        reserva.setMetodoPago(null);
        auditoria.registrar(usuario.getId(), "RECHAZAR_PAGO", "reservas", codigo, motivo);
    }

    private BigDecimal tipoCambio() {
        BigDecimal valor = configuracion.decimal(ServicioConfiguracion.PAYPAL_TIPO_CAMBIO, null);
        return valor == null || valor.signum() <= 0 ? null : valor;
    }

    static BigDecimal convertir(BigDecimal soles, BigDecimal tipoCambio) {
        return soles.divide(tipoCambio, 2, RoundingMode.HALF_UP);
    }

    private void cerrarIntentosAbiertos(Long reservaId, String motivo) {
        repositorioPago.listarPorReserva(reservaId).stream()
                .filter(pago -> pago.getEstado() == EstadoPago.DECLARADO || pago.getEstado() == EstadoPago.INICIADO)
                .forEach(pago -> {
                    pago.setEstado(EstadoPago.RECHAZADO);
                    pago.setMotivoRechazo(motivo);
                });
    }

    private void extenderPlazo(Reserva reserva, int minutos) {
        LocalDateTime limite = LocalDateTime.now().plusMinutes(minutos);
        if (reserva.getExpiraEn() == null || reserva.getExpiraEn().isBefore(limite)) {
            reserva.setExpiraEn(limite);
        }
    }

    private void exigirPendienteVigente(Reserva reserva) {
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw ExcepcionNegocio.conflicto("RESERVA_NO_PENDIENTE", "La reserva ya no está pendiente de pago.");
        }
        if (reserva.getExpiraEn() != null && !reserva.getExpiraEn().isAfter(LocalDateTime.now())) {
            throw ExcepcionNegocio.conflicto("RESERVA_VENCIDA", "El plazo de pago de la reserva ya venció.");
        }
    }

    private MetodoPago leerMetodo(String texto) {
        try {
            return MetodoPago.valueOf(texto);
        } catch (IllegalArgumentException | NullPointerException excepcion) {
            throw ExcepcionNegocio.solicitudInvalida("Selecciona un método de pago válido.");
        }
    }

    private Reserva cargarPropia(String codigo, Long clienteUsuarioId) {
        Reserva reserva = repositorioReserva.buscarPorCodigo(codigo)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("La reserva no existe."));
        return exigirPropiedad(reserva, clienteUsuarioId);
    }

    private Reserva bloquearPropia(String codigo, Long clienteUsuarioId) {
        return exigirPropiedad(bloquear(codigo), clienteUsuarioId);
    }

    private Reserva exigirPropiedad(Reserva reserva, Long clienteUsuarioId) {
        if (!reserva.getCliente().getUsuario().getId().equals(clienteUsuarioId)) {
            throw ExcepcionNegocio.noEncontrado("La reserva no existe.");
        }
        return reserva;
    }

    private Reserva bloquear(String codigo) {
        return repositorioReserva.bloquearPorCodigo(codigo)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("La reserva no existe."));
    }
}
