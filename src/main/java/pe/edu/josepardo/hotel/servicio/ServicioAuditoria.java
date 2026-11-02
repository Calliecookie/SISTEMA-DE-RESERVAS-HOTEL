package pe.edu.josepardo.hotel.servicio;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.edu.josepardo.hotel.modelo.RegistroAuditoria;
import pe.edu.josepardo.hotel.repositorio.RepositorioAuditoria;
import pe.edu.josepardo.hotel.repositorio.RepositorioUsuario;
import pe.edu.josepardo.hotel.seguridad.ResolutorIpCliente;

@Service
public class ServicioAuditoria {

    private final RepositorioAuditoria repositorio;
    private final RepositorioUsuario repositorioUsuario;
    private final ResolutorIpCliente resolutorIp;

    public ServicioAuditoria(
            RepositorioAuditoria repositorio, RepositorioUsuario repositorioUsuario, ResolutorIpCliente resolutorIp) {
        this.repositorio = repositorio;
        this.repositorioUsuario = repositorioUsuario;
        this.resolutorIp = resolutorIp;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void registrar(Long usuarioId, String accion, String entidad, Object entidadId, String detalle) {
        RegistroAuditoria registro = new RegistroAuditoria();
        if (usuarioId != null) {
            registro.setUsuario(repositorioUsuario.getReferenceById(usuarioId));
        }
        registro.setAccion(accion);
        registro.setEntidad(entidad);
        registro.setEntidadId(entidadId == null ? null : String.valueOf(entidadId));
        registro.setDetalle(recortar(detalle));
        registro.setDireccionIp(direccionActual());
        registro.setCreadoEn(LocalDateTime.now());
        repositorio.save(registro);
    }

    private String recortar(String detalle) {
        if (detalle == null) {
            return null;
        }
        return detalle.length() > 500 ? detalle.substring(0, 500) : detalle;
    }

    private String direccionActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            HttpServletRequest solicitud = atributos.getRequest();
            String ip = resolutorIp.resolver(solicitud);
            return ip.length() > 45 ? ip.substring(0, 45) : ip;
        }
        return null;
    }
}
