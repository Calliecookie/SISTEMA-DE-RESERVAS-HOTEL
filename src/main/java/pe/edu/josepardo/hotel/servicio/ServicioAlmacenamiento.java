package pe.edu.josepardo.hotel.servicio;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.josepardo.hotel.configuracion.PropiedadesHotel;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;

@Service
public class ServicioAlmacenamiento {

    private static final long TAMANO_MAXIMO = 2L * 1024 * 1024;

    private final Path carpetaComprobantes;
    private final Path carpetaImagenes;

    public ServicioAlmacenamiento(PropiedadesHotel propiedades) {
        Path raiz = Path.of(propiedades.almacenamiento().ruta()).toAbsolutePath().normalize();
        this.carpetaComprobantes = raiz.resolve("comprobantes");
        this.carpetaImagenes = raiz.resolve("imagenes");
    }

    @PostConstruct
    void preparar() throws IOException {
        Files.createDirectories(carpetaComprobantes);
        Files.createDirectories(carpetaImagenes);
    }

    public String guardarComprobante(String numero, byte[] contenido) {
        String nombre = numero.replaceAll("[^A-Za-z0-9-]", "") + ".pdf";
        try {
            Files.write(carpetaComprobantes.resolve(nombre), contenido,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return nombre;
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo guardar el comprobante", excepcion);
        }
    }

    public byte[] leerComprobante(String nombre) {
        Path ruta = resolverSeguro(carpetaComprobantes, nombre);
        try {
            return Files.readAllBytes(ruta);
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo leer el comprobante", excepcion);
        }
    }

    public boolean existeComprobante(String nombre) {
        return Files.isRegularFile(resolverSeguro(carpetaComprobantes, nombre));
    }

    public void eliminarComprobante(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolverSeguro(carpetaComprobantes, nombre));
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo eliminar el comprobante", excepcion);
        }
    }

    public String guardarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw ExcepcionNegocio.solicitudInvalida("Selecciona una imagen.");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw ExcepcionNegocio.solicitudInvalida("La imagen supera el tamaño máximo de 2 MB.");
        }
        byte[] cabecera = new byte[12];
        int leidos;
        try (InputStream entrada = archivo.getInputStream()) {
            leidos = entrada.readNBytes(cabecera, 0, cabecera.length);
        } catch (IOException excepcion) {
            throw ExcepcionNegocio.solicitudInvalida("No se pudo leer la imagen.");
        }
        String extension = detectarExtension(cabecera, leidos);
        if (extension == null) {
            throw ExcepcionNegocio.solicitudInvalida("Solo se aceptan imágenes JPG, PNG o WEBP.");
        }
        String nombre = UUID.randomUUID() + "." + extension;
        try (InputStream entrada = archivo.getInputStream()) {
            Files.copy(entrada, carpetaImagenes.resolve(nombre));
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo guardar la imagen", excepcion);
        }
        return "/imagenes/" + nombre;
    }

    public Path rutaImagen(String nombre) {
        return resolverSeguro(carpetaImagenes, nombre);
    }

    public void eliminarImagen(String url) {
        if (url == null || !url.startsWith("/imagenes/")) {
            return;
        }
        try {
            Files.deleteIfExists(resolverSeguro(carpetaImagenes, url.substring("/imagenes/".length())));
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo eliminar la imagen", excepcion);
        }
    }

    static String detectarExtension(byte[] b, int n) {
        if (n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (n >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "png";
        }
        if (n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private Path resolverSeguro(Path carpeta, String nombre) {
        Path ruta = carpeta.resolve(nombre).normalize();
        if (!ruta.startsWith(carpeta) || !ruta.getParent().equals(carpeta)) {
            throw ExcepcionNegocio.noEncontrado("Archivo no encontrado.");
        }
        return ruta;
    }
}
