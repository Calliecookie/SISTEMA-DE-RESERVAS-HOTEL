package pe.edu.josepardo.hotel.modelo;

public enum MetodoPago {
    EFECTIVO("Efectivo en recepción"), PAYPAL("PayPal"), YAPE("Yape");

    private final String etiqueta;

    MetodoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean requiereNumeroOperacion() {
        return this == YAPE;
    }

    public boolean esAutomatico() {
        return this == PAYPAL;
    }
}
