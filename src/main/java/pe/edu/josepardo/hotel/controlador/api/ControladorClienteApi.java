package pe.edu.josepardo.hotel.controlador.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.josepardo.hotel.dto.InicioPayPalDto;
import pe.edu.josepardo.hotel.dto.MetodoPagoDisponibleDto;
import pe.edu.josepardo.hotel.dto.Pagina;
import pe.edu.josepardo.hotel.dto.PerfilDto;
import pe.edu.josepardo.hotel.dto.ReservaCreadaDto;
import pe.edu.josepardo.hotel.dto.ReservaDetalleDto;
import pe.edu.josepardo.hotel.dto.ReservaResumenDto;
import pe.edu.josepardo.hotel.dto.RespuestaApi;
import pe.edu.josepardo.hotel.dto.SolicitudCambioContrasena;
import pe.edu.josepardo.hotel.dto.SolicitudCrearReserva;
import pe.edu.josepardo.hotel.dto.SolicitudMotivo;
import pe.edu.josepardo.hotel.dto.SolicitudPago;
import pe.edu.josepardo.hotel.dto.SolicitudPerfil;
import pe.edu.josepardo.hotel.seguridad.UsuarioAutenticado;
import pe.edu.josepardo.hotel.servicio.ServicioCliente;
import pe.edu.josepardo.hotel.servicio.ServicioConsultaReservas;
import pe.edu.josepardo.hotel.servicio.ServicioFlujoReserva;
import pe.edu.josepardo.hotel.servicio.ServicioPago;
import pe.edu.josepardo.hotel.servicio.ServicioReserva;

@RestController
@RequestMapping("/api/cliente")
public class ControladorClienteApi {

    private final ServicioReserva servicioReserva;
    private final ServicioConsultaReservas servicioConsulta;
    private final ServicioFlujoReserva servicioFlujo;
    private final ServicioPago servicioPago;
    private final ServicioCliente servicioCliente;

    public ControladorClienteApi(
            ServicioReserva servicioReserva,
            ServicioConsultaReservas servicioConsulta,
            ServicioFlujoReserva servicioFlujo,
            ServicioPago servicioPago,
            ServicioCliente servicioCliente) {
        this.servicioReserva = servicioReserva;
        this.servicioConsulta = servicioConsulta;
        this.servicioFlujo = servicioFlujo;
        this.servicioPago = servicioPago;
        this.servicioCliente = servicioCliente;
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaApi<ReservaCreadaDto> crearReserva(
            @Valid @RequestBody SolicitudCrearReserva solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            HttpSession sesion) {
        ReservaCreadaDto creada = servicioReserva.crearDesdeCarrito(
                usuario.getId(), ControladorCarritoApi.carrito(sesion), solicitud.observaciones());
        return RespuestaApi.exito("Tu reserva quedó registrada.", creada);
    }

    @GetMapping("/reservas")
    public RespuestaApi<Pagina<ReservaResumenDto>> misReservas(
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito(servicioConsulta.listarDeCliente(usuario.getId(), pagina, tamano));
    }

    @GetMapping("/reservas/{codigo}")
    public RespuestaApi<ReservaDetalleDto> miReserva(
            @PathVariable String codigo, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito(servicioConsulta.detalle(codigo, usuario));
    }

    @GetMapping("/reservas/{codigo}/metodos-pago")
    public RespuestaApi<List<MetodoPagoDisponibleDto>> metodosPago(
            @PathVariable String codigo, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito(servicioPago.metodosDisponibles(codigo, usuario.getId()));
    }

    @PostMapping("/reservas/{codigo}/pago")
    public RespuestaApi<Void> declararPago(
            @PathVariable String codigo,
            @Valid @RequestBody SolicitudPago solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioPago.declararPago(codigo, usuario.getId(), solicitud.metodo(), solicitud.numeroOperacion());
        return RespuestaApi.exito("Registramos tu pago. Lo verificaremos pronto.");
    }

    @PostMapping("/reservas/{codigo}/paypal")
    public RespuestaApi<InicioPayPalDto> iniciarPayPal(
            @PathVariable String codigo, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito(servicioPago.iniciarPayPal(codigo, usuario.getId()));
    }

    @PostMapping("/reservas/{codigo}/cancelar")
    public RespuestaApi<Void> cancelar(
            @PathVariable String codigo,
            @Valid @RequestBody SolicitudMotivo solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioFlujo.cancelarPorCliente(codigo, usuario.getId(), solicitud.motivo());
        return RespuestaApi.exito("Tu reserva fue cancelada.");
    }

    @GetMapping("/perfil")
    public RespuestaApi<PerfilDto> perfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito(servicioCliente.perfil(usuario.getId()));
    }

    @PutMapping("/perfil")
    public RespuestaApi<PerfilDto> actualizarPerfil(
            @Valid @RequestBody SolicitudPerfil solicitud, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito("Tus datos se actualizaron.", servicioCliente.actualizarPerfil(usuario.getId(), solicitud));
    }

    @PutMapping("/contrasena")
    public RespuestaApi<Void> cambiarContrasena(
            @Valid @RequestBody SolicitudCambioContrasena solicitud, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioCliente.cambiarContrasena(usuario.getId(), solicitud);
        return RespuestaApi.exito("Tu contraseña se actualizó.");
    }
}
