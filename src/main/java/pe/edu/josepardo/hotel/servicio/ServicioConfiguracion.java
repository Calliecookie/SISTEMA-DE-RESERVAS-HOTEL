package pe.edu.josepardo.hotel.servicio;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.ConfiguracionDto;
import pe.edu.josepardo.hotel.dto.HotelDto;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Configuracion;
import pe.edu.josepardo.hotel.repositorio.RepositorioConfiguracion;

@Service
public class ServicioConfiguracion {

    public static final String HOTEL_NOMBRE = "hotel_nombre";
    public static final String HOTEL_RUC = "hotel_ruc";
    public static final String HOTEL_DIRECCION = "hotel_direccion";
    public static final String HOTEL_TELEFONO = "hotel_telefono";
    public static final String HOTEL_CORREO = "hotel_correo";
    public static final String IMPUESTO_PORCENTAJE = "impuesto_porcentaje";
    public static final String HORAS_EXPIRACION = "horas_expiracion_pendiente";
    public static final String HORAS_VERIFICACION = "horas_verificacion_pago";
    public static final String DESCUENTO_NOCHES = "descuento_noches_minimas";
    public static final String DESCUENTO_PORCENTAJE = "descuento_porcentaje";
    public static final String PAYPAL_TIPO_CAMBIO = "paypal_tipo_cambio";
    public static final String YAPE_NUMERO = "yape_numero";
    public static final String YAPE_TITULAR = "yape_titular";
    public static final String YAPE_QR = "yape_qr";

    private final RepositorioConfiguracion repositorio;

    public ServicioConfiguracion(RepositorioConfiguracion repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public String texto(String clave) {
        return repositorio.findById(clave).map(Configuracion::getValor).orElse("");
    }

    @Transactional(readOnly = true)
    public BigDecimal decimal(String clave, BigDecimal porDefecto) {
        String valor = texto(clave);
        if (valor.isBlank()) {
            return porDefecto;
        }
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException excepcion) {
            return porDefecto;
        }
    }

    @Transactional(readOnly = true)
    public int entero(String clave, int porDefecto) {
        String valor = texto(clave);
        if (valor.isBlank()) {
            return porDefecto;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException excepcion) {
            return porDefecto;
        }
    }

    @Transactional(readOnly = true)
    public CalculadoraImportes.Parametros parametrosImporte() {
        return new CalculadoraImportes.Parametros(
                decimal(IMPUESTO_PORCENTAJE, new BigDecimal("18.00")),
                entero(DESCUENTO_NOCHES, 0),
                decimal(DESCUENTO_PORCENTAJE, BigDecimal.ZERO));
    }

    @Transactional(readOnly = true)
    public HotelDto hotel() {
        return new HotelDto(
                texto(HOTEL_NOMBRE), texto(HOTEL_RUC), texto(HOTEL_DIRECCION), texto(HOTEL_TELEFONO), texto(HOTEL_CORREO));
    }

    @Transactional(readOnly = true)
    public List<ConfiguracionDto> listar() {
        return repositorio.listarOrdenadas().stream()
                .map(item -> new ConfiguracionDto(item.getClave(), item.getValor(), item.getDescripcion(), item.getTipo()))
                .toList();
    }

    @Transactional
    public Map<String, String> actualizar(Map<String, String> valores) {
        Map<String, String> aplicados = new LinkedHashMap<>();
        for (Map.Entry<String, String> entrada : valores.entrySet()) {
            Configuracion configuracion = repositorio.findById(entrada.getKey())
                    .orElseThrow(() -> ExcepcionNegocio.solicitudInvalida("Configuración desconocida: " + entrada.getKey()));
            String valor = entrada.getValue() == null ? "" : entrada.getValue().trim();
            validar(configuracion, valor);
            if (!valor.equals(configuracion.getValor())) {
                configuracion.setValor(valor);
                aplicados.put(entrada.getKey(), valor);
            }
        }
        return aplicados;
    }

    private void validar(Configuracion configuracion, String valor) {
        if (valor.length() > 300) {
            throw ExcepcionNegocio.solicitudInvalida("El valor de \"" + configuracion.getDescripcion() + "\" es demasiado largo.");
        }
        String clave = configuracion.getClave();
        if (valor.isEmpty()) {
            if (PAYPAL_TIPO_CAMBIO.equals(clave) || clave.startsWith("yape_")) {
                return;
            }
            throw ExcepcionNegocio.solicitudInvalida("Completa: " + configuracion.getDescripcion() + ".");
        }
        if (HOTEL_RUC.equals(clave) && !valor.matches("^\\d{11}$")) {
            throw ExcepcionNegocio.solicitudInvalida("El RUC debe tener 11 dígitos.");
        }
        if (YAPE_NUMERO.equals(clave) && !valor.matches("^9\\d{8}$")) {
            throw ExcepcionNegocio.solicitudInvalida("El número de Yape debe tener 9 dígitos y empezar con 9.");
        }
        if ("NUMERO".equals(configuracion.getTipo()) && !valor.matches("^\\d{1,4}$")) {
            throw ExcepcionNegocio.solicitudInvalida("\"" + configuracion.getDescripcion() + "\" debe ser un número entero.");
        }
        if ("DECIMAL".equals(configuracion.getTipo())) {
            BigDecimal numero;
            try {
                numero = new BigDecimal(valor);
            } catch (NumberFormatException excepcion) {
                throw ExcepcionNegocio.solicitudInvalida("\"" + configuracion.getDescripcion() + "\" debe ser un número.");
            }
            if (numero.signum() < 0 || numero.compareTo(new BigDecimal("1000")) > 0) {
                throw ExcepcionNegocio.solicitudInvalida("\"" + configuracion.getDescripcion() + "\" está fuera de rango.");
            }
            if (PAYPAL_TIPO_CAMBIO.equals(clave) && numero.signum() == 0) {
                throw ExcepcionNegocio.solicitudInvalida("El tipo de cambio debe ser mayor a cero.");
            }
        }
    }
}
