package pe.edu.josepardo.hotel.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.ColumnaReporte;
import pe.edu.josepardo.hotel.dto.IndicadorReporte;
import pe.edu.josepardo.hotel.dto.PuntoSerie;
import pe.edu.josepardo.hotel.dto.TablaReporte;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.EstadoHabitacion;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.modelo.MetodoPago;
import pe.edu.josepardo.hotel.repositorio.ConsultaReportes;

@Service
public class ServicioReportes {

    private static final long DIAS_MAXIMOS = 3660;
    private static final long DIAS_PARA_AGRUPAR_POR_MES = 62;
    private static final int LIMITE_RANKING = 20;

    private final ConsultaReportes consulta;

    public ServicioReportes(ConsultaReportes consulta) {
        this.consulta = consulta;
    }

    @Transactional(readOnly = true)
    public TablaReporte generar(String clave, FiltroReporte filtroRecibido) {
        FiltroReporte filtro = normalizar(filtroRecibido);
        return switch (clave) {
            case "ventas-fechas" -> ventasPorFechas(filtro);
            case "ventas-cliente" -> filtro.clienteId() != null ? historialCliente(filtro) : ventasPorCliente(filtro);
            case "habitaciones-solicitadas" -> habitacionesSolicitadas(filtro);
            case "disponibilidad" -> disponibilidad(filtro);
            case "ventas-categoria" -> ventasPorCategoria(filtro);
            case "ingresos-generales" -> ingresosGenerales(filtro);
            default -> throw ExcepcionNegocio.noEncontrado("El reporte no existe.");
        };
    }

