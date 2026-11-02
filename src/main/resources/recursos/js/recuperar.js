import { api, datosFormulario, enviarFormulario, mostrarErrores, ruta, toast } from './nucleo.js';

const pasos = Object.fromEntries([...document.querySelectorAll('[data-paso]')].map((paso) => [paso.dataset.paso, paso]));
const indicacion = document.querySelector('[data-indicacion]');
let correo = '';

function mostrar(nombre, texto) {
  Object.entries(pasos).forEach(([clave, formulario]) => formulario.classList.toggle('oculto', clave !== nombre));
  indicacion.textContent = texto;
  pasos[nombre].querySelector('input').focus();
}

pasos.solicitar.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  correo = pasos.solicitar.correo.value.trim();
  const respuesta = await enviarFormulario(pasos.solicitar, evento.submitter, () =>
    api('/api/autenticacion/recuperacion/solicitar', { metodo: 'POST', cuerpo: { correo } }));
  if (respuesta) {
    mostrar('verificar', respuesta.mensaje);
  }
});

pasos.verificar.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const respuesta = await enviarFormulario(pasos.verificar, evento.submitter, () =>
    api('/api/autenticacion/recuperacion/verificar', {
      metodo: 'POST',
      cuerpo: { correo, codigo: pasos.verificar.codigo.value.trim() },
    }));
  if (respuesta) {
    mostrar('restablecer', respuesta.mensaje);
  }
});

pasos.restablecer.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const datos = datosFormulario(pasos.restablecer);
  if (datos.contrasena !== datos.confirmacion) {
    mostrarErrores(pasos.restablecer, { confirmacion: 'Las contraseñas no coinciden.' });
    return;
  }
  const respuesta = await enviarFormulario(pasos.restablecer, evento.submitter, () =>
    api('/api/autenticacion/recuperacion/restablecer', { metodo: 'POST', cuerpo: datos }));
  if (respuesta) {
    toast(respuesta.mensaje);
    setTimeout(() => location.assign(ruta('/ingresar')), 1200);
  }
});
