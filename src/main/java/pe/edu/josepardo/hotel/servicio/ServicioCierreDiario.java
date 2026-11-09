package pe.edu.josepardo.hotel.servicio;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.josepardo.hotel.configuracion.PropiedadesHotel;
import pe.edu.josepardo.hotel.modelo.Actor;
import pe.edu.josepardo.hotel.modelo.EstadoPago;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.repositorio.RepositorioPago;
import pe.edu.josepardo.hotel.repositorio.RepositorioReserva;

/**
 * Cierre del día: los pedidos (reservas) que el cliente no pagó se dan de baja y se eliminan,
 * las estancias cuya fecha de salida ya pasó se finalizan y se recalcula el estado de las habitaciones.
 */
@Service
public class ServicioCierreDiario {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ServicioCierreDiario.class);
    private static final String MOTIVO = "Cierre del día: el pedido no fue pagado";

    public record Resultado(int expiradas, int finalizadas, int eliminadas) {
    }

    private final RepositorioReserva repositorioReserva;
    private final RepositorioPago repositorioPago;
    private final ServicioFlujoReserva servicioFlujo;
    private final ServicioEstadoHabitacion estadoHabitacion;
    private final ServicioAlmacenamiento almacenamiento;
    private final ServicioAuditoria auditoria;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaccion;
    private final boolean eliminarNoPagadas;

    public ServicioCierreDiario(
            RepositorioReserva repositorioReserva,
            RepositorioPago repositorioPago,
            ServicioFlujoReserva servicioFlujo,
            ServicioEstadoHabitacion estadoHabitacion,
            ServicioAlmacenamiento almacenamiento,
            ServicioAuditoria auditoria,
            NamedParameterJdbcTemplate jdbc,
            PlatformTransactionManager gestorTransacciones,
            PropiedadesHotel propiedades) {
        this.repositorioReserva = repositorioReserva;
        this.repositorioPago = repositorioPago;
        this.servicioFlujo = servicioFlujo;
        this.estadoHabitacion = estadoHabitacion;
        this.almacenamiento = almacenamiento;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
        this.transaccion = new TransactionTemplate(gestorTransacciones);
        this.eliminarNoPagadas = propiedades.tareas() == null || propiedades.tareas().eliminarNoPagadas();
    }

    public Resultado ejecutar() {
        int expiradas = expirarPendientes();
        int finalizadas = finalizarEstancias();
        int eliminadas = eliminarNoPagadas ? eliminarExpiradas() : 0;
        transaccion.executeWithoutResult(estado -> estadoHabitacion.recalcularTodas());
        Resultado resultado = new Resultado(expiradas, finalizadas, eliminadas);
        if (expiradas + finalizadas + eliminadas > 0) {
            auditoria.registrar(null, "CIERRE_DEL_DIA", "reservas", null,
                    "Expiradas: " + expiradas + ", finalizadas: " + finalizadas + ", eliminadas: " + eliminadas);
            REGISTRO.info("Cierre del día: {}", resultado);
        }
        return resultado;
    }

    /** Pedidos pendientes sin un pago por verificar: al final del día ya no se esperan. */
    private int expirarPendientes() {
        List<Long> ids = jdbc.queryForList("""
                SELECT r.id FROM reservas r
                 WHERE r.estado = 'PENDIENTE'
                   AND NOT EXISTS (SELECT 1 FROM pagos p WHERE p.reserva_id = r.id AND p.estado = 'DECLARADO')
                """, Map.of(), Long.class);
        int total = 0;
        for (Long id : ids) {
            Boolean hecho = transaccion.execute(estado -> repositorioReserva.bloquearPorId(id)
                    .filter(reserva -> reserva.getEstado() == EstadoReserva.PENDIENTE)
                    .map(reserva -> {
                        servicioFlujo.aplicar(reserva, EstadoReserva.EXPIRADO, Actor.SISTEMA, null, MOTIVO);
                        repositorioPago.listarPorReserva(id).stream()
                                .filter(pago -> pago.getEstado() == EstadoPago.INICIADO)
                                .forEach(pago -> {
                                    pago.setEstado(EstadoPago.RECHAZADO);
                                    pago.setMotivoRechazo(MOTIVO);
                                });
                        return true;
                    })
                    .orElse(false));
            if (Boolean.TRUE.equals(hecho)) {
                total++;
            }
        }
        return total;
    }

    /** Huéspedes que siguen "Entregado" después de su fecha de salida. */
    private int finalizarEstancias() {
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM reservas WHERE estado = 'ENTREGADO' AND fecha_salida <= :hoy",
                Map.of("hoy", LocalDate.now()), Long.class);
        int total = 0;
        for (Long id : ids) {
            Boolean hecho = transaccion.execute(estado -> repositorioReserva.bloquearPorId(id)
                    .filter(reserva -> reserva.getEstado() == EstadoReserva.ENTREGADO)
                    .map(reserva -> {
                        servicioFlujo.aplicar(reserva, EstadoReserva.FINALIZADO, Actor.SISTEMA, null,
                                "Cierre del día: estancia concluida");
                        return true;
                    })
                    .orElse(false));
            if (Boolean.TRUE.equals(hecho)) {
                total++;
            }
        }
        return total;
    }

    /** Elimina los pedidos expirados que nunca se pagaron, junto con sus detalles y comprobantes. */
    private int eliminarExpiradas() {
        List<Long> ids = jdbc.queryForList("""
                SELECT r.id FROM reservas r
                 WHERE r.estado = 'EXPIRADO'
                   AND NOT EXISTS (SELECT 1 FROM pagos p WHERE p.reserva_id = r.id
                                     AND p.estado IN ('CONFIRMADO', 'REEMBOLSADO', 'DECLARADO'))
                """, Map.of(), Long.class);
        if (ids.isEmpty()) {
            return 0;
        }
        Map<String, Object> parametros = Map.of("ids", ids);
        List<String> archivos = jdbc.queryForList(
                "SELECT ruta_archivo FROM comprobantes WHERE reserva_id IN (:ids)", parametros, String.class);
        Integer eliminadas = transaccion.execute(estado -> {
            jdbc.update("DELETE FROM comprobantes WHERE reserva_id IN (:ids)", parametros);
            jdbc.update("DELETE FROM pagos WHERE reserva_id IN (:ids)", parametros);
            jdbc.update("DELETE FROM historial_estados_reserva WHERE reserva_id IN (:ids)", parametros);
            jdbc.update("DELETE FROM detalle_reservas WHERE reserva_id IN (:ids)", parametros);
            return jdbc.update("DELETE FROM reservas WHERE id IN (:ids) AND estado = 'EXPIRADO'", parametros);
        });
        for (String archivo : archivos) {
            try {
                almacenamiento.eliminarComprobante(archivo);
            } catch (RuntimeException excepcion) {
                REGISTRO.warn("No se pudo borrar el archivo {}: {}", archivo, excepcion.getMessage());
            }
        }
        return eliminadas == null ? 0 : eliminadas;
    }
}
