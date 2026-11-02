package pe.edu.josepardo.hotel.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.configuracion.PropiedadesHotel;
import pe.edu.josepardo.hotel.modelo.Rol;
import pe.edu.josepardo.hotel.modelo.Usuario;
import pe.edu.josepardo.hotel.repositorio.RepositorioUsuario;

@Component
@Order(1)
public class InicializadorAdministrador implements ApplicationRunner {

    private static final Logger REGISTRO = LoggerFactory.getLogger(InicializadorAdministrador.class);
    private static final String PATRON_CONTRASENA = "^(?=.*[A-Za-z])(?=.*\\d).{10,72}$";

    private final RepositorioUsuario repositorio;
    private final PasswordEncoder codificador;
    private final PropiedadesHotel.AdministradorInicial propiedades;

    public InicializadorAdministrador(
            RepositorioUsuario repositorio, PasswordEncoder codificador, PropiedadesHotel propiedades) {
        this.repositorio = repositorio;
        this.codificador = codificador;
        this.propiedades = propiedades.administradorInicial();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (repositorio.contarPorRol(Rol.ADMINISTRADOR) > 0) {
            return;
        }
        String correo = propiedades.correo();
        String contrasena = propiedades.contrasena();
        if (correo == null || correo.isBlank() || contrasena == null || !contrasena.matches(PATRON_CONTRASENA)) {
            REGISTRO.warn("No hay administrador. Define ADMIN_INICIAL_CORREO y ADMIN_INICIAL_CONTRASENA "
                    + "(mínimo 10 caracteres, con letras y números) y reinicia la aplicación.");
            return;
        }
        Usuario administrador = new Usuario();
        administrador.setCorreo(correo.trim().toLowerCase());
        administrador.setContrasenaHash(codificador.encode(contrasena));
        administrador.setRol(Rol.ADMINISTRADOR);
        administrador.setNombres(propiedades.nombres());
        administrador.setApellidos(propiedades.apellidos());
        repositorio.save(administrador);
        REGISTRO.info("Administrador inicial creado: {}", administrador.getCorreo());
    }
}
