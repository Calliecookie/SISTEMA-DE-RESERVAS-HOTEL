package pe.edu.josepardo.hotel.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.modelo.Usuario;
import pe.edu.josepardo.hotel.servicio.ServicioFlujoReserva;
import pe.edu.josepardo.hotel.servicio.ServicioPago;
import pe.edu.josepardo.hotel.servicio.ServicioReserva;

class AdministracionPrueba extends BasePruebaIntegracion {

    @Autowired
    private ServicioReserva servicioReserva;
    @Autowired
    private ServicioPago servicioPago;
    @Autowired
    private ServicioFlujoReserva servicioFlujo;

    private Usuario administrador;
    private RequestPostProcessor admin;

    @BeforeEach
    void iniciarSesion() {
        administrador = crearUsuario("admin@prueba.pe", Rol.ADMINISTRADOR);
        admin = user(autenticado(administrador));
        jdbc.update("DELETE FROM categorias WHERE id > 5");
        jdbc.update("DELETE FROM habitaciones WHERE id > 13");
    }

    private String cuerpoHabitacion(String numero) throws Exception {
        return json.writeValueAsString(Map.of("numero", numero, "nombre", "Prueba", "categoriaId", 1,
                "descripcion", "Descripción de prueba", "capacidad", 2, "precioNoche", "199.90",
                "estadoOperativo", "OPERATIVA", "caracteristicaIds", List.of(1, 2)));
    }

    @Test
    void gestionaElCicloCompletoDeHabitaciones() throws Exception {
        String creada = mvc.perform(post("/api/administracion/habitaciones").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoHabitacion("900")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.numero").value("900"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(creada).path("datos").path("id").asLong();

        mvc.perform(get("/api/administracion/habitaciones/" + id).with(admin))
                .andExpect(jsonPath("$.datos.caracteristicaIds.length()").value(2));
        mvc.perform(put("/api/administracion/habitaciones/" + id).with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoHabitacion("901")))
                .andExpect(jsonPath("$.datos.numero").value("901"));
        mvc.perform(patch("/api/administracion/habitaciones/" + id + "/activo").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(jsonPath("$.datos.activo").value(false));
        mvc.perform(delete("/api/administracion/habitaciones/" + id).with(csrf()).with(admin))
                .andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM registros_auditoria WHERE accion = 'ELIMINAR_HABITACION'", Integer.class));
    }

    @Test
    void rechazaNumerosDuplicadosYDatosInvalidos() throws Exception {
        mvc.perform(post("/api/administracion/habitaciones").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoHabitacion("101")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/administracion/habitaciones").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"numero\":\"\",\"capacidad\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.numero").exists())
                .andExpect(jsonPath("$.errores.capacidad").exists());
    }

