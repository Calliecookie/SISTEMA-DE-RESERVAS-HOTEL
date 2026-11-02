package pe.edu.josepardo.hotel.repositorio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.josepardo.hotel.modelo.HistorialEstadoReserva;

public interface RepositorioHistorialEstado extends JpaRepository<HistorialEstadoReserva, Long> {

    @Query("select h from HistorialEstadoReserva h left join fetch h.usuario where h.reserva.id = :reservaId order by h.creadoEn, h.id")
    List<HistorialEstadoReserva> listarPorReserva(@Param("reservaId") Long reservaId);
}
