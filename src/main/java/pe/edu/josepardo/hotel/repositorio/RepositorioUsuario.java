package pe.edu.josepardo.hotel.repositorio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.modelo.Usuario;

public interface RepositorioUsuario extends JpaRepository<Usuario, Long> {

    @Query("select u from Usuario u where lower(u.correo) = lower(:correo)")
    Optional<Usuario> buscarPorCorreo(@Param("correo") String correo);

    @Query("select case when count(u) > 0 then true else false end from Usuario u where lower(u.correo) = lower(:correo)")
    boolean existeCorreo(@Param("correo") String correo);

    @Query("select case when count(u) > 0 then true else false end from Usuario u where lower(u.correo) = lower(:correo) and u.id <> :id")
    boolean existeCorreoEnOtro(@Param("correo") String correo, @Param("id") Long id);

    @Query("select count(u) from Usuario u where u.rol = :rol and u.activo = true")
    long contarActivosPorRol(@Param("rol") Rol rol);

    @Query("select count(u) from Usuario u where u.rol = :rol")
    long contarPorRol(@Param("rol") Rol rol);
}
