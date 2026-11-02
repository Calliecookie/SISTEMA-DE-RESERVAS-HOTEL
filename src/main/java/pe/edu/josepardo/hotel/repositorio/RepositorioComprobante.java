package pe.edu.josepardo.hotel.repositorio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.josepardo.hotel.modelo.Comprobante;

public interface RepositorioComprobante extends JpaRepository<Comprobante, Long> {

    @Query("select c from Comprobante c where c.reserva.id = :reservaId order by c.fechaEmision desc, c.id desc")
    List<Comprobante> listarPorReserva(@Param("reservaId") Long reservaId);

    @Query("""
            select c from Comprobante c
            join fetch c.reserva r
            join fetch r.cliente cl
            join fetch cl.usuario
            where c.numero = :numero
            """)
    Optional<Comprobante> buscarPorNumero(@Param("numero") String numero);

    @Query("select count(c) from Comprobante c where c.reserva.id = :reservaId")
    long contarPorReserva(@Param("reservaId") Long reservaId);
}
