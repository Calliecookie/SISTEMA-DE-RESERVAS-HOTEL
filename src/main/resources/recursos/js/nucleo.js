const leerMeta = (nombre) => document.querySelector(`meta[name="${nombre}"]`)?.content;

export const contexto = (leerMeta('contexto') || '/').replace(/\/$/, '');
export const rol = leerMeta('rol') || null;
export const ruta = (camino) => `${contexto}${camino}`;

export class ErrorApi extends Error {
  constructor(mensaje, estado, codigo, errores) {
    super(mensaje);
    this.estado = estado;
    this.codigo = codigo;
    this.errores = errores;
  }
}

export async function api(url, { metodo = 'GET', cuerpo, archivo, senal } = {}) {
  const cabeceras = { Accept: 'application/json', 'X-Requested-With': 'fetch' };
  const cabeceraCsrf = leerMeta('_csrf_header');
  if (metodo !== 'GET' && cabeceraCsrf) {
    cabeceras[cabeceraCsrf] = leerMeta('_csrf');
  }
  const opciones = { method: metodo, headers: cabeceras, credentials: 'same-origin', signal: senal };
  if (archivo) {
    opciones.body = archivo;
  } else if (cuerpo !== undefined) {
    cabeceras['Content-Type'] = 'application/json';
    opciones.body = JSON.stringify(cuerpo);
  }
  let respuesta;
  try {
    respuesta = await fetch(ruta(url), opciones);
  } catch (error) {
    if (error.name === 'AbortError') {
      throw error;
    }
    throw new ErrorApi('No hay conexión con el servidor. Revisa tu internet e inténtalo de nuevo.', 0);
  }
  let datos = null;
  if ((respuesta.headers.get('content-type') || '').includes('application/json')) {
    datos = await respuesta.json();
  }
  if (!respuesta.ok || datos?.exito === false) {
    throw new ErrorApi(
      datos?.mensaje || 'No se pudo completar la operación. Inténtalo de nuevo.',
      respuesta.status,
      datos?.codigo,
      datos?.errores,
    );
  }
  return datos;
}

