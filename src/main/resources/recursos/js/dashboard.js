import { api, errorCarga, esc, esqueletoIndicadores, esqueletoTabla, fechaHora, icono, moneda, ruta, textoAccion } from './nucleo.js';
import { graficoBarras } from './graficos.js';
import { activarFilas, cabeceraReservas, filaReserva } from './detalle-reserva.js';

const bento = document.querySelector('[data-bento]');
const indicadores = document.querySelector('[data-indicadores]');
const grafico = document.querySelector('[data-grafico]');
const actividad = document.querySelector('[data-actividad]');
const recientes = document.querySelector('[data-recientes]');
const DIAS = ['D', 'L', 'M', 'M', 'J', 'V', 'S'];
const ESTADOS = [
  ['DISPONIBLE', 'Disponibles'],
  ['RESERVADA', 'Reservadas'],
  ['OCUPADA', 'Ocupadas'],
  ['PENDIENTE', 'Con reserva pendiente'],
  ['MANTENIMIENTO', 'En mantenimiento'],
  ['FUERA_DE_SERVICIO', 'Fuera de servicio'],
];
let ultimaSerie = [];

const tarjeta = (nombreIcono, etiqueta, valor, enlace) => `
  <${enlace ? `a href="${ruta(enlace)}"` : 'div'} class="indicador ${enlace ? 'indicador--enlace' : ''}">
    <span class="indicador__etiqueta">${icono(nombreIcono)}${esc(etiqueta)}</span>
    <span class="indicador__valor">${esc(valor)}</span>
  </${enlace ? 'a' : 'div'}>`;

function variacion(actual, anterior) {
  const previo = Number(anterior);
  if (!previo) {
    return '';
  }
  const cambio = Math.round(((Number(actual) - previo) / previo) * 100);
  return `<span class="variacion ${cambio >= 0 ? 'variacion--sube' : 'variacion--baja'}">${cambio >= 0 ? '+' : ''}${cambio} % frente al mes anterior</span>`;
}

function pintarBento(datos) {
  const estados = datos.estadosHabitaciones || {};
  const totalEstados = ESTADOS.reduce((suma, [clave]) => suma + (estados[clave] || 0), 0) || 1;
  bento.innerHTML = `
    <a class="bento__celda bento__celda--5 bento__celda--destacada aparece" href="${ruta('/administracion/ventas')}" data-tarjeta="ventas">
      <span class="bento__etiqueta">${icono('recibo')}Ventas efectuadas</span>
      <p class="bento__valor">${datos.ventasEfectuadas}</p>
      <p class="bento__nota">${datos.ventasEfectuadasMes} este mes por ${moneda(datos.ingresosMes)}, ticket promedio ${moneda(datos.ticketPromedioMes)}</p>
      <p class="bento__nota">${variacion(datos.ingresosMes, datos.ingresosMesAnterior)}</p>
      <span class="bento__accion">Ver ventas y su detalle ${icono('flecha-der')}</span>
    </a>
    <div class="bento__celda bento__celda--4 aparece">
      <span class="bento__etiqueta">${icono('cama')}Ocupación de hoy</span>
      <div class="bento__fila">
        <div class="anillo" data-anillo><span class="anillo__valor">${datos.ocupacionHoy}%</span></div>
        <ul class="bento__lista">
          <li><span>Tarifa media</span><strong>${moneda(datos.tarifaPromedio)}</strong></li>
          <li><span>RevPAR</span><strong>${moneda(datos.revpar)}</strong></li>
          <li><span>Libres hoy</span><strong>${datos.habitacionesDisponibles} de ${datos.totalHabitaciones}</strong></li>
        </ul>
      </div>
    </div>
    <a class="bento__celda bento__celda--3 bento__celda--vidrio aparece" href="${ruta('/administracion/reportes')}" data-tarjeta="reportes">
      <span class="bento__etiqueta">${icono('grafico')}Reportes</span>
      <p class="bento__valor bento__valor--medio">9 reportes</p>
      <p class="bento__nota">Por fechas, por cliente, más solicitadas, estado y disponibilidad, ocupación y RevPAR, métodos de pago y más.</p>
      <span class="bento__accion">Abrir centro de reportes ${icono('flecha-der')}</span>
    </a>
    <div class="bento__celda bento__celda--7 aparece">
      <span class="bento__etiqueta">${icono('tendencia')}Pronóstico de ocupación, próximos 14 días</span>
      <div class="pronostico" data-pronostico>${datos.ocupacionProxima.map((dia) => {
        const objeto = new Date(`${dia.fecha}T12:00:00`);
        return `<div class="pronostico__dia" title="${dia.fecha.slice(8, 10)}/${dia.fecha.slice(5, 7)}: ${dia.porcentaje} %, ${dia.ocupadas} habitaciones">
          <div class="pronostico__pista"><div class="pronostico__barra" data-alto="${dia.porcentaje}"></div></div>
          <span>${DIAS[objeto.getDay()]}<br>${dia.fecha.slice(8, 10)}</span></div>`;
      }).join('')}</div>
    </div>
    <a class="bento__celda bento__celda--5 aparece" href="${ruta('/administracion/monitor')}">
      <span class="bento__etiqueta">${icono('mapa')}Estado de las habitaciones</span>
      <div class="estados-barra" data-estados>${ESTADOS.map(([clave]) => `<span class="estados-barra__parte tono--${clave}" data-peso="${(estados[clave] || 0) / totalEstados}"></span>`).join('')}</div>
      <ul class="bento__lista">${ESTADOS.filter(([clave]) => estados[clave]).map(([clave, etiqueta]) =>
        `<li><span class="tono--${clave}"><span class="punto"></span> ${etiqueta}</span><strong>${estados[clave]}</strong></li>`).join('')}</ul>
      <span class="bento__accion">Abrir mapa en vivo ${icono('flecha-der')}</span>
    </a>`;
  requestAnimationFrame(() => {
    bento.querySelector('[data-anillo]').style.setProperty('--avance', String(datos.ocupacionHoy));
    bento.querySelectorAll('[data-alto]').forEach((barra) => { barra.style.height = `${Math.max(Number(barra.dataset.alto), 2)}%`; });
    bento.querySelectorAll('[data-peso]').forEach((parte) => {
      parte.style.flexGrow = parte.dataset.peso;
      parte.style.background = 'var(--tono)';
    });
  });
}

