import { api, esc, esqueletoIndicadores, icono, moneda } from './nucleo.js';
import { iniciarListaReservas } from './lista-reservas.js';

const resumen = document.querySelector('[data-resumen]');
const tarjeta = (nombreIcono, etiqueta, valor) => `
  <div class="indicador"><span class="indicador__etiqueta">${icono(nombreIcono)}${esc(etiqueta)}</span><span class="indicador__valor">${esc(valor)}</span></div>`;

resumen.innerHTML = esqueletoIndicadores(3);

iniciarListaReservas({
  obtener: async (parametros, senal) => (await api(`/api/administracion/ventas${parametros}`, { senal })).datos,
  alCargar: (datos) => {
    resumen.innerHTML = [
      tarjeta('recibo', 'Ingresos confirmados', moneda(datos.ingresosConfirmados)),
      tarjeta('reloj', 'Pendiente de cobro', moneda(datos.importePendiente)),
      tarjeta('lista', 'Reservas encontradas', datos.cantidadOperaciones),
    ].join('');
  },
});
