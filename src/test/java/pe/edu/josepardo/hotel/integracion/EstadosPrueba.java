package pe.edu.josepardo.hotel.integracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.servicio.ServicioCierreDiario;
import pe.edu.josepardo.hotel.servicio.ServicioReserva;

class EstadosPrueba extends BasePruebaIntegracion {

    @Autowired
    private ServicioReserva servicioReserva;
    @Autowired
    private ServicioCierreDiario servicioCierre;

    private RequestPostProcessor admin;
    private Long clienteId;

    @BeforeEach
    void preparar() {
        admin = user(autenticado(crearUsuario("admin@prueba.pe", Rol.ADMINISTRADOR)));
        clienteId = crearCliente("cliente@prueba.pe", "70000091").getUsuario().getId();
    }

    private String reservar(LocalDate entrada, long habitacion) {
        return servicioReserva.crearDesdeCarrito(clienteId, carrito(entrada, entrada.plusDays(2), habitacion), null).codigo();
    }

    private ResultActions cambiar(String codigo, Map<String, String> cuerpo) throws Exception {
        return mvc.perform(patch("/api/operaciones/reservas/" + codigo + "/estado").with(csrf()).with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo)));
    }

    private String estadoHabitacion(long id) {
        return jdbc.queryForObject("SELECT estado FROM habitaciones WHERE id = ?", String.class, id);
    }

    private String estadoReserva(String codigo) {
        return jdbc.queryForObject("SELECT estado FROM reservas WHERE codigo = ?", String.class, codigo);
    }

    @Test
    void laHabitacionCambiaDeEstadoConLaReserva() throws Exception {
        String codigo = reservar(LocalDate.now(), 4L);
        assertEquals("DISPONIBLE", estadoHabitacion(4L), "Un pedido sin pagar no aparta la habitación");

        cambiar(codigo, Map.of("estado", "PAGADO", "metodo", "EFECTIVO")).andExpect(status().isOk());
        assertEquals("PAGADO", estadoReserva(codigo));
        assertEquals("RESERVADA", estadoHabitacion(4L));

        cambiar(codigo, Map.of("estado", "CONFIRMADO")).andExpect(status().isOk());
        cambiar(codigo, Map.of("estado", "ENTREGADO")).andExpect(status().isOk());
        assertEquals("OCUPADA", estadoHabitacion(4L));

        cambiar(codigo, Map.of("estado", "FINALIZADO")).andExpect(status().isOk());
        assertEquals("FINALIZADO", estadoReserva(codigo));
        assertEquals("DISPONIBLE", estadoHabitacion(4L));
        assertEquals(5, jdbc.queryForObject(
                "SELECT COUNT(*) FROM historial_estados_reserva h JOIN reservas r ON r.id = h.reserva_id WHERE r.codigo = ?",
                Integer.class, codigo));
    }

    @Test
    void cancelarLiberaLaHabitacionYExigeMotivo() throws Exception {
        String codigo = reservar(LocalDate.now().plusDays(10), 5L);
        cambiar(codigo, Map.of("estado", "PAGADO", "metodo", "EFECTIVO")).andExpect(status().isOk());
        assertEquals("RESERVADA", estadoHabitacion(5L));

        cambiar(codigo, Map.of("estado", "CANCELADO")).andExpect(status().isBadRequest());
        cambiar(codigo, Map.of("estado", "CANCELADO", "motivo", "El cliente desistió")).andExpect(status().isOk());
        assertEquals("CANCELADO", estadoReserva(codigo));
        assertEquals("DISPONIBLE", estadoHabitacion(5L));
    }

    @Test
    void rechazaTransicionesInvalidasYAlCliente() throws Exception {
        String codigo = reservar(LocalDate.now().plusDays(5), 6L);
        cambiar(codigo, Map.of("estado", "ENTREGADO")).andExpect(status().isConflict());
        cambiar(codigo, Map.of("estado", "EXPIRADO")).andExpect(status().isBadRequest());
        cambiar(codigo, Map.of("estado", "INVENTADO")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/operaciones/reservas/" + codigo + "/estado").with(csrf())
                        .with(user(autenticado(repositorioUsuario.findById(clienteId).orElseThrow())))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"PAGADO\",\"metodo\":\"EFECTIVO\"}"))
                .andExpect(status().isForbidden());
        assertEquals("PENDIENTE", estadoReserva(codigo));
    }

    @Test
    void elListadoIndicaLosEstadosSiguientes() throws Exception {
        String codigo = reservar(LocalDate.now().plusDays(3), 7L);
        mvc.perform(get("/api/operaciones/reservas").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.elementos[0].codigo").value(codigo))
                .andExpect(jsonPath("$.datos.elementos[0].estadosSiguientes[0]").value("PAGADO"))
                .andExpect(jsonPath("$.datos.elementos[0].estadosSiguientes[1]").value("CANCELADO"));
    }

    @Test
    void elMantenimientoLoFijaElAdministradorYNoSePuedeReservar() throws Exception {
        String cuerpo = json.writeValueAsString(Map.of("numero", "301", "nombre", "Matrimonial Sol", "categoriaId", 3,
                "descripcion", "En mantenimiento", "capacidad", 2, "precioNoche", "290.00",
                "estado", "MANTENIMIENTO", "caracteristicaIds", List.of(1)));
        mvc.perform(put("/api/administracion/habitaciones/7").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estado").value("MANTENIMIENTO"));
        assertEquals("MANTENIMIENTO", estadoHabitacion(7L));
        mvc.perform(get("/api/publico/habitaciones/7")).andExpect(jsonPath("$.datos.disponible").value(false));

        String codigoOtra = reservar(LocalDate.now(), 8L);
        cambiar(codigoOtra, Map.of("estado", "PAGADO", "metodo", "EFECTIVO")).andExpect(status().isOk());
        String cuerpoOcho = json.writeValueAsString(Map.of("numero", "302", "nombre", "Matrimonial Terraza", "categoriaId", 3,
                "descripcion", "Terraza privada", "capacidad", 2, "precioNoche", "340.00",
                "estado", "DISPONIBLE", "caracteristicaIds", List.of(1)));
        mvc.perform(put("/api/administracion/habitaciones/8").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoOcho))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estado").value("RESERVADA"));
    }

    @Test
    void elCierreDelDiaEliminaLosPedidosNoPagados() throws Exception {
        String noPagada = reservar(LocalDate.now().plusDays(2), 9L);
        String pagada = reservar(LocalDate.now().plusDays(6), 10L);
        cambiar(pagada, Map.of("estado", "PAGADO", "metodo", "EFECTIVO")).andExpect(status().isOk());
        String hospedada = reservar(LocalDate.now(), 11L);
        cambiar(hospedada, Map.of("estado", "PAGADO", "metodo", "EFECTIVO")).andExpect(status().isOk());
        cambiar(hospedada, Map.of("estado", "CONFIRMADO")).andExpect(status().isOk());
        cambiar(hospedada, Map.of("estado", "ENTREGADO")).andExpect(status().isOk());
        jdbc.update("UPDATE reservas SET fecha_entrada = ?, fecha_salida = ? WHERE codigo = ?",
                LocalDate.now().minusDays(2), LocalDate.now(), hospedada);

        ServicioCierreDiario.Resultado resultado = servicioCierre.ejecutar();

        assertEquals(1, resultado.expiradas());
        assertEquals(1, resultado.finalizadas());
        assertEquals(1, resultado.eliminadas());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM reservas WHERE codigo = ?", Integer.class, noPagada));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM detalle_reservas d LEFT JOIN reservas r ON r.id = d.reserva_id WHERE r.id IS NULL",
                Integer.class));
        assertEquals("PAGADO", estadoReserva(pagada));
        assertEquals("RESERVADA", estadoHabitacion(10L));
        assertEquals("FINALIZADO", estadoReserva(hospedada));
        assertEquals("DISPONIBLE", estadoHabitacion(11L));
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM registros_auditoria WHERE accion = 'CIERRE_DEL_DIA'",
                Integer.class) > 0);
    }

    @Test
    void elPlazoDePagoNoPasaDelFinDelDia() {
        String codigo = reservar(LocalDate.now().plusDays(4), 12L);
        LocalDateTime expira = jdbc.queryForObject(
                "SELECT expira_en FROM reservas WHERE codigo = ?", LocalDateTime.class, codigo);
        assertFalse(expira.toLocalDate().isAfter(LocalDate.now()));
    }
}
