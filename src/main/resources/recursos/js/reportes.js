import { api, conCarga, consulta, datosFormulario, debounce, esc, errorCarga, esqueletoIndicadores, esqueletoTabla, fechaLocal, icono, moneda, mostrarError, ruta, vacio } from './nucleo.js';
import { graficoBarras, graficoHorizontal, graficoLinea, graficoSegmentado } from './graficos.js';

const REPORTES = [
  ['ventas-fechas', 'Ventas por fechas'],
  ['ventas-cliente', 'Ventas por cliente'],
  ['habitaciones-solicitadas', 'Habitaciones más solicitadas'],
  ['disponibilidad', 'Estado y disponibilidad'],
  ['ventas-categoria', 'Ventas por categoría'],
  ['ingresos-generales', 'Ingresos generales'],
  ['ocupacion', 'Ocupación y RevPAR'],
  ['metodos-pago', 'Métodos de pago'],
  ['anticipacion', 'Anticipación'],
];

const pestanas = document.querySelector('[data-pestanas]');
const filtros = document.querySelector('[data-filtros]');
const indicadores = document.querySelector('[data-indicadores]');
const grafico = document.querySelector('[data-grafico]');
const seccionGrafico = document.querySelector('[data-seccion-grafico]');
const tabla = document.querySelector('[data-tabla]');
const tituloTabla = document.querySelector('[data-titulo-tabla]');
const tituloGrafico = document.querySelector('[data-titulo-grafico]');
const acciones = document.querySelector('[data-acciones-cabecera]');
let actual = 'ventas-fechas';
let clienteId = null;

function pintarPestanas() {
  pestanas.innerHTML = REPORTES.map(([clave, nombre]) =>
    `<button class="pestana ${clave === actual ? 'pestana--actual' : ''}" type="button" role="tab" aria-selected="${clave === actual}" data-reporte="${clave}">${esc(nombre)}</button>`).join('');
}

function pintarFiltros() {
  const unaFecha = actual === 'disponibilidad';
  filtros.innerHTML = `
    ${unaFecha ? '' : `<label class="campo"><span class="campo__etiqueta">Fecha inicial</span><input class="entrada" type="date" name="desde" value="${fechaLocal(-180)}"></label>`}
    <label class="campo"><span class="campo__etiqueta">${unaFecha ? 'Fecha' : 'Fecha final'}</span><input class="entrada" type="date" name="hasta" value="${fechaLocal()}"></label>
    ${actual === 'ventas-cliente' ? '<label class="campo"><span class="campo__etiqueta">Cliente</span><input class="entrada" type="search" name="busqueda" placeholder="Nombre, DNI o correo" maxlength="60"></label>' : ''}`;
}

const parametros = () => consulta({ ...datosFormulario(filtros), clienteId });

function formatearCelda(valor, tipo) {
  if (tipo === 'moneda') {
    return moneda(valor);
  }
  if (tipo === 'porcentaje') {
    return `${Number(valor).toFixed(2)} %`;
  }
  if (tipo === 'historial') {
    return `<button class="boton boton--secundario boton--compacto" type="button" data-historial="${esc(valor)}">Ver historial</button>`;
  }
  return esc(valor);
}

function pintarGrafico(reporte) {
  const hay = reporte.serie.length > 0 && reporte.serie.some((punto) => Number(punto.valor) > 0);
  seccionGrafico.classList.toggle('oculto', reporte.tipoGrafico === 'ninguno');
  tituloGrafico.textContent = reporte.titulo;
  if (!hay) {
    grafico.innerHTML = '<p class="texto-suave texto-sm">No hay datos para graficar en este periodo.</p>';
    return;
  }
  const esConteo = ['habitaciones-solicitadas', 'anticipacion'].includes(reporte.clave);
  const esPorcentaje = reporte.clave === 'ocupacion';
  const formato = esPorcentaje ? (valor) => `${Number(valor)} %` : esConteo ? (valor) => `${Number(valor)}` : moneda;
  if (reporte.tipoGrafico === 'linea') {
    graficoLinea(grafico, reporte.serie, formato);
  } else if (reporte.tipoGrafico === 'barras') {
    graficoBarras(grafico, reporte.serie, formato);
  } else if (reporte.tipoGrafico === 'segmentada') {
    graficoSegmentado(grafico, reporte.serie);
  } else {
    graficoHorizontal(grafico, reporte.serie, formato);
  }
}

