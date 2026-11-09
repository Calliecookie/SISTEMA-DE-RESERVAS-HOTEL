import { api, datosFormulario, enviarFormulario, mostrarErrores, ruta, toast } from './nucleo.js';

const indicacion = document.querySelector('[data-indicacion]');
const avisoMetodo = document.querySelector('[data-aviso-metodo]');
const opciones = [...document.querySelectorAll('[data-metodo]')];
const paneles = Object.fromEntries([...document.querySelectorAll('[data-panel]')].map((panel) => [panel.dataset.panel, panel]));
const pasos = Object.fromEntries([...paneles.correo.querySelectorAll('[data-paso]')].map((paso) => [paso.dataset.paso, paso]));
const formularioClave = document.querySelector('[data-formulario-clave]');
let metodos = { correo: true, claveAdministrador: true };
let correo = '';

const AVISOS = {
  correo: 'El envío de correos no está activado en el servidor, así que el código no llegará. Usa la clave de recuperación.',
  clave: 'El servidor no tiene una clave de recuperación configurada. Defínela en ADMIN_CLAVE_RECUPERACION (mínimo 12 caracteres) o usa el código por correo.',
};

function elegir(metodo) {
  opciones.forEach((opcion) => opcion.setAttribute('aria-selected', String(opcion.dataset.metodo === metodo)));
  Object.entries(paneles).forEach(([clave, panel]) => panel.classList.toggle('oculto', clave !== metodo));
  const disponible = metodo === 'correo' ? metodos.correo : metodos.claveAdministrador;
  avisoMetodo.textContent = disponible ? '' : AVISOS[metodo];
  avisoMetodo.classList.toggle('oculto', disponible);
  indicacion.textContent = metodo === 'correo'
    ? 'Te enviaremos un código de 6 dígitos al correo del administrador.'
    : 'Ingresa la clave de recuperación que definiste al instalar el sistema.';
}

function mostrarPaso(nombre, texto) {
  Object.entries(pasos).forEach(([clave, formulario]) => formulario.classList.toggle('oculto', clave !== nombre));
  indicacion.textContent = texto;
  pasos[nombre].querySelector('input').focus();
}

opciones.forEach((opcion) => opcion.addEventListener('click', () => elegir(opcion.dataset.metodo)));

pasos.solicitar.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  correo = pasos.solicitar.correo.value.trim();
  const respuesta = await enviarFormulario(pasos.solicitar, evento.submitter, () =>
    api('/api/autenticacion/recuperacion/solicitar', { metodo: 'POST', cuerpo: { correo } }));
  if (respuesta) {
    mostrarPaso('verificar', respuesta.mensaje);
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
    mostrarPaso('restablecer', respuesta.mensaje);
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

formularioClave.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const datos = datosFormulario(formularioClave);
  if (datos.contrasena !== datos.confirmacion) {
    mostrarErrores(formularioClave, { confirmacion: 'Las contraseñas no coinciden.' });
    return;
  }
  const respuesta = await enviarFormulario(formularioClave, evento.submitter, () =>
    api('/api/autenticacion/recuperacion/administrador', { metodo: 'POST', cuerpo: datos }));
  if (respuesta) {
    toast(respuesta.mensaje);
    setTimeout(() => location.assign(ruta('/ingresar')), 1200);
  }
});

(async () => {
  try {
    metodos = (await api('/api/autenticacion/recuperacion/metodos')).datos;
  } catch {
    // Si la consulta falla se muestran ambos métodos sin aviso.
  }
  elegir(!metodos.correo && metodos.claveAdministrador ? 'clave' : 'correo');
})();
