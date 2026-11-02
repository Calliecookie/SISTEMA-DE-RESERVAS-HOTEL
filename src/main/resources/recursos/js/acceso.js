import { conCarga, ruta } from './nucleo.js';

const formulario = document.querySelector('[data-formulario-acceso]');
const aviso = document.querySelector('[data-error]');
const parametros = new URLSearchParams(location.search);
const leerMeta = (nombre) => document.querySelector(`meta[name="${nombre}"]`)?.content;

if (parametros.get('registro') === 'ok') {
  aviso.className = 'aviso aviso--exito';
  aviso.textContent = 'Tu cuenta se creó correctamente. Ya puedes iniciar sesión.';
}

formulario.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const boton = formulario.querySelector('[type="submit"]');
  aviso.classList.add('oculto');
  await conCarga(boton, async () => {
    const datos = new URLSearchParams(new FormData(formulario));
    if (parametros.get('destino')) {
      datos.set('destino', parametros.get('destino'));
    }
    let respuesta;
    let cuerpo = null;
    try {
      respuesta = await fetch(ruta('/ingresar'), {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'X-Requested-With': 'fetch', [leerMeta('_csrf_header')]: leerMeta('_csrf') },
        body: datos,
      });
      cuerpo = await respuesta.json();
    } catch {
      cuerpo = { mensaje: 'No hay conexión con el servidor. Inténtalo de nuevo.' };
    }
    if (respuesta?.ok && cuerpo.exito) {
      location.assign(cuerpo.datos.destino);
      return;
    }
    aviso.className = 'aviso aviso--error';
    aviso.textContent = cuerpo.mensaje || 'No se pudo iniciar sesión.';
    aviso.classList.remove('oculto');
    formulario.contrasena.value = '';
    formulario.contrasena.focus();
  });
});
