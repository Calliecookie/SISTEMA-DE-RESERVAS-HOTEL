package pe.edu.josepardo.hotel.repositorio;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.edu.josepardo.hotel.dto.AuditoriaDto;
import pe.edu.josepardo.hotel.dto.ClienteAdminDto;
import pe.edu.josepardo.hotel.dto.FiltroReservas;
import pe.edu.josepardo.hotel.dto.Pagina;
import pe.edu.josepardo.hotel.dto.ReservaResumenDto;
import pe.edu.josepardo.hotel.dto.VentasDto;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.modelo.MetodoPago;

@Repository
public class ConsultaReservas {

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaReservas(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Pagina<ReservaResumenDto> listar(FiltroReservas filtro, Long clienteId, int pagina, int tamano) {
        MapSqlParameterSource parametros = new MapSqlParameterSource();
        String donde = construirCondiciones(filtro, clienteId, parametros);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) " + FragmentosSql.ORIGEN_RESERVA + donde, parametros, Long.class);
        parametros.addValue("limite", tamano).addValue("desplazamiento", pagina * tamano);
        String sql = "SELECT " + FragmentosSql.COLUMNAS_RESERVA + FragmentosSql.ORIGEN_RESERVA + donde
                + " ORDER BY r.creado_en DESC, r.id DESC LIMIT :limite OFFSET :desplazamiento";
        return Pagina.de(jdbc.query(sql, parametros, (fila, indice) -> mapear(fila)), pagina, tamano, total == null ? 0 : total);
    }

    public VentasDto listarVentas(FiltroReservas filtro, int pagina, int tamano) {
        Pagina<ReservaResumenDto> resultado = listar(filtro, null, pagina, tamano);
        MapSqlParameterSource parametros = new MapSqlParameterSource();
        String donde = construirCondiciones(filtro, null, parametros);
        String sql = """
                SELECT COUNT(*) AS cantidad,
                       COALESCE(SUM(CASE WHEN EXISTS (
                         SELECT 1 FROM pagos p WHERE p.reserva_id = r.id AND p.estado = 'CONFIRMADO'
                       ) THEN r.total ELSE 0 END), 0) AS ingresos,
                       COALESCE(SUM(CASE WHEN r.estado = 'PENDIENTE' THEN r.total ELSE 0 END), 0) AS pendiente
                """ + FragmentosSql.ORIGEN_RESERVA + donde;
        return jdbc.queryForObject(sql, parametros, (fila, indice) -> new VentasDto(
                resultado,
                fila.getLong("cantidad"),
                fila.getBigDecimal("ingresos"),
                fila.getBigDecimal("pendiente")));
    }

    public List<ReservaResumenDto> listarPorEntrada(LocalDate fecha, List<String> estados, int limite) {
        String sql = "SELECT " + FragmentosSql.COLUMNAS_RESERVA + FragmentosSql.ORIGEN_RESERVA + """
                 WHERE r.fecha_entrada <= :fecha AND r.estado IN (:estados)
                 ORDER BY r.fecha_entrada, r.id LIMIT :limite
                """;
        return jdbc.query(sql, new MapSqlParameterSource()
                .addValue("fecha", fecha).addValue("estados", estados).addValue("limite", limite),
                (fila, indice) -> mapear(fila));
    }

    public List<ReservaResumenDto> listarPorSalida(LocalDate fecha, int limite) {
        String sql = "SELECT " + FragmentosSql.COLUMNAS_RESERVA + FragmentosSql.ORIGEN_RESERVA + """
                 WHERE r.fecha_salida <= :fecha AND r.estado = 'ENTREGADO'
                 ORDER BY r.fecha_salida, r.id LIMIT :limite
                """;
        return jdbc.query(sql, new MapSqlParameterSource().addValue("fecha", fecha).addValue("limite", limite),
                (fila, indice) -> mapear(fila));
    }

