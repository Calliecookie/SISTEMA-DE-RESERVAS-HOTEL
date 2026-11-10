package pe.edu.josepardo.hotel.modelo;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoHabitacion {
    DISPONIBLE("Disponible"),
    RESERVADA("Reservada"),
    OCUPADA("Ocupada"),
    MANTENIMIENTO("En mantenimiento"),
    FUERA_DE_SERVICIO("Fuera de servicio");

    public static final Set<EstadoHabitacion> MANUALES = EnumSet.of(DISPONIBLE, MANTENIMIENTO, FUERA_DE_SERVICIO);
    public static final Set<EstadoHabitacion> EN_SERVICIO = EnumSet.of(DISPONIBLE, RESERVADA, OCUPADA);

    private final String etiqueta;

    EstadoHabitacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean admiteReservas() {
        return EN_SERVICIO.contains(this);
    }
}
