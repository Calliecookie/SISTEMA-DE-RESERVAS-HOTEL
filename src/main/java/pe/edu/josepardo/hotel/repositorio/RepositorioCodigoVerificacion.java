package pe.edu.josepardo.hotel.repositorio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.josepardo.hotel.modelo.CodigoVerificacion;
import pe.edu.josepardo.hotel.modelo.PropositoCodigo;

public interface RepositorioCodigoVerificacion extends JpaRepository<CodigoVerificacion, Long> {

    @Modifying
    @Query("""
            update CodigoVerificacion c set c.invalidadoEn = :ahora
            where c.usuario.id = :usuarioId and c.proposito = :proposito
              and c.usadoEn is null and c.invalidadoEn is null
            """)
    int invalidarAnteriores(
            @Param("usuarioId") Long usuarioId,
            @Param("proposito") PropositoCodigo proposito,
            @Param("ahora") LocalDateTime ahora);

    @Query("""
            select c from CodigoVerificacion c
            where c.usuario.id = :usuarioId and c.proposito = :proposito
              and c.usadoEn is null and c.invalidadoEn is null
            order by c.id desc
            """)
    List<CodigoVerificacion> listarAbiertos(
            @Param("usuarioId") Long usuarioId, @Param("proposito") PropositoCodigo proposito);

    default Optional<CodigoVerificacion> buscarAbierto(Long usuarioId, PropositoCodigo proposito) {
        return listarAbiertos(usuarioId, proposito).stream().findFirst();
    }
}
