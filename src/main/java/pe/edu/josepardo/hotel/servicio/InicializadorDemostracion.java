package pe.edu.josepardo.hotel.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.configuracion.PropiedadesHotel;
import pe.edu.josepardo.hotel.modelo.Actor;
import pe.edu.josepardo.hotel.modelo.Cliente;
import pe.edu.josepardo.hotel.modelo.DetalleReserva;
import pe.edu.josepardo.hotel.modelo.EstadoOperativoHabitacion;
import pe.edu.josepardo.hotel.modelo.EstadoPago;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.modelo.Habitacion;
import pe.edu.josepardo.hotel.modelo.HistorialEstadoReserva;
import pe.edu.josepardo.hotel.modelo.MetodoPago;
import pe.edu.josepardo.hotel.modelo.Pago;
import pe.edu.josepardo.hotel.modelo.Reserva;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.modelo.Usuario;
import pe.edu.josepardo.hotel.repositorio.RepositorioCliente;
import pe.edu.josepardo.hotel.repositorio.RepositorioHabitacion;
import pe.edu.josepardo.hotel.repositorio.RepositorioHistorialEstado;
import pe.edu.josepardo.hotel.repositorio.RepositorioPago;
import pe.edu.josepardo.hotel.repositorio.RepositorioReserva;
import pe.edu.josepardo.hotel.repositorio.RepositorioUsuario;

@Component
@Order(2)
public class InicializadorDemostracion implements ApplicationRunner {

    private static final Logger REGISTRO = LoggerFactory.getLogger(InicializadorDemostracion.class);
    private static final String CORREO_TRABAJADOR = "recepcion@hotelintiwasi.pe";
    private static final String CORREO_CLIENTE = "cliente@hotelintiwasi.pe";
    private static final String[][] CLIENTES = {
            {"Lucía", "Quispe Mamani", "70000001"},
            {"Mateo", "Flores Huamán", "70000002"},
            {"Valeria", "Rojas Paredes", "70000003"},
            {"Diego", "Castillo Vega", "70000004"},
            {"Camila", "Torres Salazar", "70000005"},
            {"Sebastián", "Mendoza Ríos", "70000006"}
    };

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioCliente repositorioCliente;
    private final RepositorioHabitacion repositorioHabitacion;
    private final RepositorioReserva repositorioReserva;
    private final RepositorioPago repositorioPago;
    private final RepositorioHistorialEstado repositorioHistorial;
    private final PasswordEncoder codificador;
    private final CalculadoraImportes calculadora;
    private final ServicioConfiguracion configuracion;
    private final PropiedadesHotel.Demostracion propiedades;

    public InicializadorDemostracion(
            RepositorioUsuario repositorioUsuario,
            RepositorioCliente repositorioCliente,
            RepositorioHabitacion repositorioHabitacion,
            RepositorioReserva repositorioReserva,
            RepositorioPago repositorioPago,
            RepositorioHistorialEstado repositorioHistorial,
            PasswordEncoder codificador,
            CalculadoraImportes calculadora,
            ServicioConfiguracion configuracion,
            PropiedadesHotel propiedades) {
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioCliente = repositorioCliente;
        this.repositorioHabitacion = repositorioHabitacion;
        this.repositorioReserva = repositorioReserva;
        this.repositorioPago = repositorioPago;
        this.repositorioHistorial = repositorioHistorial;
        this.codificador = codificador;
        this.calculadora = calculadora;
        this.configuracion = configuracion;
        this.propiedades = propiedades.demostracion();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (!propiedades.habilitada() || repositorioUsuario.existeCorreo(CORREO_TRABAJADOR)) {
            return;
        }
        String contrasena = propiedades.contrasena();
        if (contrasena == null || contrasena.length() < 8) {
            REGISTRO.warn("DEMO_HABILITADA está activo, pero falta DEMO_CONTRASENA (mínimo 8 caracteres).");
            return;
        }
        String hash = codificador.encode(contrasena);
        crearUsuario(CORREO_TRABAJADOR, hash, Rol.TRABAJADOR, "Rosa", "Huamán Ccori");
        List<Cliente> clientes = new ArrayList<>();
        clientes.add(crearCliente(CORREO_CLIENTE, hash, "María", "Gonzales Apaza", "70000000"));
        for (int i = 0; i < CLIENTES.length; i++) {
            clientes.add(crearCliente(
                    "huesped" + (i + 1) + "@hotelintiwasi.pe", hash, CLIENTES[i][0], CLIENTES[i][1], CLIENTES[i][2]));
        }
        List<Habitacion> habitaciones = repositorioHabitacion.findAll().stream()
                .filter(habitacion -> habitacion.getEstadoOperativo() == EstadoOperativoHabitacion.OPERATIVA)
                .toList();
        if (habitaciones.isEmpty()) {
            REGISTRO.warn("No hay habitaciones. Ejecuta base-datos/datos-iniciales.sql antes de la demostración.");
            return;
        }
        generarHistorial(clientes, habitaciones);
        generarOperacionDelDia(clientes, habitaciones);
        REGISTRO.info("Datos de demostración creados");
    }

