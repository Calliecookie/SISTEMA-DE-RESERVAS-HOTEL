import { esc } from './nucleo.js';

const NS = 'http://www.w3.org/2000/svg';
const ALTO = 260;
const MARGEN = { arriba: 12, derecha: 12, abajo: 28, izquierda: 56 };

const crear = (nombre, atributos = {}) => {
  const elemento = document.createElementNS(NS, nombre);
  Object.entries(atributos).forEach(([clave, valor]) => elemento.setAttribute(clave, valor));
  return elemento;
};

function escalaAgradable(maximo) {
  if (maximo <= 0) {
    return 1;
  }
  const magnitud = 10 ** Math.floor(Math.log10(maximo));
  const fraccion = maximo / magnitud;
  const pasos = [1, 2, 2.5, 5, 10];
  return pasos.find((paso) => paso >= fraccion) * magnitud;
}

function preparar(contenedor, serie, descripcion) {
  contenedor.innerHTML = '';
  contenedor.classList.add('grafico');
  const ancho = Math.max(contenedor.clientWidth || 640, 280);
  const svg = crear('svg', { viewBox: `0 0 ${ancho} ${ALTO}`, width: ancho, height: ALTO, role: 'img', 'aria-label': descripcion });
  const tope = escalaAgradable(Math.max(...serie.map((punto) => Number(punto.valor)), 0));
  const interiorAlto = ALTO - MARGEN.arriba - MARGEN.abajo;
  const y = (valor) => MARGEN.arriba + interiorAlto * (1 - valor / tope);
  for (let linea = 0; linea <= 4; linea += 1) {
    const valor = (tope / 4) * linea;
    svg.append(crear('line', { class: linea === 0 ? 'grafico__base' : 'grafico__reja', x1: MARGEN.izquierda, x2: ancho - MARGEN.derecha, y1: y(valor), y2: y(valor) }));
    const etiqueta = crear('text', { class: 'grafico__eje', x: MARGEN.izquierda - 8, y: y(valor) + 4, 'text-anchor': 'end' });
    etiqueta.textContent = valor >= 1000 ? `${(valor / 1000).toLocaleString('es-PE', { maximumFractionDigits: 1 })} mil` : valor.toLocaleString('es-PE', { maximumFractionDigits: 1 });
    svg.append(etiqueta);
  }
  const tooltip = document.createElement('div');
  tooltip.className = 'grafico__tooltip oculto';
  contenedor.append(svg, tooltip);
  return { svg, ancho, y, tooltip };
}

function etiquetarEjeX(svg, serie, posicion, ancho) {
  const maximo = Math.max(1, Math.floor((ancho - MARGEN.izquierda) / 72));
  const salto = Math.ceil(serie.length / maximo);
  serie.forEach((punto, indice) => {
    if (indice % salto === 0) {
      const texto = crear('text', { class: 'grafico__eje', x: posicion(indice), y: ALTO - 8, 'text-anchor': 'middle' });
      texto.textContent = punto.etiqueta;
      svg.append(texto);
    }
  });
}

function mostrarTooltip(tooltip, x, y, texto) {
  tooltip.textContent = texto;
  tooltip.classList.remove('oculto');
  tooltip.style.left = `${x}px`;
  tooltip.style.top = `${y}px`;
}

export function graficoLinea(contenedor, serie, formato) {
  const { svg, ancho, y, tooltip } = preparar(contenedor, serie, `Evolución con ${serie.length} puntos`);
  const interior = ancho - MARGEN.izquierda - MARGEN.derecha;
  const x = (indice) => MARGEN.izquierda + (serie.length === 1 ? interior / 2 : (interior * indice) / (serie.length - 1));
  const puntos = serie.map((punto, indice) => `${x(indice)},${y(Number(punto.valor))}`).join(' ');
  if (serie.length > 1) {
    const base = y(0);
    svg.append(crear('polygon', { class: 'grafico__area', points: `${x(0)},${base} ${puntos} ${x(serie.length - 1)},${base}` }));
    svg.append(crear('polyline', { class: 'grafico__linea', points: puntos }));
  }
  etiquetarEjeX(svg, serie, x, ancho);
  const guia = crear('line', { class: 'grafico__guia oculto', y1: MARGEN.arriba, y2: y(0) });
  const marca = crear('circle', { class: 'grafico__punto', r: 4.5, cx: x(0), cy: y(Number(serie[0].valor)) });
  svg.append(guia, marca);
  if (serie.length > 1) {
    marca.classList.add('oculto');
  }
  svg.addEventListener('pointermove', (evento) => {
    const caja = svg.getBoundingClientRect();
    const relativo = ((evento.clientX - caja.left) * ancho) / caja.width;
    const indice = Math.round(((relativo - MARGEN.izquierda) / interior) * (serie.length - 1));
    const limitado = Math.min(serie.length - 1, Math.max(0, indice));
    const punto = serie[limitado];
    const px = x(limitado);
    const py = y(Number(punto.valor));
    guia.setAttribute('x1', px);
    guia.setAttribute('x2', px);
    guia.classList.remove('oculto');
    marca.setAttribute('cx', px);
    marca.setAttribute('cy', py);
    marca.classList.remove('oculto');
    mostrarTooltip(tooltip, px, py, `${punto.etiqueta}: ${formato(punto.valor)}`);
  });
  svg.addEventListener('pointerleave', () => {
    guia.classList.add('oculto');
    tooltip.classList.add('oculto');
    if (serie.length > 1) {
      marca.classList.add('oculto');
    }
  });
}

