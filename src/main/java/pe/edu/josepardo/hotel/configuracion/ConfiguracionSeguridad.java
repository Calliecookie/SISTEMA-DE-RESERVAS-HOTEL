package pe.edu.josepardo.hotel.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import pe.edu.josepardo.hotel.seguridad.ConversorRespuestaJson;
import pe.edu.josepardo.hotel.seguridad.FiltroLimiteAcceso;
import pe.edu.josepardo.hotel.seguridad.LimitadorSolicitudes;
import pe.edu.josepardo.hotel.seguridad.ManejadorAccesoDenegado;
import pe.edu.josepardo.hotel.seguridad.ManejadorAccesoExitoso;
import pe.edu.josepardo.hotel.seguridad.ManejadorAccesoFallido;
import pe.edu.josepardo.hotel.seguridad.PuntoEntradaAutenticacion;
import pe.edu.josepardo.hotel.seguridad.ResolutorIpCliente;

@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    private static final String POLITICA_CONTENIDO = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self' data: https://commons.wikimedia.org https://upload.wikimedia.org https://thumb.wikimedia.org",
            "font-src 'self'",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Bean
    public PasswordEncoder codificadorContrasenas() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public SecurityFilterChain cadenaSeguridad(
            HttpSecurity http,
            ManejadorAccesoExitoso manejadorExito,
            ManejadorAccesoFallido manejadorFallo,
            PuntoEntradaAutenticacion puntoEntrada,
            ManejadorAccesoDenegado manejadorDenegado,
            LimitadorSolicitudes limitador,
            ConversorRespuestaJson conversor,
            ResolutorIpCliente resolutorIp) throws Exception {
        http
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers("/recursos/**", "/imagenes/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/", "/habitaciones", "/habitaciones/*", "/carrito",
                                "/ingresar", "/registro", "/recuperar-contrasena", "/recuperar-contrasena/administrador",
                                "/creditos").permitAll()
                        .requestMatchers("/api/publico/**", "/api/autenticacion/**", "/api/carrito/**").permitAll()
                        .requestMatchers("/api/cliente/**", "/mi-cuenta", "/mi-cuenta/**", "/pagos/paypal/**").hasRole("CLIENTE")
                        .requestMatchers("/api/operaciones/**", "/trabajador", "/trabajador/**")
                                .hasAnyRole("TRABAJADOR", "ADMINISTRADOR")
                        .requestMatchers("/api/administracion/**", "/administracion", "/administracion/**")
                                .hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated())
                .formLogin(formulario -> formulario
                        .loginPage("/ingresar")
                        .loginProcessingUrl("/ingresar")
                        .usernameParameter("correo")
                        .passwordParameter("contrasena")
                        .successHandler(manejadorExito)
                        .failureHandler(manejadorFallo))
                .logout(salida -> salida
                        .logoutUrl("/salir")
                        .logoutSuccessUrl("/?sesion=cerrada")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("SESION_HOTEL"))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(puntoEntrada)
                        .accessDeniedHandler(manejadorDenegado))
                .sessionManagement(sesion -> sesion
                        .sessionFixation(fijacion -> fijacion.changeSessionId()))
                .headers(cabeceras -> cabeceras
                        .contentSecurityPolicy(politica -> politica.policyDirectives(POLITICA_CONTENIDO))
                        .referrerPolicy(referencia -> referencia
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .frameOptions(marco -> marco.deny()))
                .addFilterBefore(
                        new FiltroLimiteAcceso(limitador, conversor, resolutorIp),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
