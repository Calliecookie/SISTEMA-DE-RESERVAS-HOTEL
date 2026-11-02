import { abrirModal, api, confirmar, conCarga, datosFormulario, enviarFormulario, esc, errorCarga, esqueletoTabla, icono, mostrarError, toast, vacio } from './nucleo.js';

const filtros = document.querySelector('[data-filtros]');
const tabla = document.querySelector('[data-tabla]');
const acciones = document.querySelector('[data-acciones-cabecera]');
let categorias = [];

function pintar() {
  const texto = filtros.busqueda.value.trim().toLowerCase();
  const lista = categorias.filter((item) => item.nombre.toLowerCase().includes(texto));
  tabla.innerHTML = lista.length ? `<table class="tabla">
    <thead><tr><th>Nombre</th><th>Descripción</th><th class="derecha">Habitaciones</th><th>Estado</th><th><span class="oculto-visual">Acciones</span></th></tr></thead>
    <tbody>${lista.map((item) => `<tr>
      <td><strong>${esc(item.nombre)}</strong></td><td>${esc(item.descripcion)}</td><td class="derecha">${item.cantidadHabitaciones}</td>
      <td><span class="insignia insignia--${item.activo ? 'exito' : 'neutra'}">${item.activo ? 'Activa' : 'Desactivada'}</span></td>
      <td><div class="tabla__acciones">
        <button class="boton boton--fantasma boton--icono" type="button" data-editar="${item.id}" aria-label="Editar ${esc(item.nombre)}">${icono('lapiz')}</button>
        <button class="boton boton--fantasma boton--icono" type="button" data-activar="${item.id}" aria-label="${item.activo ? 'Desactivar' : 'Activar'} ${esc(item.nombre)}">${icono('encender')}</button>
        <button class="boton boton--fantasma boton--icono" type="button" data-eliminar="${item.id}" aria-label="Eliminar ${esc(item.nombre)}">${icono('basura')}</button>
      </div></td></tr>`).join('')}</tbody></table>`
    : vacio({ nombreIcono: 'etiquetas', titulo: 'No hay categorías', texto: 'Registra una categoría o cambia la búsqueda.' });
}

async function cargar() {
  tabla.innerHTML = esqueletoTabla(5, 4);
  try {
    categorias = (await api('/api/administracion/categorias')).datos;
    pintar();
  } catch {
    tabla.innerHTML = errorCarga();
  }
}

function formulario(item) {
  const { modal, cerrar } = abrirModal({
    titulo: item ? 'Editar categoría' : 'Registrar categoría',
    contenido: `<form class="formulario" data-formulario novalidate>
      <div class="campo" data-campo="nombre"><label class="campo__etiqueta" for="c-nombre">Nombre</label><input class="entrada" id="c-nombre" name="nombre" maxlength="80" value="${esc(item?.nombre)}" required><span class="campo__error"></span></div>
      <div class="campo" data-campo="descripcion"><label class="campo__etiqueta" for="c-descripcion">Descripción</label><textarea class="entrada" id="c-descripcion" name="descripcion" maxlength="300">${esc(item?.descripcion)}</textarea><span class="campo__error"></span></div>
    </form>`,
    pie: `<button class="boton boton--fantasma" type="button" data-cerrar>Cancelar</button><button class="boton boton--primario" type="button" data-guardar>${item ? 'Guardar cambios' : 'Registrar categoría'}</button>`,
  });
  const form = modal.querySelector('[data-formulario]');
  const guardar = modal.querySelector('[data-guardar]');
  guardar.addEventListener('click', async () => {
    const respuesta = await enviarFormulario(form, guardar, () =>
      api(item ? `/api/administracion/categorias/${item.id}` : '/api/administracion/categorias', { metodo: item ? 'PUT' : 'POST', cuerpo: datosFormulario(form) }));
    if (respuesta) {
      toast(respuesta.mensaje);
      cerrar();
      cargar();
    }
  });
}

tabla.addEventListener('click', async (evento) => {
  if (evento.target.closest('[data-reintentar]')) {
    cargar();
    return;
  }
  const buscar = (boton, clave) => categorias.find((item) => item.id === Number(boton.dataset[clave]));
  const editar = evento.target.closest('[data-editar]');
  if (editar) {
    formulario(buscar(editar, 'editar'));
    return;
  }
  const activar = evento.target.closest('[data-activar]');
  if (activar) {
    const item = buscar(activar, 'activar');
    conCarga(activar, async () => {
      try {
        const respuesta = await api(`/api/administracion/categorias/${item.id}/activo`, { metodo: 'PATCH', cuerpo: { activo: !item.activo } });
        toast(item.activo ? 'Categoría desactivada.' : 'Categoría activada.');
        Object.assign(item, respuesta.datos);
        pintar();
      } catch (error) {
        mostrarError(error);
      }
    });
    return;
  }
  const eliminar = evento.target.closest('[data-eliminar]');
  if (eliminar) {
    const item = buscar(eliminar, 'eliminar');
    const aceptado = await confirmar({ titulo: 'Eliminar categoría', mensaje: `Se eliminará la categoría ${item.nombre}. Esta acción no se puede deshacer.`, textoConfirmar: 'Eliminar categoría', peligro: true });
    if (aceptado) {
      try {
        const respuesta = await api(`/api/administracion/categorias/${item.id}`, { metodo: 'DELETE' });
        toast(respuesta.mensaje);
        cargar();
      } catch (error) {
        mostrarError(error);
      }
    }
  }
});

acciones.innerHTML = `<button class="boton boton--acento" type="button" data-nueva>${icono('mas')}Registrar categoría</button>`;
acciones.querySelector('[data-nueva]').addEventListener('click', () => formulario(null));
filtros.addEventListener('input', pintar);
filtros.addEventListener('submit', (evento) => evento.preventDefault());
cargar();
