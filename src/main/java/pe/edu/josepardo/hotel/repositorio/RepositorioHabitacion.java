package pe.edu.josepardo.hotel.repositorio;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.josepardo.hotel.modelo.Habitacion;

public interface RepositorioHabitacion extends JpaRepository<Habitacion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habitacion h where h.id in :ids order by h.id")
    List<Habitacion> bloquearPorIds(@Param("ids") Collection<Long> ids);

    @Query("select distinct h from Habitacion h join fetch h.categoria left join fetch h.caracteristicas where h.id = :id")
    Optional<Habitacion> buscarCompleta(@Param("id") Long id);

    @Query("select distinct h from Habitacion h join fetch h.categoria left join fetch h.caracteristicas order by h.numero")
    List<Habitacion> listarCompletas();

    @Query("select h from Habitacion h join fetch h.categoria where h.id in :ids")
    List<Habitacion> buscarConCategoria(@Param("ids") Collection<Long> ids);

    @Query("select case when count(h) > 0 then true else false end from Habitacion h where lower(h.numero) = lower(:numero)")
    boolean existeNumero(@Param("numero") String numero);

    @Query("select case when count(h) > 0 then true else false end from Habitacion h where lower(h.numero) = lower(:numero) and h.id <> :id")
    boolean existeNumeroEnOtra(@Param("numero") String numero, @Param("id") Long id);

    @Query("select count(h) from Habitacion h where h.categoria.id = :categoriaId")
    long contarPorCategoria(@Param("categoriaId") Long categoriaId);
}
