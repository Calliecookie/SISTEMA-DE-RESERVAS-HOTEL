import { abrirModal, api, consulta, datosFormulario, debounce, errorCarga, esc, esqueletoHabitaciones, fechaLocal, icono, moneda, mostrarError, paginar, ruta, toast, urlImagen, vacio } from './nucleo.js';
import { agregarAlCarrito, tarjetaHabitacion } from './habitaciones-vista.js';

const filtros = document.querySelector('[data-filtros]');
const resultados = document.querySelector('[data-resultados]');
const resumen = document.querySelector('[data-resumen]');
const paginacion = document.querySelector('[data-paginacion]');
const orden = document.querySelector('[data-orden]');
let controlador = null;
const MAXIMO_COMPARAR = 4;
const seleccion = new Set();
const barraComparar = document.createElement('div');
barraComparar.className = 'comparador-barra';
barraComparar.setAttribute('role', 'region');
barraComparar.setAttribute('aria-label', 'Comparador de habitaciones');
document.body.append(barraComparar);

function pintarBarraComparar() {
  barraComparar.classList.toggle('comparador-barra--visible', seleccion.size > 0);
  barraComparar.innerHTML = `<span>${seleccion.size} de ${MAXIMO_COMPARAR} para comparar</span>
    <button class="boton boton--fantasma boton--compacto" type="button" data-limpiar-comparar>Quitar</button>
    <button class="boton boton--primario boton--compacto" type="button" data-abrir-comparar ${seleccion.size < 2 ? 'disabled' : ''}>Comparar</button>`;
}

function fechasDeComparacion() {
  const entrada = filtros.fechaEntrada.value;
  const salida = filtros.fechaSalida.value;
  return entrada && salida ? { fechaEntrada: entrada, fechaSalida: salida } : { fechaEntrada: fechaLocal(1), fechaSalida: fechaLocal(2) };
}

async function abrirComparador() {
  const fechas = fechasDeComparacion();
  try {
    const { datos } = await api(`/api/publico/cotizacion${consulta({ ids: [...seleccion].join(','), ...fechas })}`);
    const lineas = datos.lineas;
    const menorTotal = Math.min(...lineas.map((linea) => Number(linea.total)));
    const mayorCapacidad = Math.max(...lineas.map((linea) => linea.habitacion.capacidad));
    const rasgos = [...new Map(lineas.flatMap((linea) => linea.habitacion.caracteristicas).map((rasgo) => [rasgo.id, rasgo])).values()]
      .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
    const fila = (titulo, celda) => `<tr><td>${titulo}</td>${lineas.map(celda).join('')}</tr>`;
    abrirModal({
      titulo: 'Comparar habitaciones',
      ancho: true,
      contenido: `<p class="texto-sm texto-suave">Precios para ${datos.noches} ${datos.noches === 1 ? 'noche' : 'noches'}, del ${fechas.fechaEntrada.split('-').reverse().join('/')} al ${fechas.fechaSalida.split('-').reverse().join('/')}.</p>
        <div class="tabla-envoltura"><table class="comparador-tabla">
          <thead><tr><th></th>${lineas.map((linea) => `<th><img class="comparador-tabla__foto" data-foto src="${esc(urlImagen(linea.habitacion.imagen))}" alt="" loading="lazy">
            <a class="comparador-tabla__nombre" href="${ruta(`/habitaciones/${linea.habitacion.id}${consulta(fechas)}`)}">${esc(linea.habitacion.nombre)}</a></th>`).join('')}</tr></thead>
          <tbody>
            ${fila('Categoría', (linea) => `<td>${esc(linea.habitacion.categoria)}</td>`)}
            ${fila('Capacidad', (linea) => `<td class="${linea.habitacion.capacidad === mayorCapacidad && lineas.some((otra) => otra.habitacion.capacidad !== mayorCapacidad) ? 'mejor' : ''}">${linea.habitacion.capacidad} ${linea.habitacion.capacidad === 1 ? 'huésped' : 'huéspedes'}</td>`)}
            ${fila('Precio por noche', (linea) => `<td>${moneda(linea.habitacion.precioNoche)}</td>`)}
            ${fila('IGV', (linea) => `<td>${moneda(linea.impuesto)}</td>`)}
            ${fila('Total de la estancia', (linea) => `<td class="${Number(linea.total) === menorTotal ? 'mejor' : ''}">${moneda(linea.total)}</td>`)}
            ${fila('En tus fechas', (linea) => `<td>${linea.habitacion.disponible ? '<span class="insignia insignia--exito">Disponible</span>' : '<span class="insignia insignia--error">No disponible</span>'}</td>`)}
            ${rasgos.map((rasgo) => fila(esc(rasgo.nombre), (linea) => `<td>${linea.habitacion.caracteristicas.some((item) => item.id === rasgo.id) ? icono('check') : '<span class="ausente">—</span>'}</td>`)).join('')}
          </tbody></table></div>`,
      pie: '<button class="boton boton--primario" type="button" data-cerrar>Listo</button>',
    });
  } catch (error) {
    mostrarError(error);
  }
}

