import { actualizarContadorCarrito, api, esc, icono, insigniaDisponibilidad, moneda, mostrarError, ruta, toast, urlImagen } from './nucleo.js';

export function tarjetaHabitacion(habitacion) {
  const rasgos = habitacion.caracteristicas.slice(0, 3)
    .map((rasgo) => `<li>${icono(rasgo.icono)}${esc(rasgo.nombre)}</li>`).join('');
  const enlace = ruta(`/habitaciones/${habitacion.id}`);
  return `
    <article class="habitacion">
      <a class="habitacion__marco" href="${enlace}" aria-label="Ver ${esc(habitacion.nombre)}">
        <img class="habitacion__foto" data-foto src="${esc(urlImagen(habitacion.imagen))}" alt="${esc(habitacion.nombre)}"
             width="960" height="720" loading="lazy" decoding="async">
        <span class="habitacion__estado">${insigniaDisponibilidad(habitacion)}</span>
      </a>
      <div class="habitacion__fila">
        <a class="habitacion__nombre" href="${enlace}">${esc(habitacion.nombre)}</a>
        <p class="habitacion__precio">${moneda(habitacion.precioNoche)} <small>+ IGV</small></p>
      </div>
      <p class="habitacion__dato">${esc(habitacion.categoria)}, habitación ${esc(habitacion.numero)}, hasta ${habitacion.capacidad} ${habitacion.capacidad === 1 ? 'huésped' : 'huéspedes'}</p>
      <p class="habitacion__descripcion">${esc(habitacion.descripcion)}</p>
      <ul class="habitacion__rasgos">${rasgos}</ul>
      <div class="habitacion__acciones">
        <button class="boton boton--acento boton--ancho" type="button" data-reservar="${habitacion.id}" data-capacidad="${habitacion.capacidad}"
          ${habitacion.disponible ? '' : 'disabled'}>${habitacion.disponible ? 'Reservar' : 'No disponible'}</button>
      </div>
    </article>`;
}

export async function agregarAlCarrito(boton, { fechaEntrada, fechaSalida, huespedes } = {}) {
  const capacidad = Number(boton.dataset.capacidad);
  const cuerpo = {
    habitacionId: Number(boton.dataset.reservar),
    huespedes: Math.min(Number(huespedes) || 1, capacidad),
    fechaEntrada: fechaEntrada || null,
    fechaSalida: fechaSalida || null,
  };
  boton.disabled = true;
  boton.classList.add('boton--cargando');
  try {
    const respuesta = await api('/api/carrito/items', { metodo: 'POST', cuerpo });
    actualizarContadorCarrito(respuesta.datos.cantidadItems);
    toast(respuesta.mensaje, 'exito', { href: ruta('/carrito'), texto: 'Ver mi reserva' });
  } catch (error) {
    mostrarError(error);
  } finally {
    boton.disabled = false;
    boton.classList.remove('boton--cargando');
  }
}
