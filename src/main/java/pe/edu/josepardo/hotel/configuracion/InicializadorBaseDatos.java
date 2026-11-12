package pe.edu.josepardo.hotel.configuracion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

@Configuration(proxyBeanMethods = false)
public class InicializadorBaseDatos {

    private static final Logger REGISTRO = LoggerFactory.getLogger(InicializadorBaseDatos.class);
    private static final String[] SCRIPTS = {"base-datos/esquema.sql", "base-datos/datos-iniciales.sql"};

    @Bean
    static BeanPostProcessor inicializadorEsquema(Environment entorno) {
        boolean activo = entorno.getProperty("hotel.base-datos.inicializar", Boolean.class, true);
        return new BeanPostProcessor() {
            private boolean revisado;

            @Override
            public Object postProcessAfterInitialization(Object bean, String nombre) {
                if (activo && !revisado && bean instanceof DataSource origen) {
                    revisado = true;
                    preparar(origen);
                }
                return bean;
            }
        };
    }

    private static void preparar(DataSource origen) {
        try (Connection conexion = origen.getConnection()) {
            String catalogo = conexion.getCatalog();
            if (existeTabla(conexion, catalogo, "habitaciones")) {
                return;
            }
            REGISTRO.info("La base de datos {} está vacía. Creando tablas y datos iniciales.", catalogo);
            try (Statement sentencia = conexion.createStatement()) {
                sentencia.execute("ALTER DATABASE `" + catalogo.replace("`", "")
                        + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }
            for (String ruta : SCRIPTS) {
                String contenido = new ClassPathResource(ruta).getContentAsString(StandardCharsets.UTF_8)
                        .replaceFirst("(?s)CREATE DATABASE.*?;", "")
                        .replaceFirst("USE\\s+\\w+\\s*;", "");
                ScriptUtils.executeSqlScript(conexion, new ByteArrayResource(contenido.getBytes(StandardCharsets.UTF_8)));
            }
            REGISTRO.info("Base de datos {} lista.", catalogo);
        } catch (SQLException | IOException excepcion) {
            throw new IllegalStateException(
                    "No se pudo preparar la base de datos. Verifica que MySQL esté encendido y que BD_USUARIO y BD_CONTRASENA sean correctos.",
                    excepcion);
        }
    }

    private static boolean existeTabla(Connection conexion, String catalogo, String tabla) throws SQLException {
        try (ResultSet resultado = conexion.getMetaData().getTables(catalogo, null, tabla, new String[] {"TABLE"})) {
            return resultado.next();
        }
    }
}
