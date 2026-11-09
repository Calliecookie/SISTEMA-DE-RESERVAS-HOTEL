package pe.edu.josepardo.hotel.tarea;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.edu.josepardo.hotel.servicio.ServicioCierreDiario;
import pe.edu.josepardo.hotel.servicio.ServicioEstadoHabitacion;

@Component
@ConditionalOnProperty(prefix = "hotel.tareas", name = "habilitadas", havingValue = "true", matchIfMissing = true)
public class TareaCierreDiario {

    private final ServicioCierreDiario servicioCierre;
    private final ServicioEstadoHabitacion estadoHabitacion;

    public TareaCierreDiario(ServicioCierreDiario servicioCierre, ServicioEstadoHabitacion estadoHabitacion) {
        this.servicioCierre = servicioCierre;
        this.estadoHabitacion = estadoHabitacion;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        estadoHabitacion.recalcularTodas();
    }

    @Scheduled(cron = "${hotel.tareas.cron-cierre-dia:0 59 23 * * *}")
    public void ejecutar() {
        servicioCierre.ejecutar();
    }
}
