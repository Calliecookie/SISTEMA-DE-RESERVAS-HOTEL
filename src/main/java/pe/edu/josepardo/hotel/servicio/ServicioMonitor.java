package pe.edu.josepardo.hotel.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.FilaPlanificadorDto;
import pe.edu.josepardo.hotel.dto.MapaHabitacionesDto;
import pe.edu.josepardo.hotel.dto.OcupacionDiaDto;
import pe.edu.josepardo.hotel.dto.PisoDto;
import pe.edu.josepardo.hotel.dto.PlanificadorDto;
import pe.edu.josepardo.hotel.dto.PuertaDto;
import pe.edu.josepardo.hotel.dto.ResultadoBusquedaDto;
import pe.edu.josepardo.hotel.dto.TramoDto;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Actor;
import pe.edu.josepardo.hotel.modelo.EstadoHabitacion;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.repositorio.ConsultaOcupacion;

@Service
public class ServicioMonitor {

    private static final int DIAS_POR_DEFECTO = 14;
    private static final int DIAS_MINIMOS = 7;
    private static final int DIAS_MAXIMOS = 31;
    private static final int DIAS_ATRAS_MAXIMOS = 365;
    private static final int HORIZONTE_MAPA = 400;
    private static final int TEXTO_MINIMO = 2;
    private static final int TEXTO_MAXIMO = 60;

    private static final Map<String, String> ETIQUETAS_SITUACION = Map.of(
            "DISPONIBLE", "Disponible",
            "RESERVADA", "Reservada",
            "OCUPADA", "Ocupada",
            "PENDIENTE", "Reserva pendiente",
            "MANTENIMIENTO", "En mantenimiento",
            "FUERA_DE_SERVICIO", "Fuera de servicio",
            "INACTIVA", "Desactivada");

    private final ConsultaOcupacion consulta;

    public ServicioMonitor(ConsultaOcupacion consulta) {
        this.consulta = consulta;
    }

    @Transactional(readOnly = true)
    public MapaHabitacionesDto mapa() {
        LocalDate hoy = LocalDate.now();
        Map<Long, List<ConsultaOcupacion.Tramo>> porHabitacion = consulta.tramos(hoy, hoy.plusDays(HORIZONTE_MAPA)).stream()
                .collect(Collectors.groupingBy(ConsultaOcupacion.Tramo::habitacionId));
        Map<String, List<PuertaDto>> pisos = new LinkedHashMap<>();
        Map<String, Long> resumen = new LinkedHashMap<>();
        ETIQUETAS_SITUACION.keySet().stream().sorted().forEach(clave -> resumen.put(clave, 0L));
        long enServicio = 0;
        long ocupadas = 0;
        for (ConsultaOcupacion.HabitacionMonitor habitacion : consulta.habitaciones()) {
            ConsultaOcupacion.Tramo tramo = elegirTramo(porHabitacion.getOrDefault(habitacion.id(), List.of()), hoy);
            String situacion = situacion(habitacion, tramo, hoy);
            resumen.merge(situacion, 1L, Long::sum);
            if (habitacion.activo() && EstadoHabitacion.valueOf(habitacion.estado()).admiteReservas()) {
                enServicio++;
                if ("OCUPADA".equals(situacion)) {
                    ocupadas++;
                }
            }
            pisos.computeIfAbsent(piso(habitacion.numero()), clave -> new ArrayList<>()).add(new PuertaDto(
                    habitacion.id(),
                    habitacion.numero(),
                    habitacion.nombre(),
                    habitacion.categoria(),
                    habitacion.capacidad(),
                    habitacion.estado(),
                    situacion,
                    ETIQUETAS_SITUACION.get(situacion),
                    tramo == null ? null : tramo.codigo(),
                    tramo == null ? null : tramo.cliente(),
                    tramo == null ? null : tramo.entrada(),
                    tramo == null ? null : tramo.salida(),
                    tramo != null && tramo.entrada().equals(hoy) && !"ENTREGADO".equals(tramo.estado()),
                    tramo != null && tramo.salida().equals(hoy) && "ENTREGADO".equals(tramo.estado())));
        }
        List<PisoDto> resultado = pisos.entrySet().stream()
                .map(entrada -> new PisoDto(entrada.getKey(), entrada.getValue()))
                .toList();
        int porcentaje = enServicio == 0 ? 0 : (int) Math.round(100.0 * ocupadas / enServicio);
        return new MapaHabitacionesDto(hoy, LocalDateTime.now(), resultado, resumen, porcentaje);
    }

