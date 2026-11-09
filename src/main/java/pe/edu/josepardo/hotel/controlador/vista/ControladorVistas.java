package pe.edu.josepardo.hotel.controlador.vista;

import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.seguridad.UsuarioAutenticado;
import pe.edu.josepardo.hotel.servicio.ServicioPago;

@Controller
public class ControladorVistas {

    private static final Map<String, String> VISTAS_ADMINISTRACION = Map.of(
            "habitaciones", "Habitaciones",
            "categorias", "Categorías",
            "usuarios", "Usuarios",
            "clientes", "Clientes",
            "reservas", "Reservas",
            "ventas", "Ventas",
            "reportes", "Centro de reportes",
            "configuracion", "Configuración",
            "auditoria", "Auditoría");

    private final ServicioPago servicioPago;

    public ControladorVistas(ServicioPago servicioPago) {
        this.servicioPago = servicioPago;
    }

    @GetMapping("/")
    public String inicio() {
        return "cliente/inicio";
    }

    @GetMapping("/habitaciones")
    public String catalogo() {
        return "cliente/catalogo";
    }

    @GetMapping("/habitaciones/{id:\\d+}")
    public String habitacion(@PathVariable Long id, Model modelo) {
        modelo.addAttribute("habitacionId", id);
        return "cliente/habitacion";
    }

    @GetMapping("/carrito")
    public String carrito() {
        return "cliente/carrito";
    }

    @GetMapping("/creditos")
    public String creditos() {
        return "cliente/creditos";
    }

    @GetMapping("/ingresar")
    public String ingresar() {
        return "cliente/ingresar";
    }

    @GetMapping("/registro")
    public String registro() {
        return "cliente/registro";
    }

    @GetMapping("/recuperar-contrasena")
    public String recuperar() {
        return "cliente/recuperar";
    }

    @GetMapping("/recuperar-contrasena/administrador")
    public String recuperarAdministrador() {
        return "administrador/recuperar";
    }

    @GetMapping("/mi-cuenta")
    public String cuenta() {
        return "cliente/cuenta";
    }

    @GetMapping("/mi-cuenta/reservas/{codigo}")
    public String reservaCliente(@PathVariable String codigo, Model modelo) {
        modelo.addAttribute("codigo", codigo);
        return "cliente/reserva";
    }

    @GetMapping("/pagos/paypal/retorno/{codigo}")
    public String retornoPayPal(@PathVariable String codigo, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        boolean completado;
        try {
            completado = servicioPago.completarPayPal(codigo, usuario.getId());
        } catch (ExcepcionNegocio excepcion) {
            completado = false;
        }
        return "redirect:/carrito?reserva=" + codigo + "&paso=5&pago=" + (completado ? "exito" : "fallido");
    }

    @GetMapping("/trabajador")
    public String panelTrabajador() {
        return "trabajador/panel";
    }

    @GetMapping("/trabajador/reservas")
    public String reservasTrabajador(Model modelo) {
        modelo.addAttribute("seccion", "reservas");
        modelo.addAttribute("titulo", "Reservas");
        return "administrador/reservas";
    }

    @GetMapping("/administracion")
    public String dashboard(Model modelo) {
        modelo.addAttribute("seccion", "dashboard");
        modelo.addAttribute("titulo", "Dashboard");
        return "administrador/dashboard";
    }

    @GetMapping("/administracion/{seccion:[a-z]+}")
    public String seccionAdministracion(@PathVariable String seccion, Model modelo) {
        String titulo = VISTAS_ADMINISTRACION.get(seccion);
        if (titulo == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        }
        modelo.addAttribute("seccion", seccion);
        modelo.addAttribute("titulo", titulo);
        return "administrador/" + seccion;
    }
}
