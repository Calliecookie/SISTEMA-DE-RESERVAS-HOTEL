package pe.edu.josepardo.hotel.servicio;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import pe.edu.josepardo.hotel.modelo.Actor;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;

@Component
public class MaquinaEstadosReserva {

    private final Map<EstadoReserva, Map<EstadoReserva, Set<Actor>>> transiciones = new EnumMap<>(EstadoReserva.class);

    public MaquinaEstadosReserva() {
        Set<Actor> personal = EnumSet.of(Actor.TRABAJADOR, Actor.ADMINISTRADOR);
        Set<Actor> personalYSistema = EnumSet.of(Actor.TRABAJADOR, Actor.ADMINISTRADOR, Actor.SISTEMA);
        Set<Actor> personalYCliente = EnumSet.of(Actor.CLIENTE, Actor.TRABAJADOR, Actor.ADMINISTRADOR);
        agregar(EstadoReserva.PENDIENTE, EstadoReserva.PAGADO, personalYSistema);
        agregar(EstadoReserva.PENDIENTE, EstadoReserva.CANCELADO, personalYCliente);
        agregar(EstadoReserva.PENDIENTE, EstadoReserva.EXPIRADO, EnumSet.of(Actor.SISTEMA));
        agregar(EstadoReserva.PAGADO, EstadoReserva.CONFIRMADO, personal);
        agregar(EstadoReserva.PAGADO, EstadoReserva.CANCELADO, personal);
        agregar(EstadoReserva.CONFIRMADO, EstadoReserva.ENTREGADO, personal);
        agregar(EstadoReserva.CONFIRMADO, EstadoReserva.CANCELADO, EnumSet.of(Actor.ADMINISTRADOR));
        agregar(EstadoReserva.ENTREGADO, EstadoReserva.FINALIZADO, personalYSistema);
    }

    private void agregar(EstadoReserva origen, EstadoReserva destino, Set<Actor> actores) {
        transiciones.computeIfAbsent(origen, clave -> new EnumMap<>(EstadoReserva.class)).put(destino, actores);
    }

    public boolean permite(EstadoReserva origen, EstadoReserva destino, Actor actor) {
        return transiciones.getOrDefault(origen, Map.of()).getOrDefault(destino, Set.of()).contains(actor);
    }

    public Set<EstadoReserva> destinos(EstadoReserva origen, Actor actor) {
        return transiciones.getOrDefault(origen, Map.of()).entrySet().stream()
                .filter(entrada -> entrada.getValue().contains(actor))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(EstadoReserva.class)));
    }
}
