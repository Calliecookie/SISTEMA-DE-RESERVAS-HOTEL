package pe.edu.josepardo.hotel.integracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import pe.edu.josepardo.hotel.dto.ReservaCreadaDto;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Cliente;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.modelo.Usuario;
import pe.edu.josepardo.hotel.repositorio.RepositorioReserva;
import pe.edu.josepardo.hotel.servicio.Carrito;
import pe.edu.josepardo.hotel.servicio.ServicioExpiracion;
import pe.edu.josepardo.hotel.servicio.ServicioFlujoReserva;
import pe.edu.josepardo.hotel.servicio.ServicioPago;
import pe.edu.josepardo.hotel.servicio.ServicioReserva;

class ReservaPrueba extends BasePruebaIntegracion {

    @Autowired
    private ServicioReserva servicioReserva;
    @Autowired
    private ServicioExpiracion servicioExpiracion;
    @Autowired
    private ServicioFlujoReserva servicioFlujo;
    @Autowired
    private ServicioPago servicioPago;
    @Autowired
    private RepositorioReserva repositorioReserva;

    private final LocalDate entrada = LocalDate.now().plusDays(20);
    private final LocalDate salida = entrada.plusDays(3);

    private String estado(String codigo) {
        return jdbc.queryForObject("SELECT estado FROM reservas WHERE codigo = ?", String.class, codigo);
    }

    private ExcepcionNegocio reservarEsperandoError(Long usuarioId, Carrito carrito) {
        return assertThrows(ExcepcionNegocio.class, () -> servicioReserva.crearDesdeCarrito(usuarioId, carrito, null));
    }

