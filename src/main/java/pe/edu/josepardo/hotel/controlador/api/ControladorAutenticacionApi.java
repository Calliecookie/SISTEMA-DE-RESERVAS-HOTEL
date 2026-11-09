package pe.edu.josepardo.hotel.controlador.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.josepardo.hotel.dto.MetodosRecuperacionDto;
import pe.edu.josepardo.hotel.dto.RespuestaApi;
import pe.edu.josepardo.hotel.dto.ResultadoDni;
import pe.edu.josepardo.hotel.dto.SolicitudConsultaDni;
import pe.edu.josepardo.hotel.dto.SolicitudRecuperacion;
import pe.edu.josepardo.hotel.dto.SolicitudRecuperacionAdmin;
import pe.edu.josepardo.hotel.dto.SolicitudRegistro;
import pe.edu.josepardo.hotel.dto.SolicitudRestablecer;
import pe.edu.josepardo.hotel.dto.SolicitudVerificarCodigo;
import pe.edu.josepardo.hotel.seguridad.ResolutorIpCliente;
import pe.edu.josepardo.hotel.servicio.ServicioAutenticacion;
import pe.edu.josepardo.hotel.servicio.ServicioRecuperacion;

@RestController
@RequestMapping("/api/autenticacion")
public class ControladorAutenticacionApi {

    private final ServicioAutenticacion servicioAutenticacion;
    private final ServicioRecuperacion servicioRecuperacion;
    private final ResolutorIpCliente resolutorIp;

    public ControladorAutenticacionApi(
            ServicioAutenticacion servicioAutenticacion,
            ServicioRecuperacion servicioRecuperacion,
            ResolutorIpCliente resolutorIp) {
        this.servicioAutenticacion = servicioAutenticacion;
        this.servicioRecuperacion = servicioRecuperacion;
        this.resolutorIp = resolutorIp;
    }

    @PostMapping("/consultar-dni")
    public RespuestaApi<ResultadoDni> consultarDni(
            @Valid @RequestBody SolicitudConsultaDni solicitud, HttpServletRequest peticion, HttpSession sesion) {
        ResultadoDni resultado = servicioAutenticacion.consultarDni(solicitud.dni(), resolutorIp.resolver(peticion), sesion);
        return RespuestaApi.exito(resultado.mensaje(), resultado);
    }

    @PostMapping("/registro")
    public RespuestaApi<Void> registrar(@Valid @RequestBody SolicitudRegistro solicitud, HttpSession sesion) {
        servicioAutenticacion.registrar(solicitud, sesion);
        return RespuestaApi.exito("Tu cuenta se creó correctamente. Ya puedes iniciar sesión.");
    }

    @PostMapping("/recuperacion/solicitar")
    public RespuestaApi<Void> solicitarCodigo(
            @Valid @RequestBody SolicitudRecuperacion solicitud, HttpServletRequest peticion) {
        servicioRecuperacion.solicitar(solicitud.correo(), resolutorIp.resolver(peticion));
        return RespuestaApi.exito("Si el correo está registrado, recibirás un código en unos minutos.");
    }

    @PostMapping("/recuperacion/verificar")
    public RespuestaApi<Void> verificarCodigo(
            @Valid @RequestBody SolicitudVerificarCodigo solicitud, HttpServletRequest peticion, HttpSession sesion) {
        servicioRecuperacion.verificar(solicitud.correo(), solicitud.codigo(), resolutorIp.resolver(peticion), sesion);
        return RespuestaApi.exito("Código verificado. Crea tu nueva contraseña.");
    }

    @GetMapping("/recuperacion/metodos")
    public RespuestaApi<MetodosRecuperacionDto> metodosRecuperacion() {
        return RespuestaApi.exito(servicioRecuperacion.metodos());
    }

    @PostMapping("/recuperacion/administrador")
    public RespuestaApi<Void> restablecerAdministrador(
            @Valid @RequestBody SolicitudRecuperacionAdmin solicitud, HttpServletRequest peticion) {
        servicioRecuperacion.restablecerAdministrador(solicitud, resolutorIp.resolver(peticion));
        return RespuestaApi.exito("La contraseña del administrador se actualizó. Ya puedes iniciar sesión.");
    }

    @PostMapping("/recuperacion/restablecer")
    public RespuestaApi<Void> restablecer(@Valid @RequestBody SolicitudRestablecer solicitud, HttpSession sesion) {
        servicioRecuperacion.restablecer(solicitud, sesion);
        return RespuestaApi.exito("Tu contraseña se actualizó. Ya puedes iniciar sesión.");
    }
}
