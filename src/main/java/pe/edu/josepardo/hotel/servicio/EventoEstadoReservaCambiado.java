package pe.edu.josepardo.hotel.servicio;

public record EventoEstadoReservaCambiado(Long reservaId, String estadoEtiqueta, String motivo) {
}
