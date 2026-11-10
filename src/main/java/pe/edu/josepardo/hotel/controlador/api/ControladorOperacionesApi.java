package pe.edu.josepardo.hotel.controlador.api;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.josepardo.hotel.dto.FiltroReservas;
import pe.edu.josepardo.hotel.dto.MapaHabitacionesDto;
import pe.edu.josepardo.hotel.dto.OperacionesDto;
import pe.edu.josepardo.hotel.dto.Pagina;
import pe.edu.josepardo.hotel.dto.PlanificadorDto;
import pe.edu.josepardo.hotel.dto.ReservaDetalleDto;
import pe.edu.josepardo.hotel.dto.ReservaResumenDto;
import pe.edu.josepardo.hotel.dto.RespuestaApi;
import pe.edu.josepardo.hotel.dto.ResultadoBusquedaDto;
import pe.edu.josepardo.hotel.dto.SolicitudCambioEstado;
import pe.edu.josepardo.hotel.dto.SolicitudMotivo;
import pe.edu.josepardo.hotel.dto.SolicitudPago;
import pe.edu.josepardo.hotel.seguridad.UsuarioAutenticado;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.servicio.ServicioCambioEstadoReserva;
import pe.edu.josepardo.hotel.servicio.ServicioConsultaReservas;
import pe.edu.josepardo.hotel.servicio.ServicioFlujoReserva;
import pe.edu.josepardo.hotel.servicio.ServicioMonitor;
import pe.edu.josepardo.hotel.servicio.ServicioPago;
import pe.edu.josepardo.hotel.servicio.ServicioPanel;

@RestController
@RequestMapping("/api/operaciones")
public class ControladorOperacionesApi {

    private final ServicioPanel servicioPanel;
    private final ServicioConsultaReservas servicioConsulta;
    private final ServicioFlujoReserva servicioFlujo;
    private final ServicioPago servicioPago;
    private final ServicioCambioEstadoReserva servicioCambioEstado;
    private final ServicioMonitor servicioMonitor;

    public ControladorOperacionesApi(
            ServicioPanel servicioPanel,
            ServicioConsultaReservas servicioConsulta,
            ServicioFlujoReserva servicioFlujo,
            ServicioPago servicioPago,
            ServicioCambioEstadoReserva servicioCambioEstado,
            ServicioMonitor servicioMonitor) {
        this.servicioPanel = servicioPanel;
        this.servicioConsulta = servicioConsulta;
        this.servicioFlujo = servicioFlujo;
        this.servicioPago = servicioPago;
        this.servicioCambioEstado = servicioCambioEstado;
        this.servicioMonitor = servicioMonitor;
    }

    @GetMapping("/resumen")
    public RespuestaApi<OperacionesDto> resumen() {
        return RespuestaApi.exito(servicioPanel.operaciones());
    }

    @GetMapping("/mapa")
    public RespuestaApi<MapaHabitacionesDto> mapa() {
        return RespuestaApi.exito(servicioMonitor.mapa());
    }

    @GetMapping("/planificador")
    public RespuestaApi<PlanificadorDto> planificador(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) Integer dias) {
        return RespuestaApi.exito(servicioMonitor.planificador(desde, dias));
    }

    @GetMapping("/buscar")
    public RespuestaApi<List<ResultadoBusquedaDto>> buscar(
            @RequestParam(required = false) String q, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito(servicioMonitor.buscar(q, usuario.getActor()));
    }

    @GetMapping("/reservas")
    public RespuestaApi<Pagina<ReservaResumenDto>> reservas(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito(servicioConsulta.listar(
                new FiltroReservas(busqueda, estado, metodoPago, fechaDesde, fechaHasta, pagina, tamano),
                usuario.getActor()));
    }

    @PatchMapping("/reservas/{codigo}/estado")
    public RespuestaApi<Void> cambiarEstado(
            @PathVariable String codigo,
            @Valid @RequestBody SolicitudCambioEstado solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        EstadoReserva nuevo = servicioCambioEstado.cambiar(codigo, solicitud, usuario);
        return RespuestaApi.exito("La reserva " + codigo + " ahora está " + nuevo.getEtiqueta().toLowerCase() + ".");
    }

    @GetMapping("/reservas/{codigo}")
    public RespuestaApi<ReservaDetalleDto> reserva(
            @PathVariable String codigo, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito(servicioConsulta.detalle(codigo, usuario));
    }

    @PostMapping("/reservas/{codigo}/confirmar-pago")
    public RespuestaApi<Void> confirmarPago(
            @PathVariable String codigo,
            @RequestBody(required = false) SolicitudPago solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioPago.confirmarPago(
                codigo, usuario,
                solicitud == null ? null : solicitud.metodo(),
                solicitud == null ? null : solicitud.numeroOperacion());
        return RespuestaApi.exito("Pago confirmado.");
    }

    @PostMapping("/reservas/{codigo}/rechazar-pago")
    public RespuestaApi<Void> rechazarPago(
            @PathVariable String codigo,
            @Valid @RequestBody SolicitudMotivo solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioPago.rechazarPago(codigo, usuario, solicitud.motivo());
        return RespuestaApi.exito("Pago rechazado.");
    }

    @PostMapping("/reservas/{codigo}/confirmar")
    public RespuestaApi<Void> confirmar(
            @PathVariable String codigo, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioFlujo.confirmar(codigo, usuario);
        return RespuestaApi.exito("Reserva confirmada.");
    }

    @PostMapping("/reservas/{codigo}/ingreso")
    public RespuestaApi<Void> registrarIngreso(
            @PathVariable String codigo, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioFlujo.registrarIngreso(codigo, usuario);
        return RespuestaApi.exito("Ingreso registrado.");
    }

    @PostMapping("/reservas/{codigo}/salida")
    public RespuestaApi<Void> registrarSalida(
            @PathVariable String codigo, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioFlujo.registrarSalida(codigo, usuario);
        return RespuestaApi.exito("Salida registrada.");
    }

    @PostMapping("/reservas/{codigo}/cancelar")
    public RespuestaApi<Void> cancelar(
            @PathVariable String codigo,
            @Valid @RequestBody SolicitudMotivo solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioFlujo.cancelarPorPersonal(codigo, usuario, solicitud.motivo());
        return RespuestaApi.exito("Reserva cancelada.");
    }
}
