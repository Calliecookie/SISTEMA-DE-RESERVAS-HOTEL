package pe.edu.josepardo.hotel.servicio;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
public class GeneradorPdf {

    private static final String BASE_DOCUMENTO = "file:///hotel/";

    private final SpringTemplateEngine motor;

    public GeneradorPdf(SpringTemplateEngine motor) {
        this.motor = motor;
    }

    public byte[] generar(String plantilla, Map<String, Object> variables) {
        Context contexto = new Context(Locale.of("es", "PE"));
        contexto.setVariables(variables);
        String html = motor.process(plantilla, contexto);
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PdfRendererBuilder constructor = new PdfRendererBuilder();
            constructor.useFastMode();
            registrarFuente(constructor, "Inter-Regular.ttf", 400);
            registrarFuente(constructor, "Inter-SemiBold.ttf", 600);
            registrarFuente(constructor, "Inter-Bold.ttf", 700);
            constructor.withHtmlContent(html, BASE_DOCUMENTO);
            constructor.toStream(salida);
            constructor.run();
            return salida.toByteArray();
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo generar el PDF", excepcion);
        }
    }

    private void registrarFuente(PdfRendererBuilder constructor, String archivo, int peso) {
        constructor.useFont(
                () -> abrirFuente(archivo), "Inter", peso, BaseRendererBuilder.FontStyle.NORMAL, true);
    }

    private InputStream abrirFuente(String archivo) {
        InputStream entrada = GeneradorPdf.class.getResourceAsStream("/recursos/fuentes/" + archivo);
        if (entrada == null) {
            throw new IllegalStateException("Fuente no encontrada: " + archivo);
        }
        return entrada;
    }
}
