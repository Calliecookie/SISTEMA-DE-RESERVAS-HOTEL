import { api, conCarga, datosFormulario, enviarFormulario, limpiarErrores, mostrarErrores, ruta } from './nucleo.js';

const formulario = document.querySelector('[data-formulario-registro]');
const estado = document.querySelector('[data-estado-dni]');
const consultar = document.querySelector('[data-consultar-dni]');
const campoDni = formulario.querySelector('[data-campo="dni"]');

function fijarNombres(nombres, apellidos, editable) {
  formulario.nombres.value = nombres || '';
  formulario.apellidos.value = apellidos || '';
  formulario.nombres.readOnly = !editable;
  formulario.apellidos.readOnly = !editable;
}

async function verificarDni() {
  const dni = formulario.dni.value.trim();
  limpiarErrores(formulario);
  campoDni.classList.remove('campo--exito');
  if (!/^\d{8}$/.test(dni)) {
    mostrarErrores(formulario, { dni: 'El DNI debe tener exactamente 8 dígitos.' });
    return;
  }
  await conCarga(consultar, async () => {
    estado.textContent = 'Consultando tu DNI';
    try {
      const respuesta = await api('/api/autenticacion/consultar-dni', { metodo: 'POST', cuerpo: { dni } });
      const datos = respuesta.datos;
      estado.textContent = datos.mensaje;
      if (datos.verificado) {
        fijarNombres(datos.nombres, datos.apellidos, false);
        campoDni.classList.add('campo--exito');
      } else if (datos.permiteManual) {
        fijarNombres('', '', true);
        formulario.nombres.focus();
      } else {
        fijarNombres('', '', false);
        mostrarErrores(formulario, { dni: datos.mensaje });
      }
    } catch (error) {
      estado.textContent = 'Usaremos tu DNI para completar tu nombre.';
      mostrarErrores(formulario, { dni: error.message });
    }
  });
}

consultar.addEventListener('click', verificarDni);
formulario.dni.addEventListener('input', () => {
  formulario.dni.value = formulario.dni.value.replace(/\D/g, '');
  campoDni.classList.remove('campo--exito');
  fijarNombres('', '', false);
  if (formulario.dni.value.length === 8) {
    verificarDni();
  }
});

formulario.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const datos = datosFormulario(formulario);
  const errores = {};
  if (!/^\d{8}$/.test(datos.dni)) {
    errores.dni = 'El DNI debe tener exactamente 8 dígitos.';
  }
  if (!datos.nombres?.trim()) {
    errores.nombres = 'Verifica tu DNI o ingresa tus nombres.';
  }
  if (!datos.apellidos?.trim()) {
    errores.apellidos = 'Verifica tu DNI o ingresa tus apellidos.';
  }
  if (!/^\S+@\S+\.\S+$/.test(datos.correo)) {
    errores.correo = 'Ingresa un correo válido.';
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(datos.contrasena)) {
    errores.contrasena = 'Debe tener entre 8 y 72 caracteres, con letras y números.';
  }
  if (datos.contrasena !== datos.confirmacion) {
    errores.confirmacion = 'Las contraseñas no coinciden.';
  }
  if (Object.keys(errores).length) {
    mostrarErrores(formulario, errores);
    return;
  }
  const respuesta = await enviarFormulario(formulario, formulario.querySelector('[type="submit"]'), () =>
    api('/api/autenticacion/registro', { metodo: 'POST', cuerpo: datos }));
  if (respuesta) {
    location.assign(ruta('/ingresar?registro=ok'));
  }
});
