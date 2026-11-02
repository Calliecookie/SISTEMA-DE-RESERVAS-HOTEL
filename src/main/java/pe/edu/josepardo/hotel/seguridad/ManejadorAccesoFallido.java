package pe.edu.josepardo.hotel.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import pe.edu.josepardo.hotel.dto.RespuestaApi;
import pe.edu.josepardo.hotel.servicio.ServicioIntentosAcceso;

@Component
public class ManejadorAccesoFallido implements AuthenticationFailureHandler {

    private static final String MENSAJE_GENERICO = "Correo o contraseña incorrectos.";
    private static final String MENSAJE_BLOQUEO =
            "No fue posible iniciar sesión por demasiados intentos. Espera unos minutos o recupera tu contraseña.";

    private final ServicioIntentosAcceso servicioIntentos;
    private final ConversorRespuestaJson conversor;

    public ManejadorAccesoFallido(ServicioIntentosAcceso servicioIntentos, ConversorRespuestaJson conversor) {
        this.servicioIntentos = servicioIntentos;
        this.conversor = conversor;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest solicitud, HttpServletResponse respuesta, AuthenticationException excepcion)
            throws IOException {
        boolean bloqueado = excepcion instanceof LockedException;
        if (!bloqueado && !(excepcion instanceof DisabledException)) {
            bloqueado = servicioIntentos.registrarFallo(solicitud.getParameter("correo"));
        }
        String mensaje = bloqueado ? MENSAJE_BLOQUEO : MENSAJE_GENERICO;
        if (conversor.esperaJson(solicitud)) {
            conversor.escribir(respuesta, HttpServletResponse.SC_UNAUTHORIZED,
                    RespuestaApi.error(mensaje, bloqueado ? "ACCESO_LIMITADO" : "CREDENCIALES_INVALIDAS"));
            return;
        }
        respuesta.sendRedirect(solicitud.getContextPath() + "/ingresar?error");
    }
}
