import { alternarTema, api, debounce, esc, icono, rol, ruta } from './nucleo.js';

const ADMIN = rol === 'ADMINISTRADOR';
const base = ADMIN ? '/administracion' : '/trabajador';

const SECCIONES = [
  ADMIN && { titulo: 'Dashboard', detalle: 'Indicadores y ventas', icono: 'tablero', href: '/administracion' },
  { titulo: 'Operación del día', detalle: 'Pagos, llegadas y salidas', icono: 'conserje', href: '/trabajador' },
  { titulo: 'Mapa de habitaciones', detalle: 'Estado en vivo y planificador', icono: 'mapa', href: `${base}/monitor` },
  { titulo: 'Reservas', detalle: 'Listado y cambio de estado', icono: 'lista', href: `${base}/reservas` },
  ADMIN && { titulo: 'Ventas', detalle: 'Ventas efectuadas y su detalle', icono: 'recibo', href: '/administracion/ventas' },
  ADMIN && { titulo: 'Reportes', detalle: 'Nueve reportes con PDF y Excel', icono: 'grafico', href: '/administracion/reportes' },
  ADMIN && { titulo: 'Habitaciones', detalle: 'Catálogo y estados', icono: 'cama', href: '/administracion/habitaciones' },
  ADMIN && { titulo: 'Categorías', detalle: 'Tipos de habitación', icono: 'etiquetas', href: '/administracion/categorias' },
  ADMIN && { titulo: 'Clientes', detalle: 'Huéspedes registrados', icono: 'usuarios', href: '/administracion/clientes' },
  ADMIN && { titulo: 'Usuarios', detalle: 'Personal y roles', icono: 'llave', href: '/administracion/usuarios' },
  ADMIN && { titulo: 'Configuración', detalle: 'Datos del hotel y pagos', icono: 'ajustes', href: '/administracion/configuracion' },
  ADMIN && { titulo: 'Auditoría', detalle: 'Bitácora de acciones', icono: 'historial', href: '/administracion/auditoria' },
  { titulo: 'Sitio público', detalle: 'Ver el catálogo como cliente', icono: 'casa', href: '/habitaciones' },
  { titulo: 'Cambiar tema', detalle: 'Alternar entre claro y oscuro', icono: 'luna', accion: alternarTema },
].filter(Boolean);

const ICONOS = { RESERVA: 'recibo', HABITACION: 'cama', CLIENTE: 'usuario' };
const GRUPOS = { RESERVA: 'Reservas', HABITACION: 'Habitaciones', CLIENTE: 'Clientes' };

let dialogo = null;
let entrada = null;
let lista = null;
let opciones = [];
let seleccion = 0;
let controlador = null;

const normalizar = (texto) => texto.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');

function ejecutar(opcion) {
  cerrar();
  if (opcion.accion) {
    opcion.accion();
    return;
  }
  if (opcion.tipo === 'RESERVA') {
    import('./detalle-reserva.js').then((modulo) => modulo.abrirDetalleReserva(opcion.referencia));
    return;
  }
  if (opcion.tipo === 'HABITACION') {
    location.assign(ruta(ADMIN
      ? `/administracion/habitaciones?busqueda=${encodeURIComponent(opcion.referencia)}`
      : `/trabajador/monitor?habitacion=${encodeURIComponent(opcion.referencia)}`));
    return;
  }
  if (opcion.tipo === 'CLIENTE') {
    location.assign(ruta(`/administracion/clientes?busqueda=${encodeURIComponent(opcion.referencia)}`));
    return;
  }
  location.assign(ruta(opcion.href));
}

function pintar(grupos) {
  opciones = grupos.flatMap((grupo) => grupo.items);
  seleccion = Math.min(seleccion, Math.max(opciones.length - 1, 0));
  if (!opciones.length) {
    lista.innerHTML = '<p class="paleta__vacio">Sin resultados. Prueba con un código de reserva, un número de habitación o un nombre.</p>';
    return;
  }
  let indice = 0;
  lista.innerHTML = grupos.filter((grupo) => grupo.items.length).map((grupo) => `
    <p class="paleta__grupo">${esc(grupo.nombre)}</p>
    ${grupo.items.map((item) => {
      const actual = indice;
      indice += 1;
      return `<button class="paleta__opcion" type="button" role="option" aria-selected="${actual === seleccion}" data-indice="${actual}">
        ${icono(item.icono)}<span class="paleta__textos"><span class="paleta__titulo">${esc(item.titulo)}</span><span class="paleta__detalle">${esc(item.detalle || '')}</span></span></button>`;
    }).join('')}`).join('');
}

