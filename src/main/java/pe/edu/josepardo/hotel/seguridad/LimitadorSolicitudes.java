package pe.edu.josepardo.hotel.seguridad;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;

@Component
public class LimitadorSolicitudes {

    private static final int MAXIMO_CLAVES = 200_000;

    private record Ventana(long inicio, long fin, int conteo) {
    }

    private final Map<String, Ventana> ventanas = new ConcurrentHashMap<>();
    private final LongSupplier reloj;

    @Autowired
    public LimitadorSolicitudes() {
        this(System::currentTimeMillis);
    }

    public LimitadorSolicitudes(LongSupplier reloj) {
        this.reloj = reloj;
    }

    public boolean permitir(String clave, int maximo, Duration duracion) {
        long ahora = reloj.getAsLong();
        if (ventanas.size() >= MAXIMO_CLAVES && !ventanas.containsKey(clave)) {
            limpiarVencidas();
            if (ventanas.size() >= MAXIMO_CLAVES) {
                return false;
            }
        }
        Ventana ventana = ventanas.compute(clave, (llave, actual) -> {
            if (actual == null || actual.fin() <= ahora) {
                return new Ventana(ahora, ahora + duracion.toMillis(), 1);
            }
            return new Ventana(actual.inicio(), actual.fin(), actual.conteo() + 1);
        });
        return ventana.conteo() <= maximo;
    }

    public boolean excedido(String clave, int maximo) {
        Ventana ventana = ventanas.get(clave);
        return ventana != null && ventana.fin() > reloj.getAsLong() && ventana.conteo() >= maximo;
    }

    public void registrar(String clave, Duration duracion) {
        permitir(clave, Integer.MAX_VALUE, duracion);
    }

    public void exigir(String clave, int maximo, Duration duracion) {
        if (!permitir(clave, maximo, duracion)) {
            throw ExcepcionNegocio.demasiadasSolicitudes();
        }
    }

    public void reiniciar(String clave) {
        ventanas.remove(clave);
    }

    public void reiniciarTodo() {
        ventanas.clear();
    }

    @Scheduled(fixedDelay = 600000)
    public void limpiarVencidas() {
        long ahora = reloj.getAsLong();
        ventanas.values().removeIf(ventana -> ventana.fin() <= ahora);
    }
}
