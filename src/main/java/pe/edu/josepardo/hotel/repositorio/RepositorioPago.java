package pe.edu.josepardo.hotel.repositorio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.josepardo.hotel.modelo.EstadoPago;
import pe.edu.josepardo.hotel.modelo.MetodoPago;
import pe.edu.josepardo.hotel.modelo.Pago;

public interface RepositorioPago extends JpaRepository<Pago, Long> {

    @Query("select p from Pago p left join fetch p.confirmadoPor where p.reserva.id = :reservaId order by p.creadoEn desc, p.id desc")
    List<Pago> listarPorReserva(@Param("reservaId") Long reservaId);

    @Query("select p from Pago p where p.reserva.id = :reservaId and p.estado = :estado order by p.id desc")
    List<Pago> listarPorReservaYEstado(@Param("reservaId") Long reservaId, @Param("estado") EstadoPago estado);

    @Query("""
            select p from Pago p
            where p.reserva.codigo = :codigo and p.estado = :estado
            order by p.id desc
            """)
    List<Pago> listarPorCodigoYEstado(@Param("codigo") String codigo, @Param("estado") EstadoPago estado);

    @Query("""
            select case when count(p) > 0 then true else false end from Pago p
            where p.metodo = :metodo and p.numeroOperacion = :numero
              and p.estado in (pe.edu.josepardo.hotel.modelo.EstadoPago.DECLARADO,
                               pe.edu.josepardo.hotel.modelo.EstadoPago.CONFIRMADO)
            """)
    boolean existeOperacionVigente(@Param("metodo") MetodoPago metodo, @Param("numero") String numero);

    default Optional<Pago> buscarVigentePorCodigo(String codigo, EstadoPago estado) {
        return listarPorCodigoYEstado(codigo, estado).stream().findFirst();
    }

    default Optional<Pago> buscarVigente(Long reservaId, EstadoPago estado) {
        return listarPorReservaYEstado(reservaId, estado).stream().findFirst();
    }
}
