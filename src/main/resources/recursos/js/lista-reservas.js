import { consulta, datosFormulario, debounce, errorCarga, esqueletoTabla, paginar, vacio } from './nucleo.js';
import { activarFilas, cabeceraReservas, filaReserva } from './detalle-reserva.js';

export function iniciarListaReservas({ obtener, alCargar, alFallar }) {
  const filtros = document.querySelector('[data-filtros]');
  const tabla = document.querySelector('[data-tabla]');
  const paginacion = document.querySelector('[data-paginacion]');
  let paginaActual = 0;
  let controlador = null;

  async function cargar(pagina = 0) {
    paginaActual = pagina;
    controlador?.abort();
    controlador = new AbortController();
    tabla.innerHTML = esqueletoTabla(8, 7);
    try {
      const resultado = await obtener(consulta({ ...datosFormulario(filtros), pagina, tamano: 10 }), controlador.signal);
      const elementos = resultado.pagina.elementos;
      alCargar?.(resultado);
      tabla.innerHTML = elementos.length
        ? `<table class="tabla">${cabeceraReservas}<tbody>${elementos.map(filaReserva).join('')}</tbody></table>`
        : vacio({ titulo: 'No hay reservas con esos filtros', texto: 'Cambia los filtros para ver más resultados.' });
      paginar(paginacion, resultado.pagina, cargar);
    } catch (error) {
      if (error.name !== 'AbortError') {
        tabla.innerHTML = errorCarga();
        paginacion.innerHTML = '';
        alFallar?.(error);
      }
    }
  }

  filtros.addEventListener('input', debounce(() => cargar(0), 300));
  filtros.addEventListener('submit', (evento) => evento.preventDefault());
  tabla.addEventListener('click', (evento) => {
    if (evento.target.closest('[data-reintentar]')) {
      cargar(paginaActual);
    }
  });
  activarFilas(tabla, () => cargar(paginaActual));
  cargar(0);
}
