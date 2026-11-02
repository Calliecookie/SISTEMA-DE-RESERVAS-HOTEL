import { abrirModal, api, conCarga, datosFormulario, enviarFormulario, esc, errorCarga, esqueletoTabla, fechaHora, icono, mostrarError, toast, vacio, textoAccion } from './nucleo.js';

const filtros = document.querySelector('[data-filtros]');
const tabla = document.querySelector('[data-tabla]');
const acciones = document.querySelector('[data-acciones-cabecera]');
let usuarios = [];

const campo = (nombre, etiqueta, control, ayuda = '') => `<div class="campo" data-campo="${nombre}"><label class="campo__etiqueta" for="c-${nombre}">${etiqueta}</label>${control}${ayuda ? `<span class="campo__ayuda">${ayuda}</span>` : ''}<span class="campo__error"></span></div>`;

function pintar() {
  const texto = filtros.busqueda.value.trim().toLowerCase();
  const lista = usuarios.filter((item) => `${item.nombres} ${item.apellidos} ${item.correo}`.toLowerCase().includes(texto));
  tabla.innerHTML = lista.length ? `<table class="tabla">
    <thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Último acceso</th><th>Estado</th><th><span class="oculto-visual">Acciones</span></th></tr></thead>
    <tbody>${lista.map((item) => `<tr>
      <td><strong>${esc(item.nombres)} ${esc(item.apellidos)}</strong></td><td>${esc(item.correo)}</td><td>${esc(item.rolEtiqueta)}</td>
      <td>${item.ultimoAcceso ? fechaHora(item.ultimoAcceso) : 'Nunca'}</td>
      <td><span class="insignia insignia--${item.activo ? 'exito' : 'neutra'}">${item.activo ? 'Activo' : 'Desactivado'}</span></td>
      <td><div class="tabla__acciones">
        <button class="boton boton--fantasma boton--icono" type="button" data-actividad="${item.id}" aria-label="Ver actividad de ${esc(item.nombres)}">${icono('historial')}</button>
        <button class="boton boton--fantasma boton--icono" type="button" data-editar="${item.id}" aria-label="Editar ${esc(item.nombres)}">${icono('lapiz')}</button>
        <button class="boton boton--fantasma boton--icono" type="button" data-activar="${item.id}" aria-label="${item.activo ? 'Desactivar' : 'Activar'} ${esc(item.nombres)}">${icono('encender')}</button>
      </div></td></tr>`).join('')}</tbody></table>`
    : vacio({ nombreIcono: 'llave', titulo: 'No hay usuarios', texto: 'Registra un trabajador o cambia la búsqueda.' });
}

async function cargar() {
  tabla.innerHTML = esqueletoTabla(4, 5);
  try {
    usuarios = (await api('/api/administracion/usuarios')).datos;
    pintar();
  } catch {
    tabla.innerHTML = errorCarga();
  }
}

function formulario(item) {
  const { modal, cerrar } = abrirModal({
    titulo: item ? 'Editar usuario' : 'Registrar usuario',
    contenido: `<form class="formulario" data-formulario novalidate>
      <div class="formulario__fila formulario__fila--2">
        ${campo('nombres', 'Nombres', `<input class="entrada" id="c-nombres" name="nombres" maxlength="100" value="${esc(item?.nombres)}" required>`)}
        ${campo('apellidos', 'Apellidos', `<input class="entrada" id="c-apellidos" name="apellidos" maxlength="120" value="${esc(item?.apellidos)}" required>`)}
      </div>
      ${campo('correo', 'Correo electrónico', `<input class="entrada" id="c-correo" type="email" name="correo" maxlength="150" value="${esc(item?.correo)}" required>`)}
      ${campo('rol', 'Rol', `<select class="entrada" id="c-rol" name="rol"><option value="TRABAJADOR" ${item?.rol === 'TRABAJADOR' ? 'selected' : ''}>Trabajador</option><option value="ADMINISTRADOR" ${item?.rol === 'ADMINISTRADOR' ? 'selected' : ''}>Administrador</option></select>`)}
      ${campo('contrasena', item ? 'Nueva contraseña (opcional)' : 'Contraseña inicial', '<input class="entrada" id="c-contrasena" type="password" name="contrasena" autocomplete="new-password">', 'Mínimo 8 caracteres, con letras y números.')}
    </form>`,
    pie: `<button class="boton boton--fantasma" type="button" data-cerrar>Cancelar</button><button class="boton boton--primario" type="button" data-guardar>${item ? 'Guardar cambios' : 'Registrar usuario'}</button>`,
  });
  const form = modal.querySelector('[data-formulario]');
  const guardar = modal.querySelector('[data-guardar]');
  guardar.addEventListener('click', async () => {
    const datos = datosFormulario(form);
    datos.contrasena = datos.contrasena || null;
    const respuesta = await enviarFormulario(form, guardar, () =>
      api(item ? `/api/administracion/usuarios/${item.id}` : '/api/administracion/usuarios', { metodo: item ? 'PUT' : 'POST', cuerpo: datos }));
    if (respuesta) {
      toast(respuesta.mensaje);
      cerrar();
      cargar();
    }
  });
}

async function verActividad(item) {
  const { cuerpo } = abrirModal({ titulo: `Actividad de ${item.nombres}`, ancho: true, contenido: esqueletoTabla(5, 3) });
  try {
    const { datos } = await api(`/api/administracion/usuarios/${item.id}/actividad`);
    cuerpo.innerHTML = datos.elementos.length ? `<div class="tabla-envoltura"><table class="tabla">
      <thead><tr><th>Fecha</th><th>Acción</th><th>Detalle</th></tr></thead>
      <tbody>${datos.elementos.map((registro) => `<tr><td>${fechaHora(registro.creadoEn)}</td><td>${esc(textoAccion(registro.accion))}</td><td>${esc(registro.detalle)}</td></tr>`).join('')}</tbody></table></div>`
      : vacio({ nombreIcono: 'historial', titulo: 'Sin actividad registrada' });
  } catch (error) {
    mostrarError(error);
  }
}

tabla.addEventListener('click', (evento) => {
  if (evento.target.closest('[data-reintentar]')) {
    cargar();
    return;
  }
  const buscar = (boton, clave) => usuarios.find((item) => item.id === Number(boton.dataset[clave]));
  const editar = evento.target.closest('[data-editar]');
  if (editar) {
    formulario(buscar(editar, 'editar'));
  }
  const actividad = evento.target.closest('[data-actividad]');
  if (actividad) {
    verActividad(buscar(actividad, 'actividad'));
  }
  const activar = evento.target.closest('[data-activar]');
  if (activar) {
    const item = buscar(activar, 'activar');
    conCarga(activar, async () => {
      try {
        const respuesta = await api(`/api/administracion/usuarios/${item.id}/activo`, { metodo: 'PATCH', cuerpo: { activo: !item.activo } });
        toast(item.activo ? 'Usuario desactivado.' : 'Usuario activado.');
        Object.assign(item, respuesta.datos);
        pintar();
      } catch (error) {
        mostrarError(error);
      }
    });
  }
});

acciones.innerHTML = `<button class="boton boton--acento" type="button" data-nueva>${icono('mas')}Registrar usuario</button>`;
acciones.querySelector('[data-nueva]').addEventListener('click', () => formulario(null));
filtros.addEventListener('input', pintar);
filtros.addEventListener('submit', (evento) => evento.preventDefault());
cargar();
