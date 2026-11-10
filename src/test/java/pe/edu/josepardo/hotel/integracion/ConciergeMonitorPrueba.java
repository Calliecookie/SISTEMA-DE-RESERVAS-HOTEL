package pe.edu.josepardo.hotel.integracion;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pe.edu.josepardo.hotel.dto.AlternativaDto;
import pe.edu.josepardo.hotel.dto.CalendarioDto;
import pe.edu.josepardo.hotel.dto.CotizacionDto;
import pe.edu.josepardo.hotel.dto.MapaHabitacionesDto;
import pe.edu.josepardo.hotel.dto.PlanificadorDto;
import pe.edu.josepardo.hotel.dto.PuertaDto;
import pe.edu.josepardo.hotel.dto.SugerenciasDto;
import pe.edu.josepardo.hotel.dto.TramoDto;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.modelo.Usuario;
import pe.edu.josepardo.hotel.servicio.ServicioConcierge;
import pe.edu.josepardo.hotel.servicio.ServicioFlujoReserva;
import pe.edu.josepardo.hotel.servicio.ServicioMonitor;
import pe.edu.josepardo.hotel.servicio.ServicioPago;
import pe.edu.josepardo.hotel.servicio.ServicioReserva;

class ConciergeMonitorPrueba extends BasePruebaIntegracion {

    @Autowired
    private ServicioReserva servicioReserva;
    @Autowired
    private ServicioPago servicioPago;
    @Autowired
    private ServicioFlujoReserva servicioFlujo;
    @Autowired
    private ServicioConcierge servicioConcierge;
    @Autowired
    private ServicioMonitor servicioMonitor;

    private final LocalDate hoy = LocalDate.now();
    private Usuario administrador;
    private Usuario trabajador;
    private Long clienteId;

    @BeforeEach
    void preparar() {
        administrador = crearUsuario("admin@prueba.pe", Rol.ADMINISTRADOR);
        trabajador = crearUsuario("recepcion@prueba.pe", Rol.TRABAJADOR);
        clienteId = crearCliente("ana.torres@prueba.pe", "70000081").getUsuario().getId();
    }

    private String reservarPagada(LocalDate entrada, int noches, long habitacion) {
        String codigo = servicioReserva
                .crearDesdeCarrito(clienteId, carrito(entrada, entrada.plusDays(noches), habitacion), null).codigo();
        servicioPago.confirmarPago(codigo, autenticado(administrador), "EFECTIVO", null);
        return codigo;
    }

    private PuertaDto puerta(MapaHabitacionesDto mapa, String numero) {
        return mapa.pisos().stream()
                .flatMap(piso -> piso.habitaciones().stream())
                .filter(item -> item.numero().equals(numero))
                .findFirst().orElseThrow();
    }

    @Test
    void cotizaConIgvYAvisaCuantasNochesFaltanParaElDescuento() {
        LocalDate entrada = hoy.plusDays(20);
        CotizacionDto corta = servicioConcierge.cotizar(List.of(4L, 7L), entrada, entrada.plusDays(3));
        assertEquals(3, corta.noches());
        assertEquals(2, corta.lineas().size());
        assertEquals(0, new BigDecimal("1530.00").compareTo(corta.resumen().subtotal()));
        assertEquals(0, new BigDecimal("275.40").compareTo(corta.resumen().impuesto()));
        assertEquals(0, new BigDecimal("1805.40").compareTo(corta.resumen().total()));
        assertEquals(0, new BigDecimal("601.80").compareTo(corta.promedioPorNoche()));
        assertEquals(2, corta.nochesParaDescuento());
        assertEquals(0, new BigDecimal("127.50").compareTo(corta.ahorroPotencial()));
        assertTrue(corta.todasDisponibles());

        CotizacionDto larga = servicioConcierge.cotizar(List.of(4L), entrada, entrada.plusDays(5));
        assertEquals(0, larga.nochesParaDescuento());
        assertEquals(0, new BigDecimal("55.00").compareTo(larga.resumen().descuento()));
    }