export function esc(valor) {
  const elemento = document.createElement('div');
  elemento.textContent = valor ?? '';
  return elemento.innerHTML.replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

export const icono = (nombre) =>
  `<svg class="icono" aria-hidden="true"><use href="${ruta('/recursos/iconos/lucide.svg')}#${esc(nombre)}"></use></svg>`;

export const moneda = (valor) =>
  `S/ ${Number(valor || 0).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export function fecha(iso) {
  if (!iso) {
    return '';
  }
  const [anio, mes, dia] = iso.slice(0, 10).split('-');
  return `${dia}/${mes}/${anio}`;
}

export function fechaHora(iso) {
  return iso ? `${fecha(iso)} ${iso.slice(11, 16)}` : '';
}

export function fechaLocal(desplazamientoDias = 0) {
  const dia = new Date();
  dia.setDate(dia.getDate() + desplazamientoDias);
  const dos = (numero) => String(numero).padStart(2, '0');
  return `${dia.getFullYear()}-${dos(dia.getMonth() + 1)}-${dos(dia.getDate())}`;
}

const ACENTOS = { habitacion: 'habitación', categoria: 'categoría', configuracion: 'configuración' };

export function textoAccion(accion) {
  const texto = accion.toLowerCase().split('_').map((palabra) => ACENTOS[palabra] || palabra).join(' ');
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

export const urlImagen = (url) => (url && url.startsWith('/') ? ruta(url) : url || '');

export function debounce(funcion, espera = 300) {
  let temporizador;
  return (...argumentos) => {
    clearTimeout(temporizador);
    temporizador = setTimeout(() => funcion(...argumentos), espera);
  };
}

function contenedorNotificaciones() {
  let contenedor = document.querySelector('.notificaciones');
  if (!contenedor) {
    contenedor = document.createElement('div');
    contenedor.className = 'notificaciones';
    contenedor.setAttribute('role', 'status');
    contenedor.setAttribute('aria-live', 'polite');
    document.body.append(contenedor);
  }
  return contenedor;
}

export function toast(mensaje, tipo = 'exito', accion = null) {
  const iconos = { exito: 'check-circulo', error: 'x-circulo', advertencia: 'alerta', info: 'info' };
  const elemento = document.createElement('div');
  elemento.className = `notificacion notificacion--${tipo}`;
  elemento.innerHTML = `${icono(iconos[tipo] || 'info')}<span class="notificacion__texto">${esc(mensaje)}</span>`;
  if (accion) {
    const enlace = document.createElement('a');
    enlace.href = accion.href;
    enlace.textContent = accion.texto;
    elemento.append(enlace);
  }
  contenedorNotificaciones().append(elemento);
  setTimeout(() => {
    elemento.classList.add('notificacion--saliendo');
    setTimeout(() => elemento.remove(), 220);
  }, tipo === 'error' ? 6500 : 4200);
}

export function mostrarError(error) {
  if (error?.name === 'AbortError') {
    return;
  }
  if (error?.estado === 401) {
    const destino = `${location.pathname.slice(contexto.length)}${location.search}`;
    toast('Inicia sesión para continuar.', 'advertencia', {
      href: ruta(`/ingresar?destino=${encodeURIComponent(destino)}`),
      texto: 'Iniciar sesión',
    });
    return;
  }
  toast(error?.message || 'No se pudo completar la operación.', 'error');
}

export function abrirModal({ titulo, contenido = '', pie = '', ancho = false, alCerrar = null }) {
  const modal = document.createElement('dialog');
  modal.className = `modal${ancho ? ' modal--ancho' : ''}`;
  modal.innerHTML = `
    <div class="modal__marco">
      <div class="modal__cabecera">
        <h2 class="modal__titulo">${esc(titulo)}</h2>
        <button class="boton boton--fantasma boton--icono" type="button" data-cerrar aria-label="Cerrar">${icono('x')}</button>
      </div>
      <div class="modal__cuerpo">${contenido}</div>
      ${pie ? `<div class="modal__pie">${pie}</div>` : ''}
    </div>`;
  document.body.append(modal);
  modal.showModal();
  requestAnimationFrame(() => modal.classList.add('modal--visible'));
  const cerrar = () => {
    modal.classList.remove('modal--visible');
    setTimeout(() => {
      modal.close();
      modal.remove();
      alCerrar?.();
    }, 200);
  };
  modal.addEventListener('cancel', (evento) => {
    evento.preventDefault();
    cerrar();
  });
  modal.addEventListener('click', (evento) => {
    if (evento.target === modal || evento.target.closest('[data-cerrar]')) {
      cerrar();
    }
  });
  return { modal, cerrar, cuerpo: modal.querySelector('.modal__cuerpo'), pie: modal.querySelector('.modal__pie') };
}

export function confirmar({ titulo, mensaje, textoConfirmar = 'Confirmar', peligro = false }) {
  return new Promise((resolver) => {
    let resultado = false;
    const { modal, cerrar } = abrirModal({
      titulo,
      contenido: `<p>${esc(mensaje)}</p>`,
      pie: `<button class="boton boton--fantasma" type="button" data-cerrar>Volver</button>
            <button class="boton ${peligro ? 'boton--peligro' : 'boton--primario'}" type="button" data-aceptar>${esc(textoConfirmar)}</button>`,
      alCerrar: () => resolver(resultado),
    });
    modal.querySelector('[data-aceptar]').addEventListener('click', () => {
      resultado = true;
      cerrar();
    });
  });
}

export function pedirMotivo({ titulo, mensaje, textoConfirmar, peligro = true }) {
  return new Promise((resolver) => {
    let resultado = null;
    const { modal, cerrar } = abrirModal({
      titulo,
      contenido: `<form class="formulario" data-motivo novalidate>
          <p>${esc(mensaje)}</p>
          <div class="campo" data-campo="motivo">
            <label class="campo__etiqueta" for="motivo-modal">Motivo</label>
            <textarea class="entrada" id="motivo-modal" name="motivo" maxlength="300" required></textarea>
            <span class="campo__error"></span>
          </div>
        </form>`,
      pie: `<button class="boton boton--fantasma" type="button" data-cerrar>Volver</button>
            <button class="boton ${peligro ? 'boton--peligro' : 'boton--primario'}" type="button" data-aceptar>${esc(textoConfirmar)}</button>`,
      alCerrar: () => resolver(resultado),
    });
    const formulario = modal.querySelector('[data-motivo]');
    modal.querySelector('[data-aceptar]').addEventListener('click', () => {
      const valor = formulario.motivo.value.trim();
      if (!valor) {
        mostrarErrores(formulario, { motivo: 'Indica el motivo.' });
        return;
      }
      resultado = valor;
      cerrar();
    });
  });
}

export function limpiarErrores(formulario) {
  formulario.querySelectorAll('.campo--error').forEach((campo) => campo.classList.remove('campo--error'));
}

export function mostrarErrores(formulario, errores) {
  limpiarErrores(formulario);
  let primero = null;
  Object.entries(errores || {}).forEach(([nombre, mensaje]) => {
    const campo = formulario.querySelector(`[data-campo="${nombre}"]`);
    if (campo) {
      campo.classList.add('campo--error');
      campo.querySelector('.campo__error').textContent = mensaje;
      primero ??= campo.querySelector('input, select, textarea');
    }
  });
  primero?.focus();
}

export async function conCarga(boton, accion) {
  boton.disabled = true;
  boton.classList.add('boton--cargando');
  try {
    return await accion();
  } finally {
    boton.disabled = false;
    boton.classList.remove('boton--cargando');
  }
}

export async function enviarFormulario(formulario, boton, accion) {
  limpiarErrores(formulario);
  try {
    return await conCarga(boton, accion);
  } catch (error) {
    if (error.errores) {
      mostrarErrores(formulario, error.errores);
    } else {
      mostrarError(error);
    }
    return undefined;
  }
}

export const datosFormulario = (formulario) => Object.fromEntries(new FormData(formulario).entries());

export const consulta = (parametros) => {
  const busqueda = new URLSearchParams();
  Object.entries(parametros).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== null && valor !== '') {
      busqueda.set(clave, valor);
    }
  });
  const texto = busqueda.toString();
  return texto ? `?${texto}` : '';
};

export const vacio = ({ nombreIcono = 'buscar', titulo, texto = '', accion = '' }) => `
  <div class="vacio">${icono(nombreIcono)}
    <p class="vacio__titulo">${esc(titulo)}</p>
    ${texto ? `<p class="vacio__texto">${esc(texto)}</p>` : ''}${accion}
  </div>`;

export const errorCarga = (accion = 'data-reintentar') => vacio({
  nombreIcono: 'alerta',
  titulo: 'No se pudo cargar la información',
  texto: 'Revisa tu conexión e inténtalo de nuevo.',
  accion: `<button class="boton boton--secundario" type="button" ${accion}>Reintentar</button>`,
});

export const esqueletoTabla = (filas = 6, columnas = 5) => `
  <table class="tabla"><tbody>
    ${Array.from({ length: filas }, () => `<tr>${Array.from({ length: columnas }, (_, indice) =>
      `<td><div class="esqueleto esqueleto--linea ${indice % 2 ? 'esqueleto--media' : ''}"></div></td>`).join('')}</tr>`).join('')}
  </tbody></table>`;

export const esqueletoIndicadores = (cantidad = 4) => Array.from({ length: cantidad }, () => `
  <div class="indicador"><div class="esqueleto esqueleto--linea esqueleto--media"></div><div class="esqueleto esqueleto--titulo"></div></div>`).join('');

export const esqueletoHabitaciones = (cantidad = 6) => Array.from({ length: cantidad }, () => `
  <div class="habitacion">
    <div class="esqueleto esqueleto--foto"></div>
    <div class="esqueleto esqueleto--titulo"></div>
    <div class="esqueleto esqueleto--linea"></div>
    <div class="esqueleto esqueleto--linea esqueleto--media"></div>
  </div>`).join('');

export function paginar(contenedor, pagina, alCambiar) {
  if (!pagina || pagina.totalPaginas <= 1) {
    contenedor.innerHTML = '';
    return;
  }
  contenedor.innerHTML = `
    <nav class="paginacion" aria-label="Paginación">
      <span>Página ${pagina.pagina + 1} de ${pagina.totalPaginas}, ${pagina.totalElementos} resultados</span>
      <span class="paginacion__botones">
        <button class="boton boton--secundario boton--icono" type="button" data-pagina="${pagina.pagina - 1}" aria-label="Página anterior" ${pagina.pagina === 0 ? 'disabled' : ''}>${icono('flecha-izq')}</button>
        <button class="boton boton--secundario boton--icono" type="button" data-pagina="${pagina.pagina + 1}" aria-label="Página siguiente" ${pagina.pagina + 1 >= pagina.totalPaginas ? 'disabled' : ''}>${icono('flecha-der')}</button>
      </span>
    </nav>`;
  contenedor.querySelectorAll('[data-pagina]').forEach((boton) =>
    boton.addEventListener('click', () => alCambiar(Number(boton.dataset.pagina))));
}

const TONOS_ESTADO = {
  PENDIENTE: 'advertencia',
  PAGADO: 'info',
  CONFIRMADO: 'info',
  ENTREGADO: 'exito',
  FINALIZADO: 'neutra',
  CANCELADO: 'error',
  EXPIRADO: 'neutra',
};

export const insigniaEstado = (estado, etiqueta) =>
  `<span class="insignia insignia--${TONOS_ESTADO[estado] || 'neutra'}">${esc(etiqueta)}</span>`;

export function insigniaDisponibilidad(habitacion) {
  if (habitacion.estadoOperativo !== 'OPERATIVA') {
    return '<span class="insignia insignia--neutra">En mantenimiento</span>';
  }
  return habitacion.disponible
    ? '<span class="insignia insignia--exito">Disponible</span>'
    : '<span class="insignia insignia--error">Ocupada</span>';
}

export function actualizarContadorCarrito(cantidad) {
  document.querySelectorAll('[data-contador-carrito]').forEach((contador) => {
    contador.textContent = cantidad;
    contador.classList.toggle('oculto', !cantidad);
  });
}

function prepararInterfaz() {
  const botonMenu = document.querySelector('[data-alternar-menu]');
  botonMenu?.addEventListener('click', () => {
    const abierto = document.querySelector('[data-menu]').classList.toggle('barra__enlaces--abierto');
    botonMenu.setAttribute('aria-expanded', String(abierto));
  });
  document.querySelector('[data-alternar-lateral]')?.addEventListener('click', () =>
    document.querySelector('[data-lateral]').classList.toggle('lateral--abierto'));
  document.addEventListener('error', (evento) => {
    if (evento.target instanceof HTMLImageElement && evento.target.hasAttribute('data-foto')) {
      evento.target.classList.add('oculto');
    }
  }, true);
  if (document.querySelector('[data-contador-carrito]')) {
    api('/api/carrito').then((respuesta) => actualizarContadorCarrito(respuesta.datos.cantidadItems)).catch(() => {});
  }
  const parametros = new URLSearchParams(location.search);
  if (parametros.get('sesion') === 'cerrada') {
    toast('Cerraste sesión.', 'info');
  }
}

prepararInterfaz();
