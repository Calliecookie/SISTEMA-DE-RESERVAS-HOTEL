package pe.edu.josepardo.hotel.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.josepardo.hotel.dto.RespuestaApi;

public class FiltroLimiteAcceso extends OncePerRequestFilter {

    private static final int MAXIMO_POR_IP = 30;
    private static final Duration VENTANA = Duration.ofMinutes(10);

    private final LimitadorSolicitudes limitador;
    private final ConversorRespuestaJson conversor;
    private final ResolutorIpCliente resolutorIp;

    public FiltroLimiteAcceso(
            LimitadorSolicitudes limitador, ConversorRespuestaJson conversor, ResolutorIpCliente resolutorIp) {
        this.limitador = limitador;
        this.conversor = conversor;
        this.resolutorIp = resolutorIp;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest solicitud) {
        String ruta = solicitud.getRequestURI().substring(solicitud.getContextPath().length());
        return !("POST".equals(solicitud.getMethod()) && "/ingresar".equals(ruta));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest solicitud, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        if (!limitador.permitir("acceso:" + resolutorIp.resolver(solicitud), MAXIMO_POR_IP, VENTANA)) {
            conversor.escribir(respuesta, 429, RespuestaApi.error(
                    "Realizaste demasiados intentos. Espera unos minutos y vuelve a intentarlo.",
                    "DEMASIADAS_SOLICITUDES"));
            return;
        }
        cadena.doFilter(solicitud, respuesta);
    }
}
