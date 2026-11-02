package pe.edu.josepardo.hotel.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import pe.edu.josepardo.hotel.dto.RespuestaApi;

@Component
public class ConversorRespuestaJson {

    private final ObjectMapper mapeador;

    public ConversorRespuestaJson(ObjectMapper mapeador) {
        this.mapeador = mapeador;
    }

    public boolean esperaJson(HttpServletRequest solicitud) {
        String ruta = solicitud.getRequestURI().substring(solicitud.getContextPath().length());
        return ruta.startsWith("/api/") || "fetch".equals(solicitud.getHeader("X-Requested-With"));
    }

    public void escribir(HttpServletResponse respuesta, int estado, RespuestaApi<?> cuerpo) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapeador.writeValue(respuesta.getWriter(), cuerpo);
    }
}
