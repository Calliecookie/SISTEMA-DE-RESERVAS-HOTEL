package pe.edu.josepardo.hotel.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.edu.josepardo.hotel.configuracion.PropiedadesHotel;

@Component
public class ClientePayPal {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ClientePayPal.class);

    public record OrdenCreada(String id, String urlAprobacion) {
    }

    public record CapturaOrden(String captureId, String estado, BigDecimal monto, String moneda) {
    }

    public static class ErrorPayPal extends RuntimeException {
        public ErrorPayPal(String mensaje) {
            super(mensaje);
        }
    }

    private final PropiedadesHotel.PayPal propiedades;
    private final ObjectMapper mapeador;
    private final HttpClient cliente;
    private String tokenAcceso;
    private Instant vencimientoToken = Instant.EPOCH;

    public ClientePayPal(PropiedadesHotel propiedades, ObjectMapper mapeador) {
        this.propiedades = propiedades.paypal();
        this.mapeador = mapeador;
        this.cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    }

    public OrdenCreada crearOrden(
            String referencia, BigDecimal monto, String moneda, String urlRetorno, String urlCancelacion, String idSolicitud) {
        ObjectNode cuerpo = mapeador.createObjectNode();
        cuerpo.put("intent", "CAPTURE");
        ObjectNode unidad = cuerpo.putArray("purchase_units").addObject();
        unidad.put("reference_id", referencia);
        ObjectNode importe = unidad.putObject("amount");
        importe.put("currency_code", moneda);
        importe.put("value", monto.setScale(2).toPlainString());
        ObjectNode contexto = cuerpo.putObject("payment_source").putObject("paypal").putObject("experience_context");
        contexto.put("return_url", urlRetorno);
        contexto.put("cancel_url", urlCancelacion);
        contexto.put("user_action", "PAY_NOW");

        JsonNode respuesta = enviar("/v2/checkout/orders", cuerpo.toString(), idSolicitud);
        String id = respuesta.path("id").asText("");
        String urlAprobacion = null;
        for (JsonNode enlace : respuesta.path("links")) {
            if ("payer-action".equals(enlace.path("rel").asText())) {
                urlAprobacion = enlace.path("href").asText();
            }
        }
        if (id.isEmpty() || urlAprobacion == null) {
            throw new ErrorPayPal("PayPal no devolvió el enlace de aprobación");
        }
        return new OrdenCreada(id, urlAprobacion);
    }

    public CapturaOrden capturarOrden(String ordenId, String idSolicitud) {
        JsonNode respuesta = enviar("/v2/checkout/orders/" + ordenId + "/capture", "{}", idSolicitud);
        JsonNode captura = respuesta.path("purchase_units").path(0).path("payments").path("captures").path(0);
        if (captura.isMissingNode()) {
            throw new ErrorPayPal("La respuesta de PayPal no incluye la captura");
        }
        return new CapturaOrden(
                captura.path("id").asText(""),
                captura.path("status").asText(""),
                new BigDecimal(captura.path("amount").path("value").asText("0")),
                captura.path("amount").path("currency_code").asText(""));
    }

    private JsonNode enviar(String ruta, String cuerpo, String idSolicitud) {
        HttpRequest solicitud = HttpRequest.newBuilder(URI.create(propiedades.urlApi() + ruta))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + obtenerToken())
                .header("PayPal-Request-Id", idSolicitud)
                .timeout(Duration.ofSeconds(propiedades.segundosEspera()))
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
        HttpResponse<String> respuesta = ejecutar(solicitud);
        if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
            REGISTRO.warn("PayPal respondió {} en {}: {}", respuesta.statusCode(), ruta, respuesta.body());
            throw new ErrorPayPal("PayPal rechazó la operación");
        }
        return leer(respuesta.body());
    }

    private synchronized String obtenerToken() {
        if (tokenAcceso != null && Instant.now().isBefore(vencimientoToken)) {
            return tokenAcceso;
        }
        String credenciales = Base64.getEncoder().encodeToString(
                (propiedades.clienteId() + ":" + propiedades.secreto()).getBytes(StandardCharsets.UTF_8));
        HttpRequest solicitud = HttpRequest.newBuilder(URI.create(propiedades.urlApi() + "/v1/oauth2/token"))
                .header("Authorization", "Basic " + credenciales)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(propiedades.segundosEspera()))
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();
        HttpResponse<String> respuesta = ejecutar(solicitud);
        if (respuesta.statusCode() != 200) {
            REGISTRO.warn("PayPal rechazó las credenciales con estado {}", respuesta.statusCode());
            throw new ErrorPayPal("No se pudo autenticar con PayPal");
        }
        JsonNode datos = leer(respuesta.body());
        tokenAcceso = datos.path("access_token").asText();
        vencimientoToken = Instant.now().plusSeconds(Math.max(datos.path("expires_in").asLong(0) - 60, 0));
        return tokenAcceso;
    }

    private HttpResponse<String> ejecutar(HttpRequest solicitud) {
        try {
            return cliente.send(solicitud, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            throw new ErrorPayPal("Operación interrumpida");
        } catch (IOException excepcion) {
            REGISTRO.warn("Sin conexión con PayPal: {}", excepcion.getMessage());
            throw new ErrorPayPal("Sin conexión con PayPal");
        }
    }

    private JsonNode leer(String cuerpo) {
        try {
            return mapeador.readTree(cuerpo);
        } catch (IOException excepcion) {
            throw new ErrorPayPal("Respuesta de PayPal no válida");
        }
    }
}
