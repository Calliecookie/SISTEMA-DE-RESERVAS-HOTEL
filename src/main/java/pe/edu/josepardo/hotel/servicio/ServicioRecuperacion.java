package pe.edu.josepardo.hotel.servicio;

import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.configuracion.PropiedadesHotel;
import pe.edu.josepardo.hotel.dto.SolicitudRestablecer;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.CodigoVerificacion;
import pe.edu.josepardo.hotel.modelo.PropositoCodigo;
import pe.edu.josepardo.hotel.modelo.Usuario;
import pe.edu.josepardo.hotel.repositorio.RepositorioCodigoVerificacion;
import pe.edu.josepardo.hotel.repositorio.RepositorioUsuario;
import pe.edu.josepardo.hotel.seguridad.LimitadorSolicitudes;

@Service
public class ServicioRecuperacion {

    private static final String ATRIBUTO_CODIGO = "recuperacionCodigoId";
    private static final String CODIGO_INVALIDO = "El código es incorrecto o ya venció.";

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioCodigoVerificacion repositorioCodigo;
    private final PasswordEncoder codificador;
    private final ServicioCorreo servicioCorreo;
    private final LimitadorSolicitudes limitador;
    private final PropiedadesHotel.Seguridad seguridad;
    private final SecureRandom aleatorio = new SecureRandom();

    public ServicioRecuperacion(
            RepositorioUsuario repositorioUsuario,
            RepositorioCodigoVerificacion repositorioCodigo,
            PasswordEncoder codificador,
            ServicioCorreo servicioCorreo,
            LimitadorSolicitudes limitador,
            PropiedadesHotel propiedades) {
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioCodigo = repositorioCodigo;
        this.codificador = codificador;
        this.servicioCorreo = servicioCorreo;
        this.limitador = limitador;
        this.seguridad = propiedades.seguridad();
    }

    @Transactional
    public void solicitar(String correo, String ip) {
        String correoNormalizado = correo.trim().toLowerCase();
        limitador.exigir("otp-ip:" + ip, 10, Duration.ofHours(1));
        if (!limitador.permitir("otp-correo:" + correoNormalizado, 3, Duration.ofMinutes(15))) {
            return;
        }
        Optional<Usuario> encontrado = repositorioUsuario.buscarPorCorreo(correoNormalizado).filter(Usuario::isActivo);
        if (encontrado.isEmpty()) {
            codificador.encode(generarCodigo());
            return;
        }
        Usuario usuario = encontrado.get();
        LocalDateTime ahora = LocalDateTime.now();
        repositorioCodigo.invalidarAnteriores(usuario.getId(), PropositoCodigo.RECUPERAR_CONTRASENA, ahora);
        String codigo = generarCodigo();
        CodigoVerificacion registro = new CodigoVerificacion();
        registro.setUsuario(usuario);
        registro.setProposito(PropositoCodigo.RECUPERAR_CONTRASENA);
        registro.setCodigoHash(codificador.encode(codigo));
        registro.setExpiraEn(ahora.plusMinutes(seguridad.minutosValidezCodigo()));
        registro.setCreadoEn(ahora);
        repositorioCodigo.save(registro);
        servicioCorreo.enviarCodigoRecuperacion(usuario.getCorreo(), usuario.getNombres(), codigo, seguridad.minutosValidezCodigo());
    }

    private String generarCodigo() {
        return String.format("%06d", aleatorio.nextInt(1_000_000));
    }

    @Transactional(noRollbackFor = ExcepcionNegocio.class)
    public void verificar(String correo, String codigo, String ip, HttpSession sesion) {
        limitador.exigir("otp-verificar:" + ip, 20, Duration.ofMinutes(15));
        Usuario usuario = repositorioUsuario.buscarPorCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> ExcepcionNegocio.solicitudInvalida(CODIGO_INVALIDO));
        CodigoVerificacion registro = repositorioCodigo
                .buscarAbierto(usuario.getId(), PropositoCodigo.RECUPERAR_CONTRASENA)
                .orElseThrow(() -> ExcepcionNegocio.solicitudInvalida(CODIGO_INVALIDO));
        LocalDateTime ahora = LocalDateTime.now();
        if (!registro.estaVigente(ahora)) {
            registro.setInvalidadoEn(ahora);
            throw ExcepcionNegocio.solicitudInvalida(CODIGO_INVALIDO);
        }
        registro.setIntentos(registro.getIntentos() + 1);
        if (!codificador.matches(codigo, registro.getCodigoHash())) {
            if (registro.getIntentos() >= seguridad.maximoIntentosCodigo()) {
                registro.setInvalidadoEn(ahora);
            }
            throw ExcepcionNegocio.solicitudInvalida(CODIGO_INVALIDO);
        }
        sesion.setAttribute(ATRIBUTO_CODIGO, registro.getId());
    }

    @Transactional
    public void restablecer(SolicitudRestablecer solicitud, HttpSession sesion) {
        if (!solicitud.contrasena().equals(solicitud.confirmacion())) {
            throw ExcepcionNegocio.solicitudInvalida("Las contraseñas no coinciden.");
        }
        if (!(sesion.getAttribute(ATRIBUTO_CODIGO) instanceof Long codigoId)) {
            throw ExcepcionNegocio.solicitudInvalida("Verifica tu código antes de cambiar la contraseña.");
        }
        CodigoVerificacion registro = repositorioCodigo.findById(codigoId)
                .orElseThrow(() -> ExcepcionNegocio.solicitudInvalida(CODIGO_INVALIDO));
        LocalDateTime ahora = LocalDateTime.now();
        if (!registro.estaVigente(ahora)) {
            sesion.removeAttribute(ATRIBUTO_CODIGO);
            throw ExcepcionNegocio.solicitudInvalida(CODIGO_INVALIDO);
        }
        Usuario usuario = registro.getUsuario();
        usuario.setContrasenaHash(codificador.encode(solicitud.contrasena()));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        registro.setUsadoEn(ahora);
        sesion.removeAttribute(ATRIBUTO_CODIGO);
    }
}
