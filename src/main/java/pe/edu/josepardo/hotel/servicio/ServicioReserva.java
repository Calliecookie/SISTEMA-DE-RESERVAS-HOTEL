package pe.edu.josepardo.hotel.servicio;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.ReservaCreadaDto;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Actor;
import pe.edu.josepardo.hotel.modelo.Cliente;
import pe.edu.josepardo.hotel.modelo.DetalleReserva;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.modelo.Habitacion;
import pe.edu.josepardo.hotel.modelo.Reserva;
import pe.edu.josepardo.hotel.repositorio.RepositorioCliente;
import pe.edu.josepardo.hotel.repositorio.RepositorioHabitacion;
import pe.edu.josepardo.hotel.repositorio.RepositorioReserva;

@Service
public class ServicioReserva {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGITUD_CODIGO = 8;

    private final RepositorioReserva repositorioReserva;
    private final RepositorioCliente repositorioCliente;
    private final RepositorioHabitacion repositorioHabitacion;
    private final ServicioDisponibilidad servicioDisponibilidad;
    private final ServicioFlujoReserva servicioFlujo;
    private final ValidadorEstancia validadorEstancia;
    private final CalculadoraImportes calculadora;
    private final ServicioConfiguracion configuracion;
    private final ApplicationEventPublisher publicador;
    private final SecureRandom aleatorio = new SecureRandom();

    public ServicioReserva(
            RepositorioReserva repositorioReserva,
            RepositorioCliente repositorioCliente,
            RepositorioHabitacion repositorioHabitacion,
            ServicioDisponibilidad servicioDisponibilidad,
            ServicioFlujoReserva servicioFlujo,
            ValidadorEstancia validadorEstancia,
            CalculadoraImportes calculadora,
            ServicioConfiguracion configuracion,
            ApplicationEventPublisher publicador) {
        this.repositorioReserva = repositorioReserva;
        this.repositorioCliente = repositorioCliente;
        this.repositorioHabitacion = repositorioHabitacion;
        this.servicioDisponibilidad = servicioDisponibilidad;
        this.servicioFlujo = servicioFlujo;
        this.validadorEstancia = validadorEstancia;
        this.calculadora = calculadora;
        this.configuracion = configuracion;
        this.publicador = publicador;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservaCreadaDto crearDesdeCarrito(Long usuarioId, Carrito carrito, String observaciones) {
        Map<Long, Integer> items = carrito.getItems();
        if (items.isEmpty()) {
            throw ExcepcionNegocio.solicitudInvalida("Tu carrito está vacío.");
        }
        Cliente cliente = repositorioCliente.buscarPorUsuarioId(usuarioId)
                .orElseThrow(() -> ExcepcionNegocio.prohibido("Solo los clientes pueden reservar."));
        if (!cliente.getUsuario().isActivo()) {
            throw ExcepcionNegocio.prohibido("Tu cuenta está desactivada.");
        }
        LocalDate entrada = carrito.getFechaEntrada();
        LocalDate salida = carrito.getFechaSalida();
        int noches = validadorEstancia.validar(entrada, salida, LocalDate.now());

        Set<Long> ids = new TreeSet<>(items.keySet());
        List<Habitacion> habitaciones = repositorioHabitacion.bloquearPorIds(ids);
        if (habitaciones.size() != ids.size()) {
            throw ExcepcionNegocio.conflicto("HABITACION_NO_DISPONIBLE", "Una de las habitaciones ya no existe.");
        }
        for (Habitacion habitacion : habitaciones) {
            if (!habitacion.esReservable()) {
                throw ExcepcionNegocio.conflicto("HABITACION_NO_DISPONIBLE",
                        "La habitación " + habitacion.getNumero() + " ya no se puede reservar.");
            }
            int huespedes = items.get(habitacion.getId());
            if (huespedes < 1 || huespedes > habitacion.getCapacidad()) {
                throw ExcepcionNegocio.solicitudInvalida(
                        "La habitación " + habitacion.getNumero() + " admite hasta " + habitacion.getCapacidad() + " huéspedes.");
            }
        }
        Set<Long> ocupadas = servicioDisponibilidad.ocupadas(ids, entrada, salida);
        if (!ocupadas.isEmpty()) {
            String numeros = habitaciones.stream()
                    .filter(habitacion -> ocupadas.contains(habitacion.getId()))
                    .map(Habitacion::getNumero)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            throw ExcepcionNegocio.conflicto("HABITACION_OCUPADA",
                    "La habitación " + numeros + " acaba de ser reservada para esas fechas.");
        }

        List<BigDecimal> precios = new ArrayList<>();
        habitaciones.forEach(habitacion -> precios.add(habitacion.getPrecioNoche()));
        CalculadoraImportes.Resumen importes = calculadora.calcular(precios, noches, configuracion.parametrosImporte());

        Reserva reserva = new Reserva();
        reserva.setCodigo(generarCodigo());
        reserva.setCliente(cliente);
        reserva.setFechaEntrada(entrada);
        reserva.setFechaSalida(salida);
        reserva.setSubtotal(importes.subtotal());
        reserva.setDescuento(importes.descuento());
        reserva.setImpuesto(importes.impuesto());
        reserva.setTotal(importes.total());
        reserva.setEstado(EstadoReserva.PENDIENTE);
        // El pedido que no se paga hasta el final del día se elimina en el cierre diario.
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime plazo = ahora.plusHours(configuracion.entero(ServicioConfiguracion.HORAS_EXPIRACION, 24));
        LocalDateTime finDelDia = ahora.toLocalDate().atTime(LocalTime.of(23, 59));
        reserva.setExpiraEn(plazo.isBefore(finDelDia) ? plazo : finDelDia);
        reserva.setObservaciones(limpiar(observaciones));

        int totalHuespedes = 0;
        for (int i = 0; i < habitaciones.size(); i++) {
            Habitacion habitacion = habitaciones.get(i);
            CalculadoraImportes.Linea linea = importes.lineas().get(i);
            int huespedes = items.get(habitacion.getId());
            totalHuespedes += huespedes;
            DetalleReserva detalle = new DetalleReserva();
            detalle.setHabitacion(habitacion);
            detalle.setCantidadHuespedes(huespedes);
            detalle.setPrecioNoche(linea.precioNoche());
            detalle.setNoches(linea.noches());
            detalle.setSubtotal(linea.subtotal());
            detalle.setDescuento(linea.descuento());
            detalle.setImpuesto(linea.impuesto());
            detalle.setTotal(linea.total());
            reserva.agregarDetalle(detalle);
        }
        reserva.setCantidadHuespedes(totalHuespedes);
        repositorioReserva.save(reserva);
        servicioFlujo.registrarInicio(reserva, Actor.CLIENTE, usuarioId);
        publicador.publishEvent(new EventoComprobanteRequerido(reserva.getId()));
        carrito.vaciar();
        return new ReservaCreadaDto(reserva.getCodigo(), reserva.getTotal(), reserva.getExpiraEn());
    }

    private String generarCodigo() {
        String codigo;
        do {
            StringBuilder texto = new StringBuilder("RES-");
            for (int i = 0; i < LONGITUD_CODIGO; i++) {
                texto.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
            }
            codigo = texto.toString();
        } while (repositorioReserva.existeCodigo(codigo));
        return codigo;
    }

    private String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}
