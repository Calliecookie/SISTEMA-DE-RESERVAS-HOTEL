package pe.edu.josepardo.hotel.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.DashboardDto;
import pe.edu.josepardo.hotel.dto.FiltroReservas;
import pe.edu.josepardo.hotel.dto.MapaHabitacionesDto;
import pe.edu.josepardo.hotel.dto.OperacionesDto;
import pe.edu.josepardo.hotel.repositorio.ConsultaDashboard;
import pe.edu.josepardo.hotel.repositorio.ConsultaOcupacion;
import pe.edu.josepardo.hotel.repositorio.ConsultaReservas;

@Service
public class ServicioPanel {

    private static final int LIMITE_LISTA = 20;
    private static final int DIAS_INDICADORES = 30;
    private static final int DIAS_PRONOSTICO = 14;

    private final ConsultaDashboard dashboard;
    private final ConsultaReservas reservas;
    private final ConsultaOcupacion ocupacion;
    private final ServicioMonitor monitor;

    public ServicioPanel(
            ConsultaDashboard dashboard, ConsultaReservas reservas, ConsultaOcupacion ocupacion, ServicioMonitor monitor) {
        this.dashboard = dashboard;
        this.reservas = reservas;
        this.ocupacion = ocupacion;
        this.monitor = monitor;
    }

    @Transactional(readOnly = true)
    public DashboardDto dashboard() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioSerie = hoy.minusMonths(5).withDayOfMonth(1).atStartOfDay();
        FiltroReservas recientes = new FiltroReservas(null, null, null, null, null, 0, 6);
        LocalDateTime inicioMesAnterior = hoy.minusMonths(1).withDayOfMonth(1).atStartOfDay();
        BigDecimal ingresosMes = dashboard.ingresos(inicioMes, null);
        long ventasMes = dashboard.ventasEfectuadas(inicioMes);
        MapaHabitacionesDto mapa = monitor.mapa();
        long habitacionesEnServicio = ocupacion.habitacionesEnServicio();
        LocalDate inicioIndicadores = hoy.minusDays(DIAS_INDICADORES - 1L);
        long nochesVendidas = 0;
        BigDecimal ingresoAlojamiento = BigDecimal.ZERO;
        for (ConsultaOcupacion.NocheVendida venta : ocupacion.nochesVendidas(inicioIndicadores, hoy.plusDays(1))) {
            LocalDate desde = venta.entrada().isBefore(inicioIndicadores) ? inicioIndicadores : venta.entrada();
            LocalDate hasta = venta.salida().isAfter(hoy.plusDays(1)) ? hoy.plusDays(1) : venta.salida();
            long noches = Math.max(ChronoUnit.DAYS.between(desde, hasta), 0);
            nochesVendidas += noches;
            ingresoAlojamiento = ingresoAlojamiento.add(venta.tarifaNeta().multiply(BigDecimal.valueOf(noches)));
        }
        return new DashboardDto(
                dashboard.totalHabitacionesActivas(),
                dashboard.habitacionesDisponiblesHoy(),
                dashboard.habitacionesOcupadasHoy(),
                dashboard.reservasPorEstados(List.of("PENDIENTE")),
                dashboard.reservasPorEstados(List.of("PAGADO", "CONFIRMADO")),
                dashboard.ingresos(null, null),
                ingresosMes,
                dashboard.ingresosPorMes(inicioSerie),
                reservas.listar(recientes, null, 0, 6).elementos(),
                dashboard.actividadReciente(8),
                dashboard.ventasEfectuadas(null),
                ventasMes,
                dashboard.ingresos(inicioMesAnterior, inicioMes),
                dividir(ingresosMes, ventasMes),
                mapa.ocupacionPorcentaje(),
                dividir(ingresoAlojamiento, nochesVendidas),
                dividir(ingresoAlojamiento, habitacionesEnServicio * DIAS_INDICADORES),
                dashboard.pagosPorVerificar(),
                dashboard.llegadasPendientes(hoy),
                dashboard.salidasPendientes(hoy),
                monitor.planificador(hoy, DIAS_PRONOSTICO).ocupacion(),
                mapa.resumen());
    }

    private BigDecimal dividir(BigDecimal valor, long divisor) {
        return divisor <= 0
                ? BigDecimal.ZERO.setScale(2)
                : valor.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public OperacionesDto operaciones() {
        LocalDate hoy = LocalDate.now();
        return new OperacionesDto(
                reservas.listarPorEntrada(hoy, List.of("CONFIRMADO"), LIMITE_LISTA),
                reservas.listarPorSalida(hoy, LIMITE_LISTA),
                reservas.listarConPagoDeclarado(LIMITE_LISTA),
                reservas.listarPorEstado("PAGADO", LIMITE_LISTA),
                dashboard.huespedesAlojados(),
                dashboard.habitacionesOcupadasHoy());
    }
}
