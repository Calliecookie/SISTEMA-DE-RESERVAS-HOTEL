package pe.edu.josepardo.hotel.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pe.edu.josepardo.hotel.dto.ReservaCreadaDto;
import pe.edu.josepardo.hotel.modelo.Cliente;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.modelo.Usuario;
import pe.edu.josepardo.hotel.servicio.ServicioReserva;

class SeguridadRutasPrueba extends BasePruebaIntegracion {

    @Autowired
    private ServicioReserva servicioReserva;

    @Test
    void lasRutasPublicasNoRequierenSesion() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/habitaciones")).andExpect(status().isOk());
        mvc.perform(get("/api/publico/habitaciones")).andExpect(status().isOk());
    }

    @Test
    void lasRutasProtegidasExigenSesion() throws Exception {
        mvc.perform(get("/api/cliente/perfil")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/operaciones/resumen")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/administracion/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/mi-cuenta")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/administracion")).andExpect(status().is3xxRedirection());
    }

    @Test
    void cadaRolSoloAccedeASuArea() throws Exception {
        var cliente = user(autenticado(crearCliente("c@prueba.pe", "70000011").getUsuario()));
        var trabajador = user(autenticado(crearUsuario("t@prueba.pe", Rol.TRABAJADOR)));
        var administrador = user(autenticado(crearUsuario("a@prueba.pe", Rol.ADMINISTRADOR)));

        mvc.perform(get("/api/cliente/perfil").with(cliente)).andExpect(status().isOk());
        mvc.perform(get("/api/operaciones/resumen").with(cliente)).andExpect(status().isForbidden());
        mvc.perform(get("/api/administracion/dashboard").with(cliente)).andExpect(status().isForbidden());

        mvc.perform(get("/api/operaciones/resumen").with(trabajador)).andExpect(status().isOk());
        mvc.perform(get("/api/administracion/dashboard").with(trabajador)).andExpect(status().isForbidden());
        mvc.perform(get("/api/cliente/perfil").with(trabajador)).andExpect(status().isForbidden());

        mvc.perform(get("/api/operaciones/resumen").with(administrador)).andExpect(status().isOk());
        mvc.perform(get("/api/administracion/dashboard").with(administrador)).andExpect(status().isOk());
    }

    @Test
    void unClienteNoPuedeVerReservasNiComprobantesAjenos() throws Exception {
        Cliente duena = crearCliente("duena@prueba.pe", "70000012");
        Usuario intruso = crearCliente("intruso@prueba.pe", "70000013").getUsuario();
        LocalDate entrada = LocalDate.now().plusDays(5);
        ReservaCreadaDto reserva = servicioReserva.crearDesdeCarrito(
                duena.getUsuario().getId(), carrito(entrada, entrada.plusDays(2), 1L), null);
        String numero = jdbc.queryForObject("SELECT numero FROM comprobantes LIMIT 1", String.class);

        mvc.perform(get("/api/cliente/reservas/" + reserva.codigo()).with(user(autenticado(intruso))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/comprobantes/" + numero).with(user(autenticado(intruso))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/comprobantes/" + numero).with(user(autenticado(duena.getUsuario()))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void enviaCabecerasDeSeguridad() throws Exception {
        mvc.perform(get("/"))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void lasRespuestasDeErrorNoExponenDetallesInternos() throws Exception {
        String cuerpo = mvc.perform(get("/api/publico/habitaciones/abc")).andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertTrue(cuerpo.contains("FORMATO_INVALIDO"));
        assertTrue(!cuerpo.contains("Exception"));
    }
}
