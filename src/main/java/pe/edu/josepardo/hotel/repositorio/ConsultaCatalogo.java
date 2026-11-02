package pe.edu.josepardo.hotel.repositorio;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.edu.josepardo.hotel.dto.CaracteristicaDto;
import pe.edu.josepardo.hotel.dto.FiltroCatalogo;
import pe.edu.josepardo.hotel.dto.HabitacionCatalogoDto;
import pe.edu.josepardo.hotel.dto.Pagina;

@Repository
public class ConsultaCatalogo {

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultaCatalogo(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Pagina<HabitacionCatalogoDto> buscar(
            FiltroCatalogo filtro, LocalDate fechaEntrada, LocalDate fechaSalida, int pagina, int tamano) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("fechaEntrada", fechaEntrada)
                .addValue("fechaSalida", fechaSalida)
                .addValue("ahora", LocalDateTime.now());
        String condiciones = construirCondiciones(filtro, parametros);
        String origen = " FROM habitaciones h JOIN categorias cat ON cat.id = h.categoria_id WHERE " + condiciones;

        Long total = jdbc.queryForObject("SELECT COUNT(*)" + origen, parametros, Long.class);
        parametros.addValue("limite", tamano).addValue("desplazamiento", pagina * tamano);
        String sql = """
                SELECT h.id, h.numero, h.nombre, h.categoria_id, cat.nombre AS categoria, h.descripcion,
                       h.capacidad, h.precio_noche, h.imagen_principal, h.estado_operativo,
                       CASE WHEN h.estado_operativo = 'OPERATIVA' AND NOT
                """ + FragmentosSql.BLOQUEA_DISPONIBILIDAD + " THEN 1 ELSE 0 END AS disponible" + origen
                + " ORDER BY " + construirOrden(filtro.orden()) + " LIMIT :limite OFFSET :desplazamiento";
        List<FilaHabitacion> filas = jdbc.query(sql, parametros, (fila, indice) -> mapear(fila));
        Map<Long, List<CaracteristicaDto>> caracteristicas = caracteristicasDe(filas.stream().map(FilaHabitacion::id).toList());
        List<HabitacionCatalogoDto> elementos = filas.stream()
                .map(fila -> fila.aDto(caracteristicas.getOrDefault(fila.id(), List.of())))
                .toList();
        return Pagina.de(elementos, pagina, tamano, total == null ? 0 : total);
    }

