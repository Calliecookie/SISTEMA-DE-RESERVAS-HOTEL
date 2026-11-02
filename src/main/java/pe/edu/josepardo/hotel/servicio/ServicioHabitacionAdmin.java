package pe.edu.josepardo.hotel.servicio;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.josepardo.hotel.dto.CaracteristicaDto;
import pe.edu.josepardo.hotel.dto.HabitacionAdminDto;
import pe.edu.josepardo.hotel.dto.SolicitudHabitacion;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Caracteristica;
import pe.edu.josepardo.hotel.modelo.Categoria;
import pe.edu.josepardo.hotel.modelo.EstadoOperativoHabitacion;
import pe.edu.josepardo.hotel.modelo.Habitacion;
import pe.edu.josepardo.hotel.repositorio.RepositorioCaracteristica;
import pe.edu.josepardo.hotel.repositorio.RepositorioCategoria;
import pe.edu.josepardo.hotel.repositorio.RepositorioDetalleReserva;
import pe.edu.josepardo.hotel.repositorio.RepositorioHabitacion;

@Service
public class ServicioHabitacionAdmin {

    private static final String PATRON_IMAGEN =
            "^(/imagenes/[A-Za-z0-9.-]+|https://(commons|upload|thumb)\\.wikimedia\\.org/\\S+)$";

    private final RepositorioHabitacion repositorio;
    private final RepositorioCategoria repositorioCategoria;
    private final RepositorioCaracteristica repositorioCaracteristica;
    private final RepositorioDetalleReserva repositorioDetalle;
    private final ServicioAlmacenamiento almacenamiento;
    private final ServicioAuditoria auditoria;