    private FiltroReporte normalizar(FiltroReporte filtro) {
        LocalDate hoy = LocalDate.now();
        LocalDate hasta = filtro.hasta() == null ? hoy : filtro.hasta();
        LocalDate desde = filtro.desde() == null ? hasta.minusDays(29) : filtro.desde();
        if (desde.isAfter(hasta)) {
            throw ExcepcionNegocio.solicitudInvalida("La fecha inicial no puede superar a la final.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > DIAS_MAXIMOS) {
            throw ExcepcionNegocio.solicitudInvalida("El rango máximo de un reporte es de 10 años.");
        }
        return new FiltroReporte(desde, hasta, filtro.busqueda(), filtro.clienteId());
    }

    private String rango(FiltroReporte filtro) {
        return "Del " + Formato.fecha(filtro.desde()) + " al " + Formato.fecha(filtro.hasta());
    }

    private TablaReporte ventasPorFechas(FiltroReporte filtro) {
        List<ConsultaReportes.OperacionVenta> operaciones = consulta.ventasPorFechas(filtro.desde(), filtro.hasta());
        BigDecimal total = operaciones.stream()
                .map(ConsultaReportes.OperacionVenta::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = operaciones.isEmpty()
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(operaciones.size()), 2, RoundingMode.HALF_UP);
        List<List<Object>> filas = new ArrayList<>();
        for (ConsultaReportes.OperacionVenta operacion : operaciones) {
            filas.add(List.of(
                    operacion.codigo(),
                    operacion.cliente(),
                    operacion.dni(),
                    Formato.fechaHora(operacion.fechaPago()),
                    MetodoPago.valueOf(operacion.metodo()).getEtiqueta(),
                    texto(operacion.habitaciones()),
                    operacion.monto()));
        }
        return new TablaReporte(
                "ventas-fechas",
                "Ventas por rango de fechas",
                rango(filtro),
                List.of(columna("Reserva"), columna("Cliente"), columna("DNI"), columna("Fecha de pago"),
                        columna("Método"), columna("Habitaciones"), moneda("Importe")),
                filas,
                List.of(
                        new IndicadorReporte("Cantidad de reservas", String.valueOf(operaciones.size())),
                        new IndicadorReporte("Total de ingresos", Formato.moneda(total)),
                        new IndicadorReporte("Promedio de venta", Formato.moneda(promedio))),
                consulta.ventasPorDia(filtro.desde(), filtro.hasta()),
                "linea");
    }

    private TablaReporte ventasPorCliente(FiltroReporte filtro) {
        List<ConsultaReportes.VentaCliente> clientes =
                consulta.ventasPorCliente(filtro.desde(), filtro.hasta(), filtro.busqueda());
        BigDecimal total = clientes.stream()
                .map(ConsultaReportes.VentaCliente::totalGastado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long operaciones = clientes.stream().mapToLong(ConsultaReportes.VentaCliente::operaciones).sum();
        List<List<Object>> filas = new ArrayList<>();
        List<PuntoSerie> serie = new ArrayList<>();
        for (ConsultaReportes.VentaCliente cliente : clientes) {
            filas.add(List.of(cliente.cliente(), cliente.dni(), cliente.correo(), cliente.operaciones(),
                    cliente.totalGastado(), cliente.clienteId()));
            if (serie.size() < 10) {
                serie.add(new PuntoSerie(cliente.cliente(), cliente.totalGastado()));
            }
        }
        return new TablaReporte(
                "ventas-cliente",
                "Ventas por cliente",
                rango(filtro),
                List.of(columna("Cliente"), columna("DNI"), columna("Correo"), numero("Operaciones"),
                        moneda("Total gastado"), new ColumnaReporte("Historial", "historial")),
                filas,
                List.of(
                        new IndicadorReporte("Clientes con compras", String.valueOf(clientes.size())),
                        new IndicadorReporte("Operaciones", String.valueOf(operaciones)),
                        new IndicadorReporte("Total facturado", Formato.moneda(total))),
                serie,
                "barras-horizontales");
    }

    private TablaReporte historialCliente(FiltroReporte filtro) {
        String nombre = consulta.nombreCliente(filtro.clienteId());
        if (nombre == null) {
            throw ExcepcionNegocio.noEncontrado("El cliente no existe.");
        }
        List<ConsultaReportes.HistorialCliente> historial = consulta.historialCliente(filtro.clienteId());
        BigDecimal pagado = historial.stream()
                .map(ConsultaReportes.HistorialCliente::pagado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long operaciones = historial.stream().filter(item -> item.pagado().signum() > 0).count();
        List<List<Object>> filas = new ArrayList<>();
        for (ConsultaReportes.HistorialCliente item : historial) {
            filas.add(List.of(
                    item.codigo(),
                    Formato.fecha(item.fechaEntrada()),
                    Formato.fecha(item.fechaSalida()),
                    EstadoReserva.valueOf(item.estado()).getEtiqueta(),
                    item.total(),
                    item.pagado()));
        }
        return new TablaReporte(
                "ventas-cliente",
                "Historial de reservas",
                nombre,
                List.of(columna("Reserva"), columna("Entrada"), columna("Salida"), columna("Estado"),
                        moneda("Total"), moneda("Pagado")),
                filas,
                List.of(
                        new IndicadorReporte("Reservas", String.valueOf(historial.size())),
                        new IndicadorReporte("Operaciones pagadas", String.valueOf(operaciones)),
                        new IndicadorReporte("Total gastado", Formato.moneda(pagado))),
                List.of(),
                "ninguno");
    }

    private TablaReporte habitacionesSolicitadas(FiltroReporte filtro) {
        List<ConsultaReportes.HabitacionSolicitada> ranking =
                consulta.habitacionesMasSolicitadas(filtro.desde(), filtro.hasta(), LIMITE_RANKING);
        List<List<Object>> filas = new ArrayList<>();
        List<PuntoSerie> serie = new ArrayList<>();
        int posicion = 1;
        for (ConsultaReportes.HabitacionSolicitada habitacion : ranking) {
            filas.add(List.of(
                    posicion++,
                    habitacion.numero() + " - " + habitacion.nombre(),
                    habitacion.categoria(),
                    habitacion.reservas(),
                    habitacion.noches(),
                    habitacion.ingresos()));
            if (serie.size() < 10) {
                serie.add(new PuntoSerie(habitacion.numero(), BigDecimal.valueOf(habitacion.reservas())));
            }
        }
        return new TablaReporte(
                "habitaciones-solicitadas",
                "Habitaciones más solicitadas",
                rango(filtro),
                List.of(numero("Ranking"), columna("Habitación"), columna("Categoría"), numero("Reservas"),
                        numero("Noches"), moneda("Ingresos generados")),
                filas,
                List.of(new IndicadorReporte(
                        "Habitación líder",
                        ranking.isEmpty() ? "Sin datos" : ranking.get(0).numero() + " - " + ranking.get(0).nombre())),
                serie,
                "barras-horizontales");
    }

    private TablaReporte disponibilidad(FiltroReporte filtro) {
        LocalDate fecha = filtro.hasta();
        List<ConsultaReportes.DisponibilidadHabitacion> inventario = consulta.inventario(fecha);
        long activas = inventario.stream().filter(ConsultaReportes.DisponibilidadHabitacion::activa).count();
        long disponibles = contar(inventario, "Disponible");
        long ocupadas = contar(inventario, "Ocupada");
        long reservadas = contar(inventario, "Reservada");
        long fuera = contar(inventario, "Fuera de servicio") + contar(inventario, "En mantenimiento");
        List<List<Object>> filas = new ArrayList<>();
        for (ConsultaReportes.DisponibilidadHabitacion habitacion : inventario) {
            filas.add(List.of(
                    habitacion.numero(),
                    habitacion.nombre(),
                    habitacion.categoria(),
                    EstadoHabitacion.valueOf(habitacion.estado()).getEtiqueta(),
                    habitacion.situacion()));
        }
        return new TablaReporte(
                "disponibilidad",
                "Estado de habitaciones y disponibilidad",
                "Situación al " + Formato.fecha(fecha),
                List.of(columna("Número"), columna("Habitación"), columna("Categoría"), columna("Estado actual"), columna("Situación en la fecha")),
                filas,
                List.of(
                        new IndicadorReporte("Total de habitaciones", String.valueOf(inventario.size())),
                        new IndicadorReporte("Activas", String.valueOf(activas)),
                        new IndicadorReporte("Disponibles", String.valueOf(disponibles)),
                        new IndicadorReporte("Ocupadas", String.valueOf(ocupadas)),
                        new IndicadorReporte("Reservadas", String.valueOf(reservadas)),
                        new IndicadorReporte("Mantenimiento o fuera de servicio", String.valueOf(fuera))),
                List.of(
                        new PuntoSerie("Disponibles", BigDecimal.valueOf(disponibles)),
                        new PuntoSerie("Ocupadas", BigDecimal.valueOf(ocupadas)),
                        new PuntoSerie("Reservadas", BigDecimal.valueOf(reservadas)),
                        new PuntoSerie("No operativas", BigDecimal.valueOf(fuera))),
                "segmentada");
    }

    private long contar(List<ConsultaReportes.DisponibilidadHabitacion> inventario, String situacion) {
        return inventario.stream().filter(item -> situacion.equals(item.situacion())).count();
    }

    private TablaReporte ventasPorCategoria(FiltroReporte filtro) {
        List<ConsultaReportes.VentaCategoria> categorias = consulta.ventasPorCategoria(filtro.desde(), filtro.hasta());
        BigDecimal total = categorias.stream()
                .map(ConsultaReportes.VentaCategoria::ingresos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<List<Object>> filas = new ArrayList<>();
        List<PuntoSerie> serie = new ArrayList<>();
        for (ConsultaReportes.VentaCategoria categoria : categorias) {
            BigDecimal participacion = total.signum() == 0
                    ? BigDecimal.ZERO
                    : categoria.ingresos().multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
            filas.add(List.of(categoria.categoria(), categoria.reservas(), categoria.ingresos(), participacion));
            serie.add(new PuntoSerie(categoria.categoria(), categoria.ingresos()));
        }
        return new TablaReporte(
                "ventas-categoria",
                "Ventas por categoría",
                rango(filtro),
                List.of(columna("Categoría"), numero("Reservas"), moneda("Ingresos"), new ColumnaReporte("Participación", "porcentaje")),
                filas,
                List.of(
                        new IndicadorReporte("Categorías con ventas", String.valueOf(categorias.size())),
                        new IndicadorReporte("Ingresos", Formato.moneda(total))),
                serie,
                "barras-horizontales");
    }

    private TablaReporte ingresosGenerales(FiltroReporte filtro) {
        ConsultaReportes.ResumenIngresos resumen = consulta.resumenIngresos(filtro.desde(), filtro.hasta());
        boolean porMes = ChronoUnit.DAYS.between(filtro.desde(), filtro.hasta()) > DIAS_PARA_AGRUPAR_POR_MES;
        List<PuntoSerie> serie = consulta.ingresosPorPeriodo(filtro.desde(), filtro.hasta(), porMes);
        List<List<Object>> filas = new ArrayList<>();
        for (PuntoSerie punto : serie) {
            filas.add(List.of(punto.etiqueta(), punto.valor()));
        }
        return new TablaReporte(
                "ingresos-generales",
                "Ingresos generales",
                rango(filtro),
                List.of(columna(porMes ? "Mes" : "Día"), moneda("Ingresos")),
                filas,
                List.of(
                        new IndicadorReporte("Ingresos totales", Formato.moneda(resumen.ingresos())),
                        new IndicadorReporte("Reservas pagadas", String.valueOf(resumen.pagadas())),
                        new IndicadorReporte("Reservas pendientes", String.valueOf(resumen.pendientes())),
                        new IndicadorReporte("Por cobrar", Formato.moneda(resumen.porCobrar()))),
                serie,
                "barras");
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private ColumnaReporte columna(String titulo) {
        return new ColumnaReporte(titulo, "texto");
    }

    private ColumnaReporte numero(String titulo) {
        return new ColumnaReporte(titulo, "numero");
    }

    private ColumnaReporte moneda(String titulo) {
        return new ColumnaReporte(titulo, "moneda");
    }
}
