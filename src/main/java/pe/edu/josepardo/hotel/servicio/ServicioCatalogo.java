package pe.edu.josepardo.hotel.servicio;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.josepardo.hotel.dto.CategoriaDto;
import pe.edu.josepardo.hotel.dto.FiltroCatalogo;
import pe.edu.josepardo.hotel.dto.HabitacionCatalogoDto;
import pe.edu.josepardo.hotel.dto.Pagina;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.repositorio.ConsultaCatalogo;
import pe.edu.josepardo.hotel.repositorio.RepositorioCategoria;

@Service
public class ServicioCatalogo {

    private static final int TAMANO_POR_DEFECTO = 9;
    private static final int TAMANO_MAXIMO = 48;

    private final ConsultaCatalogo consulta;
    private final RepositorioCategoria repositorioCategoria;
    private final ValidadorEstancia validadorEstancia;

    public ServicioCatalogo(
            ConsultaCatalogo consulta, RepositorioCategoria repositorioCategoria, ValidadorEstancia validadorEstancia) {
        this.consulta = consulta;
        this.repositorioCategoria = repositorioCategoria;
        this.validadorEstancia = validadorEstancia;
    }

    @Transactional(readOnly = true)
    public Pagina<HabitacionCatalogoDto> buscar(FiltroCatalogo filtro) {
        if (filtro.precioMinimo() != null && filtro.precioMaximo() != null
                && filtro.precioMinimo().compareTo(filtro.precioMaximo()) > 0) {
            throw ExcepcionNegocio.solicitudInvalida("El precio mínimo no puede superar al máximo.");
        }
        LocalDate hoy = LocalDate.now();
        LocalDate entrada = hoy;
        LocalDate salida = hoy.plusDays(1);
        if (filtro.fechaEntrada() != null || filtro.fechaSalida() != null) {
            validadorEstancia.validar(filtro.fechaEntrada(), filtro.fechaSalida(), hoy);
            entrada = filtro.fechaEntrada();
            salida = filtro.fechaSalida();
        }
        int pagina = filtro.pagina() == null ? 0 : Math.max(filtro.pagina(), 0);
        int tamano = filtro.tamano() == null ? TAMANO_POR_DEFECTO : Math.min(Math.max(filtro.tamano(), 1), TAMANO_MAXIMO);
        return consulta.buscar(filtro, entrada, salida, pagina, tamano);
    }

    @Transactional(readOnly = true)
    public HabitacionCatalogoDto detalle(Long id) {
        LocalDate hoy = LocalDate.now();
        HabitacionCatalogoDto habitacion = consulta.buscarPorId(id, hoy, hoy.plusDays(1));
        if (habitacion == null) {
            throw ExcepcionNegocio.noEncontrado("La habitación no existe.");
        }
        return habitacion;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDto> categoriasActivas() {
        return repositorioCategoria.listarActivas().stream()
                .map(categoria -> new CategoriaDto(categoria.getId(), categoria.getNombre(), categoria.getDescripcion(), true, 0))
                .toList();
    }
}
