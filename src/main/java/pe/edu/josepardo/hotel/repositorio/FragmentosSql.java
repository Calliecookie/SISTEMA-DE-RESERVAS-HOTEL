package pe.edu.josepardo.hotel.repositorio;

final class FragmentosSql {

    static final String BLOQUEA_DISPONIBILIDAD = """
            EXISTS (
              SELECT 1
              FROM detalle_reservas dr
              JOIN reservas rs ON rs.id = dr.reserva_id
              WHERE dr.habitacion_id = h.id
                AND rs.fecha_entrada < :fechaSalida
                AND rs.fecha_salida > :fechaEntrada
                AND (rs.estado IN ('PAGADO', 'CONFIRMADO', 'ENTREGADO')
                     OR (rs.estado = 'PENDIENTE' AND rs.expira_en > :ahora))
            )
            """;

    static final String HABITACIONES_RESERVA = """
            (SELECT GROUP_CONCAT(hb.numero ORDER BY hb.numero SEPARATOR ', ')
             FROM detalle_reservas dt
             JOIN habitaciones hb ON hb.id = dt.habitacion_id
             WHERE dt.reserva_id = r.id)
            """;

    static final String COLUMNAS_RESERVA = """
            r.codigo,
            CONCAT(u.nombres, ' ', u.apellidos) AS cliente,
            c.dni,
            u.correo,
            r.fecha_entrada,
            r.fecha_salida,
            DATEDIFF(r.fecha_salida, r.fecha_entrada) AS noches,
            r.cantidad_huespedes,
            """ + HABITACIONES_RESERVA + """
             AS habitaciones,
            r.total,
            r.estado,
            r.metodo_pago,
            r.fecha_pago,
            r.creado_en,
            r.expira_en
            """;

    static final String ORIGEN_RESERVA = """
            FROM reservas r
            JOIN clientes c ON c.id = r.cliente_id
            JOIN usuarios u ON u.id = c.usuario_id
            """;

    private FragmentosSql() {
    }
}
