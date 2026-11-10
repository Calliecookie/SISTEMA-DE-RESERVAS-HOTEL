package pe.edu.josepardo.hotel.repositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.edu.josepardo.hotel.modelo.EstadoHabitacion;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;

@Repository
public class ConsultaOcupacion {

    public record Intervalo(LocalDate entrada, LocalDate salida) {
    }

    public record HabitacionMonitor(
            Long id, String numero, String nombre, String categoria, int capacidad, String estado, boolean activo) {
    }

    public record Tramo(
            Long habitacionId,
            String codigo,
            String cliente,
            String estado,
            LocalDate entrada,
            LocalDate salida,
            int huespedes) {
    }

    public record NocheVendida(Long habitacionId, LocalDate entrada, LocalDate salida, BigDecimal tarifaNeta) {
    }

    public record Coincidencia(String tipo, String titulo, String detalle, String referencia) {
    }

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String BLOQUEA = """
            (r.estado IN ('PAGADO', 'CONFIRMADO', 'ENTREGADO')
             OR (r.estado = 'PENDIENTE' AND r.expira_en > :ahora))
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaOcupacion(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Intervalo> intervalos(Long habitacionId, LocalDate desde, LocalDate hasta) {
        String sql = """
                SELECT r.fecha_entrada, r.fecha_salida
                FROM detalle_reservas d
                JOIN reservas r ON r.id = d.reserva_id
                WHERE d.habitacion_id = :id AND r.fecha_salida > :desde AND r.fecha_entrada < :hasta AND
                """ + BLOQUEA + " ORDER BY r.fecha_entrada, r.fecha_salida";
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("id", habitacionId)
                .addValue("desde", desde)
                .addValue("hasta", hasta)
                .addValue("ahora", LocalDateTime.now());
        return jdbc.query(sql, parametros, (fila, indice) -> new Intervalo(
                fila.getDate("fecha_entrada").toLocalDate(), fila.getDate("fecha_salida").toLocalDate()));
    }

    public List<HabitacionMonitor> habitaciones() {
        String sql = """
                SELECT h.id, h.numero, h.nombre, cat.nombre AS categoria, h.capacidad, h.estado, h.activo
                FROM habitaciones h
                JOIN categorias cat ON cat.id = h.categoria_id
                ORDER BY LENGTH(h.numero), h.numero
                """;
        return jdbc.query(sql, (fila, indice) -> new HabitacionMonitor(
                fila.getLong("id"),
                fila.getString("numero"),
                fila.getString("nombre"),
                fila.getString("categoria"),
                fila.getInt("capacidad"),
                fila.getString("estado"),
                fila.getBoolean("activo")));
    }

    public List<Tramo> tramos(LocalDate desde, LocalDate hasta) {
        String sql = """
                SELECT d.habitacion_id, r.codigo, CONCAT(u.nombres, ' ', u.apellidos) AS cliente, r.estado,
                       r.fecha_entrada, r.fecha_salida, d.cantidad_huespedes
                FROM detalle_reservas d
                JOIN reservas r ON r.id = d.reserva_id
                JOIN clientes c ON c.id = r.cliente_id
                JOIN usuarios u ON u.id = c.usuario_id
                WHERE ((r.fecha_salida > :desde AND r.fecha_entrada < :hasta) OR r.estado = 'ENTREGADO') AND
                """ + BLOQUEA + " ORDER BY r.fecha_entrada, r.id";
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("desde", desde)
                .addValue("hasta", hasta)
                .addValue("ahora", LocalDateTime.now());
        return jdbc.query(sql, parametros, (fila, indice) -> new Tramo(
                fila.getLong("habitacion_id"),
                fila.getString("codigo"),
                fila.getString("cliente"),
                fila.getString("estado"),
                fila.getDate("fecha_entrada").toLocalDate(),
                fila.getDate("fecha_salida").toLocalDate(),
                fila.getInt("cantidad_huespedes")));
    }

    public List<NocheVendida> nochesVendidas(LocalDate desde, LocalDate hasta) {
        String sql = """
                SELECT d.habitacion_id, r.fecha_entrada, r.fecha_salida,
                       (d.subtotal - d.descuento) / d.noches AS tarifa_neta
                FROM detalle_reservas d
                JOIN reservas r ON r.id = d.reserva_id
                WHERE r.estado IN ('PAGADO', 'CONFIRMADO', 'ENTREGADO', 'FINALIZADO')
                  AND r.fecha_salida > :desde AND r.fecha_entrada < :hasta
                """;
        return jdbc.query(sql, Map.of("desde", desde, "hasta", hasta), (fila, indice) -> new NocheVendida(
                fila.getLong("habitacion_id"),
                fila.getDate("fecha_entrada").toLocalDate(),
                fila.getDate("fecha_salida").toLocalDate(),
                fila.getBigDecimal("tarifa_neta")));
    }

    public long habitacionesEnServicio() {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM habitaciones WHERE activo = TRUE AND estado IN ('DISPONIBLE', 'RESERVADA', 'OCUPADA')",
                Map.of(), Long.class);
        return total == null ? 0 : total;
    }

    public List<Coincidencia> buscarReservas(String texto, int limite) {
        String sql = """
                SELECT r.codigo, CONCAT(u.nombres, ' ', u.apellidos) AS cliente, r.estado, r.fecha_entrada
                FROM reservas r
                JOIN clientes c ON c.id = r.cliente_id
                JOIN usuarios u ON u.id = c.usuario_id
                WHERE LOWER(r.codigo) LIKE :texto OR LOWER(CONCAT(u.nombres, ' ', u.apellidos)) LIKE :texto
                   OR c.dni LIKE :texto
                ORDER BY r.creado_en DESC, r.id DESC
                LIMIT :limite
                """;
        return jdbc.query(sql, parametros(texto, limite), (fila, indice) -> new Coincidencia(
                "RESERVA",
                fila.getString("codigo"),
                fila.getString("cliente") + " · " + EstadoReserva.valueOf(fila.getString("estado")).getEtiqueta()
                        + " · entrada " + fila.getDate("fecha_entrada").toLocalDate().format(FECHA),
                fila.getString("codigo")));
    }

    public List<Coincidencia> buscarHabitaciones(String texto, int limite) {
        String sql = """
                SELECT h.numero, h.nombre, cat.nombre AS categoria, h.estado
                FROM habitaciones h
                JOIN categorias cat ON cat.id = h.categoria_id
                WHERE LOWER(h.numero) LIKE :texto OR LOWER(h.nombre) LIKE :texto OR LOWER(cat.nombre) LIKE :texto
                ORDER BY LENGTH(h.numero), h.numero
                LIMIT :limite
                """;
        return jdbc.query(sql, parametros(texto, limite), (fila, indice) -> new Coincidencia(
                "HABITACION",
                fila.getString("numero") + " · " + fila.getString("nombre"),
                fila.getString("categoria") + " · " + EstadoHabitacion.valueOf(fila.getString("estado")).getEtiqueta(),
                fila.getString("numero")));
    }

    public List<Coincidencia> buscarClientes(String texto, int limite) {
        String sql = """
                SELECT CONCAT(u.nombres, ' ', u.apellidos) AS cliente, c.dni, u.correo
                FROM clientes c
                JOIN usuarios u ON u.id = c.usuario_id
                WHERE LOWER(CONCAT(u.nombres, ' ', u.apellidos)) LIKE :texto OR c.dni LIKE :texto
                   OR LOWER(u.correo) LIKE :texto
                ORDER BY u.nombres, u.apellidos
                LIMIT :limite
                """;
        return jdbc.query(sql, parametros(texto, limite), (fila, indice) -> new Coincidencia(
                "CLIENTE",
                fila.getString("cliente"),
                "DNI " + fila.getString("dni") + " · " + fila.getString("correo"),
                fila.getString("dni")));
    }

    private MapSqlParameterSource parametros(String texto, int limite) {
        String patron = "%" + texto.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return new MapSqlParameterSource().addValue("texto", patron).addValue("limite", limite);
    }
}