    public ServicioHabitacionAdmin(
            RepositorioHabitacion repositorio,
            RepositorioCategoria repositorioCategoria,
            RepositorioCaracteristica repositorioCaracteristica,
            RepositorioDetalleReserva repositorioDetalle,
            ServicioAlmacenamiento almacenamiento,
            ServicioAuditoria auditoria) {
        this.repositorio = repositorio;
        this.repositorioCategoria = repositorioCategoria;
        this.repositorioCaracteristica = repositorioCaracteristica;
        this.repositorioDetalle = repositorioDetalle;
        this.almacenamiento = almacenamiento;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<HabitacionAdminDto> listar() {
        return repositorio.listarCompletas().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public HabitacionAdminDto obtener(Long id) {
        return aDto(buscarCompleta(id));
    }

    @Transactional(readOnly = true)
    public List<CaracteristicaDto> caracteristicas() {
        return repositorioCaracteristica.listarOrdenadas().stream()
                .map(item -> new CaracteristicaDto(item.getId(), item.getNombre(), item.getIcono()))
                .toList();
    }

    @Transactional
    public HabitacionAdminDto crear(SolicitudHabitacion solicitud, Long usuarioId) {
        String numero = solicitud.numero().trim();
        if (repositorio.existeNumero(numero)) {
            throw ExcepcionNegocio.conflicto("HABITACION_DUPLICADA", "Ya existe una habitación con ese número.");
        }
        Habitacion habitacion = new Habitacion();
        aplicar(habitacion, solicitud, numero);
        repositorio.save(habitacion);
        auditoria.registrar(usuarioId, "CREAR_HABITACION", "habitaciones", habitacion.getId(), numero);
        return aDto(habitacion);
    }

    @Transactional
    public HabitacionAdminDto actualizar(Long id, SolicitudHabitacion solicitud, Long usuarioId) {
        Habitacion habitacion = buscarCompleta(id);
        String numero = solicitud.numero().trim();
        if (repositorio.existeNumeroEnOtra(numero, id)) {
            throw ExcepcionNegocio.conflicto("HABITACION_DUPLICADA", "Ya existe una habitación con ese número.");
        }
        aplicar(habitacion, solicitud, numero);
        auditoria.registrar(usuarioId, "EDITAR_HABITACION", "habitaciones", id, numero);
        return aDto(habitacion);
    }

    @Transactional
    public HabitacionAdminDto cambiarActivo(Long id, boolean activo, Long usuarioId) {
        Habitacion habitacion = buscarCompleta(id);
        habitacion.setActivo(activo);
        auditoria.registrar(usuarioId, activo ? "ACTIVAR_HABITACION" : "DESACTIVAR_HABITACION",
                "habitaciones", id, habitacion.getNumero());
        return aDto(habitacion);
    }

    @Transactional
    public HabitacionAdminDto subirImagen(Long id, MultipartFile archivo, Long usuarioId) {
        Habitacion habitacion = buscarCompleta(id);
        String anterior = habitacion.getImagenPrincipal();
        habitacion.setImagenPrincipal(almacenamiento.guardarImagen(archivo));
        almacenamiento.eliminarImagen(anterior);
        auditoria.registrar(usuarioId, "CAMBIAR_IMAGEN_HABITACION", "habitaciones", id, habitacion.getNumero());
        return aDto(habitacion);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Habitacion habitacion = buscarCompleta(id);
        if (repositorioDetalle.contarPorHabitacion(id) > 0) {
            throw ExcepcionNegocio.conflicto("HABITACION_CON_RESERVAS",
                    "No se puede eliminar: la habitación tiene reservas registradas. Puedes desactivarla.");
        }
        String imagen = habitacion.getImagenPrincipal();
        String numero = habitacion.getNumero();
        repositorio.delete(habitacion);
        almacenamiento.eliminarImagen(imagen);
        auditoria.registrar(usuarioId, "ELIMINAR_HABITACION", "habitaciones", id, numero);
    }

    private void aplicar(Habitacion habitacion, SolicitudHabitacion solicitud, String numero) {
        Categoria categoria = repositorioCategoria.findById(solicitud.categoriaId())
                .orElseThrow(() -> ExcepcionNegocio.solicitudInvalida("La categoría seleccionada no existe."));
        EstadoOperativoHabitacion estado;
        try {
            estado = EstadoOperativoHabitacion.valueOf(solicitud.estadoOperativo());
        } catch (IllegalArgumentException excepcion) {
            throw ExcepcionNegocio.solicitudInvalida("El estado operativo no es válido.");
        }
        String imagen = solicitud.imagenUrl() == null ? "" : solicitud.imagenUrl().trim();
        if (!imagen.isEmpty() && !imagen.matches(PATRON_IMAGEN)) {
            throw ExcepcionNegocio.solicitudInvalida("Sube una imagen o usa una URL de Wikimedia Commons.");
        }
        Set<Long> ids = solicitud.caracteristicaIds() == null ? Set.of() : solicitud.caracteristicaIds();
        List<Caracteristica> caracteristicas = repositorioCaracteristica.findAllById(ids);
        if (caracteristicas.size() != ids.size()) {
            throw ExcepcionNegocio.solicitudInvalida("Una de las características no existe.");
        }
        habitacion.setNumero(numero);
        habitacion.setNombre(solicitud.nombre().trim());
        habitacion.setCategoria(categoria);
        habitacion.setDescripcion(solicitud.descripcion().trim());
        habitacion.setCapacidad(solicitud.capacidad());
        habitacion.setPrecioNoche(solicitud.precioNoche());
        habitacion.setEstadoOperativo(estado);
        if (!imagen.isEmpty()) {
            habitacion.setImagenPrincipal(imagen);
        }
        habitacion.setCaracteristicas(new HashSet<>(caracteristicas));
    }

    private Habitacion buscarCompleta(Long id) {
        return repositorio.buscarCompleta(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("La habitación no existe."));
    }

    private HabitacionAdminDto aDto(Habitacion habitacion) {
        List<Caracteristica> caracteristicas = habitacion.getCaracteristicas().stream()
                .sorted(Comparator.comparing(Caracteristica::getNombre))
                .toList();
        return new HabitacionAdminDto(
                habitacion.getId(),
                habitacion.getNumero(),
                habitacion.getNombre(),
                habitacion.getCategoria().getId(),
                habitacion.getCategoria().getNombre(),
                habitacion.getDescripcion(),
                habitacion.getCapacidad(),
                habitacion.getPrecioNoche(),
                habitacion.getEstadoOperativo().name(),
                habitacion.getEstadoOperativo().getEtiqueta(),
                habitacion.getImagenPrincipal(),
                habitacion.isActivo(),
                caracteristicas.stream().map(Caracteristica::getId).toList(),
                caracteristicas.stream()
                        .map(item -> new CaracteristicaDto(item.getId(), item.getNombre(), item.getIcono()))
                        .toList(),
                habitacion.getId() == null ? 0 : repositorioDetalle.contarPorHabitacion(habitacion.getId()),
                habitacion.getCreadoEn());
    }
}
