package pe.edu.josepardo.hotel.repositorio;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.modelo.Reserva;

public interface RepositorioReserva extends JpaRepository<Reserva, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.codigo = :codigo")
    Optional<Reserva> bloquearPorCodigo(@Param("codigo") String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> bloquearPorId(@Param("id") Long id);

    @Query("""
            select r from Reserva r
            join fetch r.cliente c
            join fetch c.usuario
            where r.codigo = :codigo
            """)
    Optional<Reserva> buscarPorCodigo(@Param("codigo") String codigo);

    @Query("""
            select r from Reserva r
            join fetch r.cliente c
            join fetch c.usuario
            where r.id = :id
            """)
    Optional<Reserva> buscarCompletaPorId(@Param("id") Long id);

    @Query("select r.id from Reserva r where r.estado = :estado and r.expiraEn <= :ahora")
    List<Long> buscarIdsVencidas(@Param("estado") EstadoReserva estado, @Param("ahora") LocalDateTime ahora);

    @Query("select case when count(r) > 0 then true else false end from Reserva r where r.codigo = :codigo")
    boolean existeCodigo(@Param("codigo") String codigo);
}
