package pe.edu.josepardo.hotel.integracion;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

final class ServidorSimulado {

    record Respuesta(int estado, String cuerpo) {
    }

    private final HttpServer servidor;
    private final Map<String, Respuesta> respuestas = new ConcurrentHashMap<>();
    private final List<String> solicitudes = new CopyOnWriteArrayList<>();
    private volatile String ultimoCuerpo = "";
    private volatile String ultimaAutorizacion = "";

    ServidorSimulado(int puerto) {
        try {
            servidor = HttpServer.create(new InetSocketAddress("localhost", puerto), 0);
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
        servidor.createContext("/", intercambio -> {
            String clave = intercambio.getRequestMethod() + " " + intercambio.getRequestURI().getPath();
            solicitudes.add(clave);
            ultimoCuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            ultimaAutorizacion = String.valueOf(intercambio.getRequestHeaders().getFirst("Authorization"));
            Respuesta respuesta = respuestas.getOrDefault(clave, new Respuesta(404, "{}"));
            byte[] bytes = respuesta.cuerpo().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(respuesta.estado(), bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        servidor.start();
    }

    void responder(String metodoYRuta, int estado, String cuerpo) {
        respuestas.put(metodoYRuta, new Respuesta(estado, cuerpo));
    }

    void reiniciar() {
        respuestas.clear();
        solicitudes.clear();
        ultimoCuerpo = "";
        ultimaAutorizacion = "";
    }

    List<String> solicitudes() {
        return solicitudes;
    }

    String ultimoCuerpo() {
        return ultimoCuerpo;
    }

    String ultimaAutorizacion() {
        return ultimaAutorizacion;
    }
}
