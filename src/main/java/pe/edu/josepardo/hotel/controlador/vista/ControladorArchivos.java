package pe.edu.josepardo.hotel.controlador.vista;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.seguridad.UsuarioAutenticado;
import pe.edu.josepardo.hotel.servicio.ServicioAlmacenamiento;
import pe.edu.josepardo.hotel.servicio.ServicioComprobante;

@Controller
public class ControladorArchivos {

    private final ServicioAlmacenamiento almacenamiento;
    private final ServicioComprobante servicioComprobante;

    public ControladorArchivos(ServicioAlmacenamiento almacenamiento, ServicioComprobante servicioComprobante) {
        this.almacenamiento = almacenamiento;
        this.servicioComprobante = servicioComprobante;
    }

    @GetMapping("/imagenes/{nombre:[A-Za-z0-9-]+\\.(?:jpg|png|webp)}")
    public ResponseEntity<Resource> imagen(@PathVariable String nombre) {
        Path ruta;
        try {
            ruta = almacenamiento.rutaImagen(nombre);
        } catch (ExcepcionNegocio excepcion) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!Files.isRegularFile(ruta)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        MediaType tipo = nombre.endsWith(".png")
                ? MediaType.IMAGE_PNG
                : nombre.endsWith(".webp") ? MediaType.parseMediaType("image/webp") : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(tipo)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                .body(new FileSystemResource(ruta));
    }

    @GetMapping("/comprobantes/{numero}")
    public ResponseEntity<byte[]> comprobante(
            @PathVariable String numero, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        ServicioComprobante.ArchivoComprobante archivo;
        try {
            archivo = servicioComprobante.descargar(numero, usuario);
        } catch (ExcepcionNegocio excepcion) {
            throw new ResponseStatusException(excepcion.getEstado());
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .body(archivo.contenido());
    }
}
