package pe.edu.josepardo.hotel.tarea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.edu.josepardo.hotel.servicio.ServicioExpiracion;

@Component
@ConditionalOnProperty(prefix = "hotel.tareas", name = "habilitadas", havingValue = "true", matchIfMissing = true)
public class TareaExpiracionReservas {

    private static final Logger REGISTRO = LoggerFactory.getLogger(TareaExpiracionReservas.class);

    private final ServicioExpiracion servicioExpiracion;

    public TareaExpiracionReservas(ServicioExpiracion servicioExpiracion) {
        this.servicioExpiracion = servicioExpiracion;
    }

    @Scheduled(fixedDelayString = "${hotel.tareas.milisegundos-expiracion:60000}", initialDelay = 30000)
    public void ejecutar() {
        int expiradas = servicioExpiracion.expirarVencidas();
        if (expiradas > 0) {
            REGISTRO.info("Reservas expiradas: {}", expiradas);
        }
    }
}
