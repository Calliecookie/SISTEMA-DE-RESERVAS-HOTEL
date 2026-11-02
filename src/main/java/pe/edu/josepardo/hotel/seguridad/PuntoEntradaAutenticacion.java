package pe.edu.josepardo.hotel.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import pe.edu.josepardo.hotel.dto.RespuestaApi;

@Component
public class PuntoEntradaAutenticacion implements AuthenticationEntryPoint {

    private final ConversorRespuestaJson conversor;

    public PuntoEntradaAutenticacion(ConversorRespuestaJson conversor) {
        this.conversor = conversor;
    }

    @Override
    public void commence(
            HttpServletRequest solicitud, HttpServletResponse respuesta, AuthenticationException excepcion)
            throws IOException {
        if (conversor.esperaJson(solicitud)) {
            conversor.escribir(respuesta, HttpServletResponse.SC_UNAUTHORIZED,
                    RespuestaApi.error("Inicia sesión para continuar.", "SESION_REQUERIDA"));
            return;
        }
        String contexto = solicitud.getContextPath();
        String ruta = solicitud.getRequestURI().substring(contexto.length());
        respuesta.sendRedirect(contexto + "/ingresar?destino=" + java.net.URLEncoder.encode(ruta, java.nio.charset.StandardCharsets.UTF_8));
    }
}
