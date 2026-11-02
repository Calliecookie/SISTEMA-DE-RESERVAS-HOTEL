package pe.edu.josepardo.hotel.dto;

import java.util.List;

public record TablaReporte(
        String clave,
        String titulo,
        String subtitulo,
        List<ColumnaReporte> columnas,
        List<List<Object>> filas,
        List<IndicadorReporte> indicadores,
        List<PuntoSerie> serie,
        String tipoGrafico) {
}
