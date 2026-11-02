package pe.edu.josepardo.hotel.repositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.josepardo.hotel.modelo.DetalleReserva;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;

public interface RepositorioDetalleReserva extends JpaRepository<DetalleReserva, Long> {

    @Query("""
            select distinct d.habitacion.id from DetalleReserva d
            join d.reserva r
            where d.habitacion.id in :habitacionIds
              and r.fechaEntrada < :fechaSalida
              and r.fechaSalida > :fechaEntrada
              and (r.estado in :estadosBloqueantes
                   or (r.estado = :estadoPendiente and r.expiraEn > :ahora))
            """)
    List<Long> buscarHabitacionesOcupadas(
            @Param("habitacionIds") Collection<Long> habitacionIds,
            @Param("fechaEntrada") LocalDate fechaEntrada,
            @Param("fechaSalida") LocalDate fechaSalida,
            @Param("estadosBloqueantes") Collection<EstadoReserva> estadosBloqueantes,
            @Param("estadoPendiente") EstadoReserva estadoPendiente,
            @Param("ahora") LocalDateTime ahora);

    @Query("""
            select d from DetalleReserva d
            join fetch d.habitacion h
            join fetch h.categoria
            where d.reserva.id = :reservaId
            order by h.numero
            """)
    List<DetalleReserva> listarPorReserva(@Param("reservaId") Long reservaId);

    @Query("select count(d) from DetalleReserva d where d.habitacion.id = :habitacionId")
    long contarPorHabitacion(@Param("habitacionId") Long habitacionId);
}
