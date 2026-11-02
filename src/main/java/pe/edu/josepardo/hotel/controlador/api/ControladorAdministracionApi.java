package pe.edu.josepardo.hotel.controlador.api;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.josepardo.hotel.configuracion.PropiedadesHotel;
import pe.edu.josepardo.hotel.dto.AuditoriaDto;
import pe.edu.josepardo.hotel.dto.CaracteristicaDto;
import pe.edu.josepardo.hotel.dto.CategoriaDto;
import pe.edu.josepardo.hotel.dto.ClienteAdminDto;
import pe.edu.josepardo.hotel.dto.ConfiguracionDto;
import pe.edu.josepardo.hotel.dto.DashboardDto;
import pe.edu.josepardo.hotel.dto.EstadoIntegracionesDto;
import pe.edu.josepardo.hotel.dto.FiltroReservas;
import pe.edu.josepardo.hotel.dto.HabitacionAdminDto;
import pe.edu.josepardo.hotel.dto.Pagina;
import pe.edu.josepardo.hotel.dto.RespuestaApi;
import pe.edu.josepardo.hotel.dto.SolicitudCategoria;
import pe.edu.josepardo.hotel.dto.SolicitudClienteAdmin;
import pe.edu.josepardo.hotel.dto.SolicitudConfiguracion;
import pe.edu.josepardo.hotel.dto.SolicitudEstadoActivo;
import pe.edu.josepardo.hotel.dto.SolicitudHabitacion;
import pe.edu.josepardo.hotel.dto.SolicitudUsuario;
import pe.edu.josepardo.hotel.dto.UsuarioDto;
import pe.edu.josepardo.hotel.dto.VentasDto;
import pe.edu.josepardo.hotel.repositorio.ConsultaReservas;
import pe.edu.josepardo.hotel.seguridad.UsuarioAutenticado;
import pe.edu.josepardo.hotel.servicio.ServicioAlmacenamiento;
import pe.edu.josepardo.hotel.servicio.ServicioAuditoria;
import pe.edu.josepardo.hotel.servicio.ServicioCategoria;
import pe.edu.josepardo.hotel.servicio.ServicioCliente;
import pe.edu.josepardo.hotel.servicio.ServicioConfiguracion;
import pe.edu.josepardo.hotel.servicio.ServicioConsultaReservas;
import pe.edu.josepardo.hotel.servicio.ServicioCorreo;
import pe.edu.josepardo.hotel.servicio.ServicioHabitacionAdmin;
import pe.edu.josepardo.hotel.servicio.ServicioPago;
import pe.edu.josepardo.hotel.servicio.ServicioPanel;
import pe.edu.josepardo.hotel.servicio.ServicioUsuarioAdmin;

@RestController
@RequestMapping("/api/administracion")
public class ControladorAdministracionApi {

    private final ServicioPanel servicioPanel;
    private final ServicioHabitacionAdmin servicioHabitacion;
    private final ServicioCategoria servicioCategoria;
    private final ServicioUsuarioAdmin servicioUsuario;
    private final ServicioCliente servicioCliente;
    private final ServicioConfiguracion servicioConfiguracion;
    private final ServicioConsultaReservas servicioConsulta;
    private final ServicioAuditoria servicioAuditoria;
    private final ServicioAlmacenamiento almacenamiento;
    private final ServicioPago servicioPago;
    private final ServicioCorreo servicioCorreo;
    private final ConsultaReservas consultaReservas;
    private final PropiedadesHotel propiedades;

    public ControladorAdministracionApi(
            ServicioPanel servicioPanel,
            ServicioHabitacionAdmin servicioHabitacion,
            ServicioCategoria servicioCategoria,
            ServicioUsuarioAdmin servicioUsuario,
            ServicioCliente servicioCliente,
            ServicioConfiguracion servicioConfiguracion,
            ServicioConsultaReservas servicioConsulta,
            ServicioAuditoria servicioAuditoria,
            ServicioAlmacenamiento almacenamiento,
            ServicioPago servicioPago,
            ServicioCorreo servicioCorreo,
            ConsultaReservas consultaReservas,
            PropiedadesHotel propiedades) {
        this.servicioPanel = servicioPanel;
        this.servicioHabitacion = servicioHabitacion;
        this.servicioCategoria = servicioCategoria;
        this.servicioUsuario = servicioUsuario;
        this.servicioCliente = servicioCliente;
        this.servicioConfiguracion = servicioConfiguracion;
        this.servicioConsulta = servicioConsulta;
        this.servicioAuditoria = servicioAuditoria;
        this.almacenamiento = almacenamiento;
        this.servicioPago = servicioPago;
        this.servicioCorreo = servicioCorreo;
        this.consultaReservas = consultaReservas;
        this.propiedades = propiedades;
    }

