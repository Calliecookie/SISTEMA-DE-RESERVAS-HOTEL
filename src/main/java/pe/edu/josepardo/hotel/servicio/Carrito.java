package pe.edu.josepardo.hotel.servicio;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class Carrito implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private final Map<Long, Integer> items = new LinkedHashMap<>();

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void definirEstancia(LocalDate fechaEntrada, LocalDate fechaSalida) {
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
    }

    public Map<Long, Integer> getItems() {
        return items;
    }

    public void vaciar() {
        items.clear();
    }
}
