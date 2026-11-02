package pe.edu.josepardo.hotel.servicio;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.ClienteAdminDto;
import pe.edu.josepardo.hotel.dto.Pagina;
import pe.edu.josepardo.hotel.dto.PerfilDto;
import pe.edu.josepardo.hotel.dto.SolicitudCambioContrasena;
import pe.edu.josepardo.hotel.dto.SolicitudClienteAdmin;
import pe.edu.josepardo.hotel.dto.SolicitudPerfil;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Cliente;
import pe.edu.josepardo.hotel.modelo.Usuario;
import pe.edu.josepardo.hotel.repositorio.ConsultaReservas;
import pe.edu.josepardo.hotel.repositorio.RepositorioCliente;

@Service
public class ServicioCliente {

    private final RepositorioCliente repositorio;
    private final ConsultaReservas consulta;
    private final PasswordEncoder codificador;
    private final ServicioAuditoria auditoria;

    public ServicioCliente(
            RepositorioCliente repositorio,
            ConsultaReservas consulta,
            PasswordEncoder codificador,
            ServicioAuditoria auditoria) {
        this.repositorio = repositorio;
        this.consulta = consulta;
        this.codificador = codificador;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public PerfilDto perfil(Long usuarioId) {
        return aPerfil(porUsuario(usuarioId));
    }

    @Transactional
    public PerfilDto actualizarPerfil(Long usuarioId, SolicitudPerfil solicitud) {
        Cliente cliente = porUsuario(usuarioId);
        cliente.setTelefono(limpiar(solicitud.telefono()));
        cliente.setDireccion(limpiar(solicitud.direccion()));
        return aPerfil(cliente);
    }

    @Transactional
    public void cambiarContrasena(Long usuarioId, SolicitudCambioContrasena solicitud) {
        Usuario usuario = porUsuario(usuarioId).getUsuario();
        if (!codificador.matches(solicitud.actual(), usuario.getContrasenaHash())) {
            throw ExcepcionNegocio.solicitudInvalida("Tu contraseña actual no es correcta.");
        }
        if (!solicitud.nueva().equals(solicitud.confirmacion())) {
            throw ExcepcionNegocio.solicitudInvalida("Las contraseñas no coinciden.");
        }
        usuario.setContrasenaHash(codificador.encode(solicitud.nueva()));
    }

    @Transactional(readOnly = true)
    public Pagina<ClienteAdminDto> listar(String busqueda, Integer pagina, Integer tamano) {
        int numero = pagina == null ? 0 : Math.max(pagina, 0);
        int cantidad = tamano == null ? 10 : Math.min(Math.max(tamano, 1), 100);
        return consulta.listarClientes(busqueda, numero, cantidad);
    }

    @Transactional
    public void actualizar(Long id, SolicitudClienteAdmin solicitud, Long administradorId) {
        Cliente cliente = porId(id);
        cliente.getUsuario().setNombres(solicitud.nombres().trim());
        cliente.getUsuario().setApellidos(solicitud.apellidos().trim());
        cliente.setTelefono(limpiar(solicitud.telefono()));
        cliente.setDireccion(limpiar(solicitud.direccion()));
        auditoria.registrar(administradorId, "EDITAR_CLIENTE", "clientes", id, cliente.getDni());
    }

    @Transactional
    public void cambiarActivo(Long id, boolean activo, Long administradorId) {
        Cliente cliente = porId(id);
        cliente.getUsuario().setActivo(activo);
        auditoria.registrar(administradorId, activo ? "ACTIVAR_CLIENTE" : "DESACTIVAR_CLIENTE", "clientes", id, cliente.getDni());
    }

    private Cliente porUsuario(Long usuarioId) {
        return repositorio.buscarPorUsuarioId(usuarioId)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("El cliente no existe."));
    }

    private Cliente porId(Long id) {
        return repositorio.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("El cliente no existe."));
    }

    private PerfilDto aPerfil(Cliente cliente) {
        Usuario usuario = cliente.getUsuario();
        return new PerfilDto(
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                cliente.getDni(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                cliente.isDniVerificado());
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
