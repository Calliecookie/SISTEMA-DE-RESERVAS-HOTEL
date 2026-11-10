package pe.edu.josepardo.hotel.repositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.edu.josepardo.hotel.dto.PuntoSerie;

@Repository
public class ConsultaReportes {

    public record OperacionVenta(
            String codigo, String cliente, String dni, LocalDateTime fechaPago, String metodo, String habitaciones, BigDecimal monto) {
    }

    public record VentaCliente(
            Long clienteId, String cliente, String dni, String correo, long operaciones, BigDecimal totalGastado) {
    }

    public record HistorialCliente(
            String codigo, LocalDate fechaEntrada, LocalDate fechaSalida, String estado, BigDecimal total, BigDecimal pagado) {
    }

    public record HabitacionSolicitada(
            String numero, String nombre, String categoria, long reservas, long noches, BigDecimal ingresos) {
    }

    public record EstadoInventario(
            long total, long activas, long disponibles, long ocupadas, long reservadas, long fueraDeServicio) {
    }

    public record DisponibilidadHabitacion(
            String numero, String nombre, String categoria, String estado, boolean activa, String situacion) {
    }

    public record VentaCategoria(String categoria, long reservas, BigDecimal ingresos) {
    }

    public record VentaMetodo(String metodo, long operaciones, BigDecimal ingresos) {
    }

    public record Anticipacion(String tramo, long reservas, BigDecimal nochesPromedio, BigDecimal ingresos) {
    }

    public record ResumenIngresos(long pagadas, long pendientes, BigDecimal ingresos, BigDecimal porCobrar) {
    }