    private void generarHistorial(List<Cliente> clientes, List<Habitacion> habitaciones) {
        Random azar = new Random(20261101L);
        LocalDate hoy = LocalDate.now();
        int indice = 1;
        for (int diasAtras = 170; diasAtras >= 8; diasAtras -= 3) {
            LocalDate entrada = hoy.minusDays(diasAtras);
            int noches = 1 + azar.nextInt(4);
            Cliente cliente = clientes.get(azar.nextInt(clientes.size()));
            Habitacion habitacion = habitaciones.get(azar.nextInt(habitaciones.size()));
            int caso = azar.nextInt(10);
            EstadoReserva estado = caso == 0 ? EstadoReserva.CANCELADO : caso == 1 ? EstadoReserva.EXPIRADO : EstadoReserva.FINALIZADO;
            MetodoPago metodo = azar.nextInt(3) == 0 ? MetodoPago.EFECTIVO : azar.nextBoolean() ? MetodoPago.YAPE : MetodoPago.PAYPAL;
            crearReserva(indice++, cliente, habitacion, entrada, noches, estado, metodo, entrada.minusDays(2).atTime(11, 0));
        }
    }

    private void generarOperacionDelDia(List<Cliente> clientes, List<Habitacion> habitaciones) {
        LocalDate hoy = LocalDate.now();
        int indice = 900;
        int cantidad = habitaciones.size();
        crearReserva(indice++, clientes.get(0), habitaciones.get(0 % cantidad), hoy.minusDays(1), 3, EstadoReserva.ENTREGADO, MetodoPago.YAPE, hoy.minusDays(3).atTime(9, 30));
        crearReserva(indice++, clientes.get(1), habitaciones.get(1 % cantidad), hoy.minusDays(2), 2, EstadoReserva.ENTREGADO, MetodoPago.EFECTIVO, hoy.minusDays(4).atTime(16, 10));
        crearReserva(indice++, clientes.get(2), habitaciones.get(2 % cantidad), hoy, 2, EstadoReserva.CONFIRMADO, MetodoPago.PAYPAL, hoy.minusDays(1).atTime(18, 45));
        crearReserva(indice++, clientes.get(3), habitaciones.get(3 % cantidad), hoy.plusDays(1), 2, EstadoReserva.PAGADO, MetodoPago.YAPE, hoy.atTime(8, 20));
        Reserva pendiente = crearReserva(indice, clientes.get(4), habitaciones.get(4 % cantidad), hoy.plusDays(3), 2, EstadoReserva.PENDIENTE, MetodoPago.YAPE, LocalDateTime.now().minusHours(1));
        pendiente.setExpiraEn(LocalDateTime.now().plusHours(12));
    }

