package pe.edu.josepardo.hotel.integracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pe.edu.josepardo.hotel.dto.MetodoPagoDisponibleDto;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.modelo.Usuario;
import pe.edu.josepardo.hotel.servicio.ServicioFlujoReserva;
import pe.edu.josepardo.hotel.servicio.ServicioPago;
import pe.edu.josepardo.hotel.servicio.ServicioReserva;

class PagosPrueba extends BasePruebaIntegracion {

    private static final String TOKEN = "{\"access_token\":\"token-paypal\",\"expires_in\":32000}";
    private static final String ORDEN = """
            {"id":"ORDEN-1","status":"PAYER_ACTION_REQUIRED","links":[
            {"href":"https://www.sandbox.paypal.com/checkoutnow?token=ORDEN-1","rel":"payer-action","method":"GET"}]}
            """;

    @Autowired
    private ServicioPago servicioPago;
    @Autowired
    private ServicioReserva servicioReserva;
    @Autowired
    private ServicioFlujoReserva servicioFlujo;

    private final LocalDate entrada = LocalDate.now().plusDays(30);

    private Long cliente;
    private String codigo;

    private void reservar(String dni) {
        cliente = crearCliente("c" + dni + "@prueba.pe", dni).getUsuario().getId();
        codigo = servicioReserva.crearDesdeCarrito(cliente, carrito(entrada, entrada.plusDays(3), 4L), null).codigo();
    }

    private void configurarPayPal() {
        jdbc.update("UPDATE configuraciones SET valor = '3.75' WHERE clave = 'paypal_tipo_cambio'");
        PAYPAL.responder("POST /v1/oauth2/token", 200, TOKEN);
        PAYPAL.responder("POST /v2/checkout/orders", 200, ORDEN);
    }

    private void configurarYape() {
        jdbc.update("UPDATE configuraciones SET valor = '987654321' WHERE clave = 'yape_numero'");
        jdbc.update("UPDATE configuraciones SET valor = 'Hotel Inti Wasi' WHERE clave = 'yape_titular'");
    }

    private String captura(String estado, String monto) {
        return """
                {"id":"ORDEN-1","status":"COMPLETED","purchase_units":[{"payments":{"captures":[
                {"id":"CAPTURA-9","status":"%s","amount":{"currency_code":"USD","value":"%s"}}]}}]}
                """.formatted(estado, monto);
    }

    private String estadoReserva() {
        return jdbc.queryForObject("SELECT estado FROM reservas WHERE codigo = ?", String.class, codigo);
    }

    private List<String> metodos() {
        return servicioPago.metodosDisponibles(codigo, cliente).stream().map(MetodoPagoDisponibleDto::metodo).toList();
    }

    @Test
    void soloOfreceLosMetodosConfigurados() {
        reservar("70000051");
        assertEquals(List.of("EFECTIVO"), metodos());
        configurarYape();
        assertEquals(List.of("EFECTIVO", "YAPE"), metodos());
        configurarPayPal();
        assertEquals(List.of("EFECTIVO", "YAPE", "PAYPAL"), metodos());
    }

    @Test
    void yapeExigeNumeroDeOperacionYNoAdmiteRepetidos() {
        configurarYape();
        reservar("70000052");
        assertThrows(ExcepcionNegocio.class, () -> servicioPago.declararPago(codigo, cliente, "YAPE", ""));
        servicioPago.declararPago(codigo, cliente, "YAPE", "08451277");
        assertEquals("PENDIENTE", estadoReserva());

        Long otro = crearCliente("otro@prueba.pe", "70000053").getUsuario().getId();
        String otraReserva = servicioReserva.crearDesdeCarrito(otro, carrito(entrada, entrada.plusDays(1), 5L), null).codigo();
        ExcepcionNegocio error = assertThrows(ExcepcionNegocio.class,
                () -> servicioPago.declararPago(otraReserva, otro, "YAPE", "08451277"));
        assertEquals("OPERACION_REPETIDA", error.getCodigo());
    }

    @Test
    void declararPagoExtiendeElPlazoDeVerificacion() {
        reservar("70000054");
        jdbc.update("UPDATE reservas SET expira_en = ?", LocalDateTime.now().plusMinutes(5));
        servicioPago.declararPago(codigo, cliente, "EFECTIVO", null);
        LocalDateTime expira = jdbc.queryForObject("SELECT expira_en FROM reservas", LocalDateTime.class);
        assertTrue(expira.isAfter(LocalDateTime.now().plusHours(11)));
    }

    @Test
    void elTrabajadorConfirmaElPagoYCuentaComoIngreso() {
        Usuario trabajador = crearUsuario("t@prueba.pe", Rol.TRABAJADOR);
        reservar("70000055");
        servicioPago.declararPago(codigo, cliente, "EFECTIVO", null);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM pagos WHERE estado = 'CONFIRMADO'", Integer.class));
        servicioPago.confirmarPago(codigo, autenticado(trabajador), null, null);
        assertEquals("PAGADO", estadoReserva());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM pagos WHERE estado = 'CONFIRMADO'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM registros_auditoria WHERE accion = 'CONFIRMAR_PAGO'", Integer.class));
    }

