package pe.edu.josepardo.hotel.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.CarritoDto;
import pe.edu.josepardo.hotel.dto.ItemCarritoDto;
import pe.edu.josepardo.hotel.dto.SolicitudItemCarrito;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Habitacion;
import pe.edu.josepardo.hotel.repositorio.RepositorioHabitacion;

@Service
public class ServicioCarrito {

    private static final int MAXIMO_HABITACIONES = 5;

    private final RepositorioHabitacion repositorioHabitacion;
    private final ServicioDisponibilidad servicioDisponibilidad;
    private final ValidadorEstancia validadorEstancia;
    private final CalculadoraImportes calculadora;
    private final ServicioConfiguracion configuracion;

    public ServicioCarrito(
            RepositorioHabitacion repositorioHabitacion,
            ServicioDisponibilidad servicioDisponibilidad,
            ValidadorEstancia validadorEstancia,
            CalculadoraImportes calculadora,
            ServicioConfiguracion configuracion) {
        this.repositorioHabitacion = repositorioHabitacion;
        this.servicioDisponibilidad = servicioDisponibilidad;
        this.validadorEstancia = validadorEstancia;
        this.calculadora = calculadora;
        this.configuracion = configuracion;
    }

    @Transactional(readOnly = true)
    public CarritoDto obtener(Carrito carrito) {
        Map<Long, Integer> items = carrito.getItems();
        Map<Long, Habitacion> habitaciones = items.isEmpty()
                ? Map.of()
                : repositorioHabitacion.buscarConCategoria(items.keySet()).stream()
                        .collect(Collectors.toMap(Habitacion::getId, Function.identity()));
        items.keySet().removeIf(id -> !habitaciones.containsKey(id));

        List<String> avisos = new ArrayList<>();
        int noches = 0;
        Set<Long> ocupadas = Set.of();
        boolean fechasValidas = false;
        if (carrito.getFechaEntrada() != null && carrito.getFechaSalida() != null) {
            try {
                noches = validadorEstancia.validar(carrito.getFechaEntrada(), carrito.getFechaSalida(), LocalDate.now());
                fechasValidas = true;
                ocupadas = servicioDisponibilidad.ocupadas(items.keySet(), carrito.getFechaEntrada(), carrito.getFechaSalida());
            } catch (ExcepcionNegocio excepcion) {
                avisos.add(excepcion.getMessage());
            }
        }

        List<BigDecimal> precios = new ArrayList<>();
        List<ItemCarritoDto> elementos = new ArrayList<>();
        boolean todasDisponibles = true;
        for (Map.Entry<Long, Integer> item : items.entrySet()) {
            Habitacion habitacion = habitaciones.get(item.getKey());
            boolean disponible = habitacion.esReservable() && !ocupadas.contains(habitacion.getId());
            String aviso = null;
            if (!habitacion.esReservable()) {
                aviso = "Esta habitación ya no se encuentra disponible.";
            } else if (ocupadas.contains(habitacion.getId())) {
                aviso = "No está disponible en las fechas elegidas.";
            }
            todasDisponibles &= disponible;
            precios.add(habitacion.getPrecioNoche());
            elementos.add(new ItemCarritoDto(
                    habitacion.getId(),
                    habitacion.getNumero(),
                    habitacion.getNombre(),
                    habitacion.getCategoria().getNombre(),
                    habitacion.getImagenPrincipal(),
                    habitacion.getCapacidad(),
                    item.getValue(),
                    habitacion.getPrecioNoche(),
                    noches > 0 ? habitacion.getPrecioNoche().multiply(BigDecimal.valueOf(noches)) : BigDecimal.ZERO,
                    disponible,
                    aviso));
        }

        BigDecimal cero = BigDecimal.ZERO.setScale(2);
        CalculadoraImportes.Parametros parametros = configuracion.parametrosImporte();
        CalculadoraImportes.Resumen resumen = noches > 0 && !precios.isEmpty()
                ? calculadora.calcular(precios, noches, parametros)
                : new CalculadoraImportes.Resumen(noches, cero, cero, BigDecimal.ZERO, cero, parametros.porcentajeImpuesto(), cero, List.of());
        int totalHuespedes = items.values().stream().mapToInt(Integer::intValue).sum();
        boolean listo = fechasValidas && !elementos.isEmpty() && todasDisponibles;
        return new CarritoDto(
                carrito.getFechaEntrada(),
                carrito.getFechaSalida(),
                noches,
                elementos,
                elementos.size(),
                totalHuespedes,
                resumen.subtotal(),
                resumen.descuento(),
                resumen.porcentajeDescuento(),
                resumen.impuesto(),
                resumen.porcentajeImpuesto(),
                resumen.total(),
                listo,
                avisos);
    }

    @Transactional(readOnly = true)
    public CarritoDto agregar(Carrito carrito, SolicitudItemCarrito solicitud) {
        if (!carrito.getItems().containsKey(solicitud.habitacionId()) && carrito.getItems().size() >= MAXIMO_HABITACIONES) {
            throw ExcepcionNegocio.solicitudInvalida("Puedes reservar hasta " + MAXIMO_HABITACIONES + " habitaciones a la vez.");
        }
        Habitacion habitacion = repositorioHabitacion.findById(solicitud.habitacionId())
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("La habitación no existe."));
        if (!habitacion.esReservable()) {
            throw ExcepcionNegocio.conflicto("HABITACION_NO_RESERVABLE", "Esta habitación no se puede reservar en este momento.");
        }
        validarHuespedes(habitacion, solicitud.huespedes());
        if (solicitud.fechaEntrada() != null || solicitud.fechaSalida() != null) {
            validadorEstancia.validar(solicitud.fechaEntrada(), solicitud.fechaSalida(), LocalDate.now());
            carrito.definirEstancia(solicitud.fechaEntrada(), solicitud.fechaSalida());
        }
        carrito.getItems().put(habitacion.getId(), solicitud.huespedes());
        return obtener(carrito);
    }

    @Transactional(readOnly = true)
    public CarritoDto cambiarEstancia(Carrito carrito, LocalDate fechaEntrada, LocalDate fechaSalida) {
        validadorEstancia.validar(fechaEntrada, fechaSalida, LocalDate.now());
        carrito.definirEstancia(fechaEntrada, fechaSalida);
        return obtener(carrito);
    }

    @Transactional(readOnly = true)
    public CarritoDto cambiarHuespedes(Carrito carrito, Long habitacionId, int huespedes) {
        if (!carrito.getItems().containsKey(habitacionId)) {
            throw ExcepcionNegocio.noEncontrado("La habitación no está en tu carrito.");
        }
        Habitacion habitacion = repositorioHabitacion.findById(habitacionId)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("La habitación no existe."));
        validarHuespedes(habitacion, huespedes);
        carrito.getItems().put(habitacionId, huespedes);
        return obtener(carrito);
    }

    @Transactional(readOnly = true)
    public CarritoDto quitar(Carrito carrito, Long habitacionId) {
        carrito.getItems().remove(habitacionId);
        return obtener(carrito);
    }

    @Transactional(readOnly = true)
    public CarritoDto vaciar(Carrito carrito) {
        carrito.vaciar();
        return obtener(carrito);
    }

    private void validarHuespedes(Habitacion habitacion, int huespedes) {
        if (huespedes > habitacion.getCapacidad()) {
            throw ExcepcionNegocio.solicitudInvalida(
                    "La habitación " + habitacion.getNumero() + " admite hasta " + habitacion.getCapacidad() + " huéspedes.");
        }
    }
}
