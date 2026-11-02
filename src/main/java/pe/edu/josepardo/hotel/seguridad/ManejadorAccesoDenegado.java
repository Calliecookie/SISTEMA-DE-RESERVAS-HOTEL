package pe.edu.josepardo.hotel.seguridad;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import pe.edu.josepardo.hotel.dto.RespuestaApi;

@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    private final ConversorRespuestaJson conversor;

    public ManejadorAccesoDenegado(ConversorRespuestaJson conversor) {
        this.conversor = conversor;
    }

    @Override
    public void handle(
            HttpServletRequest solicitud, HttpServletResponse respuesta, AccessDeniedException excepcion)
            throws IOException, ServletException {
        boolean csrf = excepcion instanceof CsrfException;
        if (conversor.esperaJson(solicitud)) {
            String mensaje = csrf
                    ? "Tu sesión caducó. Recarga la página e inténtalo de nuevo."
                    : "No tienes permiso para realizar esta acción.";
            conversor.escribir(respuesta, HttpServletResponse.SC_FORBIDDEN,
                    RespuestaApi.error(mensaje, csrf ? "CSRF_INVALIDO" : "ACCESO_DENEGADO"));
            return;
        }
        respuesta.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
