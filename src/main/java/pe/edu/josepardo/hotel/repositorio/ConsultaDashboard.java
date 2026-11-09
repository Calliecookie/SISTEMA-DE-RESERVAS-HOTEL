package pe.edu.josepardo.hotel.repositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.edu.josepardo.hotel.dto.ActividadDto;
import pe.edu.josepardo.hotel.dto.PuntoSerie;

@Repository
public class ConsultaDashboard {

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaDashboard(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long totalHabitacionesActivas() {
        return contar("SELECT COUNT(*) FROM habitaciones WHERE activo = TRUE", Map.of());
    }

    public long habitacionesOcupadasHoy() {
        LocalDate hoy = LocalDate.now();
        return contar("""
                SELECT COUNT(DISTINCT d.habitacion_id)
                FROM detalle_reservas d
                JOIN reservas r ON r.id = d.reserva_id
                JOIN habitaciones h ON h.id = d.habitacion_id
                WHERE r.estado = 'ENTREGADO' AND h.activo = TRUE
                  AND r.fecha_entrada <= :hoy AND r.fecha_salida >= :hoy
                """, Map.of("hoy", hoy));
    }

    public long huespedesAlojados() {
        return contar("SELECT COALESCE(SUM(cantidad_huespedes), 0) FROM reservas WHERE estado = 'ENTREGADO'", Map.of());
    }

    public long habitacionesDisponiblesHoy() {
        LocalDate hoy = LocalDate.now();
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("fechaEntrada", hoy)
                .addValue("fechaSalida", hoy.plusDays(1))
                .addValue("ahora", LocalDateTime.now());
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM habitaciones h WHERE h.activo = TRUE AND h.estado IN ('DISPONIBLE', 'RESERVADA', 'OCUPADA') AND NOT "
                        + FragmentosSql.BLOQUEA_DISPONIBILIDAD,
                parametros, Long.class);
        return total == null ? 0 : total;
    }

    public long reservasPorEstados(List<String> estados) {
        return contar("SELECT COUNT(*) FROM reservas WHERE estado IN (:estados)", Map.of("estados", estados));
    }

    public BigDecimal ingresos(LocalDateTime desde, LocalDateTime hasta) {
        MapSqlParameterSource parametros = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(monto), 0) FROM pagos WHERE estado = 'CONFIRMADO'");
        if (desde != null) {
            sql.append(" AND fecha_pago >= :desde");
            parametros.addValue("desde", desde);
        }
        if (hasta != null) {
            sql.append(" AND fecha_pago < :hasta");
            parametros.addValue("hasta", hasta);
        }
        return jdbc.queryForObject(sql.toString(), parametros, BigDecimal.class);
    }

    public List<PuntoSerie> ingresosPorMes(LocalDateTime desde) {
        String sql = """
                SELECT DATE_FORMAT(fecha_pago, '%Y-%m') AS periodo, SUM(monto) AS total
                FROM pagos
                WHERE estado = 'CONFIRMADO' AND fecha_pago >= :desde
                GROUP BY periodo
                ORDER BY periodo
                """;
        return jdbc.query(sql, Map.of("desde", desde),
                (fila, indice) -> new PuntoSerie(fila.getString("periodo"), fila.getBigDecimal("total")));
    }

    public List<ActividadDto> actividadReciente(int limite) {
        String sql = """
                SELECT a.accion, a.entidad, a.detalle, COALESCE(u.correo, 'Sistema') AS usuario, a.creado_en
                FROM registros_auditoria a
                LEFT JOIN usuarios u ON u.id = a.usuario_id
                ORDER BY a.creado_en DESC, a.id DESC
                LIMIT :limite
                """;
        return jdbc.query(sql, Map.of("limite", limite), (fila, indice) -> new ActividadDto(
                fila.getString("accion"),
                fila.getString("entidad"),
                fila.getString("detalle"),
                fila.getString("usuario"),
                ConsultaReservas.fechaHora(fila, "creado_en")));
    }

    private long contar(String sql, Map<String, ?> parametros) {
        Long total = jdbc.queryForObject(sql, parametros, Long.class);
        return total == null ? 0 : total;
    }
}
