package pe.edu.josepardo.hotel.servicio;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import pe.edu.josepardo.hotel.configuracion.PropiedadesHotel;
import pe.edu.josepardo.hotel.modelo.Comprobante;
import pe.edu.josepardo.hotel.modelo.EstadoEnvioCorreo;
import pe.edu.josepardo.hotel.repositorio.RepositorioComprobante;
import pe.edu.josepardo.hotel.repositorio.RepositorioReserva;

@Service
public class ServicioCorreo {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ServicioCorreo.class);

    private record DatosEnvio(String destino, String nombre, String numero, String codigoReserva, String archivo) {
    }

    private final ObjectProvider<JavaMailSender> proveedorRemitente;
    private final SpringTemplateEngine motor;
    private final TaskExecutor ejecutor;
    private final PropiedadesHotel.Correo propiedades;
    private final ServicioConfiguracion configuracion;
    private final RepositorioComprobante repositorioComprobante;
    private final RepositorioReserva repositorioReserva;
    private final ServicioAlmacenamiento almacenamiento;
    private final TransactionTemplate transaccion;

    public ServicioCorreo(
            ObjectProvider<JavaMailSender> proveedorRemitente,
            SpringTemplateEngine motor,
            @Qualifier("ejecutorCorreo") TaskExecutor ejecutor,
            PropiedadesHotel propiedades,
            ServicioConfiguracion configuracion,
            RepositorioComprobante repositorioComprobante,
            RepositorioReserva repositorioReserva,
            ServicioAlmacenamiento almacenamiento,
            PlatformTransactionManager gestorTransacciones) {
        this.proveedorRemitente = proveedorRemitente;
        this.motor = motor;
        this.ejecutor = ejecutor;
        this.propiedades = propiedades.correo();
        this.configuracion = configuracion;
        this.repositorioComprobante = repositorioComprobante;
        this.repositorioReserva = repositorioReserva;
        this.almacenamiento = almacenamiento;
        this.transaccion = new TransactionTemplate(gestorTransacciones);
        this.transaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public boolean estaHabilitado() {
        return propiedades.habilitado()
                && propiedades.remitente() != null
                && !propiedades.remitente().isBlank()
                && proveedorRemitente.getIfAvailable() != null;
    }

    public void enviarCodigoRecuperacion(String destino, String nombre, String codigo, int minutos) {
        if (!estaHabilitado()) {
            REGISTRO.warn("Correo desactivado: no se envió el código de recuperación");
            return;
        }
        String hotel = configuracion.texto(ServicioConfiguracion.HOTEL_NOMBRE);
        String cuerpo = renderizar("correo/codigo-recuperacion", Map.of(
                "nombre", nombre, "codigo", codigo, "minutos", minutos, "hotel", hotel));
        ejecutor.execute(() -> {
            try {
                enviar(destino, "Tu código de recuperación · " + hotel, cuerpo, null, null);
            } catch (MessagingException | MailException | UnsupportedEncodingException excepcion) {
                REGISTRO.error("No se pudo enviar el código de recuperación: {}", excepcion.getMessage());
            }
        });
    }

    public void notificarCambioEstado(Long reservaId, String estadoEtiqueta, String motivo) {
        if (!estaHabilitado()) {
            return;
        }
        String[] datos = transaccion.execute(estado -> repositorioReserva.findById(reservaId)
                .map(reserva -> new String[] {
                    reserva.getCliente().getUsuario().getCorreo(),
                    reserva.getCliente().getUsuario().getNombres(),
                    reserva.getCodigo()})
                .orElse(null));
        if (datos == null) {
            return;
        }
        String hotel = configuracion.texto(ServicioConfiguracion.HOTEL_NOMBRE);
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", datos[1]);
        variables.put("codigo", datos[2]);
        variables.put("estado", estadoEtiqueta);
        variables.put("motivo", motivo);
        variables.put("hotel", hotel);
        String cuerpo = renderizar("correo/cambio-estado", variables);
        ejecutor.execute(() -> {
            try {
                enviar(datos[0], "Tu reserva " + datos[2] + " está " + estadoEtiqueta.toLowerCase() + " · " + hotel, cuerpo, null, null);
            } catch (MessagingException | MailException | UnsupportedEncodingException excepcion) {
                REGISTRO.error("No se pudo avisar el cambio de estado de {}: {}", datos[2], excepcion.getMessage());
            }
        });
    }

    public void enviarComprobante(Long comprobanteId) {
        if (!estaHabilitado()) {
            registrarResultado(comprobanteId, EstadoEnvioCorreo.OMITIDO, "El envío de correo está desactivado.");
            return;
        }
        DatosEnvio datos = transaccion.execute(estado -> {
            Comprobante comprobante = repositorioComprobante.findById(comprobanteId).orElseThrow();
            return new DatosEnvio(
                    comprobante.getCorreoDestino(),
                    comprobante.getReserva().getCliente().getUsuario().getNombres(),
                    comprobante.getNumero(),
                    comprobante.getReserva().getCodigo(),
                    comprobante.getRutaArchivo());
        });
        ejecutor.execute(() -> entregarComprobante(comprobanteId, datos));
    }

    private void entregarComprobante(Long comprobanteId, DatosEnvio datos) {
        try {
            String hotel = configuracion.texto(ServicioConfiguracion.HOTEL_NOMBRE);
            String cuerpo = renderizar("correo/comprobante", Map.of(
                    "nombre", datos.nombre(),
                    "codigo", datos.codigoReserva(),
                    "numero", datos.numero(),
                    "hotel", hotel));
            byte[] archivo = almacenamiento.leerComprobante(datos.archivo());
            enviar(datos.destino(), "Comprobante de reserva " + datos.codigoReserva() + " · " + hotel, cuerpo,
                    datos.numero() + ".pdf", archivo);
            registrarResultado(comprobanteId, EstadoEnvioCorreo.ENVIADO, null);
        } catch (MessagingException | MailException | UnsupportedEncodingException | IllegalStateException excepcion) {
            REGISTRO.error("No se pudo enviar el comprobante {}: {}", datos.numero(), excepcion.getMessage());
            registrarResultado(comprobanteId, EstadoEnvioCorreo.FALLIDO, "No se pudo entregar el correo.");
        }
    }

    private void enviar(String destino, String asunto, String html, String nombreAdjunto, byte[] adjunto)
            throws MessagingException, UnsupportedEncodingException {
        JavaMailSender remitente = proveedorRemitente.getObject();
        MimeMessage mensaje = remitente.createMimeMessage();
        MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, adjunto != null, "UTF-8");
        ayudante.setFrom(propiedades.remitente(), propiedades.nombreRemitente());
        ayudante.setTo(destino);
        ayudante.setSubject(asunto);
        ayudante.setText(html, true);
        if (adjunto != null) {
            ayudante.addAttachment(nombreAdjunto, new ByteArrayResource(adjunto), "application/pdf");
        }
        remitente.send(mensaje);
    }

    private String renderizar(String plantilla, Map<String, Object> variables) {
        Context contexto = new Context(Locale.of("es", "PE"));
        contexto.setVariables(variables);
        return motor.process(plantilla, contexto);
    }

    private void registrarResultado(Long comprobanteId, EstadoEnvioCorreo estado, String detalle) {
        transaccion.executeWithoutResult(transaccionActual -> repositorioComprobante.findById(comprobanteId)
                .ifPresent(comprobante -> {
                    comprobante.setCorreoEstado(estado);
                    comprobante.setCorreoDetalle(detalle);
                    comprobante.setCorreoFecha(LocalDateTime.now());
                }));
    }
}
