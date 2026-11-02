package pe.edu.josepardo.hotel.seguridad;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.edu.josepardo.hotel.modelo.Actor;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.modelo.Usuario;

public class UsuarioAutenticado implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String correo;
    private final String contrasenaHash;
    private final String nombres;
    private final String nombreCompleto;
    private final Rol rol;
    private final boolean activo;
    private final boolean bloqueado;

    public UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.correo = usuario.getCorreo();
        this.contrasenaHash = usuario.getContrasenaHash();
        this.nombres = usuario.getNombres();
        this.nombreCompleto = usuario.getNombreCompleto();
        this.rol = usuario.getRol();
        this.activo = usuario.isActivo();
        LocalDateTime bloqueadoHasta = usuario.getBloqueadoHasta();
        this.bloqueado = bloqueadoHasta != null && bloqueadoHasta.isAfter(LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public String getCorreo() {
        return correo;
    }

    public String getNombres() {
        return nombres;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public Rol getRol() {
        return rol;
    }

    public Actor getActor() {
        return Actor.desde(rol);
    }

    public boolean esPersonal() {
        return rol != Rol.CLIENTE;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return contrasenaHash;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !bloqueado;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
