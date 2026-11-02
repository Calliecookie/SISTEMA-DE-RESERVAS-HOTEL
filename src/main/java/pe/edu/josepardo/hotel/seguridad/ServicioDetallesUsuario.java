package pe.edu.josepardo.hotel.seguridad;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.repositorio.RepositorioUsuario;

@Service
public class ServicioDetallesUsuario implements UserDetailsService {

    private final RepositorioUsuario repositorioUsuario;

    public ServicioDetallesUsuario(RepositorioUsuario repositorioUsuario) {
        this.repositorioUsuario = repositorioUsuario;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) {
        return repositorioUsuario.buscarPorCorreo(correo == null ? "" : correo.trim())
                .map(UsuarioAutenticado::new)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
    }
}
