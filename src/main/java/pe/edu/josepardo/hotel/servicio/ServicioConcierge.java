package pe.edu.josepardo.hotel.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.AlternativaDto;
import pe.edu.josepardo.hotel.dto.CalendarioDto;
import pe.edu.josepardo.hotel.dto.CaracteristicaDto;
import pe.edu.josepardo.hotel.dto.CotizacionDto;
import pe.edu.josepardo.hotel.dto.DiaCalendarioDto;
import pe.edu.josepardo.hotel.dto.FiltroCatalogo;
import pe.edu.josepardo.hotel.dto.HabitacionCatalogoDto;
import pe.edu.josepardo.hotel.dto.LineaCotizacionDto;
import pe.edu.josepardo.hotel.dto.ResumenImportesDto;
import pe.edu.josepardo.hotel.dto.SugerenciasDto;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.EstadoHabitacion;
import pe.edu.josepardo.hotel.repositorio.ConsultaCatalogo;
import pe.edu.josepardo.hotel.repositorio.ConsultaOcupacion;

@Service
public class ServicioConcierge {

    public static final int HABITACIONES_MAXIMAS = 4;
    private static final int DIAS_BUSQUEDA = 90;
    private static final int DIAS_CALENDARIO_MINIMOS = 7;
    private static final int DIAS_CALENDARIO_MAXIMOS = 62;
    private static final int ALTERNATIVAS_MAXIMAS = 3;
    private static final int CANDIDATAS_MAXIMAS = 48;
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ConsultaCatalogo catalogo;
    private final ConsultaOcupacion ocupacion;
    private final ValidadorEstancia validadorEstancia;
    private final CalculadoraImportes calculadora;
    private final ServicioConfiguracion configuracion;

    public ServicioConcierge(
            ConsultaCatalogo catalogo,
            ConsultaOcupacion ocupacion,
            ValidadorEstancia validadorEstancia,
            CalculadoraImportes calculadora,
            ServicioConfiguracion configuracion) {
        this.catalogo = catalogo;
        this.ocupacion = ocupacion;
        this.validadorEstancia = validadorEstancia;
        this.calculadora = calculadora;
        this.configuracion = configuracion;
    }

    @Transactional(readOnly = true)
    public CotizacionDto cotizar(List<Long> ids, LocalDate fechaEntrada, LocalDate fechaSalida) {
        Set<Long> unicos = ids == null ? Set.of() : new LinkedHashSet<>(ids);
        if (unicos.isEmpty()) {
            throw ExcepcionNegocio.solicitudInvalida("Elige al menos una habitación para cotizar.");
        }
        if (unicos.size() > HABITACIONES_MAXIMAS) {
            throw ExcepcionNegocio.solicitudInvalida(
                    "Puedes cotizar o comparar hasta " + HABITACIONES_MAXIMAS + " habitaciones a la vez.");
        }
        int noches = validadorEstancia.validar(fechaEntrada, fechaSalida, LocalDate.now());
        List<HabitacionCatalogoDto> habitaciones = new ArrayList<>();
        for (Long id : unicos) {
            HabitacionCatalogoDto habitacion = catalogo.buscarPorId(id, fechaEntrada, fechaSalida);
            if (habitacion == null) {
                throw ExcepcionNegocio.noEncontrado("Una de las habitaciones no existe.");
            }
            habitaciones.add(habitacion);
        }
        CalculadoraImportes.Parametros parametros = configuracion.parametrosImporte();
        List<BigDecimal> precios = habitaciones.stream().map(HabitacionCatalogoDto::precioNoche).toList();
        CalculadoraImportes.Resumen resumen = calculadora.calcular(precios, noches, parametros);
        List<LineaCotizacionDto> lineas = new ArrayList<>();
        for (int i = 0; i < habitaciones.size(); i++) {
            CalculadoraImportes.Linea linea = resumen.lineas().get(i);
            lineas.add(new LineaCotizacionDto(
                    habitaciones.get(i), linea.subtotal(), linea.descuento(), linea.impuesto(), linea.total()));
        }
        int nochesParaDescuento = 0;
        BigDecimal ahorroPotencial = BigDecimal.ZERO.setScale(2);
        BigDecimal porcentajeDisponible = BigDecimal.ZERO;
        boolean hayPromocion = parametros.nochesMinimasDescuento() > 0
                && parametros.porcentajeDescuento().signum() > 0
                && parametros.nochesMinimasDescuento() <= ValidadorEstancia.NOCHES_MAXIMAS;
        if (hayPromocion) {
            porcentajeDisponible = parametros.porcentajeDescuento();
            if (noches < parametros.nochesMinimasDescuento()) {
                nochesParaDescuento = parametros.nochesMinimasDescuento() - noches;
                ahorroPotencial = calculadora
                        .calcular(precios, parametros.nochesMinimasDescuento(), parametros)
                        .descuento();
            }
        }
        return new CotizacionDto(
                fechaEntrada,
                fechaSalida,
                noches,
                lineas,
                aResumen(resumen),
                resumen.total().divide(BigDecimal.valueOf(noches), 2, RoundingMode.HALF_UP),
                nochesParaDescuento,
                porcentajeDisponible,
                ahorroPotencial,
                habitaciones.stream().allMatch(HabitacionCatalogoDto::disponible));
    }

