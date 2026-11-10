import { abrirModal, api, confirmar, conCarga, datosFormulario, enviarFormulario, esc, errorCarga, esqueletoTabla, icono, insigniaHabitacion, moneda, mostrarError, toast, urlImagen, vacio } from './nucleo.js';

const filtros = document.querySelector('[data-filtros]');
const tabla = document.querySelector('[data-tabla]');
const acciones = document.querySelector('[data-acciones-cabecera]');
const resumenEstados = document.querySelector('[data-resumen-estados]');
let habitaciones = [];
let categorias = [];
let rasgos = [];

const ESTADOS = [['DISPONIBLE', 'Disponible (en servicio)'], ['MANTENIMIENTO', 'En mantenimiento'], ['FUERA_DE_SERVICIO', 'Fuera de servicio']];
const MANUALES = ESTADOS.map(([valor]) => valor);
const campo = (nombre, etiqueta, control) => `<div class="campo" data-campo="${nombre}"><label class="campo__etiqueta" for="c-${nombre}">${etiqueta}</label>${control}<span class="campo__error"></span></div>`;

function pintarResumen() {
  if (!resumenEstados) {
    return;
  }
  const activas = habitaciones.filter((item) => item.activo);
  const contar = (estado) => activas.filter((item) => item.estado === estado).length;
  resumenEstados.textContent = `${activas.length} habitaciones activas: ${contar('DISPONIBLE')} disponibles, ${contar('RESERVADA')} reservadas, `
    + `${contar('OCUPADA')} ocupadas y ${contar('MANTENIMIENTO') + contar('FUERA_DE_SERVICIO')} fuera de servicio o en mantenimiento.`;
}

function pintar() {
  pintarResumen();
  const texto = filtros.busqueda.value.trim().toLowerCase();
  const estado = filtros.estado?.value || '';
  const lista = habitaciones.filter((item) => `${item.numero} ${item.nombre} ${item.categoria}`.toLowerCase().includes(texto)
    && (!estado || item.estado === estado));
  tabla.innerHTML = lista.length ? `<table class="tabla">
    <thead><tr><th>Foto</th><th>Número</th><th>Nombre</th><th>Categoría</th><th class="derecha">Capacidad</th><th class="derecha">Precio</th><th>Estado</th><th><span class="oculto-visual">Acciones</span></th></tr></thead>
    <tbody>${lista.map((item) => `<tr>
      <td><img class="tabla__miniatura" data-foto src="${esc(urlImagen(item.imagen))}" alt="" width="56" height="42" loading="lazy"></td>
      <td><strong>${esc(item.numero)}</strong></td><td>${esc(item.nombre)}</td><td>${esc(item.categoria)}</td>
      <td class="derecha">${item.capacidad}</td><td class="derecha">${moneda(item.precioNoche)}</td>
      <td>${item.activo ? insigniaHabitacion(item.estado, item.estadoEtiqueta) : '<span class="insignia insignia--neutra">Desactivada</span>'}</td>
      <td><div class="tabla__acciones">
        <button class="boton boton--fantasma boton--icono" type="button" data-editar="${item.id}" aria-label="Editar ${esc(item.numero)}">${icono('lapiz')}</button>
        <button class="boton boton--fantasma boton--icono" type="button" data-activar="${item.id}" aria-label="${item.activo ? 'Desactivar' : 'Activar'} ${esc(item.numero)}">${icono('encender')}</button>
        <button class="boton boton--fantasma boton--icono" type="button" data-eliminar="${item.id}" aria-label="Eliminar ${esc(item.numero)}">${icono('basura')}</button>
      </div></td></tr>`).join('')}</tbody></table>`
    : vacio({ nombreIcono: 'cama', titulo: 'No hay habitaciones', texto: 'Registra una habitación o cambia la búsqueda.' });
}

async function cargar() {
  tabla.innerHTML = esqueletoTabla(8, 7);
  try {
    const [respuestaHabitaciones, respuestaCategorias, respuestaRasgos] = await Promise.all([
      api('/api/administracion/habitaciones'),
      api('/api/administracion/categorias'),
      api('/api/administracion/caracteristicas'),
    ]);
    habitaciones = respuestaHabitaciones.datos;
    categorias = respuestaCategorias.datos;
    rasgos = respuestaRasgos.datos;
    pintar();
  } catch {
    tabla.innerHTML = errorCarga();
  }
}

