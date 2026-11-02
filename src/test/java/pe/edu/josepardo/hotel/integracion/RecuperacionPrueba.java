package pe.edu.josepardo.hotel.integracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;
import pe.edu.josepardo.hotel.modelo.Rol;

class RecuperacionPrueba extends BasePruebaIntegracion {

    private ResultActions solicitar(String correo) throws Exception {
        return mvc.perform(post("/api/autenticacion/recuperacion/solicitar").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"" + correo + "\"}"));
    }

    private ResultActions verificar(String correo, String codigo, MockHttpSession sesion) throws Exception {
        return mvc.perform(post("/api/autenticacion/recuperacion/verificar").with(csrf()).session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"codigo\":\"" + codigo + "\"}"));
    }

    private ResultActions restablecer(String contrasena, MockHttpSession sesion) throws Exception {
        return mvc.perform(post("/api/autenticacion/recuperacion/restablecer").with(csrf()).session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contrasena\":\"" + contrasena + "\",\"confirmacion\":\"" + contrasena + "\"}"));
    }

    private String codigoDelCorreo(int indice) throws Exception {
        String cuerpo = correosRecibidos()[indice].getContent().toString();
        Matcher coincidencia = Pattern.compile(">(\\d{6})<").matcher(cuerpo);
        assertTrue(coincidencia.find(), "El correo debe incluir el código");
        return coincidencia.group(1);
    }

    @Test
    void completaElFlujoYElCodigoNoSePuedeReutilizar() throws Exception {
        crearUsuario("cliente@prueba.pe", Rol.CLIENTE);
        solicitar("cliente@prueba.pe").andExpect(status().isOk());
        String codigo = codigoDelCorreo(0);
        String guardado = jdbc.queryForObject("SELECT codigo_hash FROM codigos_verificacion", String.class);
        assertTrue(guardado.startsWith("$argon2id$"));

        MockHttpSession sesion = new MockHttpSession();
        verificar("cliente@prueba.pe", codigo, sesion).andExpect(status().isOk());
        restablecer("NuevaClave123", sesion).andExpect(status().isOk());
        assertTrue(codificador.matches("NuevaClave123",
                repositorioUsuario.buscarPorCorreo("cliente@prueba.pe").orElseThrow().getContrasenaHash()));

        verificar("cliente@prueba.pe", codigo, new MockHttpSession()).andExpect(status().isBadRequest());
    }

    @Test
    void noRevelaSiElCorreoExiste() throws Exception {
        crearUsuario("cliente@prueba.pe", Rol.CLIENTE);
        String existente = solicitar("cliente@prueba.pe").andReturn().getResponse().getContentAsString();
        String inexistente = solicitar("nadie@prueba.pe").andReturn().getResponse().getContentAsString();
        assertEquals(existente, inexistente);
        assertEquals(1, correosRecibidos().length);
    }

    @Test
    void invalidaElCodigoAnteriorAlPedirUnoNuevo() throws Exception {
        crearUsuario("cliente@prueba.pe", Rol.CLIENTE);
        solicitar("cliente@prueba.pe");
        String primero = codigoDelCorreo(0);
        solicitar("cliente@prueba.pe");
        String segundo = codigoDelCorreo(1);
        if (!primero.equals(segundo)) {
            verificar("cliente@prueba.pe", primero, new MockHttpSession()).andExpect(status().isBadRequest());
        }
        verificar("cliente@prueba.pe", segundo, new MockHttpSession()).andExpect(status().isOk());
    }

    @Test
    void invalidaElCodigoTrasDemasiadosIntentos() throws Exception {
        crearUsuario("cliente@prueba.pe", Rol.CLIENTE);
        solicitar("cliente@prueba.pe");
        String correcto = codigoDelCorreo(0);
        String incorrecto = correcto.equals("000000") ? "000001" : "000000";
        for (int i = 0; i < 5; i++) {
            verificar("cliente@prueba.pe", incorrecto, new MockHttpSession()).andExpect(status().isBadRequest());
        }
        verificar("cliente@prueba.pe", correcto, new MockHttpSession()).andExpect(status().isBadRequest());
    }

    @Test
    void rechazaCodigoVencido() throws Exception {
        crearUsuario("cliente@prueba.pe", Rol.CLIENTE);
        solicitar("cliente@prueba.pe");
        jdbc.update("UPDATE codigos_verificacion SET expira_en = ?", LocalDateTime.now().minusMinutes(1));
        verificar("cliente@prueba.pe", codigoDelCorreo(0), new MockHttpSession()).andExpect(status().isBadRequest());
    }

    @Test
    void noPermiteRestablecerSinVerificar() throws Exception {
        restablecer("NuevaClave123", new MockHttpSession()).andExpect(status().isBadRequest());
    }
}
