package pe.edu.josepardo.hotel.excepcion;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import pe.edu.josepardo.hotel.dto.RespuestaApi;

@RestControllerAdvice(basePackages = "pe.edu.josepardo.hotel.controlador.api")
public class ManejadorGlobalExcepciones {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    @ExceptionHandler(ExcepcionNegocio.class)
    public ResponseEntity<RespuestaApi<Void>> alFallarNegocio(ExcepcionNegocio excepcion) {
        return ResponseEntity.status(excepcion.getEstado())
                .body(RespuestaApi.error(excepcion.getMensajeSeguro(), excepcion.getCodigo()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaApi<Void>> alFallarValidacion(MethodArgumentNotValidException excepcion) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : excepcion.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        excepcion.getBindingResult().getGlobalErrors()
                .forEach(error -> errores.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(RespuestaApi.errorValidacion("Revisa los datos ingresados.", errores));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RespuestaApi<Void>> alViolarRestriccion(ConstraintViolationException excepcion) {
        return ResponseEntity.badRequest()
                .body(RespuestaApi.error("Revisa los datos ingresados.", "VALIDACION"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<RespuestaApi<Void>> alRecibirFormatoInvalido(Exception excepcion) {
        return ResponseEntity.badRequest()
                .body(RespuestaApi.error("El formato de los datos no es válido.", "FORMATO_INVALIDO"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RespuestaApi<Void>> alExcederTamano(MaxUploadSizeExceededException excepcion) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(RespuestaApi.error("El archivo supera el tamaño máximo de 2 MB.", "ARCHIVO_GRANDE"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaApi<Void>> alViolarIntegridad(DataIntegrityViolationException excepcion) {
        REGISTRO.warn("Violación de integridad: {}", excepcion.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(RespuestaApi.error("La operación entra en conflicto con datos existentes.", "CONFLICTO_DATOS"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaApi<Void>> alOcurrirError(Exception excepcion) {
        if (excepcion instanceof ErrorResponse respuesta) {
            HttpStatusCode estado = respuesta.getStatusCode();
            return ResponseEntity.status(estado)
                    .body(RespuestaApi.error("No se pudo procesar la solicitud.", "SOLICITUD_RECHAZADA"));
        }
        REGISTRO.error("Error no controlado en la API", excepcion);
        return ResponseEntity.internalServerError()
                .body(RespuestaApi.error("Ocurrió un error interno. Intenta nuevamente.", "ERROR_INTERNO"));
    }
}
