package pe.edu.josepardo.hotel.dto;

public record MetodoPagoDisponibleDto(
        String metodo,
        String etiqueta,
        String descripcion,
        boolean requiereNumeroOperacion,
        boolean redirige,
        String yapeNumero,
        String yapeTitular,
        String yapeQr,
        String montoExterno,
        String monedaExterna) {
}
