package pe.edu.josepardo.hotel.dto;

import java.util.List;

public record Pagina<T>(
        List<T> elementos,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas) {

    public static <T> Pagina<T> de(List<T> elementos, int pagina, int tamano, long total) {
        int paginas = tamano <= 0 ? 0 : (int) Math.ceil((double) total / tamano);
        return new Pagina<>(elementos, pagina, tamano, total, paginas);
    }
}
