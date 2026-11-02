package pe.edu.josepardo.hotel.configuracion;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(PropiedadesHotel.class)
public class ConfiguracionAplicacion {

    @Bean
    public TaskExecutor ejecutorCorreo(PropiedadesHotel propiedades) {
        if (!propiedades.correo().asincrono()) {
            return new SyncTaskExecutor();
        }
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(2);
        ejecutor.setMaxPoolSize(4);
        ejecutor.setQueueCapacity(200);
        ejecutor.setThreadNamePrefix("correo-");
        ejecutor.initialize();
        return ejecutor;
    }
}
