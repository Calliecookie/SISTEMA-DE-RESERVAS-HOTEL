package pe.edu.josepardo.hotel.configuracion;

import java.util.concurrent.Semaphore;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.Scheduled;
import pe.edu.josepardo.hotel.seguridad.ControlTrafico;
import pe.edu.josepardo.hotel.seguridad.ConversorRespuestaJson;
import pe.edu.josepardo.hotel.seguridad.FiltroProteccionTrafico;
import pe.edu.josepardo.hotel.seguridad.ResolutorIpCliente;

@Configuration
public class ConfiguracionProteccionTrafico {

    private ControlTrafico control;

    @Bean
    public ControlTrafico controlTrafico(PropiedadesHotel propiedades) {
        PropiedadesHotel.Trafico trafico = propiedades.trafico();
        control = new ControlTrafico(
                trafico.excesosParaBloquear(),
                trafico.minutosBloqueo(),
                trafico.maximoIpsSeguidas(),
                System::currentTimeMillis);
        return control;
    }

    @Bean
    public Semaphore cupoExportaciones(PropiedadesHotel propiedades) {
        return new Semaphore(propiedades.trafico().exportacionesSimultaneas());
    }

    @Bean
    public FilterRegistrationBean<FiltroProteccionTrafico> registroFiltroTrafico(
            ControlTrafico controlTrafico,
            ResolutorIpCliente resolutorIp,
            ConversorRespuestaJson conversor,
            PropiedadesHotel propiedades) {
        FilterRegistrationBean<FiltroProteccionTrafico> registro = new FilterRegistrationBean<>(
                new FiltroProteccionTrafico(controlTrafico, resolutorIp, conversor, propiedades.trafico()));
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registro.addUrlPatterns("/*");
        return registro;
    }

    @Scheduled(fixedDelay = 300000)
    public void limpiarControlTrafico() {
        if (control != null) {
            control.limpiar();
        }
    }
}
