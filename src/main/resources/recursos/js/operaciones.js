import { api, esc, esqueletoIndicadores, esqueletoTabla, fecha, icono, insigniaEstado, moneda, mostrarError, vacio } from './nucleo.js';
import { activarFilas } from './detalle-reserva.js';

const indicadores = document.querySelector('[data-indicadores]');
const listas = [...document.querySelectorAll('[data-lista]')];
const VACIOS = {
  pagosPorVerificar: 'No hay pagos por verificar',
  porConfirmar: 'No hay reservas por confirmar',
  llegadasHoy: 'No hay llegadas pendientes',
  salidasHoy: 'No hay salidas pendientes',
};

const fila = (reserva) => `
  <tr class="fila-accion" data-codigo="${esc(reserva.codigo)}" tabindex="0">
    <td><strong>${esc(reserva.codigo)}</strong><br><span class="texto-suave">${esc(reserva.cliente)}</span></td>
    <td>${esc(reserva.habitaciones)}<br><span class="texto-suave">${fecha(reserva.fechaEntrada)} al ${fecha(reserva.fechaSalida)}</span></td>
    <td>${insigniaEstado(reserva.estado, reserva.estadoEtiqueta)}</td>
    <td class="derecha">${moneda(reserva.total)}</td>
  </tr>`;

async function cargar() {
  indicadores.innerHTML = esqueletoIndicadores(4);
  listas.forEach((lista) => { lista.innerHTML = esqueletoTabla(3, 4); });
  try {
    const { datos } = await api('/api/operaciones/resumen');
    const tarjeta = (nombreIcono, etiqueta, valor) => `<div class="indicador"><span class="indicador__etiqueta">${icono(nombreIcono)}${esc(etiqueta)}</span><span class="indicador__valor">${esc(valor)}</span></div>`;
    indicadores.innerHTML = [
      tarjeta('entrar', 'Llegadas', datos.llegadasHoy.length),
      tarjeta('salir', 'Salidas', datos.salidasHoy.length),
      tarjeta('billetera', 'Pagos por verificar', datos.pagosPorVerificar.length),
      tarjeta('cama', 'Habitaciones ocupadas', datos.habitacionesOcupadas),
      tarjeta('usuarios', 'Huéspedes alojados', datos.huespedesAlojados),
    ].join('');
    listas.forEach((lista) => {
      const elementos = datos[lista.dataset.lista];
      lista.innerHTML = elementos.length
        ? `<table class="tabla"><tbody>${elementos.map(fila).join('')}</tbody></table>`
        : vacio({ nombreIcono: 'check-circulo', titulo: VACIOS[lista.dataset.lista] });
    });
  } catch (error) {
    mostrarError(error);
  }
}

listas.forEach((lista) => activarFilas(lista, cargar));
cargar();
