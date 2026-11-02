package pe.edu.josepardo.hotel.servicio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import pe.edu.josepardo.hotel.dto.ColumnaReporte;
import pe.edu.josepardo.hotel.dto.IndicadorReporte;
import pe.edu.josepardo.hotel.dto.TablaReporte;

@Component
public class ExportadorReportes {

    private static final String TIPO_HISTORIAL = "historial";

    private final GeneradorPdf generadorPdf;
    private final ServicioConfiguracion configuracion;

    public ExportadorReportes(GeneradorPdf generadorPdf, ServicioConfiguracion configuracion) {
        this.generadorPdf = generadorPdf;
        this.configuracion = configuracion;
    }

    public byte[] aPdf(TablaReporte reporte) {
        List<Integer> indices = columnasVisibles(reporte);
        List<Map<String, String>> columnas = new ArrayList<>();
        for (int indice : indices) {
            ColumnaReporte columna = reporte.columnas().get(indice);
            columnas.add(Map.of("titulo", columna.titulo(), "alineacion", esNumerica(columna) ? "derecha" : "izquierda"));
        }
        List<List<Map<String, String>>> filas = new ArrayList<>();
        for (List<Object> fila : reporte.filas()) {
            List<Map<String, String>> celdas = new ArrayList<>();
            for (int indice : indices) {
                ColumnaReporte columna = reporte.columnas().get(indice);
                celdas.add(Map.of(
                        "valor", formatear(fila.get(indice), columna.tipo()),
                        "alineacion", esNumerica(columna) ? "derecha" : "izquierda"));
            }
            filas.add(celdas);
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("hotel", configuracion.hotel());
        variables.put("reporte", reporte);
        variables.put("columnas", columnas);
        variables.put("filas", filas);
        variables.put("emision", Formato.fechaHora(LocalDateTime.now()));
        return generadorPdf.generar("documentos/reporte", variables);
    }

    public byte[] aExcel(TablaReporte reporte) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Reporte");
            Font fuenteTitulo = libro.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            CellStyle estiloTitulo = libro.createCellStyle();
            estiloTitulo.setFont(fuenteTitulo);
            Font fuenteCabecera = libro.createFont();
            fuenteCabecera.setBold(true);
            CellStyle estiloCabecera = libro.createCellStyle();
            estiloCabecera.setFont(fuenteCabecera);
            estiloCabecera.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            estiloCabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            DataFormat formatos = libro.createDataFormat();
            CellStyle estiloMoneda = libro.createCellStyle();
            estiloMoneda.setDataFormat(formatos.getFormat("\"S/\" #,##0.00"));
            CellStyle estiloPorcentaje = libro.createCellStyle();
            estiloPorcentaje.setDataFormat(formatos.getFormat("0.00\" %\""));

            int numeroFila = 0;
            Row titulo = hoja.createRow(numeroFila++);
            titulo.createCell(0).setCellValue(reporte.titulo());
            titulo.getCell(0).setCellStyle(estiloTitulo);
            hoja.createRow(numeroFila++).createCell(0).setCellValue(reporte.subtitulo());
            for (IndicadorReporte indicador : reporte.indicadores()) {
                Row fila = hoja.createRow(numeroFila++);
                fila.createCell(0).setCellValue(indicador.etiqueta());
                fila.createCell(1).setCellValue(indicador.valor());
            }
            numeroFila++;

            List<Integer> indices = columnasVisibles(reporte);
            Row cabecera = hoja.createRow(numeroFila++);
            for (int i = 0; i < indices.size(); i++) {
                cabecera.createCell(i).setCellValue(reporte.columnas().get(indices.get(i)).titulo());
                cabecera.getCell(i).setCellStyle(estiloCabecera);
            }
            for (List<Object> datos : reporte.filas()) {
                Row fila = hoja.createRow(numeroFila++);
                for (int i = 0; i < indices.size(); i++) {
                    int indice = indices.get(i);
                    Object valor = datos.get(indice);
                    String tipo = reporte.columnas().get(indice).tipo();
                    var celda = fila.createCell(i);
                    if (valor instanceof Number numero) {
                        celda.setCellValue(numero.doubleValue());
                        if ("moneda".equals(tipo)) {
                            celda.setCellStyle(estiloMoneda);
                        } else if ("porcentaje".equals(tipo)) {
                            celda.setCellStyle(estiloPorcentaje);
                        }
                    } else {
                        celda.setCellValue(String.valueOf(valor));
                    }
                }
            }
            for (int i = 0; i < indices.size(); i++) {
                hoja.setColumnWidth(i, 22 * 256);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo generar el Excel", excepcion);
        }
    }

    private List<Integer> columnasVisibles(TablaReporte reporte) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < reporte.columnas().size(); i++) {
            if (!TIPO_HISTORIAL.equals(reporte.columnas().get(i).tipo())) {
                indices.add(i);
            }
        }
        return indices;
    }

    private boolean esNumerica(ColumnaReporte columna) {
        return !"texto".equals(columna.tipo());
    }

    private String formatear(Object valor, String tipo) {
        if (valor instanceof BigDecimal numero) {
            return "porcentaje".equals(tipo) ? Formato.porcentaje(numero) : Formato.moneda(numero);
        }
        return String.valueOf(valor);
    }
}
