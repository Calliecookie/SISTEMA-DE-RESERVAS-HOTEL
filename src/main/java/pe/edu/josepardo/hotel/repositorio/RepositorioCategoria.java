package pe.edu.josepardo.hotel.repositorio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.josepardo.hotel.modelo.Categoria;

public interface RepositorioCategoria extends JpaRepository<Categoria, Long> {

    @Query("select c from Categoria c where c.activo = true order by c.nombre")
    List<Categoria> listarActivas();

    @Query("select c from Categoria c order by c.nombre")
    List<Categoria> listarTodas();

    @Query("select case when count(c) > 0 then true else false end from Categoria c where lower(c.nombre) = lower(:nombre)")
    boolean existeNombre(@Param("nombre") String nombre);

    @Query("select case when count(c) > 0 then true else false end from Categoria c where lower(c.nombre) = lower(:nombre) and c.id <> :id")
    boolean existeNombreEnOtra(@Param("nombre") String nombre, @Param("id") Long id);
}
