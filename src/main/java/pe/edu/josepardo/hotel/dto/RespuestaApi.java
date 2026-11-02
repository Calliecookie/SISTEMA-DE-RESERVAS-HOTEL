package pe.edu.josepardo.hotel.dto;

import java.util.Map;

public record RespuestaApi<T>(
        boolean exito,
        String mensaje,
        T datos,
        Map<String, String> errores,
        String codigo) {

    public static <T> RespuestaApi<T> exito(String mensaje, T datos) {
        return new RespuestaApi<>(true, mensaje, datos, null, null);
    }

    public static <T> RespuestaApi<T> exito(T datos) {
        return exito("Operación realizada.", datos);
    }

    public static RespuestaApi<Void> exito(String mensaje) {
        return new RespuestaApi<>(true, mensaje, null, null, null);
    }

    public static RespuestaApi<Void> error(String mensaje, String codigo) {
        return new RespuestaApi<>(false, mensaje, null, null, codigo);
    }

    public static RespuestaApi<Void> errorValidacion(String mensaje, Map<String, String> errores) {
        return new RespuestaApi<>(false, mensaje, null, errores, "VALIDACION");
    }
}