    @Test
    void registraReservaConDetalleHistorialComprobanteYCorreo() throws Exception {
        Cliente cliente = crearCliente("c@prueba.pe", "70000031");
        Carrito carrito = carrito(entrada, salida, 4L, 7L);
        ReservaCreadaDto creada = servicioReserva.crearDesdeCarrito(cliente.getUsuario().getId(), carrito, "Llegada tarde");

        assertTrue(creada.codigo().matches("^RES-[A-Z2-9]{8}$"));
        assertEquals(0, new BigDecimal("1805.40").compareTo(creada.total()));
        assertEquals("PENDIENTE", estado(creada.codigo()));
        assertNotNull(creada.expiraEn());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM detalle_reservas", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM historial_estados_reserva", Integer.class));
        assertTrue(carrito.getItems().isEmpty());

        String archivo = jdbc.queryForObject("SELECT ruta_archivo FROM comprobantes", String.class);
        byte[] pdf = Files.readAllBytes(Path.of("target/almacenamiento-pruebas/comprobantes", archivo));
        assertEquals("%PDF", new String(pdf, 0, 4));
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
            assertTrue(texto.contains("SON: MIL OCHOCIENTOS CINCO CON 40/100 SOLES"), "La boleta debe mostrar el total en letras");
        }
        assertEquals(1, correosRecibidos().length);
        assertEquals("c@prueba.pe", correosRecibidos()[0].getAllRecipients()[0].toString());
        assertEquals("ENVIADO", jdbc.queryForObject("SELECT correo_estado FROM comprobantes", String.class));
    }

    @Test
    void conservaElPrecioPactadoAunqueCambieLaTarifa() {
        Cliente cliente = crearCliente("c@prueba.pe", "70000032");
        servicioReserva.crearDesdeCarrito(cliente.getUsuario().getId(), carrito(entrada, salida, 4L), null);
        jdbc.update("UPDATE habitaciones SET precio_noche = 999 WHERE id = 4");
        BigDecimal pactado = jdbc.queryForObject("SELECT precio_noche FROM detalle_reservas", BigDecimal.class);
        jdbc.update("UPDATE habitaciones SET precio_noche = 220 WHERE id = 4");
        assertEquals(0, new BigDecimal("220.00").compareTo(pactado));
    }

    @Test
    void impideCruzarFechasEnLaMismaHabitacion() {
        Long primero = crearCliente("a@prueba.pe", "70000033").getUsuario().getId();
        Long segundo = crearCliente("b@prueba.pe", "70000034").getUsuario().getId();
        servicioReserva.crearDesdeCarrito(primero, carrito(entrada, salida, 1L), null);

        assertEquals("HABITACION_OCUPADA", reservarEsperandoError(segundo, carrito(entrada, salida, 1L)).getCodigo());
        assertEquals("HABITACION_OCUPADA", reservarEsperandoError(segundo, carrito(entrada.minusDays(1), entrada.plusDays(1), 1L)).getCodigo());
        assertEquals("HABITACION_OCUPADA", reservarEsperandoError(segundo, carrito(salida.minusDays(1), salida.plusDays(1), 1L)).getCodigo());
        assertEquals("HABITACION_OCUPADA", reservarEsperandoError(segundo, carrito(entrada.minusDays(1), salida.plusDays(1), 1L)).getCodigo());
    }

    @Test
    void permiteEntrarElMismoDiaEnQueOtroSale() {
        Long primero = crearCliente("a@prueba.pe", "70000035").getUsuario().getId();
        Long segundo = crearCliente("b@prueba.pe", "70000036").getUsuario().getId();
        servicioReserva.crearDesdeCarrito(primero, carrito(entrada, salida, 1L), null);
        assertNotNull(servicioReserva.crearDesdeCarrito(segundo, carrito(salida, salida.plusDays(2), 1L), null));
        assertNotNull(servicioReserva.crearDesdeCarrito(segundo, carrito(entrada.minusDays(2), entrada, 1L), null));
    }

    @Test
    void soloUnaSolicitudSimultaneaObtieneLaHabitacion() throws Exception {
        int participantes = 16;
        List<Long> usuarios = new ArrayList<>();
        for (int i = 0; i < participantes; i++) {
            usuarios.add(crearCliente("c" + i + "@prueba.pe", String.format("7100%04d", i)).getUsuario().getId());
        }
        ExecutorService ejecutor = Executors.newFixedThreadPool(participantes);
        CountDownLatch salida_ = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (Long usuario : usuarios) {
            Callable<Boolean> tarea = () -> {
                salida_.await();
                try {
                    servicioReserva.crearDesdeCarrito(usuario, carrito(entrada, salida, 2L), null);
                    return true;
                } catch (ExcepcionNegocio excepcion) {
                    assertEquals("HABITACION_OCUPADA", excepcion.getCodigo());
                    return false;
                }
            };
            resultados.add(ejecutor.submit(tarea));
        }
        salida_.countDown();
        int exitos = 0;
        for (Future<Boolean> resultado : resultados) {
            if (resultado.get()) {
                exitos++;
            }
        }
        ejecutor.shutdown();
        assertEquals(1, exitos);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM reservas", Integer.class));
    }

    @Test
    void reservasSimultaneasConHabitacionesEnDistintoOrdenNoSeInterbloquean() throws Exception {
        Long uno = crearCliente("a@prueba.pe", "70000037").getUsuario().getId();
        Long dos = crearCliente("b@prueba.pe", "70000038").getUsuario().getId();
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        Carrito primero = new Carrito();
        primero.definirEstancia(entrada, salida);
        primero.getItems().put(5L, 1);
        primero.getItems().put(6L, 1);
        Carrito segundo = new Carrito();
        segundo.definirEstancia(entrada, salida);
        segundo.getItems().put(6L, 1);
        segundo.getItems().put(5L, 1);
        List<Future<Boolean>> resultados = List.of(
                ejecutor.submit(() -> intentar(uno, primero)),
                ejecutor.submit(() -> intentar(dos, segundo)));
        int exitos = 0;
        for (Future<Boolean> resultado : resultados) {
            exitos += resultado.get() ? 1 : 0;
        }
        ejecutor.shutdown();
        assertEquals(1, exitos);
    }

    private boolean intentar(Long usuario, Carrito carrito) {
        try {
            servicioReserva.crearDesdeCarrito(usuario, carrito, null);
            return true;
        } catch (ExcepcionNegocio excepcion) {
            return false;
        }
    }

    @Test
    void laTareaExpiraPendientesVencidasYLiberaLaHabitacion() {
        Long primero = crearCliente("a@prueba.pe", "70000039").getUsuario().getId();
        Long segundo = crearCliente("b@prueba.pe", "70000040").getUsuario().getId();
        ReservaCreadaDto reserva = servicioReserva.crearDesdeCarrito(primero, carrito(entrada, salida, 3L), null);
        jdbc.update("UPDATE reservas SET expira_en = ?", LocalDateTime.now().minusMinutes(1));

        assertEquals(1, servicioExpiracion.expirarVencidas());
        assertEquals("EXPIRADO", estado(reserva.codigo()));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM historial_estados_reserva", Integer.class));
        assertEquals(1, repositorioReserva.count());
        assertNotNull(servicioReserva.crearDesdeCarrito(segundo, carrito(entrada, salida, 3L), null));
        assertEquals(0, servicioExpiracion.expirarVencidas());
    }

    @Test
    void unaPendienteVencidaNoBloqueaAunqueLaTareaNoHayaCorrido() {
        Long primero = crearCliente("a@prueba.pe", "70000041").getUsuario().getId();
        Long segundo = crearCliente("b@prueba.pe", "70000042").getUsuario().getId();
        servicioReserva.crearDesdeCarrito(primero, carrito(entrada, salida, 3L), null);
        jdbc.update("UPDATE reservas SET expira_en = ?", LocalDateTime.now().minusMinutes(1));
        assertNotNull(servicioReserva.crearDesdeCarrito(segundo, carrito(entrada, salida, 3L), null));
    }

    @Test
    void recorreElFlujoCompletoHastaFinalizar() {
        Usuario trabajador = crearUsuario("t@prueba.pe", Rol.TRABAJADOR);
        Long cliente = crearCliente("c@prueba.pe", "70000043").getUsuario().getId();
        LocalDate hoy = LocalDate.now();
        String codigo = servicioReserva.crearDesdeCarrito(cliente, carrito(hoy, hoy.plusDays(1), 8L), null).codigo();
        servicioPago.declararPago(codigo, cliente, "EFECTIVO", null);
        servicioPago.confirmarPago(codigo, autenticado(trabajador), null, null);
        assertEquals(EstadoReserva.PAGADO.name(), estado(codigo));
        servicioFlujo.confirmar(codigo, autenticado(trabajador));
        servicioFlujo.registrarIngreso(codigo, autenticado(trabajador));
        assertEquals(EstadoReserva.ENTREGADO.name(), estado(codigo));
        servicioFlujo.registrarSalida(codigo, autenticado(trabajador));
        assertEquals(EstadoReserva.FINALIZADO.name(), estado(codigo));
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM historial_estados_reserva", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM comprobantes", Integer.class));
    }

    @Test
    void noRegistraIngresoAntesDeLaFechaDeEntrada() {
        Usuario trabajador = crearUsuario("t@prueba.pe", Rol.TRABAJADOR);
        Long cliente = crearCliente("c@prueba.pe", "70000044").getUsuario().getId();
        String codigo = servicioReserva.crearDesdeCarrito(cliente, carrito(entrada, salida, 8L), null).codigo();
        servicioPago.confirmarPago(codigo, autenticado(trabajador), "EFECTIVO", null);
        servicioFlujo.confirmar(codigo, autenticado(trabajador));
        ExcepcionNegocio error = assertThrows(ExcepcionNegocio.class,
                () -> servicioFlujo.registrarIngreso(codigo, autenticado(trabajador)));
        assertEquals("INGRESO_ANTICIPADO", error.getCodigo());
    }

    @Test
    void cancelarLiberaLaHabitacionYElClienteSoloCancelaLasSuyas() throws Exception {
        Cliente duena = crearCliente("a@prueba.pe", "70000045");
        Cliente otra = crearCliente("b@prueba.pe", "70000046");
        String codigo = servicioReserva.crearDesdeCarrito(duena.getUsuario().getId(), carrito(entrada, salida, 9L), null).codigo();
        mvc.perform(post("/api/cliente/reservas/" + codigo + "/cancelar").with(csrf()).with(user(autenticado(otra.getUsuario())))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"No corresponde\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/cliente/reservas/" + codigo + "/cancelar").with(csrf()).with(user(autenticado(duena.getUsuario())))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Cambio de planes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true));
        assertEquals("CANCELADO", estado(codigo));
        assertNotNull(servicioReserva.crearDesdeCarrito(otra.getUsuario().getId(), carrito(entrada, salida, 9L), null));
    }

    @Test
    void rechazaReservaDeCarritoVacioOConFechasInvalidas() {
        Long usuario = crearCliente("c@prueba.pe", "70000047").getUsuario().getId();
        assertThrows(ExcepcionNegocio.class, () -> servicioReserva.crearDesdeCarrito(usuario, carrito(entrada, salida), null));
        assertThrows(ExcepcionNegocio.class, () -> servicioReserva.crearDesdeCarrito(
                usuario, carrito(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 1L), null));
    }

    @Test
    void laBaseDeDatosRechazaFechasInvertidas() {
        Long clienteId = crearCliente("c@prueba.pe", "70000048").getId();
        assertThrows(Exception.class, () -> jdbc.update("""
                INSERT INTO reservas (codigo, cliente_id, fecha_entrada, fecha_salida, cantidad_huespedes, subtotal, impuesto, total)
                VALUES ('RES-INVALIDA', ?, '2026-12-10', '2026-12-05', 1, 0, 0, 0)
                """, clienteId));
    }
}
