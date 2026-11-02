package pe.edu.josepardo.hotel.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import pe.edu.josepardo.hotel.dto.RespuestaApi;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.servicio.ServicioIntentosAcceso;

@Component
public class ManejadorAccesoExitoso implements AuthenticationSuccessHandler {

    private final ServicioIntentosAcceso servicioIntentos;
    private final ConversorRespuestaJson conversor;

    public ManejadorAccesoExitoso(ServicioIntentosAcceso servicioIntentos, ConversorRespuestaJson conversor) {
        this.servicioIntentos = servicioIntentos;
        this.conversor = conversor;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest solicitud, HttpServletResponse respuesta, Authentication autenticacion)
            throws IOException {
        UsuarioAutenticado usuario = (UsuarioAutenticado) autenticacion.getPrincipal();
        servicioIntentos.registrarExito(usuario.getId());
        String destino = resolverDestino(solicitud.getParameter("destino"), usuario.getRol(), solicitud.getContextPath());
        if (conversor.esperaJson(solicitud)) {
            conversor.escribir(respuesta, HttpServletResponse.SC_OK,
                    RespuestaApi.exito("Bienvenido, " + usuario.getNombres() + ".", Map.of("destino", destino)));
            return;
        }
        respuesta.sendRedirect(destino);
    }

    static String resolverDestino(String solicitado, Rol rol, String contexto) {
        boolean seguro = solicitado != null
                && solicitado.startsWith("/")
                && !solicitado.startsWith("//")
                && !solicitado.contains("\\")
                && !solicitado.contains(":");
        if (rol == Rol.CLIENTE && seguro) {
            return contexto + solicitado;
        }
        return contexto + switch (rol) {
            case ADMINISTRADOR -> "/administracion";
            case TRABAJADOR -> "/trabajador";
            case CLIENTE -> "/mi-cuenta";
        };
    }
}
