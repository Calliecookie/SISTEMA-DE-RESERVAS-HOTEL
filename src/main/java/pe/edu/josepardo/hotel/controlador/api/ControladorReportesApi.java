package pe.edu.josepardo.hotel.controlador.api;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.Semaphore;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.josepardo.hotel.dto.RespuestaApi;
import pe.edu.josepardo.hotel.dto.TablaReporte;
import pe.edu.josepardo.hotel.excepcion.ExcepcionNegocio;
import pe.edu.josepardo.hotel.servicio.ExportadorReportes;
import pe.edu.josepardo.hotel.servicio.FiltroReporte;
import pe.edu.josepardo.hotel.servicio.ServicioReportes;

@RestController
@RequestMapping("/api/administracion/reportes")
public class ControladorReportesApi {

    private static final MediaType TIPO_EXCEL =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ServicioReportes servicioReportes;
    private final ExportadorReportes exportador;
    private final Semaphore cupoExportaciones;

    public ControladorReportesApi(
            ServicioReportes servicioReportes, ExportadorReportes exportador, Semaphore cupoExportaciones) {
        this.servicioReportes = servicioReportes;
        this.exportador = exportador;
        this.cupoExportaciones = cupoExportaciones;
    }

    @GetMapping("/{clave}")
    public RespuestaApi<TablaReporte> reporte(
            @PathVariable String clave,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Long clienteId) {
        return RespuestaApi.exito(servicioReportes.generar(clave, new FiltroReporte(desde, hasta, busqueda, clienteId)));
    }

    @GetMapping("/{clave}/exportar")
    public ResponseEntity<byte[]> exportar(
            @PathVariable String clave,
            @RequestParam String formato,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Long clienteId) {
        if (!cupoExportaciones.tryAcquire()) {
            throw ExcepcionNegocio.servicioNoDisponible("Hay varias exportaciones en curso. Intenta en unos segundos.");
        }
        try {
            TablaReporte reporte = servicioReportes.generar(clave, new FiltroReporte(desde, hasta, busqueda, clienteId));
            boolean pdf = "pdf".equals(formato);
            if (!pdf && !"xlsx".equals(formato)) {
                throw ExcepcionNegocio.solicitudInvalida("El formato debe ser pdf o xlsx.");
            }
            byte[] contenido = pdf ? exportador.aPdf(reporte) : exportador.aExcel(reporte);
            String nombre = "reporte-" + clave + (pdf ? ".pdf" : ".xlsx");
            return ResponseEntity.ok()
                    .contentType(pdf ? MediaType.APPLICATION_PDF : TIPO_EXCEL)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(nombre, StandardCharsets.UTF_8).build().toString())
                    .body(contenido);
        } finally {
            cupoExportaciones.release();
        }
    }
}
