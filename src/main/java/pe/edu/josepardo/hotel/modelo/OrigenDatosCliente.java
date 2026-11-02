package pe.edu.josepardo.hotel.modelo;

public enum OrigenDatosCliente {
    FACTILIZA("Factiliza"), MANUAL("Ingreso manual");

    private final String etiqueta;

    OrigenDatosCliente(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