function pintarGrafico() {
  if (ultimaSerie.length) {
    graficoBarras(grafico, ultimaSerie, moneda);
  } else {
    grafico.innerHTML = '<p class="texto-suave texto-sm">Aún no hay ingresos para mostrar.</p>';
  }
}

async function cargar() {
  bento.innerHTML = '<div class="bento__celda bento__celda--5"><div class="esqueleto esqueleto--bloque"></div></div><div class="bento__celda bento__celda--4"><div class="esqueleto esqueleto--bloque"></div></div><div class="bento__celda bento__celda--3"><div class="esqueleto esqueleto--bloque"></div></div>';
  indicadores.innerHTML = esqueletoIndicadores(6);
  recientes.innerHTML = esqueletoTabla(5, 6);
  grafico.innerHTML = '<div class="esqueleto esqueleto--grafico"></div>';
  try {
    const { datos } = await api('/api/administracion/dashboard');
    pintarBento(datos);
    indicadores.innerHTML = [
      tarjeta('reloj', 'Reservas pendientes', datos.reservasPendientes, '/administracion/reservas'),
      tarjeta('billete', 'Pagos por verificar', datos.pagosPorVerificar, '/trabajador'),
      tarjeta('ingreso', 'Llegadas pendientes', datos.llegadasHoy, '/trabajador'),
      tarjeta('salir', 'Salidas pendientes', datos.salidasHoy, '/trabajador'),
      tarjeta('calendario', 'Reservas confirmadas', datos.reservasConfirmadas, '/administracion/reservas'),
      tarjeta('billetera', 'Ingresos acumulados', moneda(datos.ingresosAcumulados), '/administracion/ventas'),
    ].join('');
    ultimaSerie = datos.ingresosPorMes;
    pintarGrafico();
    actividad.innerHTML = datos.actividadReciente.length
      ? datos.actividadReciente.map((item) => `<li><strong>${esc(textoAccion(item.accion))}</strong><br>
          <span class="texto-suave">${esc(item.usuario)}, ${fechaHora(item.creadoEn)}</span></li>`).join('')
      : '<li class="texto-suave">Aún no hay actividad registrada.</li>';
    recientes.innerHTML = `<div class="tabla-envoltura"><table class="tabla">${cabeceraReservas}<tbody>${datos.reservasRecientes.map((reserva) => filaReserva(reserva)).join('')}</tbody></table></div>`;
  } catch {
    bento.innerHTML = '';
    indicadores.innerHTML = '';
    recientes.innerHTML = errorCarga();
    grafico.innerHTML = '';
  }
}

recientes.addEventListener('click', (evento) => {
  if (evento.target.closest('[data-reintentar]')) {
    cargar();
  }
});
activarFilas(recientes, cargar);
cargar();
