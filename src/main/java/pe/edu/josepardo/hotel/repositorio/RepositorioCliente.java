package pe.edu.josepardo.hotel.repositorio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.josepardo.hotel.modelo.Cliente;

public interface RepositorioCliente extends JpaRepository<Cliente, Long> {

    @Query("select c from Cliente c join fetch c.usuario where c.usuario.id = :usuarioId")
    Optional<Cliente> buscarPorUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("select case when count(c) > 0 then true else false end from Cliente c where c.dni = :dni")
    boolean existeDni(@Param("dni") String dni);
}
