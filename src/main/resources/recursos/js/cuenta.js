import { api, datosFormulario, enviarFormulario, esc, errorCarga, esqueletoTabla, fecha, insigniaEstado, moneda, mostrarError, mostrarErrores, paginar, ruta, toast, vacio } from './nucleo.js';

const perfil = document.querySelector('[data-perfil]');
const reservas = document.querySelector('[data-reservas]');
const paginacion = document.querySelector('[data-paginacion]');
const formularioContrasena = document.querySelector('[data-contrasena]');

function pintarPerfil(datos) {
  perfil.innerHTML = `
    <dl class="lista-datos">
      <div class="lista-datos__fila"><dt>Nombre</dt><dd>${esc(datos.nombres)} ${esc(datos.apellidos)}</dd></div>
      <div class="lista-datos__fila"><dt>DNI</dt><dd>${esc(datos.dni)}</dd></div>
      <div class="lista-datos__fila"><dt>Correo</dt><dd>${esc(datos.correo)}</dd></div>
    </dl>
    <form class="formulario" data-perfil-formulario novalidate>
      <div class="campo" data-campo="telefono"><label class="campo__etiqueta" for="telefono">Celular</label><input class="entrada" id="telefono" name="telefono" inputmode="numeric" maxlength="9" value="${esc(datos.telefono)}"><span class="campo__error"></span></div>
      <div class="campo" data-campo="direccion"><label class="campo__etiqueta" for="direccion">Dirección</label><input class="entrada" id="direccion" name="direccion" maxlength="200" value="${esc(datos.direccion)}"><span class="campo__error"></span></div>
      <button class="boton boton--primario" type="submit">Guardar datos</button>
    </form>`;
  const formulario = perfil.querySelector('[data-perfil-formulario]');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const respuesta = await enviarFormulario(formulario, evento.submitter, () =>
      api('/api/cliente/perfil', { metodo: 'PUT', cuerpo: datosFormulario(formulario) }));
    if (respuesta) {
      toast(respuesta.mensaje);
    }
  });
}

async function cargarReservas(pagina = 0) {
  reservas.innerHTML = esqueletoTabla(4, 4);
  try {
    const { datos } = await api(`/api/cliente/reservas?pagina=${pagina}&tamano=6`);
    reservas.innerHTML = datos.elementos.length ? `<div class="tabla-envoltura"><table class="tabla">
      <thead><tr><th>Reserva</th><th>Fechas</th><th>Estado</th><th class="derecha">Total</th></tr></thead>
      <tbody>${datos.elementos.map((reserva) => `<tr>
        <td><a class="enlace" href="${ruta(`/mi-cuenta/reservas/${encodeURIComponent(reserva.codigo)}`)}">${esc(reserva.codigo)}</a><br><span class="texto-suave">Habitación ${esc(reserva.habitaciones)}</span></td>
        <td>${fecha(reserva.fechaEntrada)} al ${fecha(reserva.fechaSalida)}</td>
        <td>${insigniaEstado(reserva.estado, reserva.estadoEtiqueta)}</td>
        <td class="derecha">${moneda(reserva.total)}</td></tr>`).join('')}</tbody></table></div>`
      : vacio({ nombreIcono: 'calendario', titulo: 'Aún no tienes reservas', texto: 'Cuando reserves, las verás aquí.', accion: `<a class="boton boton--acento" href="${ruta('/habitaciones')}">Ver habitaciones</a>` });
    paginar(paginacion, datos, cargarReservas);
  } catch {
    reservas.innerHTML = errorCarga();
  }
}

reservas.addEventListener('click', (evento) => {
  if (evento.target.closest('[data-reintentar]')) {
    cargarReservas();
  }
});

formularioContrasena.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const datos = datosFormulario(formularioContrasena);
  if (datos.nueva !== datos.confirmacion) {
    mostrarErrores(formularioContrasena, { confirmacion: 'Las contraseñas no coinciden.' });
    return;
  }
  const respuesta = await enviarFormulario(formularioContrasena, evento.submitter, () =>
    api('/api/cliente/contrasena', { metodo: 'PUT', cuerpo: datos }));
  if (respuesta) {
    toast(respuesta.mensaje);
    formularioContrasena.reset();
  }
});

perfil.innerHTML = '<div class="esqueleto esqueleto--bloque"></div>';
api('/api/cliente/perfil').then((respuesta) => pintarPerfil(respuesta.datos)).catch(mostrarError);
cargarReservas();