    @Transactional(readOnly = true)
    public SugerenciasDto sugerir(Long habitacionId, LocalDate fechaEntrada, LocalDate fechaSalida) {
        int noches = validadorEstancia.validar(fechaEntrada, fechaSalida, LocalDate.now());
        HabitacionCatalogoDto origen = catalogo.buscarPorId(habitacionId, fechaEntrada, fechaSalida);
        if (origen == null) {
            throw ExcepcionNegocio.noEncontrado("La habitación no existe.");
        }
        if (origen.disponible()) {
            return new SugerenciasDto(true, noches, fechaEntrada, fechaSalida, List.of());
        }
        LocalDate proximaEntrada = enServicio(origen) ? proximaVentana(habitacionId, fechaEntrada, noches) : null;
        LocalDate proximaSalida = proximaEntrada == null ? null : proximaEntrada.plusDays(noches);
        FiltroCatalogo filtro = new FiltroCatalogo(
                null, null, null, null, null, fechaEntrada, fechaSalida, true, null, 0, CANDIDATAS_MAXIMAS);
        List<AlternativaDto> alternativas = catalogo
                .buscar(filtro, fechaEntrada, fechaSalida, 0, CANDIDATAS_MAXIMAS)
                .elementos().stream()
                .filter(candidata -> !candidata.id().equals(habitacionId))
                .map(candidata -> evaluar(origen, candidata))
                .sorted(Comparator.comparingInt(AlternativaDto::afinidad).reversed()
                        .thenComparing(alternativa -> alternativa.diferenciaPrecio().abs()))
                .limit(ALTERNATIVAS_MAXIMAS)
                .toList();
        return new SugerenciasDto(false, noches, proximaEntrada, proximaSalida, alternativas);
    }

    @Transactional(readOnly = true)
    public CalendarioDto calendario(Long habitacionId, LocalDate desdeSolicitado, Integer diasSolicitados) {
        LocalDate hoy = LocalDate.now();
        HabitacionCatalogoDto habitacion = catalogo.buscarPorId(habitacionId, hoy, hoy.plusDays(1));
        if (habitacion == null) {
            throw ExcepcionNegocio.noEncontrado("La habitación no existe.");
        }
        LocalDate desde = desdeSolicitado == null || desdeSolicitado.isBefore(hoy) ? hoy : desdeSolicitado;
        int dias = diasSolicitados == null
                ? 42
                : Math.min(Math.max(diasSolicitados, DIAS_CALENDARIO_MINIMOS), DIAS_CALENDARIO_MAXIMOS);
        boolean enServicio = enServicio(habitacion);
        List<ConsultaOcupacion.Intervalo> intervalos = ocupacion.intervalos(habitacionId, desde, desde.plusDays(dias));
        List<DiaCalendarioDto> resultado = new ArrayList<>();
        int disponibles = 0;
        for (int i = 0; i < dias; i++) {
            LocalDate fecha = desde.plusDays(i);
            boolean libre = enServicio && intervalos.stream()
                    .noneMatch(intervalo -> !fecha.isBefore(intervalo.entrada()) && fecha.isBefore(intervalo.salida()));
            if (libre) {
                disponibles++;
            }
            resultado.add(new DiaCalendarioDto(fecha, libre));
        }
        return new CalendarioDto(habitacionId, desde, enServicio, disponibles, resultado);
    }

