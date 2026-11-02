import { api, errorCarga, esqueletoHabitaciones, fechaLocal } from './nucleo.js';
import { agregarAlCarrito, tarjetaHabitacion } from './habitaciones-vista.js';

const destacadas = document.querySelector('[data-destacadas]');
const buscador = document.querySelector('[data-buscador]');

buscador.fechaEntrada.min = fechaLocal();
buscador.fechaSalida.min = fechaLocal(1);
buscador.fechaEntrada.addEventListener('change', () => {
  if (buscador.fechaEntrada.value) {
    const siguiente = new Date(`${buscador.fechaEntrada.value}T12:00:00`);
    siguiente.setDate(siguiente.getDate() + 1);
    buscador.fechaSalida.min = siguiente.toISOString().slice(0, 10);
  }
});

async function cargar() {
  destacadas.innerHTML = esqueletoHabitaciones(3);
  try {
    const respuesta = await api('/api/publico/habitaciones?tamano=3&soloDisponibles=true');
    destacadas.innerHTML = respuesta.datos.elementos.map(tarjetaHabitacion).join('');
  } catch {
    destacadas.innerHTML = errorCarga();
  }
}

destacadas.addEventListener('click', (evento) => {
  const reservar = evento.target.closest('[data-reservar]');
  if (reservar) {
    agregarAlCarrito(reservar);
  }
  if (evento.target.closest('[data-reintentar]')) {
    cargar();
  }
});

cargar();