    private Reserva crearReserva(
            int indice, Cliente cliente, Habitacion habitacion, LocalDate entrada, int noches,
            EstadoReserva estado, MetodoPago metodo, LocalDateTime creadoEn) {
        CalculadoraImportes.Resumen importes = calculadora.calcular(
                List.of(habitacion.getPrecioNoche()), noches, configuracion.parametrosImporte());
        CalculadoraImportes.Linea linea = importes.lineas().get(0);
        Reserva reserva = new Reserva();
        reserva.setCodigo(String.format("RES-DEMO%04d", indice));
        reserva.setCliente(cliente);
        reserva.setFechaEntrada(entrada);
        reserva.setFechaSalida(entrada.plusDays(noches));
        reserva.setCantidadHuespedes(1);
        reserva.setSubtotal(importes.subtotal());
        reserva.setDescuento(importes.descuento());
        reserva.setImpuesto(importes.impuesto());
        reserva.setTotal(importes.total());
        reserva.setEstado(estado);
        reserva.setCreadoEn(creadoEn);
        DetalleReserva detalle = new DetalleReserva();
        detalle.setHabitacion(habitacion);
        detalle.setCantidadHuespedes(1);
        detalle.setPrecioNoche(linea.precioNoche());
        detalle.setNoches(noches);
        detalle.setSubtotal(linea.subtotal());
        detalle.setDescuento(linea.descuento());
        detalle.setImpuesto(linea.impuesto());
        detalle.setTotal(linea.total());
        reserva.agregarDetalle(detalle);

        boolean pagada = estado == EstadoReserva.FINALIZADO || estado == EstadoReserva.ENTREGADO
                || estado == EstadoReserva.CONFIRMADO || estado == EstadoReserva.PAGADO;
        LocalDateTime fechaPago = creadoEn.plusHours(3);
        if (pagada) {
            reserva.setMetodoPago(metodo);
            reserva.setFechaPago(fechaPago);
        }
        repositorioReserva.save(reserva);

        registrarHistorial(reserva, null, EstadoReserva.PENDIENTE, Actor.CLIENTE, creadoEn);
        if (pagada) {
            Pago pago = new Pago();
            pago.setReserva(reserva);
            pago.setMetodo(metodo);
            pago.setMonto(reserva.getTotal());
            pago.setEstado(EstadoPago.CONFIRMADO);
            pago.setNumeroOperacion(metodo == MetodoPago.EFECTIVO ? null : String.format("DEMO%06d", indice));
            pago.setRegistradoPor(cliente.getUsuario());
            pago.setFechaPago(fechaPago);
            repositorioPago.save(pago);
            registrarHistorial(reserva, EstadoReserva.PENDIENTE, EstadoReserva.PAGADO, Actor.TRABAJADOR, fechaPago);
        }
        if (estado == EstadoReserva.CANCELADO || estado == EstadoReserva.EXPIRADO) {
            registrarHistorial(reserva, EstadoReserva.PENDIENTE, estado,
                    estado == EstadoReserva.EXPIRADO ? Actor.SISTEMA : Actor.CLIENTE, creadoEn.plusDays(1));
        }
        if (estado == EstadoReserva.CONFIRMADO || estado == EstadoReserva.ENTREGADO || estado == EstadoReserva.FINALIZADO) {
            registrarHistorial(reserva, EstadoReserva.PAGADO, EstadoReserva.CONFIRMADO, Actor.TRABAJADOR, fechaPago.plusHours(1));
        }
        if (estado == EstadoReserva.ENTREGADO || estado == EstadoReserva.FINALIZADO) {
            registrarHistorial(reserva, EstadoReserva.CONFIRMADO, EstadoReserva.ENTREGADO, Actor.TRABAJADOR, entrada.atTime(14, 0));
        }
        if (estado == EstadoReserva.FINALIZADO) {
            registrarHistorial(reserva, EstadoReserva.ENTREGADO, EstadoReserva.FINALIZADO, Actor.TRABAJADOR, reserva.getFechaSalida().atTime(11, 0));
        }
        return reserva;
    }

    private void registrarHistorial(Reserva reserva, EstadoReserva origen, EstadoReserva destino, Actor actor, LocalDateTime fecha) {
        HistorialEstadoReserva historial = new HistorialEstadoReserva();
        historial.setReserva(reserva);
        historial.setEstadoAnterior(origen);
        historial.setEstadoNuevo(destino);
        historial.setActor(actor);
        historial.setMotivo("Datos de demostración");
        historial.setCreadoEn(fecha);
        repositorioHistorial.save(historial);
    }

    private Usuario crearUsuario(String correo, String hash, Rol rol, String nombres, String apellidos) {
        Usuario usuario = new Usuario();
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(hash);
        usuario.setRol(rol);
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        return repositorioUsuario.save(usuario);
    }

    private Cliente crearCliente(String correo, String hash, String nombres, String apellidos, String dni) {
        Cliente cliente = new Cliente();
        cliente.setUsuario(crearUsuario(correo, hash, Rol.CLIENTE, nombres, apellidos));
        cliente.setDni(dni);
        return repositorioCliente.save(cliente);
    }
}
