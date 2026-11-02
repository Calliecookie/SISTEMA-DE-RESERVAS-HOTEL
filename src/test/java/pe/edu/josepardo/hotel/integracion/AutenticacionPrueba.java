package pe.edu.josepardo.hotel.integracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import pe.edu.josepardo.hotel.modelo.Cliente;
import pe.edu.josepardo.hotel.modelo.OrigenDatosCliente;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.modelo.Usuario;

class AutenticacionPrueba extends BasePruebaIntegracion {

    private static final String RESPUESTA_FACTILIZA = """
            {"status":200,"success":true,"message":"Exito","data":{"numero":"45678912","nombres":"ANA LUCIA",
            "apellido_paterno":"QUISPE","apellido_materno":"ROJAS","nombre_completo":"QUISPE ROJAS ANA LUCIA"}}
            """;

    private String registro(String dni, String correo, String nombres, String apellidos) throws Exception {
        return json.writeValueAsString(Map.of("dni", dni, "correo", correo, "contrasena", CONTRASENA,
                "confirmacion", CONTRASENA, "nombres", nombres, "apellidos", apellidos));
    }

    private org.springframework.test.web.servlet.ResultActions consultarDni(String dni, MockHttpSession sesion) throws Exception {
        return mvc.perform(post("/api/autenticacion/consultar-dni").with(csrf()).session(sesion)
                .contentType(MediaType.APPLICATION_JSON).content("{\"dni\":\"" + dni + "\"}"));
    }

    @Test
    void consultaDniDesdeElBackendConToken() throws Exception {
        FACTILIZA.responder("GET /v1/dni/info/45678912", 200, RESPUESTA_FACTILIZA);
        consultarDni("45678912", new MockHttpSession())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.verificado").value(true))
                .andExpect(jsonPath("$.datos.nombres").value("ANA LUCIA"))
                .andExpect(jsonPath("$.datos.apellidos").value("QUISPE ROJAS"));
        assertEquals("Bearer token-de-prueba", FACTILIZA.ultimaAutorizacion());
    }

    @Test
    void rechazaDniConFormatoInvalido() throws Exception {
        consultarDni("1234", new MockHttpSession())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.dni").exists());
        assertTrue(FACTILIZA.solicitudes().isEmpty());
    }

    @Test
    void informaCuandoFactilizaNoEncuentraElDni() throws Exception {
        FACTILIZA.responder("GET /v1/dni/info/11111111", 400, "{\"status\":400,\"success\":false,\"message\":\"Bad Request\"}");
        consultarDni("11111111", new MockHttpSession())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.verificado").value(false))
                .andExpect(jsonPath("$.datos.permiteManual").value(false));
    }

    @Test
    void permiteIngresoManualSiFactilizaFalla() throws Exception {
        FACTILIZA.responder("GET /v1/dni/info/22222222", 500, "{}");
        consultarDni("22222222", new MockHttpSession())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.permiteManual").value(true));
    }

    @Test
    void limitaConsultasExcesivasDeDni() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        FACTILIZA.responder("GET /v1/dni/info/33333333", 500, "{}");
        for (int i = 0; i < 5; i++) {
            consultarDni("33333333", sesion).andExpect(status().isOk());
        }
        consultarDni("33333333", sesion).andExpect(status().isTooManyRequests());
    }

    @Test
    void registraConDatosVerificadosIgnorandoLosEnviados() throws Exception {
        FACTILIZA.responder("GET /v1/dni/info/45678912", 200, RESPUESTA_FACTILIZA);
        MockHttpSession sesion = new MockHttpSession();
        consultarDni("45678912", sesion).andExpect(status().isOk());
        mvc.perform(post("/api/autenticacion/registro").with(csrf()).session(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("45678912", "ana@prueba.pe", "Otro", "Nombre")))
                .andExpect(status().isOk());
        Usuario usuario = repositorioUsuario.buscarPorCorreo("ana@prueba.pe").orElseThrow();
        Cliente cliente = repositorioCliente.buscarPorUsuarioId(usuario.getId()).orElseThrow();
        assertEquals("ANA LUCIA", usuario.getNombres());
        assertEquals(Rol.CLIENTE, usuario.getRol());
        assertTrue(cliente.isDniVerificado());
        assertEquals(OrigenDatosCliente.FACTILIZA, cliente.getOrigenDatos());
        assertTrue(usuario.getContrasenaHash().startsWith("$argon2id$"));
    }

    @Test
    void rechazaDniYCorreoDuplicados() throws Exception {
        crearCliente("existe@prueba.pe", "70000001");
        mvc.perform(post("/api/autenticacion/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(registro("70000001", "nuevo@prueba.pe", "Ana", "Rojas")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DNI_REGISTRADO"));
        mvc.perform(post("/api/autenticacion/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(registro("70000002", "EXISTE@prueba.pe", "Ana", "Rojas")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_REGISTRADO"));
    }

    @Test
    void rechazaContrasenaDebil() throws Exception {
        String cuerpo = json.writeValueAsString(Map.of("dni", "70000003", "correo", "a@prueba.pe",
                "contrasena", "abc", "confirmacion", "abc", "nombres", "Ana", "apellidos", "Rojas"));
        mvc.perform(post("/api/autenticacion/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.contrasena").exists());
    }

    @Test
    void iniciaSesionYRedirigeSegunRol() throws Exception {
        crearUsuario("admin@prueba.pe", Rol.ADMINISTRADOR);
        mvc.perform(post("/ingresar").with(csrf()).header("X-Requested-With", "fetch")
                        .param("correo", "admin@prueba.pe").param("contrasena", CONTRASENA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.destino").value("/administracion"));
    }

    @Test
    void credencialesInvalidasNoRevelanSiElCorreoExiste() throws Exception {
        crearUsuario("real@prueba.pe", Rol.CLIENTE);
        String existente = mvc.perform(post("/ingresar").with(csrf()).header("X-Requested-With", "fetch")
                        .param("correo", "real@prueba.pe").param("contrasena", "incorrecta1"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String inexistente = mvc.perform(post("/ingresar").with(csrf()).header("X-Requested-With", "fetch")
                        .param("correo", "nadie@prueba.pe").param("contrasena", "incorrecta1"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertEquals(existente, inexistente);
    }

    @Test
    void bloqueaLaCuentaTrasVariosIntentosFallidos() throws Exception {
        crearUsuario("bloqueo@prueba.pe", Rol.CLIENTE);
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/ingresar").with(csrf()).header("X-Requested-With", "fetch")
                    .param("correo", "bloqueo@prueba.pe").param("contrasena", "incorrecta1"));
        }
        mvc.perform(post("/ingresar").with(csrf()).header("X-Requested-With", "fetch")
                        .param("correo", "bloqueo@prueba.pe").param("contrasena", CONTRASENA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("ACCESO_LIMITADO"));
        assertFalse(repositorioUsuario.buscarPorCorreo("bloqueo@prueba.pe").orElseThrow().getBloqueadoHasta() == null);
    }

    @Test
    void rechazaOperacionesSinTokenCsrf() throws Exception {
        mvc.perform(post("/api/autenticacion/registro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
    }
}
