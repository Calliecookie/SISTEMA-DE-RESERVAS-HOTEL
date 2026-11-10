package pe.edu.josepardo.hotel.integracion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

@TestConfiguration(proxyBeanMethods = false)
class CargadorEsquemaPrueba {

    private static boolean cargado;

    @Bean
    static BeanPostProcessor cargadorEsquema() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String nombre) {
                if (bean instanceof DataSource origen) {
                    cargar(origen);
                }
                return bean;
            }
        };
    }

    private static synchronized void cargar(DataSource origen) {
        if (cargado) {
            return;
        }
        try (Connection conexion = origen.getConnection()) {
            ejecutar(conexion, "base-datos/esquema.sql");
            ejecutar(conexion, "base-datos/datos-iniciales.sql");
            cargado = true;
        } catch (IOException | SQLException excepcion) {
            throw new IllegalStateException("No se pudo cargar el esquema de pruebas", excepcion);
        }
    }

    private static void ejecutar(Connection conexion, String ruta) throws IOException {
        String contenido = Files.readString(Path.of(ruta), StandardCharsets.UTF_8)
                .replaceFirst("(?s)CREATE DATABASE.*?;", "")
                .replaceFirst("USE hotel_reservas;", "");
        ScriptUtils.executeSqlScript(conexion, new ByteArrayResource(contenido.getBytes(StandardCharsets.UTF_8)));
    }
}
