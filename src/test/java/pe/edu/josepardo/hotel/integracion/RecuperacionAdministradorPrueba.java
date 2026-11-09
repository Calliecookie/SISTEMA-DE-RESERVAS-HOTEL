package pe.edu.josepardo.hotel.integracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.modelo.Usuario;

class RecuperacionAdministradorPrueba extends BasePruebaIntegracion {

    private static final String CLAVE = "Clave-Maestra-Prueba-2026";

    private ResultActions recuperar(String correo, String clave, String contrasena) throws Exception {
        return mvc.perform(post("/api/autenticacion/recuperacion/administrador").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "correo", correo, "clave", clave, "contrasena", contrasena, "confirmacion", contrasena))));
    }

    private String hash(String correo) {
        return repositorioUsuario.buscarPorCorreo(correo).orElseThrow().getContrasenaHash();
    }

    @Test
    void laPaginaEsPublicaYMuestraLosMetodos() throws Exception {
        mvc.perform(get("/recuperar-contrasena/administrador")).andExpect(status().isOk());
        mvc.perform(get("/api/autenticacion/recuperacion/metodos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.correo").value(true))
                .andExpect(jsonPath("$.datos.claveAdministrador").value(true));
    }

    @Test
    void restableceConLaClaveYDesbloqueaLaCuenta() throws Exception {
        Usuario admin = crearUsuario("admin@prueba.pe", Rol.ADMINISTRADOR);
        jdbc.update("UPDATE usuarios SET intentos_fallidos = 5, bloqueado_hasta = ? WHERE id = ?",
                LocalDateTime.now().plusMinutes(10), admin.getId());

        recuperar("ADMIN@prueba.pe", CLAVE, "NuevaClaveAdmin1").andExpect(status().isOk());

        Usuario actualizado = repositorioUsuario.buscarPorCorreo("admin@prueba.pe").orElseThrow();
        assertTrue(codificador.matches("NuevaClaveAdmin1", actualizado.getContrasenaHash()));
        assertEquals(0, actualizado.getIntentosFallidos());
        assertNull(actualizado.getBloqueadoHasta());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM registros_auditoria WHERE accion = 'RECUPERAR_CONTRASENA_ADMIN'", Integer.class));
    }

    @Test
    void rechazaClaveIncorrectaYUsuariosQueNoSonAdministradores() throws Exception {
        crearUsuario("admin@prueba.pe", Rol.ADMINISTRADOR);
        crearUsuario("cliente@prueba.pe", Rol.CLIENTE);
        String antes = hash("admin@prueba.pe");

        recuperar("admin@prueba.pe", "clave-equivocada-123", "NuevaClaveAdmin1").andExpect(status().isBadRequest());
        recuperar("cliente@prueba.pe", CLAVE, "NuevaClaveAdmin1").andExpect(status().isBadRequest());
        recuperar("admin@prueba.pe", CLAVE, "corta1").andExpect(status().isBadRequest());

        assertEquals(antes, hash("admin@prueba.pe"));
        assertFalse(codificador.matches("NuevaClaveAdmin1", hash("cliente@prueba.pe")));
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM registros_auditoria WHERE accion = 'RECUPERACION_ADMIN_FALLIDA'", Integer.class));
    }

    @Test
    void limitaLosIntentos() throws Exception {
        crearUsuario("admin@prueba.pe", Rol.ADMINISTRADOR);
        for (int i = 0; i < 5; i++) {
            recuperar("admin@prueba.pe", "clave-equivocada-" + i, "NuevaClaveAdmin1").andExpect(status().isBadRequest());
        }
        recuperar("admin@prueba.pe", CLAVE, "NuevaClaveAdmin1").andExpect(status().isTooManyRequests());
    }
}
