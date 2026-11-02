package pe.edu.josepardo.hotel.servicio;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.CategoriaDto;
import pe.edu.josepardo.hotel.dto.SolicitudCategoria;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.modelo.Categoria;
import pe.edu.josepardo.hotel.repositorio.RepositorioCategoria;
import pe.edu.josepardo.hotel.repositorio.RepositorioHabitacion;

@Service
public class ServicioCategoria {

    private final RepositorioCategoria repositorio;
    private final RepositorioHabitacion repositorioHabitacion;
    private final ServicioAuditoria auditoria;

    public ServicioCategoria(
            RepositorioCategoria repositorio, RepositorioHabitacion repositorioHabitacion, ServicioAuditoria auditoria) {
        this.repositorio = repositorio;
        this.repositorioHabitacion = repositorioHabitacion;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDto> listar() {
        return repositorio.listarTodas().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaDto obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public CategoriaDto crear(SolicitudCategoria solicitud, Long usuarioId) {
        String nombre = solicitud.nombre().trim();
        if (repositorio.existeNombre(nombre)) {
            throw ExcepcionNegocio.conflicto("CATEGORIA_DUPLICADA", "Ya existe una categoría con ese nombre.");
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(limpiar(solicitud.descripcion()));
        repositorio.save(categoria);
        auditoria.registrar(usuarioId, "CREAR_CATEGORIA", "categorias", categoria.getId(), nombre);
        return aDto(categoria);
    }

    @Transactional
    public CategoriaDto actualizar(Long id, SolicitudCategoria solicitud, Long usuarioId) {
        Categoria categoria = buscar(id);
        String nombre = solicitud.nombre().trim();
        if (repositorio.existeNombreEnOtra(nombre, id)) {
            throw ExcepcionNegocio.conflicto("CATEGORIA_DUPLICADA", "Ya existe una categoría con ese nombre.");
        }
        categoria.setNombre(nombre);
        categoria.setDescripcion(limpiar(solicitud.descripcion()));
        auditoria.registrar(usuarioId, "EDITAR_CATEGORIA", "categorias", id, nombre);
        return aDto(categoria);
    }

    @Transactional
    public CategoriaDto cambiarActivo(Long id, boolean activo, Long usuarioId) {
        Categoria categoria = buscar(id);
        categoria.setActivo(activo);
        auditoria.registrar(usuarioId, activo ? "ACTIVAR_CATEGORIA" : "DESACTIVAR_CATEGORIA", "categorias", id, categoria.getNombre());
        return aDto(categoria);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Categoria categoria = buscar(id);
        if (repositorioHabitacion.contarPorCategoria(id) > 0) {
            throw ExcepcionNegocio.conflicto("CATEGORIA_CON_HABITACIONES",
                    "No se puede eliminar: la categoría tiene habitaciones asociadas. Puedes desactivarla.");
        }
        repositorio.delete(categoria);
        auditoria.registrar(usuarioId, "ELIMINAR_CATEGORIA", "categorias", id, categoria.getNombre());
    }

    private Categoria buscar(Long id) {
        return repositorio.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("La categoría no existe."));
    }

    private CategoriaDto aDto(Categoria categoria) {
        return new CategoriaDto(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.isActivo(),
                categoria.getId() == null ? 0 : repositorioHabitacion.contarPorCategoria(categoria.getId()));
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
