package pe.edu.josepardo.hotel.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.edu.josepardo.hotel.configuracion.PropiedadesHotel;

@Component
public class ClienteFactiliza {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ClienteFactiliza.class);

    public record PersonaDni(String dni, String nombres, String apellidoPaterno, String apellidoMaterno) {

        public String apellidos() {
            return (apellidoPaterno + " " + apellidoMaterno).trim();
        }
    }

    public static class ServicioNoDisponible extends RuntimeException {
        public ServicioNoDisponible(String mensaje) {
            super(mensaje);
        }
    }

    private final PropiedadesHotel.Factiliza propiedades;
    private final ObjectMapper mapeador;
    private final HttpClient cliente;

    public ClienteFactiliza(PropiedadesHotel propiedades, ObjectMapper mapeador) {
        this.propiedades = propiedades.factiliza();
        this.mapeador = mapeador;
        this.cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public Optional<PersonaDni> consultar(String dni) {
        if (!propiedades.tieneToken()) {
            throw new ServicioNoDisponible("Token de Factiliza no configurado");
        }
        HttpRequest solicitud = HttpRequest.newBuilder(URI.create(propiedades.urlBase() + "/dni/info/" + dni))
                .header("Authorization", "Bearer " + propiedades.token())
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(propiedades.segundosEspera()))
                .GET()
                .build();
        try {
            HttpResponse<String> respuesta = cliente.send(solicitud, HttpResponse.BodyHandlers.ofString());
            return interpretar(respuesta.statusCode(), respuesta.body(), dni);
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            throw new ServicioNoDisponible("Consulta interrumpida");
        } catch (IOException excepcion) {
            REGISTRO.warn("No se pudo consultar Factiliza: {}", excepcion.getMessage());
            throw new ServicioNoDisponible("Sin conexión con Factiliza");
        }
    }

    private Optional<PersonaDni> interpretar(int estado, String cuerpo, String dni) throws IOException {
        if (estado == 400 || estado == 404) {
            return Optional.empty();
        }
        if (estado != 200) {
            REGISTRO.warn("Factiliza respondió con estado {}", estado);
            throw new ServicioNoDisponible("Respuesta inesperada de Factiliza");
        }
        JsonNode raiz = mapeador.readTree(cuerpo);
        JsonNode datos = raiz.path("data");
        if (!raiz.path("success").asBoolean(false) || datos.isMissingNode() || datos.isNull()) {
            return Optional.empty();
        }
        String nombres = datos.path("nombres").asText("").trim();
        if (nombres.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PersonaDni(
                dni,
                nombres,
                datos.path("apellido_paterno").asText("").trim(),
                datos.path("apellido_materno").asText("").trim()));
    }
}
