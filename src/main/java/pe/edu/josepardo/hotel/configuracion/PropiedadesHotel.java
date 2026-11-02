package pe.edu.josepardo.hotel.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hotel")
public record PropiedadesHotel(
        String urlPublica,
        Almacenamiento almacenamiento,
        Factiliza factiliza,
        PayPal paypal,
        Trafico trafico,
        Correo correo,
        Seguridad seguridad,
        AdministradorInicial administradorInicial,
        Demostracion demostracion) {

    public record Almacenamiento(String ruta) {
    }

    public record Factiliza(String urlBase, String token, boolean permitirManual, int segundosEspera) {

        public boolean tieneToken() {
            return token != null && !token.isBlank();
        }
    }

    public record PayPal(
            String entorno,
            String urlBase,
            String clienteId,
            String secreto,
            String moneda,
            int segundosEspera) {

        public boolean tieneCredenciales() {
            return clienteId != null && !clienteId.isBlank() && secreto != null && !secreto.isBlank();
        }

        public boolean esProduccion() {
            return "live".equalsIgnoreCase(entorno);
        }

        public String urlApi() {
            if (urlBase != null && !urlBase.isBlank()) {
                return urlBase;
            }
            return esProduccion() ? "https://api-m.paypal.com" : "https://api-m.sandbox.paypal.com";
        }
    }

    public record Trafico(
            boolean habilitado,
            int limitePorMinuto,
            int limiteApiPorMinuto,
            int excesosParaBloquear,
            int minutosBloqueo,
            int maximoIpsSeguidas,
            int exportacionesSimultaneas,
            String proxiesConfiables) {
    }

    public record Correo(boolean habilitado, String remitente, String nombreRemitente, boolean asincrono) {
    }

    public record Seguridad(
            int maximoIntentosAcceso,
            int minutosBloqueoAcceso,
            int minutosValidezCodigo,
            int maximoIntentosCodigo) {
    }

    public record AdministradorInicial(String correo, String contrasena, String nombres, String apellidos) {
    }

    public record Demostracion(boolean habilitada, String contrasena) {
    }
}
