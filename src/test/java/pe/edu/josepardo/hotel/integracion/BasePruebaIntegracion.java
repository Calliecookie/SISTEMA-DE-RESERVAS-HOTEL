package pe.edu.josepardo.hotel.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.josepardo.hotel.modelo.Cliente;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.modelo.Usuario;
import pe.edu.josepardo.hotel.repositorio.RepositorioCliente;
import pe.edu.josepardo.hotel.repositorio.RepositorioUsuario;
import pe.edu.josepardo.hotel.seguridad.LimitadorSolicitudes;
import pe.edu.josepardo.hotel.seguridad.UsuarioAutenticado;
import pe.edu.josepardo.hotel.servicio.Carrito;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prueba")
@Import(CargadorEsquemaPrueba.class)
abstract class BasePruebaIntegracion {

    protected static final String CONTRASENA = "Clave12345";
    protected static final ServidorSimulado FACTILIZA = new ServidorSimulado(39091);
    protected static final ServidorSimulado PAYPAL = new ServidorSimulado(39092);
    private static String hashContrasena;

    @RegisterExtension
    static final GreenMailExtension CORREO = new GreenMailExtension(ServerSetupTest.SMTP).withPerMethodLifecycle(false);

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected DataSource origenDatos;
    @Autowired
    protected RepositorioUsuario repositorioUsuario;
    @Autowired
    protected RepositorioCliente repositorioCliente;
    @Autowired
    protected PasswordEncoder codificador;
    @Autowired
    protected LimitadorSolicitudes limitador;

    @BeforeEach
    void prepararDatos() throws Exception {
        synchronized (BasePruebaIntegracion.class) {
            if (hashContrasena == null) {
                hashContrasena = codificador.encode(CONTRASENA);
            }
        }
        try (Connection conexion = origenDatos.getConnection(); Statement sentencia = conexion.createStatement()) {
            sentencia.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String tabla : List.of("registros_auditoria", "historial_estados_reserva", "codigos_verificacion",
                    "comprobantes", "pagos", "detalle_reservas", "reservas", "clientes", "usuarios")) {
                sentencia.execute("TRUNCATE TABLE " + tabla);
            }
            sentencia.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
        jdbc.update("UPDATE configuraciones SET valor = '' WHERE clave IN ('paypal_tipo_cambio', 'yape_numero', 'yape_titular', 'yape_qr')");
        jdbc.update("UPDATE habitaciones SET estado = 'DISPONIBLE', activo = TRUE WHERE id <> 13");
        limitador.reiniciarTodo();
        FACTILIZA.reiniciar();
        PAYPAL.reiniciar();
        CORREO.purgeEmailFromAllMailboxes();
    }

    protected Usuario crearUsuario(String correo, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(hashContrasena);
        usuario.setRol(rol);
        usuario.setNombres("Nombre");
        usuario.setApellidos("Apellido Prueba");
        return repositorioUsuario.save(usuario);
    }

    protected Cliente crearCliente(String correo, String dni) {
        Cliente cliente = new Cliente();
        cliente.setUsuario(crearUsuario(correo, Rol.CLIENTE));
        cliente.setDni(dni);
        return repositorioCliente.save(cliente);
    }

    protected UsuarioAutenticado autenticado(Usuario usuario) {
        return new UsuarioAutenticado(repositorioUsuario.findById(usuario.getId()).orElseThrow());
    }

    protected Carrito carrito(LocalDate entrada, LocalDate salida, long... habitaciones) {
        Carrito carrito = new Carrito();
        carrito.definirEstancia(entrada, salida);
        for (long habitacion : habitaciones) {
            carrito.getItems().put(habitacion, 1);
        }
        return carrito;
    }

    protected MimeMessage[] correosRecibidos() {
        return CORREO.getReceivedMessages();
    }
}
