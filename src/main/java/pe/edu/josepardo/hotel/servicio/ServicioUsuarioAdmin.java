package pe.edu.josepardo.hotel.servicio;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.SolicitudUsuario;
import pe.edu.josepardo.hotel.dto.UsuarioDto;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.modelo.Usuario;
import pe.edu.josepardo.hotel.repositorio.RepositorioUsuario;

@Service
public class ServicioUsuarioAdmin {

    private final RepositorioUsuario repositorio;
    private final PasswordEncoder codificador;
    private final ServicioAuditoria auditoria;

    public ServicioUsuarioAdmin(RepositorioUsuario repositorio, PasswordEncoder codificador, ServicioAuditoria auditoria) {
        this.repositorio = repositorio;
        this.codificador = codificador;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listarPersonal() {
        return repositorio.findAll(Sort.by("nombres", "apellidos")).stream()
                .filter(usuario -> usuario.getRol() != Rol.CLIENTE)
                .map(this::aDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDto obtener(Long id) {
        return aDto(buscarPersonal(id));
    }

    @Transactional
    public UsuarioDto crear(SolicitudUsuario solicitud, Long administradorId) {
        if (solicitud.contrasena() == null || solicitud.contrasena().isBlank()) {
            throw ExcepcionNegocio.solicitudInvalida("Ingresa una contraseña inicial.");
        }
        String correo = solicitud.correo().trim().toLowerCase();
        if (repositorio.existeCorreo(correo)) {
            throw ExcepcionNegocio.conflicto("CORREO_REGISTRADO", "Ese correo ya está registrado.");
        }
        Usuario usuario = new Usuario();
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(codificador.encode(solicitud.contrasena()));
        usuario.setRol(leerRol(solicitud.rol()));
        usuario.setNombres(solicitud.nombres().trim());
        usuario.setApellidos(solicitud.apellidos().trim());
        repositorio.save(usuario);
        auditoria.registrar(administradorId, "CREAR_USUARIO", "usuarios", usuario.getId(), correo + " · " + usuario.getRol());
        return aDto(usuario);
    }

    @Transactional
    public UsuarioDto actualizar(Long id, SolicitudUsuario solicitud, Long administradorId) {
        Usuario usuario = buscarPersonal(id);
        String correo = solicitud.correo().trim().toLowerCase();
        if (repositorio.existeCorreoEnOtro(correo, id)) {
            throw ExcepcionNegocio.conflicto("CORREO_REGISTRADO", "Ese correo ya está registrado.");
        }
        Rol rol = leerRol(solicitud.rol());
        if (usuario.getRol() == Rol.ADMINISTRADOR && rol != Rol.ADMINISTRADOR) {
            exigirOtroAdministrador(usuario);
        }
        usuario.setCorreo(correo);
        usuario.setRol(rol);
        usuario.setNombres(solicitud.nombres().trim());
        usuario.setApellidos(solicitud.apellidos().trim());
        if (solicitud.contrasena() != null && !solicitud.contrasena().isBlank()) {
            usuario.setContrasenaHash(codificador.encode(solicitud.contrasena()));
        }
        auditoria.registrar(administradorId, "EDITAR_USUARIO", "usuarios", id, correo + " · " + rol);
        return aDto(usuario);
    }

    @Transactional
    public UsuarioDto cambiarActivo(Long id, boolean activo, Long administradorId) {
        Usuario usuario = buscarPersonal(id);
        if (!activo) {
            if (id.equals(administradorId)) {
                throw ExcepcionNegocio.conflicto("AUTODESACTIVACION", "No puedes desactivar tu propia cuenta.");
            }
            if (usuario.getRol() == Rol.ADMINISTRADOR) {
                exigirOtroAdministrador(usuario);
            }
        }
        usuario.setActivo(activo);
        auditoria.registrar(administradorId, activo ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO", "usuarios", id, usuario.getCorreo());
        return aDto(usuario);
    }

    private void exigirOtroAdministrador(Usuario usuario) {
        if (usuario.isActivo() && repositorio.contarActivosPorRol(Rol.ADMINISTRADOR) <= 1) {
            throw ExcepcionNegocio.conflicto("ULTIMO_ADMINISTRADOR", "Debe quedar al menos un administrador activo.");
        }
    }

    private Rol leerRol(String texto) {
        try {
            Rol rol = Rol.valueOf(texto);
            if (rol == Rol.CLIENTE) {
                throw ExcepcionNegocio.solicitudInvalida("El personal solo puede ser Trabajador o Administrador.");
            }
            return rol;
        } catch (IllegalArgumentException excepcion) {
            throw ExcepcionNegocio.solicitudInvalida("El rol indicado no es válido.");
        }
    }

    private Usuario buscarPersonal(Long id) {
        return repositorio.findById(id)
                .filter(usuario -> usuario.getRol() != Rol.CLIENTE)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("El usuario no existe."));
    }

    private UsuarioDto aDto(Usuario usuario) {
        return new UsuarioDto(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getRol().name(),
                usuario.getRol().getEtiqueta(),
                usuario.isActivo(),
                usuario.getUltimoAcceso(),
                usuario.getCreadoEn());
    }
}
