package pe.edu.josepardo.hotel.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.DashboardDto;
import pe.edu.josepardo.hotel.dto.FiltroReservas;
import pe.edu.josepardo.hotel.dto.OperacionesDto;
import pe.edu.josepardo.hotel.repositorio.ConsultaDashboard;
import pe.edu.josepardo.hotel.repositorio.ConsultaReservas;

@Service
public class ServicioPanel {

    private static final int LIMITE_LISTA = 20;

    private final ConsultaDashboard dashboard;
    private final ConsultaReservas reservas;

    public ServicioPanel(ConsultaDashboard dashboard, ConsultaReservas reservas) {
        this.dashboard = dashboard;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public DashboardDto dashboard() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioSerie = hoy.minusMonths(5).withDayOfMonth(1).atStartOfDay();
        FiltroReservas recientes = new FiltroReservas(null, null, null, null, null, 0, 6);
        return new DashboardDto(
                dashboard.totalHabitacionesActivas(),
                dashboard.habitacionesDisponiblesHoy(),
                dashboard.habitacionesOcupadasHoy(),
                dashboard.reservasPorEstados(List.of("PENDIENTE")),
                dashboard.reservasPorEstados(List.of("PAGADO", "CONFIRMADO")),
                dashboard.ingresos(null, null),
                dashboard.ingresos(inicioMes, null),
                dashboard.ingresosPorMes(inicioSerie),
                reservas.listar(recientes, null, 0, 6).elementos(),
                dashboard.actividadReciente(8));
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
