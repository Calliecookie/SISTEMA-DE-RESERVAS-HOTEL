package pe.edu.josepardo.hotel.servicio;

import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.configuracion.PropiedadesHotel;
import pe.edu.josepardo.hotel.dto.ResultadoDni;
import pe.edu.josepardo.hotel.dto.SolicitudRegistro;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Cliente;
import pe.edu.josepardo.hotel.modelo.OrigenDatosCliente;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.modelo.Usuario;
import pe.edu.josepardo.hotel.repositorio.RepositorioCliente;
import pe.edu.josepardo.hotel.repositorio.RepositorioUsuario;
import pe.edu.josepardo.hotel.seguridad.LimitadorSolicitudes;

@Service
public class ServicioAutenticacion {

    private static final String ATRIBUTO_DNI = "dniVerificado";

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioCliente repositorioCliente;
    private final ClienteFactiliza clienteFactiliza;
    private final PasswordEncoder codificador;
    private final LimitadorSolicitudes limitador;
    private final boolean permiteManual;

    public ServicioAutenticacion(
            RepositorioUsuario repositorioUsuario,
            RepositorioCliente repositorioCliente,
            ClienteFactiliza clienteFactiliza,
            PasswordEncoder codificador,
            LimitadorSolicitudes limitador,
            PropiedadesHotel propiedades) {
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioCliente = repositorioCliente;
        this.clienteFactiliza = clienteFactiliza;
        this.codificador = codificador;
        this.limitador = limitador;
        this.permiteManual = propiedades.factiliza().permitirManual();
    }

    public ResultadoDni consultarDni(String dni, String ip, HttpSession sesion) {
        limitador.exigir("dni-minuto:" + ip, 5, Duration.ofMinutes(1));
        limitador.exigir("dni-hora:" + ip, 20, Duration.ofHours(1));
        try {
            Optional<ClienteFactiliza.PersonaDni> persona = clienteFactiliza.consultar(dni);
            if (persona.isEmpty()) {
                return new ResultadoDni(dni, null, null, false, false, "No encontramos ese DNI. Revisa el número.");
            }
            ClienteFactiliza.PersonaDni encontrada = persona.get();
            sesion.setAttribute(ATRIBUTO_DNI, new DniVerificado(dni, encontrada.nombres(), encontrada.apellidos()));
            return new ResultadoDni(dni, encontrada.nombres(), encontrada.apellidos(), true, false, "Datos encontrados.");
        } catch (ClienteFactiliza.ServicioNoDisponible excepcion) {
            if (permiteManual) {
                return new ResultadoDni(dni, null, null, false, true,
                        "No pudimos verificar el DNI ahora. Ingresa tus nombres y apellidos.");
            }
            throw ExcepcionNegocio.servicioNoDisponible("La verificación de DNI no está disponible. Intenta más tarde.");
        }
    }

    @Transactional
    public Long registrar(SolicitudRegistro solicitud, HttpSession sesion) {
        if (!solicitud.contrasena().equals(solicitud.confirmacion())) {
            throw ExcepcionNegocio.solicitudInvalida("Las contraseñas no coinciden.");
        }
        String correo = solicitud.correo().trim().toLowerCase();
        if (repositorioUsuario.existeCorreo(correo)) {
            throw ExcepcionNegocio.conflicto("CORREO_REGISTRADO", "Ese correo ya está registrado.");
        }
        if (repositorioCliente.existeDni(solicitud.dni())) {
            throw ExcepcionNegocio.conflicto("DNI_REGISTRADO", "Ese DNI ya está registrado.");
        }
        DniVerificado verificado = sesion.getAttribute(ATRIBUTO_DNI) instanceof DniVerificado dato
                && dato.dni().equals(solicitud.dni()) ? dato : null;
        if (verificado == null && !permiteManual) {
            throw new ExcepcionNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "DNI_NO_VERIFICADO", "Primero verifica tu DNI.");
        }
        String nombres = verificado != null ? verificado.nombres() : limpiar(solicitud.nombres());
        String apellidos = verificado != null ? verificado.apellidos() : limpiar(solicitud.apellidos());
        if (nombres.isEmpty() || apellidos.isEmpty()) {
            throw ExcepcionNegocio.solicitudInvalida("Ingresa tus nombres y apellidos.");
        }

        Usuario usuario = new Usuario();
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(codificador.encode(solicitud.contrasena()));
        usuario.setRol(Rol.CLIENTE);
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        repositorioUsuario.save(usuario);

        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setDni(solicitud.dni());
        cliente.setDniVerificado(verificado != null);
        cliente.setOrigenDatos(verificado != null ? OrigenDatosCliente.FACTILIZA : OrigenDatosCliente.MANUAL);
        repositorioCliente.save(cliente);

        sesion.removeAttribute(ATRIBUTO_DNI);
        return usuario.getId();
    }

    private String limpiar(String texto) {
        return texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
    }

    public record DniVerificado(String dni, String nombres, String apellidos) implements java.io.Serializable {
    }
}