function formulario(item) {
  const { modal, cerrar } = abrirModal({
    titulo: item ? `Editar habitación ${item.numero}` : 'Registrar habitación',
    ancho: true,
    contenido: `<form class="formulario" data-formulario novalidate>
      <div class="formulario__fila formulario__fila--3">
        ${campo('numero', 'Número', `<input class="entrada" id="c-numero" name="numero" maxlength="10" value="${esc(item?.numero)}" required>`)}
        ${campo('categoriaId', 'Categoría', `<select class="entrada" id="c-categoriaId" name="categoriaId" required><option value="">Selecciona</option>${categorias.map((categoria) => `<option value="${categoria.id}" ${item?.categoriaId === categoria.id ? 'selected' : ''}>${esc(categoria.nombre)}</option>`).join('')}</select>`)}
        ${campo('estado', 'Estado', `<select class="entrada" id="c-estado" name="estado">${ESTADOS.map(([valor, texto]) => `<option value="${valor}" ${(MANUALES.includes(item?.estado) ? item.estado : 'DISPONIBLE') === valor ? 'selected' : ''}>${texto}</option>`).join('')}</select>${item && !MANUALES.includes(item.estado) ? `<span class="campo__ayuda">Ahora está ${esc(item.estadoEtiqueta.toLowerCase())} por una reserva.</span>` : ''}`)}
      </div>
      ${campo('nombre', 'Nombre', `<input class="entrada" id="c-nombre" name="nombre" maxlength="120" value="${esc(item?.nombre)}" required>`)}
      ${campo('descripcion', 'Descripción', `<textarea class="entrada" id="c-descripcion" name="descripcion" maxlength="600" required>${esc(item?.descripcion)}</textarea>`)}
      <div class="formulario__fila formulario__fila--2">
        ${campo('capacidad', 'Capacidad', `<input class="entrada" id="c-capacidad" type="number" name="capacidad" min="1" max="10" value="${item?.capacidad ?? 2}" required>`)}
        ${campo('precioNoche', 'Precio por noche (S/)', `<input class="entrada" id="c-precioNoche" type="number" name="precioNoche" min="1" step="0.01" value="${item?.precioNoche ?? ''}" required>`)}
      </div>
      ${campo('imagenUrl', 'URL de imagen de Wikimedia Commons (opcional)', `<input class="entrada" id="c-imagenUrl" name="imagenUrl" maxlength="500" value="${esc(item?.imagen?.startsWith('http') ? item.imagen : '')}">`)}
      ${campo('archivo', 'Subir imagen (JPG, PNG o WEBP, máximo 2 MB)', '<input class="entrada" id="c-archivo" type="file" name="archivo" accept="image/jpeg,image/png,image/webp">')}
      <fieldset class="campo"><legend class="campo__etiqueta">Características</legend><div class="selector-rasgos">
        ${rasgos.map((rasgo) => `<label class="casilla"><input type="checkbox" name="rasgo" value="${rasgo.id}" ${item?.caracteristicaIds.includes(rasgo.id) ? 'checked' : ''}>${esc(rasgo.nombre)}</label>`).join('')}
      </div></fieldset>
    </form>`,
    pie: `<button class="boton boton--fantasma" type="button" data-cerrar>Cancelar</button><button class="boton boton--primario" type="button" data-guardar>${item ? 'Guardar cambios' : 'Registrar habitación'}</button>`,
  });
  const form = modal.querySelector('[data-formulario]');
  const guardar = modal.querySelector('[data-guardar]');
  guardar.addEventListener('click', async () => {
    const datos = datosFormulario(form);
    const cuerpo = {
      numero: datos.numero,
      nombre: datos.nombre,
      categoriaId: datos.categoriaId ? Number(datos.categoriaId) : null,
      descripcion: datos.descripcion,
      capacidad: datos.capacidad ? Number(datos.capacidad) : null,
      precioNoche: datos.precioNoche || null,
      estado: datos.estado,
      imagenUrl: datos.imagenUrl || null,
      caracteristicaIds: [...form.querySelectorAll('[name="rasgo"]:checked')].map((casilla) => Number(casilla.value)),
    };
    const respuesta = await enviarFormulario(form, guardar, async () => {
      const guardado = await api(item ? `/api/administracion/habitaciones/${item.id}` : '/api/administracion/habitaciones', { metodo: item ? 'PUT' : 'POST', cuerpo });
      const archivo = form.archivo.files[0];
      if (archivo) {
        const envio = new FormData();
        envio.append('archivo', archivo);
        await api(`/api/administracion/habitaciones/${guardado.datos.id}/imagen`, { metodo: 'POST', archivo: envio });
      }
      return guardado;
    });
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
  const editar = evento.target.closest('[data-editar]');
  if (editar) {
    formulario(habitaciones.find((item) => item.id === Number(editar.dataset.editar)));
    return;
  }
  const activar = evento.target.closest('[data-activar]');
  if (activar) {
    const item = habitaciones.find((elemento) => elemento.id === Number(activar.dataset.activar));
    conCarga(activar, async () => {
      try {
        const respuesta = await api(`/api/administracion/habitaciones/${item.id}/activo`, { metodo: 'PATCH', cuerpo: { activo: !item.activo } });
        toast(item.activo ? 'Habitación desactivada.' : 'Habitación activada.');
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
    const item = habitaciones.find((elemento) => elemento.id === Number(eliminar.dataset.eliminar));
    const aceptado = await confirmar({ titulo: 'Eliminar habitación', mensaje: `Se eliminará la habitación ${item.numero}. Esta acción no se puede deshacer.`, textoConfirmar: 'Eliminar habitación', peligro: true });
    if (aceptado) {
      try {
        const respuesta = await api(`/api/administracion/habitaciones/${item.id}`, { metodo: 'DELETE' });
        toast(respuesta.mensaje);
        cargar();
      } catch (error) {
        mostrarError(error);
      }
    }
  }
});

acciones.innerHTML = `<button class="boton boton--acento" type="button" data-nueva>${icono('mas')}Registrar habitación</button>`;
acciones.querySelector('[data-nueva]').addEventListener('click', () => formulario(null));
filtros.addEventListener('input', pintar);
filtros.addEventListener('submit', (evento) => evento.preventDefault());
cargar();