    public HabitacionCatalogoDto buscarPorId(Long id, LocalDate fechaEntrada, LocalDate fechaSalida) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("fechaEntrada", fechaEntrada)
                .addValue("fechaSalida", fechaSalida)
                .addValue("ahora", LocalDateTime.now());
        String sql = """
                SELECT h.id, h.numero, h.nombre, h.categoria_id, cat.nombre AS categoria, h.descripcion,
                       h.capacidad, h.precio_noche, h.imagen_principal, h.estado_operativo,
                       CASE WHEN h.estado_operativo = 'OPERATIVA' AND NOT
                """ + FragmentosSql.BLOQUEA_DISPONIBILIDAD + """
                 THEN 1 ELSE 0 END AS disponible
                FROM habitaciones h
                JOIN categorias cat ON cat.id = h.categoria_id
                WHERE h.id = :id AND h.activo = TRUE AND cat.activo = TRUE
                """;
        List<FilaHabitacion> filas = jdbc.query(sql, parametros, (fila, indice) -> mapear(fila));
        if (filas.isEmpty()) {
            return null;
        }
        FilaHabitacion fila = filas.get(0);
        return fila.aDto(caracteristicasDe(List.of(fila.id())).getOrDefault(fila.id(), List.of()));
    }

    private String construirCondiciones(FiltroCatalogo filtro, MapSqlParameterSource parametros) {
        List<String> condiciones = new ArrayList<>();
        condiciones.add("h.activo = TRUE");
        condiciones.add("cat.activo = TRUE");
        condiciones.add("h.estado_operativo <> 'FUERA_DE_SERVICIO'");
        if (filtro.busqueda() != null && !filtro.busqueda().isBlank()) {
            condiciones.add("""
                    (LOWER(h.nombre) LIKE :busqueda OR LOWER(h.numero) LIKE :busqueda
                     OR LOWER(h.descripcion) LIKE :busqueda OR LOWER(cat.nombre) LIKE :busqueda)
                    """);
            parametros.addValue("busqueda", "%" + escaparComodines(filtro.busqueda().trim().toLowerCase(Locale.ROOT)) + "%");
        }
        if (filtro.categoriaId() != null) {
            condiciones.add("h.categoria_id = :categoriaId");
            parametros.addValue("categoriaId", filtro.categoriaId());
        }
        if (filtro.precioMinimo() != null) {
            condiciones.add("h.precio_noche >= :precioMinimo");
            parametros.addValue("precioMinimo", filtro.precioMinimo());
        }
        if (filtro.precioMaximo() != null) {
            condiciones.add("h.precio_noche <= :precioMaximo");
            parametros.addValue("precioMaximo", filtro.precioMaximo());
        }
        if (filtro.capacidad() != null) {
            condiciones.add("h.capacidad >= :capacidad");
            parametros.addValue("capacidad", filtro.capacidad());
        }
        if (Boolean.TRUE.equals(filtro.soloDisponibles())) {
            condiciones.add("h.estado_operativo = 'OPERATIVA'");
            condiciones.add("NOT " + FragmentosSql.BLOQUEA_DISPONIBILIDAD);
        }
        return String.join(" AND ", condiciones);
    }

    private String construirOrden(String orden) {
        if ("precio_asc".equals(orden)) {
            return "h.precio_noche ASC, h.numero ASC";
        }
        if ("precio_desc".equals(orden)) {
            return "h.precio_noche DESC, h.numero ASC";
        }
        return "disponible DESC, h.precio_noche ASC, h.numero ASC";
    }

    private String escaparComodines(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Map<Long, List<CaracteristicaDto>> caracteristicasDe(List<Long> habitacionIds) {
        Map<Long, List<CaracteristicaDto>> resultado = new HashMap<>();
        if (habitacionIds.isEmpty()) {
            return resultado;
        }
        String sql = """
                SELECT hc.habitacion_id, c.id, c.nombre, c.icono
                FROM habitacion_caracteristicas hc
                JOIN caracteristicas c ON c.id = hc.caracteristica_id
                WHERE hc.habitacion_id IN (:ids)
                ORDER BY c.nombre
                """;
        jdbc.query(sql, Map.of("ids", habitacionIds), (ResultSet fila) -> {
            resultado.computeIfAbsent(fila.getLong("habitacion_id"), clave -> new ArrayList<>())
                    .add(new CaracteristicaDto(fila.getLong("id"), fila.getString("nombre"), fila.getString("icono")));
        });
        return resultado;
    }

    private FilaHabitacion mapear(ResultSet fila) throws SQLException {
        return new FilaHabitacion(
                fila.getLong("id"),
                fila.getString("numero"),
                fila.getString("nombre"),
                fila.getLong("categoria_id"),
                fila.getString("categoria"),
                fila.getString("descripcion"),
                fila.getInt("capacidad"),
                fila.getBigDecimal("precio_noche"),
                fila.getString("imagen_principal"),
                fila.getString("estado_operativo"),
                fila.getInt("disponible") == 1);
    }

    private record FilaHabitacion(
            Long id,
            String numero,
            String nombre,
            Long categoriaId,
            String categoria,
            String descripcion,
            int capacidad,
            BigDecimal precioNoche,
            String imagen,
            String estadoOperativo,
            boolean disponible) {

        HabitacionCatalogoDto aDto(List<CaracteristicaDto> caracteristicas) {
            return new HabitacionCatalogoDto(
                    id, numero, nombre, categoriaId, categoria, descripcion, capacidad,
                    precioNoche, imagen, estadoOperativo, disponible, caracteristicas);
        }
    }
}
