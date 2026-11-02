package pe.edu.josepardo.hotel.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import pe.edu.josepardo.hotel.configuracion.PropiedadesHotel;

@Component
public class ResolutorIpCliente {

    private final Set<String> proxiesConfiables;

    public ResolutorIpCliente(PropiedadesHotel propiedades) {
        String lista = propiedades.trafico().proxiesConfiables();
        this.proxiesConfiables = lista == null ? Set.of() : Arrays.stream(lista.split(","))
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public String resolver(HttpServletRequest solicitud) {
        return resolver(solicitud.getRemoteAddr(), solicitud.getHeader("X-Forwarded-For"), proxiesConfiables);
    }

    static String resolver(String direccionRemota, String reenviadoPara, Set<String> confiables) {
        if (!confiables.contains(direccionRemota) || reenviadoPara == null || reenviadoPara.isBlank()) {
            return ControlTrafico.normalizarIp(direccionRemota);
        }
        String[] saltos = reenviadoPara.split(",");
        for (int i = saltos.length - 1; i >= 0; i--) {
            String salto = saltos[i].trim();
            if (!salto.isEmpty() && !confiables.contains(salto)) {
                return ControlTrafico.normalizarIp(salto);
            }
        }
        return ControlTrafico.normalizarIp(direccionRemota);
    }
}