    public List<ReservaResumenDto> listarConPagoDeclarado(int limite) {
        String sql = "SELECT " + FragmentosSql.COLUMNAS_RESERVA + FragmentosSql.ORIGEN_RESERVA + """
                 WHERE r.estado = 'PENDIENTE'
                   AND EXISTS (SELECT 1 FROM pagos p WHERE p.reserva_id = r.id AND p.estado = 'DECLARADO')
                 ORDER BY r.expira_en, r.id LIMIT :limite
                """;
        return jdbc.query(sql, new MapSqlParameterSource().addValue("limite", limite), (fila, indice) -> mapear(fila));
    }

    public List<ReservaResumenDto> listarPorEstado(String estado, int limite) {
        String sql = "SELECT " + FragmentosSql.COLUMNAS_RESERVA + FragmentosSql.ORIGEN_RESERVA + """
                 WHERE r.estado = :estado
                 ORDER BY r.fecha_entrada, r.id LIMIT :limite
                """;
        return jdbc.query(sql, new MapSqlParameterSource().addValue("estado", estado).addValue("limite", limite),
                (fila, indice) -> mapear(fila));
    }

    public Pagina<ClienteAdminDto> listarClientes(String busqueda, int pagina, int tamano) {
        MapSqlParameterSource parametros = new MapSqlParameterSource();
        String donde = "";
        if (busqueda != null && !busqueda.isBlank()) {
            donde = """
                     WHERE (LOWER(CONCAT(u.nombres, ' ', u.apellidos)) LIKE :busqueda
                        OR c.dni LIKE :busqueda OR LOWER(u.correo) LIKE :busqueda)
                    """;
            parametros.addValue("busqueda", patron(busqueda));
        }
        String origen = " FROM clientes c JOIN usuarios u ON u.id = c.usuario_id" + donde;
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + origen, parametros, Long.class);
        parametros.addValue("limite", tamano).addValue("desplazamiento", pagina * tamano);
        String sql = """
                SELECT c.id, c.usuario_id, u.nombres, u.apellidos, u.correo, c.dni, c.telefono, c.direccion,
                       c.dni_verificado, c.origen_datos, u.activo, c.creado_en,
                       (SELECT COUNT(*) FROM reservas r WHERE r.cliente_id = c.id) AS cantidad_reservas,
                       (SELECT COALESCE(SUM(p.monto), 0) FROM pagos p JOIN reservas r ON r.id = p.reserva_id
                        WHERE r.cliente_id = c.id AND p.estado = 'CONFIRMADO') AS total_pagado
                """ + origen + " ORDER BY c.creado_en DESC, c.id DESC LIMIT :limite OFFSET :desplazamiento";
        List<ClienteAdminDto> elementos = jdbc.query(sql, parametros, (fila, indice) -> new ClienteAdminDto(
                fila.getLong("id"),
                fila.getLong("usuario_id"),
                fila.getString("nombres"),
                fila.getString("apellidos"),
                fila.getString("correo"),
                fila.getString("dni"),
                fila.getString("telefono"),
                fila.getString("direccion"),
                fila.getBoolean("dni_verificado"),
                fila.getString("origen_datos"),
                fila.getBoolean("activo"),
                fila.getLong("cantidad_reservas"),
                fila.getBigDecimal("total_pagado"),
                fechaHora(fila, "creado_en")));
        return Pagina.de(elementos, pagina, tamano, total == null ? 0 : total);
    }

    public Pagina<AuditoriaDto> listarAuditoria(String busqueda, Long usuarioId, int pagina, int tamano) {
        MapSqlParameterSource parametros = new MapSqlParameterSource();
        List<String> condiciones = new ArrayList<>();
        if (busqueda != null && !busqueda.isBlank()) {
            condiciones.add("""
                    (LOWER(a.accion) LIKE :busqueda OR LOWER(a.entidad) LIKE :busqueda
                     OR LOWER(a.detalle) LIKE :busqueda OR LOWER(u.correo) LIKE :busqueda)
                    """);
            parametros.addValue("busqueda", patron(busqueda));
        }
        if (usuarioId != null) {
            condiciones.add("a.usuario_id = :usuarioId");
            parametros.addValue("usuarioId", usuarioId);
        }
        String donde = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        String origen = " FROM registros_auditoria a LEFT JOIN usuarios u ON u.id = a.usuario_id" + donde;
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + origen, parametros, Long.class);
        parametros.addValue("limite", tamano).addValue("desplazamiento", pagina * tamano);
        String sql = """
                SELECT a.id, COALESCE(u.correo, 'Sistema') AS usuario, a.accion, a.entidad, a.entidad_id,
                       a.detalle, a.direccion_ip, a.creado_en
                """ + origen + " ORDER BY a.creado_en DESC, a.id DESC LIMIT :limite OFFSET :desplazamiento";
        List<AuditoriaDto> elementos = jdbc.query(sql, parametros, (fila, indice) -> new AuditoriaDto(
                fila.getLong("id"),
                fila.getString("usuario"),
                fila.getString("accion"),
                fila.getString("entidad"),
                fila.getString("entidad_id"),
                fila.getString("detalle"),
                fila.getString("direccion_ip"),
                fechaHora(fila, "creado_en")));
        return Pagina.de(elementos, pagina, tamano, total == null ? 0 : total);
    }

    private String construirCondiciones(FiltroReservas filtro, Long clienteId, MapSqlParameterSource parametros) {
        List<String> condiciones = new ArrayList<>();
        if (clienteId != null) {
            condiciones.add("r.cliente_id = :clienteId");
            parametros.addValue("clienteId", clienteId);
        }
        if (filtro.busqueda() != null && !filtro.busqueda().isBlank()) {
            condiciones.add("""
                    (LOWER(r.codigo) LIKE :busqueda OR c.dni LIKE :busqueda OR LOWER(u.correo) LIKE :busqueda
                     OR LOWER(CONCAT(u.nombres, ' ', u.apellidos)) LIKE :busqueda)
                    """);
            parametros.addValue("busqueda", patron(filtro.busqueda()));
        }
        if (filtro.estado() != null && !filtro.estado().isBlank()) {
            condiciones.add("r.estado = :estado");
            parametros.addValue("estado", filtro.estado());
        }
        if (filtro.metodoPago() != null && !filtro.metodoPago().isBlank()) {
            condiciones.add("r.metodo_pago = :metodoPago");
            parametros.addValue("metodoPago", filtro.metodoPago());
        }
        if (filtro.fechaDesde() != null) {
            condiciones.add("r.creado_en >= :fechaDesde");
            parametros.addValue("fechaDesde", filtro.fechaDesde().atStartOfDay());
        }
        if (filtro.fechaHasta() != null) {
            condiciones.add("r.creado_en < :fechaHasta");
            parametros.addValue("fechaHasta", filtro.fechaHasta().plusDays(1).atStartOfDay());
        }
        return condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
    }

    private String patron(String texto) {
        String limpio = texto.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + limpio + "%";
    }

    static LocalDateTime fechaHora(ResultSet fila, String columna) throws SQLException {
        Timestamp valor = fila.getTimestamp(columna);
        return valor == null ? null : valor.toLocalDateTime();
    }

    private ReservaResumenDto mapear(ResultSet fila) throws SQLException {
        EstadoReserva estado = EstadoReserva.valueOf(fila.getString("estado"));
        String metodo = fila.getString("metodo_pago");
        BigDecimal total = fila.getBigDecimal("total");
        return new ReservaResumenDto(
                fila.getString("codigo"),
                fila.getString("cliente"),
                fila.getString("dni"),
                fila.getString("correo"),
                fila.getDate("fecha_entrada").toLocalDate(),
                fila.getDate("fecha_salida").toLocalDate(),
                fila.getInt("noches"),
                fila.getInt("cantidad_huespedes"),
                fila.getString("habitaciones"),
                total,
                estado.name(),
                estado.getEtiqueta(),
                metodo,
                metodo == null ? null : MetodoPago.valueOf(metodo).getEtiqueta(),
                fechaHora(fila, "fecha_pago"),
                fechaHora(fila, "creado_en"),
                fechaHora(fila, "expira_en"),
                List.of());
    }
}
