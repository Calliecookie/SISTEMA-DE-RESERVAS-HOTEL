package pe.edu.josepardo.hotel.controlador.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.josepardo.hotel.dto.CalendarioDto;
import pe.edu.josepardo.hotel.dto.CategoriaDto;
import pe.edu.josepardo.hotel.dto.CotizacionDto;
import pe.edu.josepardo.hotel.dto.DisponibilidadDto;
import pe.edu.josepardo.hotel.dto.FiltroCatalogo;
import pe.edu.josepardo.hotel.dto.HabitacionCatalogoDto;
import pe.edu.josepardo.hotel.dto.Pagina;
import pe.edu.josepardo.hotel.dto.RespuestaApi;
import pe.edu.josepardo.hotel.dto.SugerenciasDto;
import pe.edu.josepardo.hotel.servicio.ServicioCatalogo;
import pe.edu.josepardo.hotel.servicio.ServicioConcierge;
import pe.edu.josepardo.hotel.servicio.ServicioDisponibilidad;

@RestController
@RequestMapping("/api/publico")
public class ControladorPublicoApi {

    private final ServicioCatalogo servicioCatalogo;
    private final ServicioDisponibilidad servicioDisponibilidad;
    private final ServicioConcierge servicioConcierge;

    public ControladorPublicoApi(
            ServicioCatalogo servicioCatalogo,
            ServicioDisponibilidad servicioDisponibilidad,
            ServicioConcierge servicioConcierge) {
        this.servicioCatalogo = servicioCatalogo;
        this.servicioDisponibilidad = servicioDisponibilidad;
        this.servicioConcierge = servicioConcierge;
    }

    @GetMapping("/cotizacion")
    public RespuestaApi<CotizacionDto> cotizacion(
            @RequestParam List<Long> ids,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida) {
        return RespuestaApi.exito(servicioConcierge.cotizar(ids, fechaEntrada, fechaSalida));
    }

    @GetMapping("/habitaciones/{id}/sugerencias")
    public RespuestaApi<SugerenciasDto> sugerencias(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida) {
        return RespuestaApi.exito(servicioConcierge.sugerir(id, fechaEntrada, fechaSalida));
    }

    @GetMapping("/habitaciones/{id}/calendario")
    public RespuestaApi<CalendarioDto> calendario(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) Integer dias) {
        return RespuestaApi.exito(servicioConcierge.calendario(id, desde, dias));
    }

    @GetMapping("/categorias")
    public RespuestaApi<List<CategoriaDto>> categorias() {
        return RespuestaApi.exito(servicioCatalogo.categoriasActivas());
    }

    @GetMapping("/habitaciones")
    public RespuestaApi<Pagina<HabitacionCatalogoDto>> habitaciones(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) BigDecimal precioMinimo,
            @RequestParam(required = false) BigDecimal precioMaximo,
            @RequestParam(required = false) Integer capacidad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrada,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida,
            @RequestParam(required = false) Boolean soloDisponibles,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano) {
        FiltroCatalogo filtro = new FiltroCatalogo(busqueda, categoriaId, precioMinimo, precioMaximo, capacidad,
                fechaEntrada, fechaSalida, soloDisponibles, orden, pagina, tamano);
        return RespuestaApi.exito(servicioCatalogo.buscar(filtro));
    }

    @GetMapping("/habitaciones/{id}")
    public RespuestaApi<HabitacionCatalogoDto> habitacion(@PathVariable Long id) {
        return RespuestaApi.exito(servicioCatalogo.detalle(id));
    }

    @GetMapping("/habitaciones/{id}/disponibilidad")
    public RespuestaApi<DisponibilidadDto> disponibilidad(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida) {
        return RespuestaApi.exito(servicioDisponibilidad.consultar(id, fechaEntrada, fechaSalida));
    }
}