function marcar() {
  lista.querySelectorAll('[data-indice]').forEach((boton) => {
    const activo = Number(boton.dataset.indice) === seleccion;
    boton.setAttribute('aria-selected', String(activo));
    if (activo) {
      boton.scrollIntoView({ block: 'nearest' });
    }
  });
}

function seccionesFiltradas(texto) {
  const consulta = normalizar(texto);
  return SECCIONES.filter((item) => !consulta || normalizar(`${item.titulo} ${item.detalle}`).includes(consulta));
}

const buscarRemoto = debounce(async (texto) => {
  controlador?.abort();
  controlador = new AbortController();
  try {
    const { datos } = await api(`/api/operaciones/buscar?q=${encodeURIComponent(texto)}`, { senal: controlador.signal });
    if (entrada.value.trim() !== texto) {
      return;
    }
    const grupos = Object.keys(GRUPOS).map((tipo) => ({
      nombre: GRUPOS[tipo],
      items: datos.filter((item) => item.tipo === tipo).map((item) => ({ ...item, icono: ICONOS[tipo] })),
    }));
    pintar([...grupos, { nombre: 'Ir a', items: seccionesFiltradas(texto) }]);
  } catch (error) {
    if (error.name !== 'AbortError') {
      pintar([{ nombre: 'Ir a', items: seccionesFiltradas(texto) }]);
    }
  }
}, 180);

function actualizar() {
  const texto = entrada.value.trim();
  seleccion = 0;
  pintar([{ nombre: 'Ir a', items: seccionesFiltradas(texto) }]);
  if (texto.length >= 2) {
    buscarRemoto(texto);
  }
}

function cerrar() {
  dialogo?.close();
}

function construir() {
  dialogo = document.createElement('dialog');
  dialogo.className = 'paleta';
  dialogo.setAttribute('aria-label', 'Búsqueda rápida');
  dialogo.innerHTML = `
    <div class="paleta__campo">${icono('buscar')}<input class="paleta__entrada" type="search" placeholder="Busca una reserva, habitación${ADMIN ? ', cliente' : ''} o sección" maxlength="60" autocomplete="off" aria-label="Buscar"><kbd>Esc</kbd></div>
    <div class="paleta__lista" role="listbox"></div>
    <div class="paleta__pie"><span><kbd>↑</kbd><kbd>↓</kbd> navegar</span><span><kbd>Enter</kbd> abrir</span><span><kbd>Ctrl K</kbd> mostrar u ocultar</span></div>`;
  document.body.append(dialogo);
  entrada = dialogo.querySelector('.paleta__entrada');
  lista = dialogo.querySelector('.paleta__lista');
  entrada.addEventListener('input', actualizar);
  entrada.addEventListener('keydown', (evento) => {
    if (evento.key === 'ArrowDown' || evento.key === 'ArrowUp') {
      evento.preventDefault();
      if (opciones.length) {
        seleccion = (seleccion + (evento.key === 'ArrowDown' ? 1 : opciones.length - 1)) % opciones.length;
        marcar();
      }
    } else if (evento.key === 'Enter' && opciones[seleccion]) {
      evento.preventDefault();
      ejecutar(opciones[seleccion]);
    }
  });
  lista.addEventListener('click', (evento) => {
    const boton = evento.target.closest('[data-indice]');
    if (boton) {
      ejecutar(opciones[Number(boton.dataset.indice)]);
    }
  });
  lista.addEventListener('mousemove', (evento) => {
    const boton = evento.target.closest('[data-indice]');
    if (boton && Number(boton.dataset.indice) !== seleccion) {
      seleccion = Number(boton.dataset.indice);
      marcar();
    }
  });
  dialogo.addEventListener('click', (evento) => {
    if (evento.target === dialogo) {
      cerrar();
    }
  });
}

export function abrirPaleta() {
  if (!dialogo) {
    construir();
  }
  if (dialogo.open) {
    cerrar();
    return;
  }
  entrada.value = '';
  dialogo.showModal();
  actualizar();
  entrada.focus();
}

export function iniciarPaleta() {
  document.addEventListener('keydown', (evento) => {
    if ((evento.ctrlKey || evento.metaKey) && evento.key.toLowerCase() === 'k') {
      evento.preventDefault();
      abrirPaleta();
    }
  });
  document.querySelectorAll('[data-abrir-paleta]').forEach((boton) => boton.addEventListener('click', abrirPaleta));
}
