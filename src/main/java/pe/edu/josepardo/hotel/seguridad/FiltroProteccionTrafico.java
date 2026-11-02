package pe.edu.josepardo.hotel.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.josepardo.hotel.configuracion.PropiedadesHotel;
import pe.edu.josepardo.hotel.dto.RespuestaApi;

public class FiltroProteccionTrafico extends OncePerRequestFilter {

    private static final Logger REGISTRO = LoggerFactory.getLogger(FiltroProteccionTrafico.class);
    private static final String MENSAJE = "Recibimos demasiadas solicitudes desde tu conexión. Espera un momento e inténtalo de nuevo.";

    private final ControlTrafico control;
    private final ResolutorIpCliente resolutorIp;
    private final ConversorRespuestaJson conversor;
    private final PropiedadesHotel.Trafico propiedades;

    public FiltroProteccionTrafico(
            ControlTrafico control,
            ResolutorIpCliente resolutorIp,
            ConversorRespuestaJson conversor,
            PropiedadesHotel.Trafico propiedades) {
        this.control = control;
        this.resolutorIp = resolutorIp;
        this.conversor = conversor;
        this.propiedades = propiedades;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest solicitud) {
        String ruta = solicitud.getRequestURI().substring(solicitud.getContextPath().length());
        return !propiedades.habilitado() || ruta.startsWith("/recursos/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest solicitud, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String ruta = solicitud.getRequestURI().substring(solicitud.getContextPath().length());
        int limite = ruta.startsWith("/api/") ? propiedades.limiteApiPorMinuto() : propiedades.limitePorMinuto();
        String ip = resolutorIp.resolver(solicitud);
        ControlTrafico.Decision decision = control.evaluar(ip, limite);
        if (decision == ControlTrafico.Decision.PERMITIR) {
            cadena.doFilter(solicitud, respuesta);
            return;
        }
        if (decision == ControlTrafico.Decision.BLOQUEAR) {
            REGISTRO.warn("Tráfico rechazado por bloqueo temporal: {}", ip);
        }
        respuesta.setHeader("Retry-After", String.valueOf(control.segundosRestantes(ip)));
        conversor.escribir(respuesta, 429, RespuestaApi.error(MENSAJE, "TRAFICO_LIMITADO"));
    }
}