    @Test
    void noConfirmaPagoDeReservaVencida() {
        Usuario trabajador = crearUsuario("t@prueba.pe", Rol.TRABAJADOR);
        reservar("70000056");
        jdbc.update("UPDATE reservas SET expira_en = ?", LocalDateTime.now().minusMinutes(1));
        ExcepcionNegocio error = assertThrows(ExcepcionNegocio.class,
                () -> servicioPago.confirmarPago(codigo, autenticado(trabajador), "EFECTIVO", null));
        assertEquals("RESERVA_VENCIDA", error.getCodigo());
    }

    @Test
    void rechazarPermiteDeclararDeNuevo() {
        configurarYape();
        Usuario trabajador = crearUsuario("t@prueba.pe", Rol.TRABAJADOR);
        reservar("70000057");
        servicioPago.declararPago(codigo, cliente, "YAPE", "11112222");
        servicioPago.rechazarPago(codigo, autenticado(trabajador), "No se encontró la operación");
        servicioPago.declararPago(codigo, cliente, "YAPE", "11112222");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM pagos WHERE estado = 'DECLARADO'", Integer.class));
    }

    @Test
    void cancelarUnaReservaPagadaMarcaElReembolso() {
        Usuario trabajador = crearUsuario("t@prueba.pe", Rol.TRABAJADOR);
        reservar("70000058");
        servicioPago.confirmarPago(codigo, autenticado(trabajador), "EFECTIVO", null);
        servicioFlujo.cancelarPorPersonal(codigo, autenticado(trabajador), "El huésped no viajará");
        assertEquals("REEMBOLSADO", jdbc.queryForObject("SELECT estado FROM pagos", String.class));
    }

    @Test
    void payPalCobraEnDolaresYConfirmaLaReserva() {
        configurarPayPal();
        reservar("70000059");
        String urlAprobacion = servicioPago.iniciarPayPal(codigo, cliente).urlAprobacion();
        assertTrue(urlAprobacion.contains("token=ORDEN-1"));
        assertTrue(PAYPAL.ultimoCuerpo().contains("\"currency_code\":\"USD\""));
        assertTrue(PAYPAL.ultimoCuerpo().contains("\"value\":\"207.68\""));
        assertEquals("INICIADO", jdbc.queryForObject("SELECT estado FROM pagos", String.class));

        PAYPAL.responder("POST /v2/checkout/orders/ORDEN-1/capture", 201, captura("COMPLETED", "207.68"));
        assertTrue(servicioPago.completarPayPal(codigo, cliente));
        assertEquals("PAGADO", estadoReserva());
        assertEquals("CAPTURA-9", jdbc.queryForObject("SELECT numero_operacion FROM pagos", String.class));
        assertEquals(0, new BigDecimal("778.80").compareTo(jdbc.queryForObject("SELECT monto FROM pagos", BigDecimal.class)));
    }

    @Test
    void payPalNoConfirmaSiElMontoCapturadoNoCoincide() {
        configurarPayPal();
        reservar("70000060");
        servicioPago.iniciarPayPal(codigo, cliente);
        PAYPAL.responder("POST /v2/checkout/orders/ORDEN-1/capture", 201, captura("COMPLETED", "1.00"));
        assertFalse(servicioPago.completarPayPal(codigo, cliente));
        assertEquals("PENDIENTE", estadoReserva());
    }

    @Test
    void payPalNoConfirmaSiLaCapturaNoEstaCompleta() {
        configurarPayPal();
        reservar("70000061");
        servicioPago.iniciarPayPal(codigo, cliente);
        PAYPAL.responder("POST /v2/checkout/orders/ORDEN-1/capture", 201, captura("PENDING", "207.68"));
        assertFalse(servicioPago.completarPayPal(codigo, cliente));
        assertEquals("PENDIENTE", estadoReserva());
    }

    @Test
    void payPalNoSeOfreceSinTipoDeCambio() {
        reservar("70000062");
        ExcepcionNegocio error = assertThrows(ExcepcionNegocio.class, () -> servicioPago.iniciarPayPal(codigo, cliente));
        assertEquals("METODO_NO_DISPONIBLE", error.getCodigo());
        assertTrue(PAYPAL.solicitudes().isEmpty());
    }

    @Test
    void unClienteNoPuedePagarLaReservaDeOtro() {
        reservar("70000063");
        Long intruso = crearCliente("i@prueba.pe", "70000064").getUsuario().getId();
        ExcepcionNegocio error = assertThrows(ExcepcionNegocio.class,
                () -> servicioPago.declararPago(codigo, intruso, "EFECTIVO", null));
        assertEquals("NO_ENCONTRADO", error.getCodigo());
    }
}