export function graficoBarras(contenedor, serie, formato) {
  const { svg, ancho, y, tooltip } = preparar(contenedor, serie, `Barras con ${serie.length} periodos`);
  const interior = ancho - MARGEN.izquierda - MARGEN.derecha;
  const banda = interior / serie.length;
  const grosor = Math.min(24, Math.max(4, banda - 2));
  const x = (indice) => MARGEN.izquierda + banda * indice + banda / 2;
  serie.forEach((punto, indice) => {
    const tope = y(Number(punto.valor));
    const alto = Math.max(y(0) - tope, 1);
    const radio = Math.min(4, alto, grosor / 2);
    const izquierda = x(indice) - grosor / 2;
    const derecha = izquierda + grosor;
    const barra = crear('path', {
      class: 'grafico__barra',
      d: `M${izquierda},${y(0)} V${tope + radio} Q${izquierda},${tope} ${izquierda + radio},${tope} H${derecha - radio} Q${derecha},${tope} ${derecha},${tope + radio} V${y(0)} Z`,
      tabindex: 0,
    });
    const mostrar = () => mostrarTooltip(tooltip, x(indice), tope, `${punto.etiqueta}: ${formato(punto.valor)}`);
    barra.addEventListener('pointerenter', mostrar);
    barra.addEventListener('focus', mostrar);
    barra.addEventListener('pointerleave', () => tooltip.classList.add('oculto'));
    barra.addEventListener('blur', () => tooltip.classList.add('oculto'));
    svg.append(barra);
  });
  etiquetarEjeX(svg, serie, x, ancho);
}

export function graficoHorizontal(contenedor, serie, formato) {
  const maximo = Math.max(...serie.map((punto) => Number(punto.valor)), 1);
  contenedor.innerHTML = `<div class="grafico-horizontal">${serie.map((punto) => `
    <div class="grafico-horizontal__fila">
      <span class="grafico-horizontal__nombre" title="${esc(punto.etiqueta)}">${esc(punto.etiqueta)}</span>
      <span class="grafico-horizontal__pista"><span class="grafico-horizontal__barra" data-ancho="${(Number(punto.valor) / maximo) * 100}"></span></span>
      <span class="numero">${formato(punto.valor)}</span>
    </div>`).join('')}</div>`;
  contenedor.querySelectorAll('[data-ancho]').forEach((barra) => {
    barra.style.display = 'block';
    barra.style.width = `${barra.dataset.ancho}%`;
  });
}

export function graficoSegmentado(contenedor, serie) {
  const total = serie.reduce((suma, punto) => suma + Number(punto.valor), 0) || 1;
  contenedor.innerHTML = `
    <div class="grafico-segmentado" role="img" aria-label="${esc(serie.map((punto) => `${punto.etiqueta}: ${Number(punto.valor)}`).join(', '))}">
      ${serie.map((punto, indice) => {
        const valor = Number(punto.valor);
        const parte = (valor / total) * 100;
        return valor > 0 ? `<div class="grafico-segmentado__parte grafico-segmentado__parte--${indice + 1}" data-ancho="${parte}" title="${esc(punto.etiqueta)}: ${valor}">${parte >= 10 ? `${Math.round(parte)} %` : ''}</div>` : '';
      }).join('')}
    </div>
    <div class="leyenda">${serie.map((punto, indice) => `<span class="leyenda__item"><span class="leyenda__muestra leyenda__muestra--${indice + 1}"></span>${esc(punto.etiqueta)}</span>`).join('')}</div>`;
  contenedor.querySelectorAll('[data-ancho]').forEach((parte) => {
    parte.style.flex = `${parte.dataset.ancho} 1 0`;
  });
}
