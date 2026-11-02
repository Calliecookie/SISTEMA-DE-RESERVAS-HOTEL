package pe.edu.josepardo.hotel.integracion;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import pe.edu.josepardo.hotel.modelo.Cliente;
import pe.edu.josepardo.hotel.servicio.ServicioReserva;

class CatalogoCarritoPrueba extends BasePruebaIntegracion {

    @Autowired
    private ServicioReserva servicioReserva;

    private final LocalDate entrada = LocalDate.now().plusDays(10);
    private final LocalDate salida = entrada.plusDays(3);

    @Test
    void buscaPorTextoEnTiempoReal() throws Exception {
        mvc.perform(get("/api/publico/habitaciones").param("busqueda", "suite"))
                .andExpect(jsonPath("$.datos.totalElementos").value(2));
    }

    @Test
    void laBusquedaTrataLosComodinesComoTexto() throws Exception {
        mvc.perform(get("/api/publico/habitaciones").param("busqueda", "%' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.totalElementos").value(0));
    }

    @Test
    void filtraPorCategoriaPrecioYCapacidad() throws Exception {
        mvc.perform(get("/api/publico/habitaciones").param("categoriaId", "5"))
                .andExpect(jsonPath("$.datos.totalElementos").value(2));
        mvc.perform(get("/api/publico/habitaciones").param("precioMinimo", "500").param("precioMaximo", "650"))
                .andExpect(jsonPath("$.datos.totalElementos").value(2));
        mvc.perform(get("/api/publico/habitaciones").param("capacidad", "6"))
                .andExpect(jsonPath("$.datos.totalElementos").value(1));
    }

    @Test
    void ordenaPorPrecio() throws Exception {
        mvc.perform(get("/api/publico/habitaciones").param("orden", "precio_desc").param("tamano", "1"))
                .andExpect(jsonPath("$.datos.elementos[0].numero").value("502"));
        mvc.perform(get("/api/publico/habitaciones").param("orden", "precio_asc").param("tamano", "1"))
                .andExpect(jsonPath("$.datos.elementos[0].numero").value("103"));
    }

    @Test
    void rechazaRangoDePreciosInvertido() throws Exception {
        mvc.perform(get("/api/publico/habitaciones").param("precioMinimo", "500").param("precioMaximo", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void marcaOcupadaSegunLasFechasConsultadas() throws Exception {
        Cliente cliente = crearCliente("c@prueba.pe", "70000021");
        servicioReserva.crearDesdeCarrito(cliente.getUsuario().getId(), carrito(entrada, salida, 11L), null);

        mvc.perform(get("/api/publico/habitaciones/11/disponibilidad")
                        .param("fechaEntrada", entrada.plusDays(1).toString())
                        .param("fechaSalida", salida.plusDays(1).toString()))
                .andExpect(jsonPath("$.datos.disponible").value(false));
        mvc.perform(get("/api/publico/habitaciones/11/disponibilidad")
                        .param("fechaEntrada", salida.toString())
                        .param("fechaSalida", salida.plusDays(2).toString()))
                .andExpect(jsonPath("$.datos.disponible").value(true));
        mvc.perform(get("/api/publico/habitaciones").param("categoriaId", "5").param("soloDisponibles", "true")
                        .param("fechaEntrada", entrada.toString()).param("fechaSalida", salida.toString()))
                .andExpect(jsonPath("$.datos.totalElementos").value(1));
    }

    @Test
    void ocultaHabitacionesDesactivadas() throws Exception {
        jdbc.update("UPDATE habitaciones SET activo = FALSE WHERE id = 11");
        mvc.perform(get("/api/publico/habitaciones/11")).andExpect(status().isNotFound());
    }

    private String item(long habitacion, int huespedes) {
        return "{\"habitacionId\":" + habitacion + ",\"huespedes\":" + huespedes + ",\"fechaEntrada\":\"" + entrada
                + "\",\"fechaSalida\":\"" + salida + "\",\"precioNoche\":1,\"total\":1}";
    }

    @Test
    void calculaImportesEnElServidorIgnorandoPreciosEnviados() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/api/carrito/items").with(csrf()).session(sesion)
                        .contentType(MediaType.APPLICATION_JSON).content(item(4, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.noches").value(3))
                .andExpect(jsonPath("$.datos.subtotal").value(660.00))
                .andExpect(jsonPath("$.datos.impuesto").value(118.80))
                .andExpect(jsonPath("$.datos.total").value(778.80))
                .andExpect(jsonPath("$.datos.listo").value(true));
    }

    @Test
    void gestionaHabitacionesHuespedesYFechas() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/api/carrito/items").with(csrf()).session(sesion)
                .contentType(MediaType.APPLICATION_JSON).content(item(9, 1)));
        mvc.perform(post("/api/carrito/items").with(csrf()).session(sesion)
                        .contentType(MediaType.APPLICATION_JSON).content(item(1, 1)))
                .andExpect(jsonPath("$.datos.items", hasSize(2)));
        mvc.perform(patch("/api/carrito/items/9").with(csrf()).session(sesion)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"huespedes\":4}"))
                .andExpect(jsonPath("$.datos.totalHuespedes").value(5));
        mvc.perform(patch("/api/carrito/items/9").with(csrf()).session(sesion)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"huespedes\":5}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/carrito/estancia").with(csrf()).session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaEntrada\":\"" + entrada + "\",\"fechaSalida\":\"" + entrada.plusDays(5) + "\"}"))
                .andExpect(jsonPath("$.datos.noches").value(5))
                .andExpect(jsonPath("$.datos.descuento", greaterThan(0.0)));
        mvc.perform(delete("/api/carrito/items/1").with(csrf()).session(sesion))
                .andExpect(jsonPath("$.datos.items", hasSize(1)));
        mvc.perform(delete("/api/carrito").with(csrf()).session(sesion))
                .andExpect(jsonPath("$.datos.items", hasSize(0)));
        assertTrue(sesion.getAttribute("carrito") != null);
    }

    @Test
    void noAgregaHabitacionEnMantenimiento() throws Exception {
        mvc.perform(post("/api/carrito/items").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(item(13, 1)))
                .andExpect(status().isConflict());
    }

    @Test
    void avisaSiUnaHabitacionDelCarritoDejaDeEstarDisponible() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/api/carrito/items").with(csrf()).session(sesion)
                .contentType(MediaType.APPLICATION_JSON).content(item(7, 1)));
        Cliente cliente = crearCliente("c@prueba.pe", "70000022");
        servicioReserva.crearDesdeCarrito(cliente.getUsuario().getId(), carrito(entrada, salida, 7L), null);
        mvc.perform(get("/api/carrito").session(sesion))
                .andExpect(jsonPath("$.datos.listo").value(false))
                .andExpect(jsonPath("$.datos.items[0].disponible").value(false));
    }
}