    @Test
    void laCotizacionEsPublicaYValidaSusLimites() throws Exception {
        LocalDate entrada = hoy.plusDays(10);
        mvc.perform(get("/api/publico/cotizacion").param("ids", "1,2,3")
                        .param("fechaEntrada", entrada.toString()).param("fechaSalida", entrada.plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.lineas", hasSize(3)))
                .andExpect(jsonPath("$.datos.lineas[0].habitacion.caracteristicas", hasSize(greaterThan(0))));
        mvc.perform(get("/api/publico/cotizacion").param("ids", "1,2,3,4,5")
                        .param("fechaEntrada", entrada.toString()).param("fechaSalida", entrada.plusDays(2).toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/publico/cotizacion").param("ids", "9999")
                        .param("fechaEntrada", entrada.toString()).param("fechaSalida", entrada.plusDays(2).toString()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/publico/cotizacion").param("ids", "1")
                        .param("fechaEntrada", entrada.toString()).param("fechaSalida", entrada.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sugiereLaProximaFechaLibreYHabitacionesAfines() {
        LocalDate entrada = hoy.plusDays(12);
        reservarPagada(entrada, 3, 4L);
        reservarPagada(entrada.plusDays(3), 2, 4L);

        SugerenciasDto sugerencias = servicioConcierge.sugerir(4L, entrada.plusDays(1), entrada.plusDays(3));

        assertFalse(sugerencias.disponible());
        assertEquals(entrada.plusDays(5), sugerencias.proximaEntrada());
        assertEquals(entrada.plusDays(7), sugerencias.proximaSalida());
        assertEquals(3, sugerencias.alternativas().size());
        AlternativaDto primera = sugerencias.alternativas().get(0);
        assertEquals("Superior", primera.habitacion().categoria());
        assertTrue(primera.habitacion().disponible());
        assertTrue(primera.afinidad() >= sugerencias.alternativas().get(2).afinidad());
        assertFalse(primera.motivos().isEmpty());

        assertTrue(servicioConcierge.sugerir(5L, entrada, entrada.plusDays(2)).disponible());
    }

    @Test
    void elCalendarioMarcaLasNochesOcupadas() throws Exception {
        LocalDate entrada = hoy.plusDays(3);
        reservarPagada(entrada, 2, 6L);

        CalendarioDto calendario = servicioConcierge.calendario(6L, hoy, 14);

        assertEquals(14, calendario.dias().size());
        assertEquals(12, calendario.diasDisponibles());
        assertTrue(calendario.dias().get(2).disponible());
        assertFalse(calendario.dias().get(3).disponible());
        assertFalse(calendario.dias().get(4).disponible());
        assertTrue(calendario.dias().get(5).disponible());
        assertEquals(0, servicioConcierge.calendario(13L, hoy, 7).diasDisponibles());
        mvc.perform(get("/api/publico/habitaciones/6/calendario")).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.dias", hasSize(42)));
    }

    @Test
    void elMapaMuestraLaSituacionDeCadaHabitacion() {
        String hospedada = reservarPagada(hoy, 2, 1L);
        servicioFlujo.confirmar(hospedada, autenticado(administrador));
        servicioFlujo.registrarIngreso(hospedada, autenticado(administrador));
        reservarPagada(hoy.plusDays(4), 2, 2L);
        servicioReserva.crearDesdeCarrito(clienteId, carrito(hoy.plusDays(1), hoy.plusDays(2), 3L), null);

        MapaHabitacionesDto mapa = servicioMonitor.mapa();

        assertEquals("Piso 1", mapa.pisos().get(0).piso());
        assertEquals(5, mapa.pisos().size());
        PuertaDto ocupada = puerta(mapa, "101");
        assertEquals("OCUPADA", ocupada.situacion());
        assertEquals(hospedada, ocupada.codigoReserva());
        assertNotNull(ocupada.huesped());
        assertEquals("RESERVADA", puerta(mapa, "102").situacion());
        assertEquals("PENDIENTE", puerta(mapa, "103").situacion());
        assertEquals("Reserva pendiente", puerta(mapa, "103").situacionEtiqueta());
        assertEquals("MANTENIMIENTO", puerta(mapa, "104").situacion());
        assertEquals("DISPONIBLE", puerta(mapa, "201").situacion());
        assertEquals(1L, mapa.resumen().get("OCUPADA"));
        assertEquals(9L, mapa.resumen().get("DISPONIBLE"));
        assertEquals(8, mapa.ocupacionPorcentaje());
    }

    @Test
    void elPlanificadorUbicaCadaReservaEnSuTramo() {
        String codigo = reservarPagada(hoy.plusDays(2), 3, 5L);
        reservarPagada(hoy.plusDays(12), 5, 5L);

        PlanificadorDto plan = servicioMonitor.planificador(hoy, 14);

        assertEquals(14, plan.dias().size());
        assertEquals(13, plan.habitaciones().size());
        List<TramoDto> tramos = plan.habitaciones().stream()
                .filter(fila -> fila.numero().equals("202")).findFirst().orElseThrow().tramos();
        assertEquals(2, tramos.size());
        assertEquals(codigo, tramos.get(0).codigo());
        assertEquals(2, tramos.get(0).inicio());
        assertEquals(3, tramos.get(0).noches());
        assertFalse(tramos.get(0).continuaDespues());
        assertEquals(12, tramos.get(1).inicio());
        assertEquals(2, tramos.get(1).noches());
        assertTrue(tramos.get(1).continuaDespues());
        assertEquals(0, plan.ocupacion().get(0).ocupadas());
        assertEquals(1, plan.ocupacion().get(2).ocupadas());
        assertEquals(8, plan.ocupacion().get(2).porcentaje());
        assertEquals(31, servicioMonitor.planificador(hoy, 90).dias().size());
    }

    @Test
    void laBusquedaRapidaRespetaElRol() throws Exception {
        String codigo = reservarPagada(hoy.plusDays(6), 1, 8L);
        RequestPostProcessor admin = user(autenticado(administrador));
        RequestPostProcessor recepcion = user(autenticado(trabajador));

        mvc.perform(get("/api/operaciones/buscar").param("q", codigo.substring(4)).with(recepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].tipo").value("RESERVA"))
                .andExpect(jsonPath("$.datos[0].referencia").value(codigo));
        mvc.perform(get("/api/operaciones/buscar").param("q", "suite").with(recepcion))
                .andExpect(jsonPath("$.datos", hasSize(2)))
                .andExpect(jsonPath("$.datos[0].tipo").value("HABITACION"));
        mvc.perform(get("/api/operaciones/buscar").param("q", "70000081").with(admin))
                .andExpect(jsonPath("$.datos[?(@.tipo == 'CLIENTE')]", hasSize(1)));
        mvc.perform(get("/api/operaciones/buscar").param("q", "ana.torres").with(recepcion))
                .andExpect(jsonPath("$.datos", hasSize(0)));
        mvc.perform(get("/api/operaciones/buscar").param("q", "a").with(admin))
                .andExpect(jsonPath("$.datos", hasSize(0)));
        mvc.perform(get("/api/operaciones/buscar").param("q", "100%").with(admin))
                .andExpect(jsonPath("$.datos", hasSize(0)));
        mvc.perform(get("/api/operaciones/mapa")).andExpect(status().isUnauthorized());
        mvc.perform(get("/administracion/monitor").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/trabajador/monitor").with(recepcion)).andExpect(status().isOk());
        mvc.perform(get("/administracion/monitor").with(recepcion)).andExpect(status().isForbidden());
        mvc.perform(get("/api/operaciones/planificador")
                        .with(user(autenticado(repositorioUsuario.findById(clienteId).orElseThrow()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void elDashboardIncluyeVentasOcupacionYPronostico() throws Exception {
        String hospedada = reservarPagada(hoy, 2, 1L);
        servicioFlujo.confirmar(hospedada, autenticado(administrador));
        servicioFlujo.registrarIngreso(hospedada, autenticado(administrador));

        mvc.perform(get("/api/administracion/dashboard").with(user(autenticado(administrador))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.ventasEfectuadas").value(1))
                .andExpect(jsonPath("$.datos.ventasEfectuadasMes").value(1))
                .andExpect(jsonPath("$.datos.ocupacionHoy").value(8))
                .andExpect(jsonPath("$.datos.tarifaPromedio").value(150.00))
                .andExpect(jsonPath("$.datos.ocupacionProxima", hasSize(14)))
                .andExpect(jsonPath("$.datos.estadosHabitaciones.OCUPADA").value(1));
    }

    @Test
    void generaLosReportesAvanzados() throws Exception {
        String hospedada = reservarPagada(hoy, 2, 1L);
        servicioFlujo.confirmar(hospedada, autenticado(administrador));
        reservarPagada(hoy.plusDays(1), 1, 11L);
        RequestPostProcessor admin = user(autenticado(administrador));
        String desde = hoy.toString();
        String hasta = hoy.plusDays(2).toString();

        mvc.perform(get("/api/administracion/reportes/ocupacion").param("desde", desde).param("hasta", hasta).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.filas", hasSize(3)))
                .andExpect(jsonPath("$.datos.filas[0][1]").value(1))
                .andExpect(jsonPath("$.datos.filas[1][1]").value(2))
                .andExpect(jsonPath("$.datos.filas[1][3]").value(770.00))
                .andExpect(jsonPath("$.datos.filas[1][4]").value(385.00))
                .andExpect(jsonPath("$.datos.indicadores[1].valor").value("3"));
        mvc.perform(get("/api/administracion/reportes/metodos-pago").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.filas[0][0]").value("Efectivo en recepción"))
                .andExpect(jsonPath("$.datos.filas[0][1]").value(2))
                .andExpect(jsonPath("$.datos.filas[0][3]").value(100.00));
        mvc.perform(get("/api/administracion/reportes/anticipacion").param("desde", desde).param("hasta", hasta).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.filas", hasSize(2)))
                .andExpect(jsonPath("$.datos.filas[0][0]").value("El mismo día"))
                .andExpect(jsonPath("$.datos.indicadores[0].valor").value("2"));
        for (String clave : List.of("ocupacion", "metodos-pago", "anticipacion")) {
            for (String formato : List.of("pdf", "xlsx")) {
                byte[] archivo = mvc.perform(get("/api/administracion/reportes/" + clave + "/exportar")
                                .param("formato", formato).with(admin))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
                assertTrue(archivo.length > 1000, clave + "." + formato);
            }
        }
        mvc.perform(get("/api/administracion/reportes/ocupacion")
                        .param("desde", hoy.minusYears(2).toString()).param("hasta", hasta).with(admin))
                .andExpect(status().isBadRequest());
    }
}
