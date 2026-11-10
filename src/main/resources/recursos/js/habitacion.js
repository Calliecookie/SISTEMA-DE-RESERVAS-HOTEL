import { api, consulta, debounce, esc, fecha, fechaLocal, icono, insigniaDisponibilidad, moneda, mostrarError, ruta, urlImagen, vacio } from './nucleo.js';
import { agregarAlCarrito } from './habitaciones-vista.js';

const ficha = document.querySelector('[data-ficha]');
const id = document.body.dataset.habitacionId;
const NOMBRES_DIA = ['lu', 'ma', 'mi', 'ju', 'vi', 'sá', 'do'];
const DIAS_CALENDARIO = 62;

ficha.innerHTML = `
  <div class="esqueleto esqueleto--foto"></div>
  <div class="resumen"><div class="esqueleto esqueleto--titulo"></div><div class="esqueleto esqueleto--linea"></div><div class="esqueleto esqueleto--linea esqueleto--media"></div><div class="esqueleto esqueleto--bloque"></div></div>`;

const siguiente = (iso, dias = 1) => {
  const base = new Date(`${iso}T12:00:00`);
  base.setDate(base.getDate() + dias);
  return `${base.getFullYear()}-${String(base.getMonth() + 1).padStart(2, '0')}-${String(base.getDate()).padStart(2, '0')}`;
};

