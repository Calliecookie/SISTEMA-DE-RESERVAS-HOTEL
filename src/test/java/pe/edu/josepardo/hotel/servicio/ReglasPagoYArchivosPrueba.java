package pe.edu.josepardo.hotel.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ReglasPagoYArchivosPrueba {

    @Test
    void convierteSolesADolaresConDosDecimales() {
        assertEquals(new BigDecimal("207.68"), ServicioPago.convertir(new BigDecimal("778.80"), new BigDecimal("3.75")));
    }

    @Test
    void detectaImagenesPorSuContenido() {
        assertEquals("jpg", ServicioAlmacenamiento.detectarExtension(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}, 4));
        assertEquals("png", ServicioAlmacenamiento.detectarExtension(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, 8));
        assertEquals("webp", ServicioAlmacenamiento.detectarExtension("RIFF0000WEBP".getBytes(), 12));
    }

    @Test
    void rechazaArchivosDisfrazadosDeImagen() {
        assertNull(ServicioAlmacenamiento.detectarExtension("<script>alert(1)</script>".getBytes(), 12));
        assertNull(ServicioAlmacenamiento.detectarExtension("GIF89a000000".getBytes(), 12));
    }
}
