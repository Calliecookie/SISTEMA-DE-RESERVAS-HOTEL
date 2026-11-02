package pe.edu.josepardo.hotel.modelo;

import java.util.List;

public enum EstadoReserva {
    PENDIENTE("Pendiente"),
    PAGADO("Pagado"),
    CONFIRMADO("Confirmado"),
    ENTREGADO("Entregado"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado"),
    EXPIRADO("Expirado");

    public static final List<EstadoReserva> BLOQUEAN_SIN_VENCIMIENTO = List.of(PAGADO, CONFIRMADO, ENTREGADO);

    private final String etiqueta;

    EstadoReserva(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean esTerminal() {
        return this == FINALIZADO || this == CANCELADO || this == EXPIRADO;
    }
}