barraComparar.addEventListener('click', (evento) => {
  if (evento.target.closest('[data-limpiar-comparar]')) {
    seleccion.clear();
    resultados.querySelectorAll('[data-comparar]').forEach((casilla) => { casilla.checked = false; });
    pintarBarraComparar();
  }
  if (evento.target.closest('[data-abrir-comparar]')) {
    abrirComparador();
  }
});

function aplicarParametrosUrl() {
  const parametros = new URLSearchParams(location.search);
  ['fechaEntrada', 'fechaSalida', 'capacidad', 'categoriaId', 'busqueda'].forEach((nombre) => {
    if (parametros.get(nombre) && filtros.elements[nombre]) {
      filtros.elements[nombre].value = parametros.get(nombre);
    }
  });
  if (parametros.get('fechaEntrada')) {
    filtros.soloDisponibles.checked = true;
  }
}

function filtroActual(pagina) {
  const datos = datosFormulario(filtros);
  const unaFecha = Boolean(datos.fechaEntrada) !== Boolean(datos.fechaSalida);
  if (unaFecha) {
    delete datos.fechaEntrada;
    delete datos.fechaSalida;
  }
  return { ...datos, orden: orden.value, pagina, tamano: 8 };
}

async function buscar(pagina = 0) {
  controlador?.abort();
  controlador = new AbortController();
  resultados.innerHTML = esqueletoHabitaciones(4);
  resumen.textContent = 'Buscando habitaciones';
  try {
    const respuesta = await api(`/api/publico/habitaciones${consulta(filtroActual(pagina))}`, { senal: controlador.signal });
    const datos = respuesta.datos;
    resumen.textContent = `${datos.totalElementos} ${datos.totalElementos === 1 ? 'habitación encontrada' : 'habitaciones encontradas'}`;
    resultados.innerHTML = datos.elementos.length
      ? datos.elementos.map((habitacion) => tarjetaHabitacion(habitacion, { comparar: seleccion })).join('')
      : vacio({
        titulo: 'Ninguna habitación coincide',
        texto: 'Prueba con otras fechas o quita algunos filtros.',
        accion: '<button class="boton boton--secundario" type="button" data-limpiar>Limpiar filtros</button>',
      });
    paginar(paginacion, datos, (numero) => {
      buscar(numero);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  } catch (error) {
    if (error.name === 'AbortError') {
      return;
    }
    resumen.textContent = '';
    if (error.estado === 400) {
      toast(error.message, 'advertencia');
      resultados.innerHTML = vacio({ nombreIcono: 'calendario', titulo: 'Revisa los filtros', texto: error.message });
    } else {
      resultados.innerHTML = errorCarga();
    }
    paginacion.innerHTML = '';
  }
}

resultados.addEventListener('change', (evento) => {
  const casilla = evento.target.closest('[data-comparar]');
  if (!casilla) {
    return;
  }
  const habitacionId = Number(casilla.dataset.comparar);
  if (casilla.checked && seleccion.size >= MAXIMO_COMPARAR) {
    casilla.checked = false;
    toast(`Puedes comparar hasta ${MAXIMO_COMPARAR} habitaciones.`, 'advertencia');
    return;
  }
  if (casilla.checked) {
    seleccion.add(habitacionId);
  } else {
    seleccion.delete(habitacionId);
  }
  pintarBarraComparar();
});

const buscarConPausa = debounce(() => buscar(0), 300);
filtros.addEventListener('input', buscarConPausa);
filtros.addEventListener('submit', (evento) => evento.preventDefault());
filtros.addEventListener('reset', () => setTimeout(() => buscar(0), 0));
orden.addEventListener('change', () => buscar(0));

resultados.addEventListener('click', (evento) => {
  const reservar = evento.target.closest('[data-reservar]');
  if (reservar) {
    agregarAlCarrito(reservar, {
      fechaEntrada: filtros.fechaEntrada.value,
      fechaSalida: filtros.fechaSalida.value,
      huespedes: filtros.capacidad.value,
    });
  }
  if (evento.target.closest('[data-limpiar]')) {
    filtros.reset();
  }
  if (evento.target.closest('[data-reintentar]')) {
    buscar(0);
  }
});

async function iniciar() {
  filtros.fechaEntrada.min = fechaLocal();
  filtros.fechaSalida.min = fechaLocal(1);
  try {
    const respuesta = await api('/api/publico/categorias');
    const selector = filtros.categoriaId;
    respuesta.datos.forEach((categoria) => selector.add(new Option(categoria.nombre, categoria.id)));
  } catch {
    toast('No se pudieron cargar las categorías.', 'advertencia', { href: ruta('/habitaciones'), texto: 'Recargar' });
  }
  aplicarParametrosUrl();
  buscar(0);
}

iniciar();
