import { api, consulta, esc, fechaLocal, icono, insigniaDisponibilidad, moneda, mostrarError, ruta, urlImagen, vacio } from './nucleo.js';
import { agregarAlCarrito } from './habitaciones-vista.js';

const ficha = document.querySelector('[data-ficha]');
const id = document.body.dataset.habitacionId;

ficha.innerHTML = `
  <div class="esqueleto esqueleto--foto"></div>
  <div class="resumen"><div class="esqueleto esqueleto--titulo"></div><div class="esqueleto esqueleto--linea"></div><div class="esqueleto esqueleto--linea esqueleto--media"></div><div class="esqueleto esqueleto--bloque"></div></div>`;

function pintar(habitacion) {
  document.title = `${habitacion.nombre} | ${document.title.split('|').pop().trim()}`;
  ficha.innerHTML = `
    <div>
      <div class="ficha__marco"><img data-foto src="${esc(urlImagen(habitacion.imagen))}" alt="${esc(habitacion.nombre)}" width="960" height="720" decoding="async"></div>
      <ul class="ficha__rasgos">${habitacion.caracteristicas.map((rasgo) => `<li>${icono(rasgo.icono)}${esc(rasgo.nombre)}</li>`).join('')}</ul>
    </div>
    <div class="formulario">
      <p class="habitacion__dato">${esc(habitacion.categoria)}, habitación ${esc(habitacion.numero)}</p>
      <h1 class="ficha__titulo">${esc(habitacion.nombre)}</h1>
      <p>${insigniaDisponibilidad(habitacion)}</p>
      <p>${esc(habitacion.descripcion)}</p>
      <p class="habitacion__precio">${moneda(habitacion.precioNoche)} <small>por noche + IGV, hasta ${habitacion.capacidad} ${habitacion.capacidad === 1 ? 'huésped' : 'huéspedes'}</small></p>
      <form class="superficie superficie--relleno formulario" data-disponibilidad novalidate>
        <div class="formulario__fila formulario__fila--2">
          <label class="campo"><span class="campo__etiqueta">Entrada</span><input class="entrada" type="date" name="fechaEntrada" min="${fechaLocal()}" required></label>
          <label class="campo"><span class="campo__etiqueta">Salida</span><input class="entrada" type="date" name="fechaSalida" min="${fechaLocal(1)}" required></label>
        </div>
        <label class="campo"><span class="campo__etiqueta">Huéspedes</span>
          <select class="entrada" name="huespedes">${Array.from({ length: habitacion.capacidad }, (_, indice) => `<option value="${indice + 1}">${indice + 1}</option>`).join('')}</select></label>
        <div data-resultado aria-live="polite"></div>
        <button class="boton boton--acento boton--ancho" type="button" data-reservar="${habitacion.id}" data-capacidad="${habitacion.capacidad}">Añadir a mi reserva</button>
      </form>
    </div>`;
  const formulario = ficha.querySelector('[data-disponibilidad]');
  const resultado = ficha.querySelector('[data-resultado]');
  const reservar = ficha.querySelector('[data-reservar]');

  async function comprobar() {
    const { fechaEntrada, fechaSalida } = formulario;
    if (!fechaEntrada.value || !fechaSalida.value) {
      return;
    }
    resultado.innerHTML = '<div class="esqueleto esqueleto--linea"></div>';
    try {
      const respuesta = await api(`/api/publico/habitaciones/${id}/disponibilidad${consulta({ fechaEntrada: fechaEntrada.value, fechaSalida: fechaSalida.value })}`);
      const datos = respuesta.datos;
      resultado.innerHTML = `<div class="aviso aviso--${datos.disponible ? 'exito' : 'error'}">${icono(datos.disponible ? 'check-circulo' : 'x-circulo')}
        <span>${esc(datos.mensaje)} ${datos.noches} ${datos.noches === 1 ? 'noche' : 'noches'}, total con IGV ${moneda(datos.importes.total)}.</span></div>`;
      reservar.disabled = !datos.disponible;
    } catch (error) {
      resultado.innerHTML = `<div class="aviso aviso--error">${icono('alerta')}<span>${esc(error.message)}</span></div>`;
      reservar.disabled = true;
    }
  }

  formulario.addEventListener('change', comprobar);
  reservar.addEventListener('click', () => agregarAlCarrito(reservar, {
    fechaEntrada: formulario.fechaEntrada.value,
    fechaSalida: formulario.fechaSalida.value,
    huespedes: formulario.huespedes.value,
  }));
}

api(`/api/publico/habitaciones/${id}`)
  .then((respuesta) => pintar(respuesta.datos))
  .catch((error) => {
    if (error.estado === 404) {
      ficha.innerHTML = vacio({
        nombreIcono: 'puerta',
        titulo: 'Esta habitación no está disponible',
        texto: 'Puede que ya no forme parte del catálogo.',
        accion: `<a class="boton boton--primario" href="${ruta('/habitaciones')}">Ver habitaciones</a>`,
      });
    } else {
      mostrarError(error);
    }
  });