    private static final String ORIGEN_PAGOS = """
            FROM pagos p
            JOIN reservas r ON r.id = p.reserva_id
            JOIN clientes c ON c.id = r.cliente_id
            JOIN usuarios u ON u.id = c.usuario_id
            WHERE p.estado = 'CONFIRMADO' AND p.fecha_pago >= :desde AND p.fecha_pago < :hasta
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaReportes(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<OperacionVenta> ventasPorFechas(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT r.codigo, CONCAT(u.nombres, ' ', u.apellidos) AS cliente, c.dni, p.fecha_pago, p.metodo, "
                + FragmentosSql.HABITACIONES_RESERVA + " AS habitaciones, p.monto " + ORIGEN_PAGOS
                + " ORDER BY p.fecha_pago, r.id";
        return jdbc.query(sql, rango(desde, hasta), (fila, indice) -> new OperacionVenta(
                fila.getString("codigo"),
                fila.getString("cliente"),
                fila.getString("dni"),
                ConsultaReservas.fechaHora(fila, "fecha_pago"),
                fila.getString("metodo"),
                fila.getString("habitaciones"),
                fila.getBigDecimal("monto")));
    }

    public List<PuntoSerie> ventasPorDia(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT DATE_FORMAT(p.fecha_pago, '%Y-%m-%d') AS periodo, SUM(p.monto) AS total " + ORIGEN_PAGOS
                + " GROUP BY periodo ORDER BY periodo";
        return jdbc.query(sql, rango(desde, hasta),
                (fila, indice) -> new PuntoSerie(fila.getString("periodo"), fila.getBigDecimal("total")));
    }

    public List<VentaCliente> ventasPorCliente(LocalDate desde, LocalDate hasta, String busqueda) {
        MapSqlParameterSource parametros = rango(desde, hasta);
        String filtro = "";
        if (busqueda != null && !busqueda.isBlank()) {
            filtro = """
                     AND (LOWER(CONCAT(u.nombres, ' ', u.apellidos)) LIKE :busqueda
                          OR c.dni LIKE :busqueda OR LOWER(u.correo) LIKE :busqueda)
                    """;
            parametros.addValue("busqueda", "%" + busqueda.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        String sql = """
                SELECT c.id, CONCAT(u.nombres, ' ', u.apellidos) AS cliente, c.dni, u.correo,
                       COUNT(p.id) AS operaciones, SUM(p.monto) AS total
                """ + ORIGEN_PAGOS + filtro + """
                 GROUP BY c.id, u.nombres, u.apellidos, c.dni, u.correo
                 ORDER BY total DESC, cliente
                """;
        return jdbc.query(sql, parametros, (fila, indice) -> new VentaCliente(
                fila.getLong("id"),
                fila.getString("cliente"),
                fila.getString("dni"),
                fila.getString("correo"),
                fila.getLong("operaciones"),
                fila.getBigDecimal("total")));
    }

    public List<VentaMetodo> ventasPorMetodo(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT p.metodo, COUNT(p.id) AS operaciones, SUM(p.monto) AS total " + ORIGEN_PAGOS
                + " GROUP BY p.metodo ORDER BY total DESC";
        return jdbc.query(sql, rango(desde, hasta), (fila, indice) -> new VentaMetodo(
                fila.getString("metodo"), fila.getLong("operaciones"), fila.getBigDecimal("total")));
    }

    public List<Anticipacion> anticipacion(LocalDate desde, LocalDate hasta) {
        String sql = """
                SELECT t.tramo, COUNT(*) AS reservas, AVG(t.noches) AS noches_promedio, SUM(t.total) AS ingresos
                FROM (
                  SELECT CASE
                           WHEN DATEDIFF(r.fecha_entrada, DATE(r.creado_en)) <= 0 THEN '1. El mismo día'
                           WHEN DATEDIFF(r.fecha_entrada, DATE(r.creado_en)) <= 3 THEN '2. De 1 a 3 días'
                           WHEN DATEDIFF(r.fecha_entrada, DATE(r.creado_en)) <= 7 THEN '3. De 4 a 7 días'
                           WHEN DATEDIFF(r.fecha_entrada, DATE(r.creado_en)) <= 14 THEN '4. De 8 a 14 días'
                           WHEN DATEDIFF(r.fecha_entrada, DATE(r.creado_en)) <= 30 THEN '5. De 15 a 30 días'
                           ELSE '6. Más de 30 días'
                         END AS tramo,
                         DATEDIFF(r.fecha_salida, r.fecha_entrada) AS noches,
                         r.total
                  FROM reservas r
                  WHERE r.estado IN ('PAGADO', 'CONFIRMADO', 'ENTREGADO', 'FINALIZADO')
                    AND r.fecha_entrada >= :desde AND r.fecha_entrada < :hasta
                ) t
                GROUP BY t.tramo
                ORDER BY t.tramo
                """;
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("desde", desde)
                .addValue("hasta", hasta.plusDays(1));
        return jdbc.query(sql, parametros, (fila, indice) -> new Anticipacion(
                fila.getString("tramo").substring(3),
                fila.getLong("reservas"),
                fila.getBigDecimal("noches_promedio"),
                fila.getBigDecimal("ingresos")));
    }

    public List<HistorialCliente> historialCliente(Long clienteId) {
        String sql = """
                SELECT r.codigo, r.fecha_entrada, r.fecha_salida, r.estado, r.total,
                       (SELECT COALESCE(SUM(p.monto), 0) FROM pagos p
                        WHERE p.reserva_id = r.id AND p.estado = 'CONFIRMADO') AS pagado
                FROM reservas r
                WHERE r.cliente_id = :clienteId
                ORDER BY r.creado_en DESC, r.id DESC
                """;
        return jdbc.query(sql, Map.of("clienteId", clienteId), (fila, indice) -> new HistorialCliente(
                fila.getString("codigo"),
                fila.getDate("fecha_entrada").toLocalDate(),
                fila.getDate("fecha_salida").toLocalDate(),
                fila.getString("estado"),
                fila.getBigDecimal("total"),
                fila.getBigDecimal("pagado")));
    }

    public String nombreCliente(Long clienteId) {
        List<String> nombres = jdbc.query("""
                SELECT CONCAT(u.nombres, ' ', u.apellidos, ', DNI ', c.dni) AS nombre
                FROM clientes c JOIN usuarios u ON u.id = c.usuario_id
                WHERE c.id = :clienteId
                """, Map.of("clienteId", clienteId), (fila, indice) -> fila.getString("nombre"));
        return nombres.isEmpty() ? null : nombres.get(0);
    }

    public List<HabitacionSolicitada> habitacionesMasSolicitadas(LocalDate desde, LocalDate hasta, int limite) {
        MapSqlParameterSource parametros = rango(desde, hasta).addValue("limite", limite);
        String sql = """
                SELECT h.numero, h.nombre, cat.nombre AS categoria,
                       COUNT(DISTINCT r.id) AS reservas,
                       SUM(d.noches) AS noches,
                       COALESCE(SUM(CASE WHEN EXISTS (
                         SELECT 1 FROM pagos pg
                         WHERE pg.reserva_id = r.id AND pg.estado = 'CONFIRMADO'
                       ) THEN d.total ELSE 0 END), 0) AS ingresos
                FROM detalle_reservas d
                JOIN reservas r ON r.id = d.reserva_id
                JOIN habitaciones h ON h.id = d.habitacion_id
                JOIN categorias cat ON cat.id = h.categoria_id
                WHERE r.creado_en >= :desde AND r.creado_en < :hasta
                  AND r.estado NOT IN ('CANCELADO', 'EXPIRADO')
                GROUP BY h.id, h.numero, h.nombre, cat.nombre
                ORDER BY reservas DESC, ingresos DESC, h.numero
                LIMIT :limite
                """;
        return jdbc.query(sql, parametros, (fila, indice) -> new HabitacionSolicitada(
                fila.getString("numero"),
                fila.getString("nombre"),
                fila.getString("categoria"),
                fila.getLong("reservas"),
                fila.getLong("noches"),
                fila.getBigDecimal("ingresos")));
    }

    public List<DisponibilidadHabitacion> inventario(LocalDate fecha) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("fechaEntrada", fecha)
                .addValue("fechaSalida", fecha.plusDays(1))
                .addValue("ahora", LocalDateTime.now());
        String sql = """
                SELECT h.numero, h.nombre, cat.nombre AS categoria, h.estado, h.activo,
                       EXISTS (
                         SELECT 1 FROM detalle_reservas de
                         JOIN reservas re ON re.id = de.reserva_id
                         WHERE de.habitacion_id = h.id AND re.estado = 'ENTREGADO'
                           AND re.fecha_entrada < :fechaSalida AND re.fecha_salida > :fechaEntrada
                       ) AS ocupada,
                """ + FragmentosSql.BLOQUEA_DISPONIBILIDAD + """
                 AS bloqueada
                FROM habitaciones h
                JOIN categorias cat ON cat.id = h.categoria_id
                ORDER BY h.numero
                """;
        return jdbc.query(sql, parametros, (fila, indice) -> {
            boolean activa = fila.getBoolean("activo");
            String estado = fila.getString("estado");
            String situacion;
            if (!activa) {
                situacion = "Inactiva";
            } else if ("MANTENIMIENTO".equals(estado)) {
                situacion = "En mantenimiento";
            } else if ("FUERA_DE_SERVICIO".equals(estado)) {
                situacion = "Fuera de servicio";
            } else if (fila.getBoolean("ocupada")) {
                situacion = "Ocupada";
            } else if (fila.getBoolean("bloqueada")) {
                situacion = "Reservada";
            } else {
                situacion = "Disponible";
            }
            return new DisponibilidadHabitacion(
                    fila.getString("numero"), fila.getString("nombre"), fila.getString("categoria"), estado, activa, situacion);
        });
    }

    public List<VentaCategoria> ventasPorCategoria(LocalDate desde, LocalDate hasta) {
        String sql = """
                SELECT cat.nombre AS categoria,
                       COUNT(DISTINCT r.id) AS reservas,
                       SUM(d.total) AS ingresos
                FROM pagos p
                JOIN reservas r ON r.id = p.reserva_id
                JOIN detalle_reservas d ON d.reserva_id = r.id
                JOIN habitaciones h ON h.id = d.habitacion_id
                JOIN categorias cat ON cat.id = h.categoria_id
                WHERE p.estado = 'CONFIRMADO' AND p.fecha_pago >= :desde AND p.fecha_pago < :hasta
                GROUP BY cat.id, cat.nombre
                ORDER BY ingresos DESC
                """;
        return jdbc.query(sql, rango(desde, hasta), (fila, indice) -> new VentaCategoria(
                fila.getString("categoria"), fila.getLong("reservas"), fila.getBigDecimal("ingresos")));
    }

    public ResumenIngresos resumenIngresos(LocalDate desde, LocalDate hasta) {
        String sql = """
                SELECT
                  (SELECT COUNT(DISTINCT p.reserva_id) FROM pagos p
                   WHERE p.estado = 'CONFIRMADO' AND p.fecha_pago >= :desde AND p.fecha_pago < :hasta) AS pagadas,
                  (SELECT COUNT(*) FROM reservas r
                   WHERE r.estado = 'PENDIENTE' AND r.creado_en >= :desde AND r.creado_en < :hasta) AS pendientes,
                  (SELECT COALESCE(SUM(p.monto), 0) FROM pagos p
                   WHERE p.estado = 'CONFIRMADO' AND p.fecha_pago >= :desde AND p.fecha_pago < :hasta) AS ingresos,
                  (SELECT COALESCE(SUM(r.total), 0) FROM reservas r
                   WHERE r.estado = 'PENDIENTE' AND r.creado_en >= :desde AND r.creado_en < :hasta) AS por_cobrar
                """;
        return jdbc.queryForObject(sql, rango(desde, hasta), (fila, indice) -> new ResumenIngresos(
                fila.getLong("pagadas"), fila.getLong("pendientes"),
                fila.getBigDecimal("ingresos"), fila.getBigDecimal("por_cobrar")));
    }

    public List<PuntoSerie> ingresosPorPeriodo(LocalDate desde, LocalDate hasta, boolean porMes) {
        String formato = porMes ? "%Y-%m" : "%Y-%m-%d";
        String sql = "SELECT DATE_FORMAT(p.fecha_pago, '" + formato + "') AS periodo, SUM(p.monto) AS total "
                + ORIGEN_PAGOS + " GROUP BY periodo ORDER BY periodo";
        return jdbc.query(sql, rango(desde, hasta),
                (fila, indice) -> new PuntoSerie(fila.getString("periodo"), fila.getBigDecimal("total")));
    }

    private MapSqlParameterSource rango(LocalDate desde, LocalDate hasta) {
        return new MapSqlParameterSource()
                .addValue("desde", desde.atStartOfDay())
                .addValue("hasta", hasta.plusDays(1).atStartOfDay());
    }
}
