package pe.edu.josepardo.hotel.repositorio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.josepardo.hotel.modelo.Caracteristica;

public interface RepositorioCaracteristica extends JpaRepository<Caracteristica, Long> {

    @Query("select c from Caracteristica c order by c.nombre")
    List<Caracteristica> listarOrdenadas();
}
