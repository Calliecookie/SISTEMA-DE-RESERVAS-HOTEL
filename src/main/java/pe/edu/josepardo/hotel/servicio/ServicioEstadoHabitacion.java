package pe.edu.josepardo.hotel.servicio;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.modelo.EstadoHabitacion;

@Service
public class ServicioEstadoHabitacion {

    private static final String ACTUALIZAR = """
            UPDATE habitaciones h
               SET h.estado = CASE
                     WHEN EXISTS (
                       SELECT 1 FROM detalle_reservas d JOIN reservas r ON r.id = d.reserva_id
                        WHERE d.habitacion_id = h.id AND r.estado = 'ENTREGADO') THEN 'OCUPADA'
                     WHEN EXISTS (
                       SELECT 1 FROM detalle_reservas d JOIN reservas r ON r.id = d.reserva_id
                        WHERE d.habitacion_id = h.id AND r.estado IN ('PAGADO', 'CONFIRMADO')
                          AND r.fecha_salida > :hoy) THEN 'RESERVADA'
                     ELSE 'DISPONIBLE' END
             WHERE h.estado IN ('DISPONIBLE', 'RESERVADA', 'OCUPADA')
            """;

    @PersistenceContext
    private EntityManager gestor;

    @Transactional
    public int recalcular(Collection<Long> habitacionIds) {
        if (habitacionIds == null || habitacionIds.isEmpty()) {
            return 0;
        }
        gestor.flush();
        Query consulta = gestor.createNativeQuery(ACTUALIZAR + " AND h.id IN (:ids)")
                .setParameter("hoy", LocalDate.now())
                .setParameter("ids", habitacionIds);
        return consulta.executeUpdate();
    }

    @Transactional(readOnly = true)
    public EstadoHabitacion estadoActual(Long habitacionId) {
        Object valor = gestor.createNativeQuery("SELECT estado FROM habitaciones WHERE id = :id")
                .setParameter("id", habitacionId)
                .getSingleResult();
        return EstadoHabitacion.valueOf(valor.toString());
    }

    @Transactional
    public int recalcularTodas() {
        gestor.flush();
        return gestor.createNativeQuery(ACTUALIZAR).setParameter("hoy", LocalDate.now()).executeUpdate();
    }
}
