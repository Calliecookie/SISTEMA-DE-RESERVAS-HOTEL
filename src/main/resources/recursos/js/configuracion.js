import { api, conCarga, datosFormulario, enviarFormulario, esc, icono, mostrarError, toast, urlImagen } from './nucleo.js';

const formulario = document.querySelector('[data-configuracion]');
const integraciones = document.querySelector('[data-integraciones]');
const qr = document.querySelector('[data-qr]');

const estado = (activo, textoActivo, textoInactivo) =>
  `<span class="insignia insignia--${activo ? 'exito' : 'advertencia'}">${activo ? textoActivo : textoInactivo}</span>`;

async function cargarIntegraciones() {
  const { datos } = await api('/api/administracion/configuracion/integraciones');
  integraciones.innerHTML = `
    <div class="estado-integracion"><span>PayPal: credenciales (${esc(datos.paypalEntorno)})</span>${estado(datos.paypalCredenciales, 'Configuradas', 'Faltan en .env')}</div>
    <div class="estado-integracion"><span>PayPal: tipo de cambio a ${esc(datos.paypalMoneda)}</span>${estado(datos.paypalTipoCambio, 'Definido', 'Falta definir')}</div>
    <div class="estado-integracion"><span>Yape: número y titular</span>${estado(datos.yapeConfigurado, 'Configurado', 'Falta definir')}</div>
    <div class="estado-integracion"><span>Factiliza: consulta de DNI</span>${estado(datos.factilizaConfigurada, 'Configurada', 'Falta token en .env')}</div>
    <div class="estado-integracion"><span>Correo SMTP</span>${estado(datos.correoHabilitado, 'Activo', 'Desactivado')}</div>`;
}

function pintarQr(valor) {
  qr.innerHTML = `
    ${valor ? `<img class="vista-previa" src="${esc(urlImagen(valor))}" alt="Código QR de Yape">` : '<p class="texto-sm texto-suave">Aún no subiste un código QR.</p>'}
    <div class="campo"><label class="campo__etiqueta" for="archivo-qr">Imagen JPG, PNG o WEBP, máximo 2 MB</label><input class="entrada" id="archivo-qr" type="file" accept="image/jpeg,image/png,image/webp"></div>
    <button class="boton boton--secundario" type="button" data-subir>${icono('subir')}Subir código QR</button>`;
  const subir = qr.querySelector('[data-subir]');
  subir.addEventListener('click', () => conCarga(subir, async () => {
    const archivo = qr.querySelector('#archivo-qr').files[0];
    if (!archivo) {
      toast('Selecciona una imagen.', 'advertencia');
      return;
    }
    const envio = new FormData();
    envio.append('archivo', archivo);
    try {
      const respuesta = await api('/api/administracion/configuracion/yape-qr', { metodo: 'POST', archivo: envio });
      toast(respuesta.mensaje);
      await cargar();
    } catch (error) {
      mostrarError(error);
    }
  }));
}

async function cargar() {
  try {
    const { datos } = await api('/api/administracion/configuracion');
    const editables = datos.filter((item) => item.clave !== 'yape_qr');
    formulario.innerHTML = `${editables.map((item) => `
      <div class="campo" data-campo="${esc(item.clave)}">
        <label class="campo__etiqueta" for="c-${esc(item.clave)}">${esc(item.descripcion)}</label>
        <input class="entrada" id="c-${esc(item.clave)}" name="${esc(item.clave)}" value="${esc(item.valor)}" ${item.tipo === 'TEXTO' ? '' : 'inputmode="decimal"'}>
        <span class="campo__error"></span>
      </div>`).join('')}
      <button class="boton boton--primario" type="submit">Guardar configuración</button>`;
    pintarQr(datos.find((item) => item.clave === 'yape_qr')?.valor);
    await cargarIntegraciones();
  } catch (error) {
    mostrarError(error);
  }
}

formulario.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const respuesta = await enviarFormulario(formulario, evento.submitter, () =>
    api('/api/administracion/configuracion', { metodo: 'PUT', cuerpo: { valores: datosFormulario(formulario) } }));
  if (respuesta) {
    toast(respuesta.mensaje);
    cargarIntegraciones();
  }
});

cargar();