    @GetMapping("/dashboard")
    public RespuestaApi<DashboardDto> dashboard() {
        return RespuestaApi.exito(servicioPanel.dashboard());
    }

    @GetMapping("/habitaciones")
    public RespuestaApi<List<HabitacionAdminDto>> habitaciones() {
        return RespuestaApi.exito(servicioHabitacion.listar());
    }

    @GetMapping("/habitaciones/{id}")
    public RespuestaApi<HabitacionAdminDto> habitacion(@PathVariable Long id) {
        return RespuestaApi.exito(servicioHabitacion.obtener(id));
    }

    @GetMapping("/caracteristicas")
    public RespuestaApi<List<CaracteristicaDto>> caracteristicas() {
        return RespuestaApi.exito(servicioHabitacion.caracteristicas());
    }

    @PostMapping("/habitaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaApi<HabitacionAdminDto> crearHabitacion(
            @Valid @RequestBody SolicitudHabitacion solicitud, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito("Habitación registrada.", servicioHabitacion.crear(solicitud, usuario.getId()));
    }

    @PutMapping("/habitaciones/{id}")
    public RespuestaApi<HabitacionAdminDto> actualizarHabitacion(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudHabitacion solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito("Habitación actualizada.", servicioHabitacion.actualizar(id, solicitud, usuario.getId()));
    }

    @PatchMapping("/habitaciones/{id}/activo")
    public RespuestaApi<HabitacionAdminDto> cambiarActivoHabitacion(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudEstadoActivo solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito("Estado actualizado.", servicioHabitacion.cambiarActivo(id, solicitud.activo(), usuario.getId()));
    }

    @PostMapping("/habitaciones/{id}/imagen")
    public RespuestaApi<HabitacionAdminDto> subirImagen(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito("Imagen actualizada.", servicioHabitacion.subirImagen(id, archivo, usuario.getId()));
    }

    @DeleteMapping("/habitaciones/{id}")
    public RespuestaApi<Void> eliminarHabitacion(
            @PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioHabitacion.eliminar(id, usuario.getId());
        return RespuestaApi.exito("Habitación eliminada.");
    }

    @GetMapping("/categorias")
    public RespuestaApi<List<CategoriaDto>> categorias() {
        return RespuestaApi.exito(servicioCategoria.listar());
    }

    @GetMapping("/categorias/{id}")
    public RespuestaApi<CategoriaDto> categoria(@PathVariable Long id) {
        return RespuestaApi.exito(servicioCategoria.obtener(id));
    }

    @PostMapping("/categorias")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaApi<CategoriaDto> crearCategoria(
            @Valid @RequestBody SolicitudCategoria solicitud, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito("Categoría registrada.", servicioCategoria.crear(solicitud, usuario.getId()));
    }

    @PutMapping("/categorias/{id}")
    public RespuestaApi<CategoriaDto> actualizarCategoria(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudCategoria solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito("Categoría actualizada.", servicioCategoria.actualizar(id, solicitud, usuario.getId()));
    }

    @PatchMapping("/categorias/{id}/activo")
    public RespuestaApi<CategoriaDto> cambiarActivoCategoria(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudEstadoActivo solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito("Estado actualizado.", servicioCategoria.cambiarActivo(id, solicitud.activo(), usuario.getId()));
    }

    @DeleteMapping("/categorias/{id}")
    public RespuestaApi<Void> eliminarCategoria(
            @PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioCategoria.eliminar(id, usuario.getId());
        return RespuestaApi.exito("Categoría eliminada.");
    }

    @GetMapping("/usuarios")
    public RespuestaApi<List<UsuarioDto>> usuarios() {
        return RespuestaApi.exito(servicioUsuario.listarPersonal());
    }

    @GetMapping("/usuarios/{id}")
    public RespuestaApi<UsuarioDto> usuario(@PathVariable Long id) {
        return RespuestaApi.exito(servicioUsuario.obtener(id));
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaApi<UsuarioDto> crearUsuario(
            @Valid @RequestBody SolicitudUsuario solicitud, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito("Usuario registrado.", servicioUsuario.crear(solicitud, usuario.getId()));
    }

    @PutMapping("/usuarios/{id}")
    public RespuestaApi<UsuarioDto> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudUsuario solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito("Usuario actualizado.", servicioUsuario.actualizar(id, solicitud, usuario.getId()));
    }

