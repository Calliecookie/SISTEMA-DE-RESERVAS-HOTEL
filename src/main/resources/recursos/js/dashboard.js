import { api, errorCarga, esc, esqueletoIndicadores, esqueletoTabla, fechaHora, icono, moneda, ruta, textoAccion } from './nucleo.js';
import { graficoBarras } from './graficos.js';
import { activarFilas, cabeceraReservas, filaReserva } from './detalle-reserva.js';

const indicadores = document.querySelector('[data-indicadores]');
const grafico = document.querySelector('[data-grafico]');
const actividad = document.querySelector('[data-actividad]');
const recientes = document.querySelector('[data-recientes]');

const tarjeta = (nombreIcono, etiqueta, valor, enlace) => `
  <${enlace ? `a href="${ruta(enlace)}"` : 'div'} class="indicador ${enlace ? 'indicador--enlace' : ''}">
    <span class="indicador__etiqueta">${icono(nombreIcono)}${esc(etiqueta)}</span>
    <span class="indicador__valor">${esc(valor)}</span>
  </${enlace ? 'a' : 'div'}>`;

async function cargar() {
  indicadores.innerHTML = esqueletoIndicadores(6);
  recientes.innerHTML = esqueletoTabla(5, 6);
  grafico.innerHTML = '<div class="esqueleto esqueleto--grafico"></div>';
  try {
    const { datos } = await api('/api/administracion/dashboard');
    indicadores.innerHTML = [
      tarjeta('cama', 'Total de habitaciones', datos.totalHabitaciones, '/administracion/habitaciones'),
      tarjeta('check-circulo', 'Disponibles hoy', datos.habitacionesDisponibles),
      tarjeta('usuarios', 'Ocupadas', datos.habitacionesOcupadas),
      tarjeta('reloj', 'Reservas pendientes', datos.reservasPendientes, '/administracion/reservas'),
      tarjeta('calendario', 'Reservas confirmadas', datos.reservasConfirmadas, '/administracion/reservas'),
      tarjeta('recibo', 'Ingresos acumulados', moneda(datos.ingresosAcumulados), '/administracion/ventas'),
      tarjeta('billetera', 'Ingresos del mes', moneda(datos.ingresosMes), '/administracion/ventas'),
    ].join('');
    if (datos.ingresosPorMes.length) {
      graficoBarras(grafico, datos.ingresosPorMes, moneda);
    } else {
      grafico.innerHTML = '<p class="texto-suave texto-sm">Aún no hay ingresos para mostrar.</p>';
    }
    actividad.innerHTML = datos.actividadReciente.length
      ? datos.actividadReciente.map((item) => `<li><strong>${esc(textoAccion(item.accion))}</strong><br>
          <span class="texto-suave">${esc(item.usuario)}, ${fechaHora(item.creadoEn)}</span></li>`).join('')
      : '<li class="texto-suave">Aún no hay actividad registrada.</li>';
    recientes.innerHTML = `<div class="tabla-envoltura"><table class="tabla">${cabeceraReservas}<tbody>${datos.reservasRecientes.map(filaReserva).join('')}</tbody></table></div>`;
  } catch {
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