    private LocalDate proximaVentana(Long habitacionId, LocalDate desde, int noches) {
        LocalDate limite = desde.plusDays(DIAS_BUSQUEDA);
        List<ConsultaOcupacion.Intervalo> intervalos = ocupacion.intervalos(habitacionId, desde, limite.plusDays(noches));
        LocalDate candidata = desde;
        for (ConsultaOcupacion.Intervalo intervalo : intervalos) {
            if (!intervalo.entrada().isBefore(candidata.plusDays(noches))) {
                break;
            }
            if (intervalo.salida().isAfter(candidata)) {
                candidata = intervalo.salida();
            }
        }
        return candidata.isAfter(limite) ? null : candidata;
    }

    private AlternativaDto evaluar(HabitacionCatalogoDto origen, HabitacionCatalogoDto candidata) {
        List<String> motivos = new ArrayList<>();
        int puntos = 0;
        if (candidata.categoriaId().equals(origen.categoriaId())) {
            puntos += 40;
            motivos.add("Misma categoría: " + candidata.categoria());
        }
        if (candidata.capacidad() == origen.capacidad()) {
            puntos += 25;
            motivos.add("Misma capacidad");
        } else if (candidata.capacidad() > origen.capacidad()) {
            puntos += 20;
            motivos.add("Más espacio: hasta " + candidata.capacidad() + " huéspedes");
        }
        BigDecimal diferencia = candidata.precioNoche().subtract(origen.precioNoche());
        BigDecimal proporcion = diferencia.abs().divide(origen.precioNoche(), 4, RoundingMode.HALF_UP);
        BigDecimal cercania = BigDecimal.ONE.subtract(proporcion.min(BigDecimal.ONE));
        puntos += cercania.multiply(BigDecimal.valueOf(25)).setScale(0, RoundingMode.HALF_UP).intValue();
        if (diferencia.signum() < 0) {
            motivos.add("Ahorras " + Formato.moneda(diferencia.abs()) + " por noche");
        } else if (diferencia.signum() == 0) {
            motivos.add("Mismo precio por noche");
        } else if (proporcion.multiply(CIEN).compareTo(BigDecimal.valueOf(15)) <= 0) {
            motivos.add("Precio similar");
        }
        Set<Long> rasgosOrigen = origen.caracteristicas().stream().map(CaracteristicaDto::id).collect(Collectors.toSet());
        if (!rasgosOrigen.isEmpty()) {
            long compartidos = candidata.caracteristicas().stream()
                    .filter(rasgo -> rasgosOrigen.contains(rasgo.id()))
                    .count();
            puntos += (int) Math.round(10.0 * compartidos / rasgosOrigen.size());
            if (compartidos == rasgosOrigen.size()) {
                motivos.add("Incluye los mismos servicios");
            } else if (compartidos > 0) {
                motivos.add("Comparte " + compartidos + " de " + rasgosOrigen.size() + " servicios");
            }
        }
        return new AlternativaDto(candidata, Math.min(puntos, 100), motivos, diferencia);
    }

    private boolean enServicio(HabitacionCatalogoDto habitacion) {
        return EstadoHabitacion.valueOf(habitacion.estado()).admiteReservas();
    }

    private ResumenImportesDto aResumen(CalculadoraImportes.Resumen resumen) {
        return new ResumenImportesDto(
                resumen.noches(),
                resumen.subtotal(),
                resumen.descuento(),
                resumen.porcentajeDescuento(),
                resumen.impuesto(),
                resumen.porcentajeImpuesto(),
                resumen.total());
    }
}
