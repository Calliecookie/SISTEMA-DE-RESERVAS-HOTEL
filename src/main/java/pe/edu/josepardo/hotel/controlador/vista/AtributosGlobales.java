package pe.edu.josepardo.hotel.controlador.vista;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import pe.edu.josepardo.hotel.dto.HotelDto;
import pe.edu.josepardo.hotel.servicio.ServicioConfiguracion;

@ControllerAdvice(assignableTypes = ControladorVistas.class)
public class AtributosGlobales {

    private final ServicioConfiguracion configuracion;

    public AtributosGlobales(ServicioConfiguracion configuracion) {
        this.configuracion = configuracion;
    }

    @ModelAttribute("hotel")
    public HotelDto hotel() {
        return configuracion.hotel();
    }
}
