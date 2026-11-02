package pe.edu.josepardo.hotel;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class AplicacionHotel extends SpringBootServletInitializer {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
    }

    public static void main(String[] argumentos) {
        SpringApplication.run(AplicacionHotel.class, argumentos);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder constructor) {
        return constructor.sources(AplicacionHotel.class);
    }
}
