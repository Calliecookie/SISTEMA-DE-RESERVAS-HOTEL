package pe.edu.josepardo.hotel.repositorio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.josepardo.hotel.modelo.Configuracion;

public interface RepositorioConfiguracion extends JpaRepository<Configuracion, String> {

    @Query("select c from Configuracion c order by c.clave")
    List<Configuracion> listarOrdenadas();
}
