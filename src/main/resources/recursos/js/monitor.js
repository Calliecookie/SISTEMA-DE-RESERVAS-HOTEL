import { api, errorCarga, esc, fecha, fechaLocal, icono, mostrarError } from './nucleo.js';
import { abrirDetalleReserva } from './detalle-reserva.js';

const mapa = document.querySelector('[data-mapa]');
const leyenda = document.querySelector('[data-leyenda]');
const actualizado = document.querySelector('[data-actualizado]');
const plan = document.querySelector('[data-plan]');
const selectorDias = document.querySelector('[data-plan-dias]');
const INTERVALO = 30000;
const ORDEN = ['DISPONIBLE', 'RESERVADA', 'OCUPADA', 'PENDIENTE', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO', 'INACTIVA'];
const ETIQUETAS = {
  DISPONIBLE: 'Disponibles',
  RESERVADA: 'Reservadas',
  OCUPADA: 'Ocupadas',
  PENDIENTE: 'Reserva pendiente',
  MANTENIMIENTO: 'En mantenimiento',
  FUERA_DE_SERVICIO: 'Fuera de servicio',
  INACTIVA: 'Desactivadas',
};
const DIAS = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb'];
const resaltada = new URLSearchParams(location.search).get('habitacion');
let filtro = null;
let datosMapa = null;
let desdePlan = fechaLocal();

const sumarDias = (iso, dias) => {
  const base = new Date(`${iso}T12:00:00`);
  base.setDate(base.getDate() + dias);
  return `${base.getFullYear()}-${String(base.getMonth() + 1).padStart(2, '0')}-${String(base.getDate()).padStart(2, '0')}`;
};

function pintarLeyenda() {
  leyenda.innerHTML = ORDEN.filter((clave) => datosMapa.resumen[clave] > 0 || ['DISPONIBLE', 'RESERVADA', 'OCUPADA'].includes(clave))
    .map((clave) => `<button class="leyenda__item tono--${clave}" type="button" data-filtro="${clave}" aria-pressed="${filtro === clave}">
      <span class="punto"></span>${esc(ETIQUETAS[clave])} <strong>${datosMapa.resumen[clave] || 0}</strong></button>`).join('');
}

function puerta(habitacion) {
  const atenuada = (filtro && habitacion.situacion !== filtro) || (resaltada && !filtro && habitacion.numero !== resaltada);
  const aviso = habitacion.llegaHoy ? 'Llega hoy' : habitacion.saleHoy ? 'Sale hoy' : '';
  const detalle = habitacion.huesped
    ? `<span class="puerta-hab__huesped">${esc(habitacion.huesped)}</span><span class="puerta-hab__categoria">${fecha(habitacion.fechaEntrada)} al ${fecha(habitacion.fechaSalida)}</span>`
    : `<span class="puerta-hab__categoria">${esc(habitacion.categoria)}, ${habitacion.capacidad} ${habitacion.capacidad === 1 ? 'huésped' : 'huéspedes'}</span>`;
  return `<button class="puerta-hab tono--${habitacion.situacion} ${atenuada ? 'puerta-hab--atenuada' : ''}" type="button"
      ${habitacion.codigoReserva ? `data-codigo="${esc(habitacion.codigoReserva)}"` : ''}
      aria-label="Habitación ${esc(habitacion.numero)}, ${esc(habitacion.situacionEtiqueta)}${habitacion.huesped ? `, ${esc(habitacion.huesped)}` : ''}">
      ${aviso ? `<span class="puerta-hab__aviso">${aviso}</span>` : ''}
      <span class="puerta-hab__numero">${esc(habitacion.numero)}</span>
      <span class="puerta-hab__estado"><span class="punto"></span>${esc(habitacion.situacionEtiqueta)}</span>
      ${detalle}
    </button>`;
}

function pintarMapa() {
  pintarLeyenda();
  mapa.innerHTML = datosMapa.pisos.map((piso) => `
    <div class="piso"><h3 class="piso__titulo">${esc(piso.piso)}</h3>
      <div class="puertas">${piso.habitaciones.map(puerta).join('')}</div></div>`).join('');
  actualizado.textContent = `Ocupación ${datosMapa.ocupacionPorcentaje} %, actualizado ${datosMapa.actualizadoEn.slice(11, 16)}`;
}

async function cargarMapa(silencioso = false) {
  if (!silencioso) {
    mapa.innerHTML = '<div class="bloque__cuerpo"><div class="esqueleto esqueleto--bloque"></div><div class="esqueleto esqueleto--bloque"></div></div>';
  }
  try {
    datosMapa = (await api('/api/operaciones/mapa')).datos;
    pintarMapa();
  } catch (error) {
    if (!silencioso) {
      mapa.innerHTML = errorCarga();
      actualizado.textContent = 'Sin conexión';
    }
  }
}

function pintarPlan(datos) {
  const hoy = fechaLocal();
  const rejilla = document.createElement('div');
  rejilla.className = 'plan__rejilla';
  rejilla.style.gridTemplateColumns = `11rem repeat(${datos.dias.length}, minmax(2.6rem, 1fr))`;
  const agregar = (clase, columna, fila, html, extra = {}) => {
    const celda = document.createElement(extra.etiqueta || 'div');
    celda.className = clase;
    celda.style.gridColumn = columna;
    celda.style.gridRow = String(fila);
    celda.innerHTML = html;
    Object.entries(extra.datos || {}).forEach(([clave, valor]) => { celda.dataset[clave] = valor; });
    if (extra.titulo) {
      celda.title = extra.titulo;
    }
    if (extra.etiqueta === 'button') {
      celda.type = 'button';
    }
    rejilla.append(celda);
  };
  agregar('plan__habitacion plan__celda--cabecera', '1', 1, 'Habitación');
  datos.dias.forEach((dia, indice) => {
    const objeto = new Date(`${dia}T12:00:00`);
    const finde = objeto.getDay() === 0 || objeto.getDay() === 6;
    agregar(`plan__celda plan__celda--cabecera ${dia === hoy ? 'plan__celda--hoy' : ''} ${finde ? 'plan__celda--finde' : ''}`,
      String(indice + 2), 1, `${DIAS[objeto.getDay()]}<br>${dia.slice(8, 10)}/${dia.slice(5, 7)}`);
  });
  datos.habitaciones.forEach((habitacion, indiceFila) => {
    const fila = indiceFila + 2;
    agregar(`plan__habitacion tono--${habitacion.estado}`, '1', fila,
      `<span class="punto"></span><strong>${esc(habitacion.numero)}</strong> ${esc(habitacion.categoria)}`, { titulo: habitacion.nombre });
    datos.dias.forEach((dia, indice) => {
      const objeto = new Date(`${dia}T12:00:00`);
      const finde = objeto.getDay() === 0 || objeto.getDay() === 6;
      agregar(`plan__celda ${dia === hoy ? 'plan__celda--hoy' : ''} ${finde ? 'plan__celda--finde' : ''}`, String(indice + 2), fila, '');
    });
    habitacion.tramos.forEach((tramo) => {
      agregar(`plan__tramo tono--${tramo.estado} ${tramo.continuaAntes ? 'plan__tramo--antes' : ''} ${tramo.continuaDespues ? 'plan__tramo--despues' : ''}`,
        `${tramo.inicio + 2} / span ${tramo.noches}`, fila, esc(tramo.cliente), {
          etiqueta: 'button',
          datos: { codigo: tramo.codigo },
          titulo: `${tramo.codigo}: ${tramo.cliente}, ${tramo.estadoEtiqueta}, del ${fecha(tramo.entrada)} al ${fecha(tramo.salida)}`,
        });
    });
  });
  const filaOcupacion = datos.habitaciones.length + 2;
  agregar('plan__habitacion plan__celda--cabecera', '1', filaOcupacion, 'Ocupación');
  datos.ocupacion.forEach((dia, indice) => {
    agregar('plan__celda plan__ocupacion', String(indice + 2), filaOcupacion, `${dia.porcentaje}%`,
      { titulo: `${dia.ocupadas} habitaciones con reserva el ${fecha(dia.fecha)}` });
  });
  plan.replaceChildren(rejilla);
}

async function cargarPlan(silencioso = false) {
  if (!silencioso) {
    plan.innerHTML = '<div class="bloque__cuerpo"><div class="esqueleto esqueleto--grafico"></div></div>';
  }
  try {
    const { datos } = await api(`/api/operaciones/planificador?desde=${desdePlan}&dias=${selectorDias.value}`);
    pintarPlan(datos);
  } catch (error) {
    if (!silencioso) {
      plan.innerHTML = errorCarga();
      mostrarError(error);
    }
  }
}

const refrescar = (silencioso) => Promise.all([cargarMapa(silencioso), cargarPlan(silencioso)]);

leyenda.addEventListener('click', (evento) => {
  const boton = evento.target.closest('[data-filtro]');
  if (boton) {
    filtro = filtro === boton.dataset.filtro ? null : boton.dataset.filtro;
    pintarMapa();
  }
});

document.querySelector('.panel__seccion').addEventListener('click', (evento) => {
  const elemento = evento.target.closest('[data-codigo]');
  if (elemento) {
    abrirDetalleReserva(elemento.dataset.codigo, { alCambiar: () => refrescar(true) });
  }
  if (evento.target.closest('[data-reintentar]')) {
    refrescar(false);
  }
});

document.querySelectorAll('[data-plan-mover]').forEach((boton) => boton.addEventListener('click', () => {
  desdePlan = sumarDias(desdePlan, Number(boton.dataset.planMover));
  cargarPlan();
}));
document.querySelector('[data-plan-hoy]').addEventListener('click', () => {
  desdePlan = fechaLocal();
  cargarPlan();
});
selectorDias.addEventListener('change', () => cargarPlan());

setInterval(() => {
  if (!document.hidden && !document.querySelector('dialog[open]')) {
    refrescar(true);
  }
}, INTERVALO);
document.addEventListener('visibilitychange', () => {
  if (!document.hidden) {
    refrescar(true);
  }
});

refrescar(false);