function pintarTabla(reporte) {
  tituloTabla.textContent = reporte.subtitulo;
  tabla.innerHTML = reporte.filas.length ? `<table class="tabla">
    <thead><tr>${reporte.columnas.map((columna) => `<th class="${columna.tipo === 'texto' ? '' : 'derecha'}">${esc(columna.titulo)}</th>`).join('')}</tr></thead>
    <tbody>${reporte.filas.map((fila) => `<tr>${fila.map((celda, indice) => {
      const columna = reporte.columnas[indice];
      return `<td class="${columna.tipo === 'texto' ? '' : 'derecha'}">${formatearCelda(celda, columna.tipo)}</td>`;
    }).join('')}</tr>`).join('')}</tbody></table>`
    : vacio({ nombreIcono: 'grafico', titulo: 'Sin datos en este periodo', texto: 'Amplía el rango de fechas para ver resultados.' });
}

async function cargar() {
  indicadores.innerHTML = esqueletoIndicadores(3);
  tabla.innerHTML = esqueletoTabla(8, 5);
  grafico.innerHTML = '<div class="esqueleto esqueleto--grafico"></div>';
  try {
    const { datos } = await api(`/api/administracion/reportes/${actual}${parametros()}`);
    indicadores.innerHTML = datos.indicadores.map((indicador) =>
      `<div class="indicador"><span class="indicador__etiqueta">${esc(indicador.etiqueta)}</span><span class="indicador__valor">${esc(indicador.valor)}</span></div>`).join('');
    pintarGrafico(datos);
    pintarTabla(datos);
  } catch (error) {
    indicadores.innerHTML = '';
    grafico.innerHTML = '';
    if (error.estado === 400) {
      mostrarError(error);
      tabla.innerHTML = vacio({ nombreIcono: 'calendario', titulo: 'Revisa las fechas', texto: error.message });
    } else {
      tabla.innerHTML = errorCarga();
    }
  }
}

async function exportar(boton, formato) {
  await conCarga(boton, async () => {
    try {
      const url = `/api/administracion/reportes/${actual}/exportar${consulta({ ...datosFormulario(filtros), clienteId, formato })}`;
      const respuesta = await fetch(ruta(url), { credentials: 'same-origin', headers: { 'X-Requested-With': 'fetch' } });
      if (!respuesta.ok) {
        const cuerpo = await respuesta.json().catch(() => null);
        throw new Error(cuerpo?.mensaje || 'No se pudo generar el archivo.');
      }
      const enlace = document.createElement('a');
      enlace.href = URL.createObjectURL(await respuesta.blob());
      enlace.download = `reporte-${actual}.${formato}`;
      enlace.click();
      URL.revokeObjectURL(enlace.href);
    } catch (error) {
      mostrarError(error);
    }
  });
}

pestanas.addEventListener('click', (evento) => {
  const boton = evento.target.closest('[data-reporte]');
  if (boton) {
    actual = boton.dataset.reporte;
    clienteId = null;
    pintarPestanas();
    pintarFiltros();
    cargar();
  }
});

tabla.addEventListener('click', (evento) => {
  const historial = evento.target.closest('[data-historial]');
  if (historial) {
    clienteId = historial.dataset.historial;
    cargar();
  }
  if (evento.target.closest('[data-reintentar]')) {
    cargar();
  }
});

filtros.addEventListener('input', debounce(() => {
  clienteId = null;
  cargar();
}, 350));
filtros.addEventListener('submit', (evento) => evento.preventDefault());

acciones.classList.add('no-imprimir');
acciones.innerHTML = `<div class="campo-grupo">
  <button class="boton boton--secundario boton--compacto" type="button" data-imprimir>${icono('imprimir')}Imprimir</button>
  <button class="boton boton--secundario boton--compacto" type="button" data-exportar="pdf">${icono('documento')}PDF</button>
  <button class="boton boton--secundario boton--compacto" type="button" data-exportar="xlsx">${icono('excel')}Excel</button></div>`;
acciones.querySelector('[data-imprimir]').addEventListener('click', () => window.print());
acciones.querySelectorAll('[data-exportar]').forEach((boton) => boton.addEventListener('click', () => exportar(boton, boton.dataset.exportar)));

pintarPestanas();
pintarFiltros();
cargar();
