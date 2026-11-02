package pe.edu.josepardo.hotel.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.DisponibilidadDto;
import pe.edu.josepardo.hotel.dto.ResumenImportesDto;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.EstadoReserva;
import pe.edu.josepardo.hotel.modelo.Habitacion;
import pe.edu.josepardo.hotel.repositorio.RepositorioDetalleReserva;
import pe.edu.josepardo.hotel.repositorio.RepositorioHabitacion;

@Service
public class ServicioDisponibilidad {

    private final RepositorioDetalleReserva repositorioDetalle;
    private final RepositorioHabitacion repositorioHabitacion;
    private final ValidadorEstancia validadorEstancia;
    private final CalculadoraImportes calculadora;
    private final ServicioConfiguracion configuracion;

    public ServicioDisponibilidad(
            RepositorioDetalleReserva repositorioDetalle,
            RepositorioHabitacion repositorioHabitacion,
            ValidadorEstancia validadorEstancia,
            CalculadoraImportes calculadora,
            ServicioConfiguracion configuracion) {
        this.repositorioDetalle = repositorioDetalle;
        this.repositorioHabitacion = repositorioHabitacion;
        this.validadorEstancia = validadorEstancia;
        this.calculadora = calculadora;
        this.configuracion = configuracion;
    }

    public Set<Long> ocupadas(Collection<Long> habitacionIds, LocalDate fechaEntrada, LocalDate fechaSalida) {
        if (habitacionIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(repositorioDetalle.buscarHabitacionesOcupadas(
                habitacionIds,
                fechaEntrada,
                fechaSalida,
                EstadoReserva.BLOQUEAN_SIN_VENCIMIENTO,
                EstadoReserva.PENDIENTE,
                LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public DisponibilidadDto consultar(Long habitacionId, LocalDate fechaEntrada, LocalDate fechaSalida) {
        int noches = validadorEstancia.validar(fechaEntrada, fechaSalida, LocalDate.now());
        Habitacion habitacion = repositorioHabitacion.findById(habitacionId)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("La habitación no existe."));
        boolean disponible = habitacion.esReservable()
                && ocupadas(List.of(habitacionId), fechaEntrada, fechaSalida).isEmpty();
        CalculadoraImportes.Resumen resumen = calculadora.calcular(
                List.of(habitacion.getPrecioNoche()), noches, configuracion.parametrosImporte());
        String mensaje = disponible ? "Disponible para esas fechas." : "No está disponible en esas fechas.";
        return new DisponibilidadDto(disponible, noches, mensaje, aDto(resumen));
    }

    public ResumenImportesDto aDto(CalculadoraImportes.Resumen resumen) {
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