    @Transactional(readOnly = true)
    public PlanificadorDto planificador(LocalDate desdeSolicitado, Integer diasSolicitados) {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = desdeSolicitado == null ? hoy : desdeSolicitado;
        if (Math.abs(ChronoUnit.DAYS.between(hoy, desde)) > DIAS_ATRAS_MAXIMOS) {
            throw ExcepcionNegocio.solicitudInvalida("El planificador muestra hasta un año antes o después de hoy.");
        }
        int cantidad = diasSolicitados == null
                ? DIAS_POR_DEFECTO
                : Math.min(Math.max(diasSolicitados, DIAS_MINIMOS), DIAS_MAXIMOS);
        LocalDate hasta = desde.plusDays(cantidad);
        List<LocalDate> dias = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            dias.add(desde.plusDays(i));
        }
        Map<Long, List<ConsultaOcupacion.Tramo>> porHabitacion = consulta.tramos(desde, hasta).stream()
                .filter(tramo -> tramo.salida().isAfter(desde) && tramo.entrada().isBefore(hasta))
                .collect(Collectors.groupingBy(ConsultaOcupacion.Tramo::habitacionId));
        int[] ocupadasPorDia = new int[cantidad];
        List<FilaPlanificadorDto> filas = new ArrayList<>();
        int enServicio = 0;
        for (ConsultaOcupacion.HabitacionMonitor habitacion : consulta.habitaciones()) {
            if (!habitacion.activo()) {
                continue;
            }
            if (EstadoHabitacion.valueOf(habitacion.estado()).admiteReservas()) {
                enServicio++;
            }
            List<TramoDto> tramos = new ArrayList<>();
            for (ConsultaOcupacion.Tramo tramo : porHabitacion.getOrDefault(habitacion.id(), List.of())) {
                LocalDate inicioVisible = tramo.entrada().isBefore(desde) ? desde : tramo.entrada();
                LocalDate finVisible = tramo.salida().isAfter(hasta) ? hasta : tramo.salida();
                int inicio = (int) ChronoUnit.DAYS.between(desde, inicioVisible);
                int noches = (int) ChronoUnit.DAYS.between(inicioVisible, finVisible);
                if (noches < 1) {
                    continue;
                }
                for (int i = inicio; i < inicio + noches; i++) {
                    ocupadasPorDia[i]++;
                }
                tramos.add(new TramoDto(
                        tramo.codigo(),
                        tramo.cliente(),
                        tramo.estado(),
                        EstadoReserva.valueOf(tramo.estado()).getEtiqueta(),
                        tramo.entrada(),
                        tramo.salida(),
                        inicio,
                        noches,
                        tramo.entrada().isBefore(desde),
                        tramo.salida().isAfter(hasta)));
            }
            filas.add(new FilaPlanificadorDto(
                    habitacion.id(), habitacion.numero(), habitacion.nombre(), habitacion.categoria(),
                    habitacion.estado(), tramos));
        }
        List<OcupacionDiaDto> ocupacion = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            int porcentaje = enServicio == 0 ? 0 : (int) Math.round(100.0 * ocupadasPorDia[i] / enServicio);
            ocupacion.add(new OcupacionDiaDto(dias.get(i), ocupadasPorDia[i], Math.min(porcentaje, 100)));
        }
        return new PlanificadorDto(desde, dias, filas, ocupacion);
    }

    @Transactional(readOnly = true)
    public List<ResultadoBusquedaDto> buscar(String texto, Actor actor) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.length() < TEXTO_MINIMO) {
            return List.of();
        }
        if (limpio.length() > TEXTO_MAXIMO) {
            limpio = limpio.substring(0, TEXTO_MAXIMO);
        }
        List<ConsultaOcupacion.Coincidencia> coincidencias = new ArrayList<>(consulta.buscarReservas(limpio, 6));
        coincidencias.addAll(consulta.buscarHabitaciones(limpio, 5));
        if (actor == Actor.ADMINISTRADOR) {
            coincidencias.addAll(consulta.buscarClientes(limpio, 5));
        }
        return coincidencias.stream()
                .map(item -> new ResultadoBusquedaDto(item.tipo(), item.titulo(), item.detalle(), item.referencia()))
                .toList();
    }

    private ConsultaOcupacion.Tramo elegirTramo(List<ConsultaOcupacion.Tramo> tramos, LocalDate hoy) {
        ConsultaOcupacion.Tramo vigente = null;
        ConsultaOcupacion.Tramo proximo = null;
        for (ConsultaOcupacion.Tramo tramo : tramos) {
            if ("ENTREGADO".equals(tramo.estado())) {
                return tramo;
            }
            boolean cubreHoy = !tramo.entrada().isAfter(hoy) && tramo.salida().isAfter(hoy);
            if (cubreHoy && (vigente == null || prioridad(tramo) > prioridad(vigente))) {
                vigente = tramo;
            } else if (tramo.entrada().isAfter(hoy) && proximo == null) {
                proximo = tramo;
            }
        }
        return vigente != null ? vigente : proximo;
    }

    private int prioridad(ConsultaOcupacion.Tramo tramo) {
        return "PENDIENTE".equals(tramo.estado()) ? 0 : 1;
    }

    private String situacion(ConsultaOcupacion.HabitacionMonitor habitacion, ConsultaOcupacion.Tramo tramo, LocalDate hoy) {
        if (!habitacion.activo()) {
            return "INACTIVA";
        }
        EstadoHabitacion estado = EstadoHabitacion.valueOf(habitacion.estado());
        if (!estado.admiteReservas()) {
            return estado.name();
        }
        if (tramo == null) {
            return estado.name();
        }
        if ("ENTREGADO".equals(tramo.estado())) {
            return "OCUPADA";
        }
        if ("PENDIENTE".equals(tramo.estado())) {
            return "PENDIENTE";
        }
        return "RESERVADA";
    }

    private String piso(String numero) {
        String digitos = numero == null ? "" : numero.replaceAll("\\D", "");
        if (digitos.length() >= 3) {
            return "Piso " + digitos.substring(0, digitos.length() - 2);
        }
        return "Otros ambientes";
    }
}
