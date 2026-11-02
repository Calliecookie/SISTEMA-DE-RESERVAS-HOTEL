package pe.edu.josepardo.hotel.servicio;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.configuracion.PropiedadesHotel;
import pe.edu.josepardo.hotel.modelo.Usuario;
import pe.edu.josepardo.hotel.repositorio.RepositorioUsuario;

@Service
public class ServicioIntentosAcceso {

    private final RepositorioUsuario repositorioUsuario;
    private final PropiedadesHotel.Seguridad seguridad;

    public ServicioIntentosAcceso(RepositorioUsuario repositorioUsuario, PropiedadesHotel propiedades) {
        this.repositorioUsuario = repositorioUsuario;
        this.seguridad = propiedades.seguridad();
    }

    @Transactional
    public boolean registrarFallo(String correo) {
        if (correo == null || correo.isBlank()) {
            return false;
        }
        Usuario usuario = repositorioUsuario.buscarPorCorreo(correo.trim()).orElse(null);
        if (usuario == null) {
            return false;
        }
        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);
        if (intentos >= seguridad.maximoIntentosAcceso()) {
            usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(seguridad.minutosBloqueoAcceso()));
            usuario.setIntentosFallidos(0);
            return true;
        }
        return false;
    }

    @Transactional
    public void registrarExito(Long usuarioId) {
        repositorioUsuario.findById(usuarioId).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuario.setUltimoAcceso(LocalDateTime.now());
        });
    }
}
