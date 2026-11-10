package pe.edu.josepardo.hotel.servicio;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Comprobante;
import pe.edu.josepardo.hotel.modelo.DetalleReserva;
import pe.edu.josepardo.hotel.modelo.EstadoPago;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.modelo.Reserva;
import pe.edu.josepardo.hotel.repositorio.RepositorioComprobante;
import pe.edu.josepardo.hotel.repositorio.RepositorioDetalleReserva;
import pe.edu.josepardo.hotel.repositorio.RepositorioPago;
import pe.edu.josepardo.hotel.repositorio.RepositorioReserva;
import pe.edu.josepardo.hotel.seguridad.UsuarioAutenticado;

@Service
public class ServicioComprobante {

    public record ArchivoComprobante(String nombre, byte[] contenido) {
    }

    private final RepositorioComprobante repositorioComprobante;
    private final RepositorioReserva repositorioReserva;
    private final RepositorioDetalleReserva repositorioDetalle;
    private final RepositorioPago repositorioPago;
    private final GeneradorPdf generadorPdf;
    private final ServicioAlmacenamiento almacenamiento;
    private final ServicioConfiguracion configuracion;
    private final ServicioCorreo servicioCorreo;
    private final TransactionTemplate transaccion;

    public ServicioComprobante(
            RepositorioComprobante repositorioComprobante,
            RepositorioReserva repositorioReserva,
            RepositorioDetalleReserva repositorioDetalle,
            RepositorioPago repositorioPago,
            GeneradorPdf generadorPdf,
            ServicioAlmacenamiento almacenamiento,
            ServicioConfiguracion configuracion,
            ServicioCorreo servicioCorreo,
            PlatformTransactionManager gestorTransacciones) {
        this.repositorioComprobante = repositorioComprobante;
        this.repositorioReserva = repositorioReserva;
        this.repositorioDetalle = repositorioDetalle;
        this.repositorioPago = repositorioPago;
        this.generadorPdf = generadorPdf;
        this.almacenamiento = almacenamiento;
        this.configuracion = configuracion;
        this.servicioCorreo = servicioCorreo;
        this.transaccion = new TransactionTemplate(gestorTransacciones);
        this.transaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void emitirYEnviar(Long reservaId) {
        Long comprobanteId = transaccion.execute(estado -> emitir(reservaId));
        servicioCorreo.enviarComprobante(comprobanteId);
    }

    public ArchivoComprobante descargar(String numero, UsuarioAutenticado usuario) {
        return transaccion.execute(estado -> {
            Comprobante comprobante = repositorioComprobante.buscarPorNumero(numero)
                    .orElseThrow(() -> ExcepcionNegocio.noEncontrado("El comprobante no existe."));
            Reserva reserva = comprobante.getReserva();
            if (!usuario.esPersonal() && !reserva.getCliente().getUsuario().getId().equals(usuario.getId())) {
                throw ExcepcionNegocio.noEncontrado("El comprobante no existe.");
            }
            if (!almacenamiento.existeComprobante(comprobante.getRutaArchivo())) {
                almacenamiento.guardarComprobante(comprobante.getNumero(), generarPdf(reserva, comprobante));
            }
            return new ArchivoComprobante(
                    comprobante.getNumero() + ".pdf", almacenamiento.leerComprobante(comprobante.getRutaArchivo()));
        });
    }

    private Long emitir(Long reservaId) {
        Reserva reserva = repositorioReserva.buscarCompletaPorId(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva inexistente: " + reservaId));
        long emitidos = repositorioComprobante.contarPorReserva(reservaId);
        Comprobante comprobante = new Comprobante();
        comprobante.setReserva(reserva);
        comprobante.setNumero("CR-" + reserva.getCodigo() + "-" + String.format("%02d", emitidos + 1));
        comprobante.setEstadoPago(estadoPago(reserva));
        comprobante.setFechaEmision(LocalDateTime.now());
        comprobante.setCorreoDestino(reserva.getCliente().getUsuario().getCorreo());
        String archivo = almacenamiento.guardarComprobante(comprobante.getNumero(), generarPdf(reserva, comprobante));
        comprobante.setRutaArchivo(archivo);
        return repositorioComprobante.save(comprobante).getId();
    }

    private String estadoPago(Reserva reserva) {
        EstadoReserva estado = reserva.getEstado();
        if (estado == EstadoReserva.CANCELADO) {
            return "Reserva cancelada";
        }
        if (estado == EstadoReserva.EXPIRADO) {
            return "Reserva expirada";
        }
        if (estado != EstadoReserva.PENDIENTE) {
            return "Pagado";
        }
        boolean porVerificar = repositorioPago.buscarVigente(reserva.getId(), EstadoPago.DECLARADO).isPresent();
        return porVerificar ? "Pago por verificar" : "Pendiente de pago";
    }

    private byte[] generarPdf(Reserva reserva, Comprobante comprobante) {
        List<DetalleReserva> detalles = repositorioDetalle.listarPorReserva(reserva.getId());
        List<Map<String, String>> lineas = detalles.stream().map(detalle -> Map.of(
                "habitacion", detalle.getHabitacion().getNumero() + " - " + detalle.getHabitacion().getNombre(),
                "categoria", detalle.getHabitacion().getCategoria().getNombre(),
                "huespedes", String.valueOf(detalle.getCantidadHuespedes()),
                "noches", String.valueOf(detalle.getNoches()),
                "precio", Formato.moneda(detalle.getPrecioNoche()),
                "subtotal", Formato.moneda(detalle.getSubtotal()))).toList();
        Map<String, Object> variables = new HashMap<>();
        variables.put("hotel", configuracion.hotel());
        variables.put("logo", logoEnBase64());
        variables.put("numero", comprobante.getNumero());
        variables.put("codigo", reserva.getCodigo());
        variables.put("cliente", reserva.getCliente().getUsuario().getNombreCompleto());
        variables.put("dni", reserva.getCliente().getDni());
        variables.put("correo", reserva.getCliente().getUsuario().getCorreo());
        variables.put("entrada", Formato.fecha(reserva.getFechaEntrada()));
        variables.put("salida", Formato.fecha(reserva.getFechaSalida()));
        variables.put("noches", reserva.getNoches());
        variables.put("huespedes", reserva.getCantidadHuespedes());
        variables.put("lineas", lineas);
        variables.put("subtotal", Formato.moneda(reserva.getSubtotal()));
        variables.put("descuento", Formato.moneda(reserva.getDescuento()));
        variables.put("hayDescuento", reserva.getDescuento().signum() > 0);
        variables.put("impuesto", Formato.moneda(reserva.getImpuesto()));
        variables.put("porcentajeImpuesto", Formato.decimal(configuracion.parametrosImporte().porcentajeImpuesto()));
        variables.put("total", Formato.moneda(reserva.getTotal()));
        variables.put("totalLetras", Formato.importeEnLetras(reserva.getTotal()));
        variables.put("estadoPago", comprobante.getEstadoPago());
        variables.put("pagado", "Pagado".equals(comprobante.getEstadoPago()));
        variables.put("metodoPago", reserva.getMetodoPago() == null ? "Por definir" : reserva.getMetodoPago().getEtiqueta());
        variables.put("fechaPago", reserva.getFechaPago() == null ? "" : Formato.fechaHora(reserva.getFechaPago()));
        variables.put("emision", Formato.fechaHora(comprobante.getFechaEmision()));
        return generadorPdf.generar("documentos/comprobante", variables);
    }

    private String logoEnBase64() {
        try (var entrada = new ClassPathResource("recursos/imagenes/logo-documento.png").getInputStream()) {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(entrada.readAllBytes());
        } catch (java.io.IOException excepcion) {
            throw new IllegalStateException("No se encontró el logotipo del comprobante", excepcion);
        }
    }
}
