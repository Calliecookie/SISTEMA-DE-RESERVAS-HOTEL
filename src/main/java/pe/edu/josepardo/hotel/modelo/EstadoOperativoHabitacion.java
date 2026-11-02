package pe.edu.josepardo.hotel.modelo;

public enum EstadoOperativoHabitacion {
    OPERATIVA("Operativa"), MANTENIMIENTO("En mantenimiento"), FUERA_DE_SERVICIO("Fuera de servicio");

    private final String etiqueta;

    EstadoOperativoHabitacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
