package pe.edu.josepardo.hotel.controlador.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.josepardo.hotel.dto.CarritoDto;
import pe.edu.josepardo.hotel.dto.RespuestaApi;
import pe.edu.josepardo.hotel.dto.SolicitudEstancia;
import pe.edu.josepardo.hotel.dto.SolicitudHuespedes;
import pe.edu.josepardo.hotel.dto.SolicitudItemCarrito;
import pe.edu.josepardo.hotel.servicio.Carrito;
import pe.edu.josepardo.hotel.servicio.ServicioCarrito;

@RestController
@RequestMapping("/api/carrito")
public class ControladorCarritoApi {

    public static final String ATRIBUTO_CARRITO = "carrito";

    private final ServicioCarrito servicioCarrito;

    public ControladorCarritoApi(ServicioCarrito servicioCarrito) {
        this.servicioCarrito = servicioCarrito;
    }

    public static Carrito carrito(HttpSession sesion) {
        Object actual = sesion.getAttribute(ATRIBUTO_CARRITO);
        if (actual instanceof Carrito carrito) {
            return carrito;
        }
        Carrito nuevo = new Carrito();
        sesion.setAttribute(ATRIBUTO_CARRITO, nuevo);
        return nuevo;
    }

    @GetMapping
    public RespuestaApi<CarritoDto> obtener(HttpSession sesion) {
        return RespuestaApi.exito(servicioCarrito.obtener(carrito(sesion)));
    }

    @PutMapping("/estancia")
    public RespuestaApi<CarritoDto> cambiarEstancia(@Valid @RequestBody SolicitudEstancia solicitud, HttpSession sesion) {
        return RespuestaApi.exito("Fechas actualizadas.",
                servicioCarrito.cambiarEstancia(carrito(sesion), solicitud.fechaEntrada(), solicitud.fechaSalida()));
    }

    @PostMapping("/items")
    public RespuestaApi<CarritoDto> agregar(@Valid @RequestBody SolicitudItemCarrito solicitud, HttpSession sesion) {
        return RespuestaApi.exito("Habitación añadida a tu reserva.", servicioCarrito.agregar(carrito(sesion), solicitud));
    }

    @PatchMapping("/items/{habitacionId}")
    public RespuestaApi<CarritoDto> cambiarHuespedes(
            @PathVariable Long habitacionId, @Valid @RequestBody SolicitudHuespedes solicitud, HttpSession sesion) {
        return RespuestaApi.exito("Huéspedes actualizados.",
                servicioCarrito.cambiarHuespedes(carrito(sesion), habitacionId, solicitud.huespedes()));
    }

    @DeleteMapping("/items/{habitacionId}")
    public RespuestaApi<CarritoDto> quitar(@PathVariable Long habitacionId, HttpSession sesion) {
        return RespuestaApi.exito("Habitación retirada.", servicioCarrito.quitar(carrito(sesion), habitacionId));
    }

    @DeleteMapping
    public RespuestaApi<CarritoDto> vaciar(HttpSession sesion) {
        return RespuestaApi.exito("Carrito vaciado.", servicioCarrito.vaciar(carrito(sesion)));
    }
}