    @PatchMapping("/usuarios/{id}/activo")
    public RespuestaApi<UsuarioDto> cambiarActivoUsuario(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudEstadoActivo solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaApi.exito("Estado actualizado.", servicioUsuario.cambiarActivo(id, solicitud.activo(), usuario.getId()));
    }

    @GetMapping("/usuarios/{id}/actividad")
    public RespuestaApi<Pagina<AuditoriaDto>> actividadUsuario(
            @PathVariable Long id, @RequestParam(required = false) Integer pagina) {
        return RespuestaApi.exito(consultaReservas.listarAuditoria(null, id, pagina == null ? 0 : Math.max(pagina, 0), 20));
    }

    @GetMapping("/clientes")
    public RespuestaApi<Pagina<ClienteAdminDto>> clientes(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano) {
        return RespuestaApi.exito(servicioCliente.listar(busqueda, pagina, tamano));
    }

    @PutMapping("/clientes/{id}")
    public RespuestaApi<Void> actualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudClienteAdmin solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioCliente.actualizar(id, solicitud, usuario.getId());
        return RespuestaApi.exito("Cliente actualizado.");
    }

    @PatchMapping("/clientes/{id}/activo")
    public RespuestaApi<Void> cambiarActivoCliente(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudEstadoActivo solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicioCliente.cambiarActivo(id, solicitud.activo(), usuario.getId());
        return RespuestaApi.exito("Estado actualizado.");
    }

    @GetMapping("/ventas")
    public RespuestaApi<VentasDto> ventas(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano) {
        return RespuestaApi.exito(servicioConsulta.listarVentas(
                new FiltroReservas(busqueda, estado, metodoPago, fechaDesde, fechaHasta, pagina, tamano)));
    }

    @GetMapping("/configuracion")
    public RespuestaApi<List<ConfiguracionDto>> configuracion() {
        return RespuestaApi.exito(servicioConfiguracion.listar());
    }

    @GetMapping("/configuracion/integraciones")
    public RespuestaApi<EstadoIntegracionesDto> integraciones() {
        return RespuestaApi.exito(new EstadoIntegracionesDto(
                propiedades.paypal().tieneCredenciales(),
                propiedades.paypal().esProduccion() ? "Producción" : "Sandbox",
                propiedades.paypal().moneda(),
                !servicioConfiguracion.texto(ServicioConfiguracion.PAYPAL_TIPO_CAMBIO).isBlank(),
                servicioPago.yapeDisponible(),
                propiedades.factiliza().tieneToken(),
                servicioCorreo.estaHabilitado()));
    }

    @PutMapping("/configuracion")
    public RespuestaApi<Void> actualizarConfiguracion(
            @Valid @RequestBody SolicitudConfiguracion solicitud, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Map<String, String> aplicados = servicioConfiguracion.actualizar(solicitud.valores());
        if (!aplicados.isEmpty()) {
            servicioAuditoria.registrar(usuario.getId(), "EDITAR_CONFIGURACION", "configuraciones", null,
                    String.join(", ", aplicados.keySet()));
        }
        return RespuestaApi.exito("Configuración guardada.");
    }

    @PostMapping("/configuracion/yape-qr")
    public RespuestaApi<Void> subirQrYape(
            @RequestParam("archivo") MultipartFile archivo, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        String anterior = servicioConfiguracion.texto(ServicioConfiguracion.YAPE_QR);
        String url = almacenamiento.guardarImagen(archivo);
        servicioConfiguracion.actualizar(Map.of(ServicioConfiguracion.YAPE_QR, url));
        almacenamiento.eliminarImagen(anterior);
        servicioAuditoria.registrar(usuario.getId(), "CAMBIAR_QR_YAPE", "configuraciones", ServicioConfiguracion.YAPE_QR, null);
        return RespuestaApi.exito("Código QR actualizado.");
    }

    @GetMapping("/auditoria")
    public RespuestaApi<Pagina<AuditoriaDto>> auditoria(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano) {
        int numero = pagina == null ? 0 : Math.max(pagina, 0);
        int cantidad = tamano == null ? 20 : Math.min(Math.max(tamano, 1), 100);
        return RespuestaApi.exito(consultaReservas.listarAuditoria(busqueda, null, numero, cantidad));
    }
}
