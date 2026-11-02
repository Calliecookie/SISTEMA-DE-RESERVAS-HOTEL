import { api, consulta, datosFormulario, debounce, errorCarga, esqueletoHabitaciones, fechaLocal, paginar, ruta, toast, vacio } from './nucleo.js';
import { agregarAlCarrito, tarjetaHabitacion } from './habitaciones-vista.js';

const filtros = document.querySelector('[data-filtros]');
const resultados = document.querySelector('[data-resultados]');
const resumen = document.querySelector('[data-resumen]');
const paginacion = document.querySelector('[data-paginacion]');
const orden = document.querySelector('[data-orden]');
let controlador = null;

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
      ? datos.elementos.map(tarjetaHabitacion).join('')
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