function pintar(habitacion) {
  document.title = `${habitacion.nombre} | ${document.title.split('|').pop().trim()}`;
  ficha.innerHTML = `
    <div>
      <div class="ficha__marco"><img data-foto src="${esc(urlImagen(habitacion.imagen))}" alt="${esc(habitacion.nombre)}" width="960" height="720" decoding="async"></div>
      <ul class="ficha__rasgos">${habitacion.caracteristicas.map((rasgo) => `<li>${icono(rasgo.icono)}${esc(rasgo.nombre)}</li>`).join('')}</ul>
      <section class="ficha__calendario">
        <h2 class="ficha__seccion-titulo">Disponibilidad</h2>
        <div class="superficie superficie--relleno calendario" data-calendario aria-live="polite"><div class="esqueleto esqueleto--grafico"></div></div>
      </section>
    </div>
    <div class="formulario">
      <p class="habitacion__dato">${esc(habitacion.categoria)}, habitación ${esc(habitacion.numero)}</p>
      <h1 class="ficha__titulo">${esc(habitacion.nombre)}</h1>
      <p>${insigniaDisponibilidad(habitacion)}</p>
      <p>${esc(habitacion.descripcion)}</p>
      <p class="habitacion__precio">${moneda(habitacion.precioNoche)} <small>por noche + IGV, hasta ${habitacion.capacidad} ${habitacion.capacidad === 1 ? 'huésped' : 'huéspedes'}</small></p>
      <form class="superficie superficie--relleno formulario" data-disponibilidad novalidate>
        <div class="formulario__fila formulario__fila--2">
          <label class="campo"><span class="campo__etiqueta">Entrada</span><input class="entrada" type="date" name="fechaEntrada" min="${fechaLocal()}" required></label>
          <label class="campo"><span class="campo__etiqueta">Salida</span><input class="entrada" type="date" name="fechaSalida" min="${fechaLocal(1)}" required></label>
        </div>
        <label class="campo"><span class="campo__etiqueta">Huéspedes</span>
          <select class="entrada" name="huespedes">${Array.from({ length: habitacion.capacidad }, (_, indice) => `<option value="${indice + 1}">${indice + 1}</option>`).join('')}</select></label>
        <div data-resultado aria-live="polite"><p class="texto-sm texto-suave">Elige tus fechas en el calendario o en los campos para ver el precio con IGV.</p></div>
        <button class="boton boton--acento boton--ancho" type="button" data-reservar="${habitacion.id}" data-capacidad="${habitacion.capacidad}" disabled>Añadir a mi reserva</button>
      </form>
      <div data-sugerencias aria-live="polite"></div>
    </div>`;
  const formulario = ficha.querySelector('[data-disponibilidad]');
  const resultado = ficha.querySelector('[data-resultado]');
  const reservar = ficha.querySelector('[data-reservar]');
  const sugerencias = ficha.querySelector('[data-sugerencias]');
  const calendario = ficha.querySelector('[data-calendario]');
  let dias = [];
  let mesVisible = 0;

  function mesesDisponibles() {
    return [...new Set(dias.map((dia) => dia.fecha.slice(0, 7)))];
  }

  function pintarCalendario() {
    const meses = mesesDisponibles();
    const mes = meses[mesVisible];
    const delMes = dias.filter((dia) => dia.fecha.startsWith(mes));
    const primero = new Date(`${delMes[0].fecha}T12:00:00`);
    const hueco = (primero.getDay() + 6) % 7;
    const entrada = formulario.fechaEntrada.value;
    const salida = formulario.fechaSalida.value;
    const textoMes = new Date(`${mes}-15T12:00:00`).toLocaleDateString('es-PE', { month: 'long', year: 'numeric' });
    const titulo = textoMes.charAt(0).toUpperCase() + textoMes.slice(1);
    calendario.innerHTML = `
      <div class="calendario__cabecera">
        <button class="boton boton--secundario boton--icono" type="button" data-mes="-1" aria-label="Mes anterior" ${mesVisible === 0 ? 'disabled' : ''}>${icono('flecha-izq')}</button>
        <span class="calendario__mes">${esc(titulo)}</span>
        <button class="boton boton--secundario boton--icono" type="button" data-mes="1" aria-label="Mes siguiente" ${mesVisible >= meses.length - 1 ? 'disabled' : ''}>${icono('flecha-der')}</button>
      </div>
      <div class="calendario__rejilla">
        ${NOMBRES_DIA.map((nombre) => `<span class="calendario__nombre">${nombre}</span>`).join('')}
        ${'<span></span>'.repeat(hueco)}
        ${delMes.map((dia) => {
          const enRango = entrada && salida && dia.fecha > entrada && dia.fecha < salida;
          const extremo = dia.fecha === entrada || (salida && dia.fecha === salida);
          const clase = extremo ? 'calendario__dia--extremo' : enRango ? 'calendario__dia--rango' : dia.disponible ? 'calendario__dia--libre' : 'calendario__dia--ocupado';
          const elegible = dia.disponible || dia.fecha === salida || (entrada && !salida && dia.fecha > entrada);
          return `<button class="calendario__dia ${clase}" type="button" data-dia="${dia.fecha}" ${elegible ? '' : 'disabled'}
            aria-label="${fecha(dia.fecha)}, ${dia.disponible ? 'disponible' : 'ocupada'}">${Number(dia.fecha.slice(8, 10))}</button>`;
        }).join('')}
      </div>
      <div class="calendario__leyenda">
        <span><i class="calendario__muestra calendario__muestra--libre"></i>Disponible</span>
        <span><i class="calendario__muestra calendario__muestra--ocupado"></i>Ocupada</span>
        <span><i class="calendario__muestra calendario__muestra--rango"></i>Tu estancia</span>
      </div>`;
  }

  async function cargarCalendario() {
    try {
      const { datos } = await api(`/api/publico/habitaciones/${id}/calendario?dias=${DIAS_CALENDARIO}`);
      dias = datos.dias;
      pintarCalendario();
    } catch {
      calendario.innerHTML = '<p class="texto-sm texto-suave">No se pudo cargar el calendario. Usa los campos de fecha.</p>';
    }
  }

  function pintarSugerencias(datos, parametros) {
    if (datos.disponible) {
      sugerencias.innerHTML = '';
      return;
    }
    const proxima = datos.proximaEntrada
      ? `<div class="aviso aviso--info">${icono('calendario')}<span>Esta habitación vuelve a estar libre para ${datos.noches} ${datos.noches === 1 ? 'noche' : 'noches'} del <strong>${fecha(datos.proximaEntrada)}</strong> al <strong>${fecha(datos.proximaSalida)}</strong>.
          <button class="enlace" type="button" data-usar-fechas="${datos.proximaEntrada}|${datos.proximaSalida}">Usar esas fechas</button></span></div>`
      : '';
    const alternativas = datos.alternativas.length
      ? `<h2 class="ficha__seccion-titulo">Disponibles en tus fechas</h2><div class="alternativas">${datos.alternativas.map((alternativa) => `
          <a class="alternativa" href="${ruta(`/habitaciones/${alternativa.habitacion.id}${consulta(parametros)}`)}">
            <img class="alternativa__foto" data-foto src="${esc(urlImagen(alternativa.habitacion.imagen))}" alt="" loading="lazy" width="176" height="132">
            <div><span class="alternativa__nombre">${esc(alternativa.habitacion.nombre)}</span> <span class="afinidad">${alternativa.afinidad} % afín</span>
              <p class="habitacion__precio">${moneda(alternativa.habitacion.precioNoche)} <small>por noche + IGV</small></p>
              <p class="alternativa__motivos">${alternativa.motivos.map(esc).join(' · ')}</p></div>
          </a>`).join('')}</div>`
      : '';
    sugerencias.innerHTML = `<div class="formulario aparece">${proxima}${alternativas}</div>`;
  }

  async function cotizar() {
    const fechaEntrada = formulario.fechaEntrada.value;
    const fechaSalida = formulario.fechaSalida.value;
    if (dias.length) {
      pintarCalendario();
    }
    if (!fechaEntrada || !fechaSalida) {
      reservar.disabled = true;
      return;
    }
    const parametros = { fechaEntrada, fechaSalida };
    resultado.innerHTML = '<div class="esqueleto esqueleto--bloque"></div>';
    try {
      const { datos } = await api(`/api/publico/cotizacion${consulta({ ids: id, ...parametros })}`);
      const resumen = datos.resumen;
      const disponible = datos.todasDisponibles;
      const pista = datos.nochesParaDescuento > 0
        ? `<p class="cotizador__pista">${icono('destello')}<span>Añade ${datos.nochesParaDescuento} ${datos.nochesParaDescuento === 1 ? 'noche' : 'noches'} más y obtén ${Number(datos.porcentajeDescuentoDisponible)} % de descuento: ahorrarías ${moneda(datos.ahorroPotencial)}.</span></p>`
        : '';
      resultado.innerHTML = `
        <div class="aviso aviso--${disponible ? 'exito' : 'error'}">${icono(disponible ? 'check-circulo' : 'x-circulo')}
          <span>${disponible ? 'Disponible para tus fechas.' : 'No está disponible en esas fechas.'}</span></div>
        <div class="cotizador aparece" data-cotizacion>
          <div class="cotizador__fila"><span>${moneda(habitacion.precioNoche)} × ${datos.noches} ${datos.noches === 1 ? 'noche' : 'noches'}</span><span>${moneda(resumen.subtotal)}</span></div>
          ${Number(resumen.descuento) > 0 ? `<div class="cotizador__fila cotizador__fila--ahorro"><span>Descuento por estadía larga (${Number(resumen.porcentajeDescuento)} %)</span><span>- ${moneda(resumen.descuento)}</span></div>` : ''}
          <div class="cotizador__fila"><span>IGV (${Number(resumen.porcentajeImpuesto)} %)</span><span>${moneda(resumen.impuesto)}</span></div>
          <div class="cotizador__fila cotizador__fila--total"><span>Total</span><span>${moneda(resumen.total)}</span></div>
          <div class="cotizador__fila"><span class="texto-suave">Promedio por noche con IGV</span><span class="texto-suave">${moneda(datos.promedioPorNoche)}</span></div>
          ${pista}
        </div>`;
      reservar.disabled = !disponible;
      if (disponible) {
        sugerencias.innerHTML = '';
      } else {
        const respuesta = await api(`/api/publico/habitaciones/${id}/sugerencias${consulta(parametros)}`);
        pintarSugerencias(respuesta.datos, parametros);
      }
    } catch (error) {
      resultado.innerHTML = `<div class="aviso aviso--error">${icono('alerta')}<span>${esc(error.message)}</span></div>`;
      sugerencias.innerHTML = '';
      reservar.disabled = true;
    }
  }

  const cotizarConPausa = debounce(cotizar, 250);

  calendario.addEventListener('click', (evento) => {
    const mes = evento.target.closest('[data-mes]');
    if (mes) {
      mesVisible += Number(mes.dataset.mes);
      pintarCalendario();
      return;
    }
    const dia = evento.target.closest('[data-dia]');
    if (!dia) {
      return;
    }
    const elegido = dia.dataset.dia;
    const entrada = formulario.fechaEntrada.value;
    const salida = formulario.fechaSalida.value;
    if (!entrada || salida || elegido <= entrada) {
      formulario.fechaEntrada.value = elegido;
      formulario.fechaSalida.value = '';
    } else {
      formulario.fechaSalida.value = elegido;
    }
    cotizar();
  });

  sugerencias.addEventListener('click', (evento) => {
    const boton = evento.target.closest('[data-usar-fechas]');
    if (boton) {
      const [entrada, salida] = boton.dataset.usarFechas.split('|');
      formulario.fechaEntrada.value = entrada;
      formulario.fechaSalida.value = salida;
      cotizar();
    }
  });

  formulario.addEventListener('change', () => {
    if (formulario.fechaEntrada.value && formulario.fechaSalida.value && formulario.fechaSalida.value <= formulario.fechaEntrada.value) {
      formulario.fechaSalida.value = siguiente(formulario.fechaEntrada.value);
    }
    cotizarConPausa();
  });
  reservar.addEventListener('click', () => agregarAlCarrito(reservar, {
    fechaEntrada: formulario.fechaEntrada.value,
    fechaSalida: formulario.fechaSalida.value,
    huespedes: formulario.huespedes.value,
  }));

  const iniciales = new URLSearchParams(location.search);
  if (iniciales.get('fechaEntrada') && iniciales.get('fechaSalida')) {
    formulario.fechaEntrada.value = iniciales.get('fechaEntrada');
    formulario.fechaSalida.value = iniciales.get('fechaSalida');
  }
  cargarCalendario().then(cotizar);
}

api(`/api/publico/habitaciones/${id}`)
  .then((respuesta) => pintar(respuesta.datos))
  .catch((error) => {
    if (error.estado === 404) {
      ficha.innerHTML = vacio({
        nombreIcono: 'puerta',
        titulo: 'Esta habitación no está disponible',
        texto: 'Puede que ya no forme parte del catálogo.',
        accion: `<a class="boton boton--primario" href="${ruta('/habitaciones')}">Ver habitaciones</a>`,
      });
    } else {
      mostrarError(error);
    }
  });
