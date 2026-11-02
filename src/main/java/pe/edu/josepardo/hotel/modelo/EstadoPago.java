package pe.edu.josepardo.hotel.modelo;

public enum EstadoPago {
    INICIADO("En proceso"), DECLARADO("Por verificar"), CONFIRMADO("Confirmado"), RECHAZADO("Rechazado"), REEMBOLSADO("Reembolsado");

    private final String etiqueta;

    EstadoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