    @Test
    void noEliminaHabitacionConReservas() throws Exception {
        Long cliente = crearCliente("c@prueba.pe", "70000071").getUsuario().getId();
        LocalDate entrada = LocalDate.now().plusDays(3);
        servicioReserva.crearDesdeCarrito(cliente, carrito(entrada, entrada.plusDays(1), 2L), null);
        mvc.perform(delete("/api/administracion/habitaciones/2").with(csrf()).with(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("HABITACION_CON_RESERVAS"));
    }

    @Test
    void aceptaImagenesRealesYRechazaArchivosDisfrazados() throws Exception {
        byte[] png = new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        mvc.perform(multipart("/api/administracion/habitaciones/1/imagen")
                        .file(new MockMultipartFile("archivo", "foto.png", "image/png", png)).with(csrf()).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.imagen", containsString("/imagenes/")));
        mvc.perform(multipart("/api/administracion/habitaciones/2/imagen")
                        .file(new MockMultipartFile("archivo", "foto.png", "image/png", "<script>".getBytes())).with(csrf()).with(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void gestionaCategoriasYProtegeLasQueTienenHabitaciones() throws Exception {
        mvc.perform(post("/api/administracion/categorias").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/administracion/categorias").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"suite\"}"))
                .andExpect(status().isConflict());
        String creada = mvc.perform(post("/api/administracion/categorias").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Económica\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(creada).path("datos").path("id").asLong();
        mvc.perform(delete("/api/administracion/categorias/" + id).with(csrf()).with(admin)).andExpect(status().isOk());
        mvc.perform(delete("/api/administracion/categorias/1").with(csrf()).with(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_CON_HABITACIONES"));
    }

    @Test
    void gestionaTrabajadoresYProtegeAlUltimoAdministrador() throws Exception {
        String cuerpo = json.writeValueAsString(Map.of("nombres", "Rosa", "apellidos", "Huamán",
                "correo", "rosa@prueba.pe", "rol", "TRABAJADOR", "contrasena", CONTRASENA));
        mvc.perform(post("/api/administracion/usuarios").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.rol").value("TRABAJADOR"));
        mvc.perform(patch("/api/administracion/usuarios/" + administrador.getId() + "/activo").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isConflict());
        String degradar = json.writeValueAsString(Map.of("nombres", "Admin", "apellidos", "Único",
                "correo", "admin@prueba.pe", "rol", "TRABAJADOR"));
        mvc.perform(put("/api/administracion/usuarios/" + administrador.getId()).with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(degradar))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ULTIMO_ADMINISTRADOR"));
    }

    @Test
    void unaCuentaDesactivadaNoPuedeIniciarSesion() throws Exception {
        Usuario cliente = crearCliente("c@prueba.pe", "70000072").getUsuario();
        long clienteId = repositorioCliente.buscarPorUsuarioId(cliente.getId()).orElseThrow().getId();
        mvc.perform(patch("/api/administracion/clientes/" + clienteId + "/activo").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk());
        mvc.perform(post("/ingresar").with(csrf()).header("X-Requested-With", "fetch")
                        .param("correo", "c@prueba.pe").param("contrasena", CONTRASENA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validaLaConfiguracion() throws Exception {
        mvc.perform(put("/api/administracion/configuracion").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valores\":{\"hotel_ruc\":\"123\"}}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/administracion/configuracion").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valores\":{\"yape_numero\":\"987654321\"}}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/administracion/configuracion").with(csrf()).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valores\":{\"clave_inventada\":\"1\"}}"))
                .andExpect(status().isBadRequest());
    }

    private void prepararVentas() {
        Usuario trabajador = crearUsuario("t@prueba.pe", Rol.TRABAJADOR);
        Long uno = crearCliente("a@prueba.pe", "70000073").getUsuario().getId();
        Long dos = crearCliente("b@prueba.pe", "70000074").getUsuario().getId();
        LocalDate entrada = LocalDate.now().plusDays(4);
        String pagada = servicioReserva.crearDesdeCarrito(uno, carrito(entrada, entrada.plusDays(2), 4L), null).codigo();
        servicioPago.confirmarPago(pagada, autenticado(trabajador), "EFECTIVO", null);
        String reembolsada = servicioReserva.crearDesdeCarrito(dos, carrito(entrada, entrada.plusDays(2), 5L), null).codigo();
        servicioPago.confirmarPago(reembolsada, autenticado(trabajador), "EFECTIVO", null);
        servicioFlujo.cancelarPorPersonal(reembolsada, autenticado(trabajador), "Cancelación de prueba");
        servicioReserva.crearDesdeCarrito(dos, carrito(entrada, entrada.plusDays(2), 7L), null);
    }

    @Test
    void losIngresosSoloConsideranPagosConfirmados() throws Exception {
        prepararVentas();
        String esperado = "S/ 519.20";
        mvc.perform(get("/api/administracion/reportes/ventas-fechas").with(admin))
                .andExpect(jsonPath("$.datos.indicadores[0].valor").value("1"))
                .andExpect(jsonPath("$.datos.indicadores[1].valor").value(esperado));
        mvc.perform(get("/api/administracion/reportes/ingresos-generales").with(admin))
                .andExpect(jsonPath("$.datos.indicadores[0].valor").value(esperado))
                .andExpect(jsonPath("$.datos.indicadores[2].valor").value("1"));
        mvc.perform(get("/api/administracion/reportes/ventas-categoria").with(admin))
                .andExpect(jsonPath("$.datos.indicadores[1].valor").value(esperado));
        mvc.perform(get("/api/administracion/dashboard").with(admin))
                .andExpect(jsonPath("$.datos.ingresosAcumulados").value(519.20));
    }

    @Test
    void generaLosSeisReportes() throws Exception {
        prepararVentas();
        for (String clave : List.of("ventas-fechas", "ventas-cliente", "habitaciones-solicitadas",
                "disponibilidad", "ventas-categoria", "ingresos-generales")) {
            mvc.perform(get("/api/administracion/reportes/" + clave).with(admin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.datos.columnas").isArray());
        }
        mvc.perform(get("/api/administracion/reportes/inventado").with(admin)).andExpect(status().isNotFound());
    }

    @Test
    void elHistorialMuestraLasReservasDelCliente() throws Exception {
        prepararVentas();
        Long clienteId = jdbc.queryForObject("SELECT id FROM clientes WHERE dni = '70000074'", Long.class);
        mvc.perform(get("/api/administracion/reportes/ventas-cliente").param("clienteId", clienteId.toString()).with(admin))
                .andExpect(jsonPath("$.datos.titulo").value("Historial de reservas"))
                .andExpect(jsonPath("$.datos.filas.length()").value(2));
    }

    @Test
    void exportaReportesEnPdfYExcel() throws Exception {
        prepararVentas();
        byte[] pdf = mvc.perform(get("/api/administracion/reportes/ventas-fechas/exportar").param("formato", "pdf").with(admin))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals("%PDF".getBytes(), java.util.Arrays.copyOf(pdf, 4));
        byte[] excel = mvc.perform(get("/api/administracion/reportes/ventas-fechas/exportar").param("formato", "xlsx").with(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            assertEquals("Ventas por rango de fechas", libro.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertTrue(libro.getSheetAt(0).getLastRowNum() > 5);
        }
    }

    @Test
    void filtraVentasPorEstadoMetodoYTexto() throws Exception {
        prepararVentas();
        mvc.perform(get("/api/administracion/ventas").param("estado", "CANCELADO").with(admin))
                .andExpect(jsonPath("$.datos.cantidadOperaciones").value(1));
        mvc.perform(get("/api/administracion/ventas").param("metodoPago", "EFECTIVO").with(admin))
                .andExpect(jsonPath("$.datos.cantidadOperaciones").value(2));
        mvc.perform(get("/api/administracion/ventas").param("busqueda", "70000073").with(admin))
                .andExpect(jsonPath("$.datos.cantidadOperaciones").value(1));
        mvc.perform(get("/api/administracion/ventas").param("estado", "INVENTADO").with(admin))
                .andExpect(status().isBadRequest());
    }
}
