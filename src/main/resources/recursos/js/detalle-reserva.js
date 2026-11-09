import { abrirModal, api, confirmar, conCarga, esc, fecha, fechaHora, insigniaEstado, moneda, mostrarError, pedirMotivo, ruta, toast } from './nucleo.js';

const ETIQUETAS = {
  CONFIRMAR_PAGO: 'Confirmar pago',
  RECHAZAR_PAGO: 'Rechazar pago',
  CONFIRMAR: 'Confirmar reserva',
  INGRESO: 'Registrar ingreso',
  SALIDA: 'Registrar salida',
  CANCELAR: 'Cancelar reserva',
};

const ETAPAS = [
  ['PENDIENTE', 'Pendiente de pago'],
  ['PAGADO', 'Pagado'],
  ['CONFIRMADO', 'Confirmado'],
  ['ENTREGADO', 'Entregado (huésped en el hotel)'],
  ['FINALIZADO', 'Finalizado'],
];

const ETIQUETAS_ESTADO = {
  PAGADO: 'Pagado',
  CONFIRMADO: 'Confirmado',
  ENTREGADO: 'Entregado (registrar ingreso)',
  FINALIZADO: 'Finalizado (registrar salida)',
  CANCELADO: 'Cancelado',
};

export function seguimientoReserva(reserva) {
  if (reserva.estado === 'CANCELADO' || reserva.estado === 'EXPIRADO') {
    const texto = reserva.estado === 'CANCELADO'
      ? 'Esta reserva fue cancelada.'
      : 'El plazo de pago venció y la reserva se dio de baja.';
    return `<div class="aviso aviso--advertencia" role="status">${texto}</div>`;
  }
  const actual = ETAPAS.findIndex(([estado]) => estado === reserva.estado);
  return `<ol class="pasos seguimiento" aria-label="Avance de la reserva">${ETAPAS.map(([estado, etiqueta], indice) => {
    const clase = indice === actual ? 'pasos__item--actual' : indice < actual ? 'pasos__item--hecho' : '';
    return `<li class="pasos__item ${clase}" ${indice === actual ? 'aria-current="step"' : ''}><span class="pasos__numero">Paso ${indice + 1}</span>${etiqueta}</li>`;
  }).join('')}</ol>`;
}

export function contenidoDetalle(reserva) {
  const lineas = reserva.lineas.map((linea) => `
    <tr><td>${esc(linea.numero)}</td><td>${esc(linea.nombre)}</td><td>${esc(linea.categoria)}</td>
    <td class="derecha">${linea.huespedes}</td><td class="derecha">${linea.noches}</td>
    <td class="derecha">${moneda(linea.precioNoche)}</td><td class="derecha">${moneda(linea.subtotal)}</td></tr>`).join('');
  const pagos = reserva.pagos.length ? `<table class="tabla"><thead><tr><th>Método</th><th>Operación</th><th>Estado</th><th>Fecha</th><th class="derecha">Monto</th></tr></thead><tbody>
    ${reserva.pagos.map((pago) => `<tr><td>${esc(pago.metodoEtiqueta)}</td><td>${esc(pago.numeroOperacion || 'Sin número')}</td>
      <td>${esc(pago.estadoEtiqueta)}${pago.motivoRechazo ? `<br><span class="texto-suave">${esc(pago.motivoRechazo)}</span>` : ''}</td>
      <td>${fechaHora(pago.fechaPago || pago.creadoEn)}</td>
      <td class="derecha">${moneda(pago.monto)}${pago.montoExterno ? `<br><span class="texto-suave">${esc(pago.monedaExterna)} ${Number(pago.montoExterno).toFixed(2)}</span>` : ''}</td></tr>`).join('')}
    </tbody></table>` : '<p class="texto-sm texto-suave">Aún no hay pagos registrados.</p>';
  const comprobantes = reserva.comprobantes.length ? `<ul class="linea-tiempo">${reserva.comprobantes.map((comprobante) => `
    <li><a class="enlace" href="${ruta(comprobante.url)}" target="_blank" rel="noopener">${esc(comprobante.numero)}</a>
    <br><span class="texto-suave">${esc(comprobante.estadoPago)}, ${fechaHora(comprobante.fechaEmision)}. Correo: ${esc(comprobante.correoEstadoEtiqueta)}</span></li>`).join('')}</ul>`
    : '<p class="texto-sm texto-suave">El comprobante se está generando.</p>';
  const historial = `<ul class="linea-tiempo">${reserva.historial.map((item) => `
    <li><strong>${esc(item.estadoEtiqueta)}</strong><br><span class="texto-suave">${fechaHora(item.creadoEn)}, ${esc(item.usuario || item.actor)}${item.motivo ? `. ${esc(item.motivo)}` : ''}</span></li>`).join('')}</ul>`;
  return `
    <div class="detalle-reserva">
      <p>${insigniaEstado(reserva.estado, reserva.estadoEtiqueta)}</p>
      ${seguimientoReserva(reserva)}
      <div class="detalle-reserva__columnas">
        <section><h3>Cliente</h3><dl class="lista-datos">
          <div class="lista-datos__fila"><dt>Nombre</dt><dd>${esc(reserva.cliente)}</dd></div>
          <div class="lista-datos__fila"><dt>DNI</dt><dd>${esc(reserva.dni)}</dd></div>
          <div class="lista-datos__fila"><dt>Correo</dt><dd>${esc(reserva.correo)}</dd></div>
          ${reserva.telefono ? `<div class="lista-datos__fila"><dt>Teléfono</dt><dd>${esc(reserva.telefono)}</dd></div>` : ''}
        </dl></section>
        <section><h3>Estancia</h3><dl class="lista-datos">
          <div class="lista-datos__fila"><dt>Entrada</dt><dd>${fecha(reserva.fechaEntrada)}</dd></div>
          <div class="lista-datos__fila"><dt>Salida</dt><dd>${fecha(reserva.fechaSalida)}</dd></div>
          <div class="lista-datos__fila"><dt>Noches</dt><dd>${reserva.noches}</dd></div>
          <div class="lista-datos__fila"><dt>Huéspedes</dt><dd>${reserva.cantidadHuespedes}</dd></div>
          <div class="lista-datos__fila"><dt>Registrada</dt><dd>${fechaHora(reserva.creadoEn)}</dd></div>
        </dl></section>
      </div>
      <section><h3>Habitaciones</h3><div class="tabla-envoltura"><table class="tabla">
        <thead><tr><th>Número</th><th>Nombre</th><th>Categoría</th><th class="derecha">Huéspedes</th><th class="derecha">Noches</th><th class="derecha">Por noche</th><th class="derecha">Subtotal</th></tr></thead>
        <tbody>${lineas}</tbody></table></div></section>
      <div class="detalle-reserva__columnas">
        <section><h3>Importes</h3><dl class="lista-datos">
          <div class="lista-datos__fila"><dt>Subtotal</dt><dd class="numero">${moneda(reserva.subtotal)}</dd></div>
          ${Number(reserva.descuento) > 0 ? `<div class="lista-datos__fila"><dt>Descuento</dt><dd class="numero">- ${moneda(reserva.descuento)}</dd></div>` : ''}
          <div class="lista-datos__fila"><dt>IGV</dt><dd class="numero">${moneda(reserva.impuesto)}</dd></div>
          <div class="lista-datos__fila lista-datos__fila--total"><dt>Total</dt><dd class="numero">${moneda(reserva.total)}</dd></div>
        </dl>
        <p class="texto-sm texto-suave">Método de pago: ${esc(reserva.metodoPagoEtiqueta || 'Sin definir')}${reserva.fechaPago ? `, pagado el ${fechaHora(reserva.fechaPago)}` : ''}</p>
        ${reserva.observaciones ? `<p class="texto-sm">Observaciones: ${esc(reserva.observaciones)}</p>` : ''}
        </section>
        <section><h3>Comprobantes</h3>${comprobantes}</section>
      </div>
      <section><h3>Pagos</h3><div class="tabla-envoltura">${pagos}</div></section>
      <section><h3>Historial</h3>${historial}</section>
    </div>`;
}

function pedirPagoPresencial() {
  return new Promise((resolver) => {
    let resultado = null;
    const { modal, cerrar } = abrirModal({
      titulo: 'Registrar pago',
      contenido: `<form class="formulario" data-pago-presencial novalidate>
        <label class="campo"><span class="campo__etiqueta">Método de pago</span>
          <select class="entrada" name="metodo"><option value="EFECTIVO">Efectivo</option><option value="YAPE">Yape</option></select></label>
        <label class="campo"><span class="campo__etiqueta">Número de operación (solo Yape)</span>
          <input class="entrada" name="numeroOperacion" maxlength="40" autocomplete="off"></label>
      </form>`,
      pie: '<button class="boton boton--fantasma" type="button" data-cerrar>Volver</button><button class="boton boton--primario" type="button" data-aceptar>Confirmar pago</button>',
      alCerrar: () => resolver(resultado),
    });
    modal.querySelector('[data-aceptar]').addEventListener('click', () => {
      const formulario = modal.querySelector('[data-pago-presencial]');
      resultado = { metodo: formulario.metodo.value, numeroOperacion: formulario.numeroOperacion.value.trim() || null };
      cerrar();
    });
  });
}

async function ejecutarAccion(accion, reserva, base) {
  const url = `${base}/${encodeURIComponent(reserva.codigo)}`;
  if (accion === 'CONFIRMAR_PAGO') {
    const declarado = reserva.pagos.find((pago) => pago.estado === 'DECLARADO');
    let cuerpo = {};
    if (declarado) {
      const aceptado = await confirmar({
        titulo: 'Confirmar pago',
        mensaje: `Confirma que recibiste ${moneda(reserva.total)} por ${declarado.metodoEtiqueta}.`,
        textoConfirmar: 'Confirmar pago',
      });
      if (!aceptado) {
        return null;
      }
    } else {
      cuerpo = await pedirPagoPresencial();
      if (!cuerpo) {
        return null;
      }
    }
    return api(`${url}/confirmar-pago`, { metodo: 'POST', cuerpo });
  }
  if (accion === 'RECHAZAR_PAGO' || accion === 'CANCELAR') {
    const cancelar = accion === 'CANCELAR';
    const motivo = await pedirMotivo({
      titulo: ETIQUETAS[accion],
      mensaje: cancelar
        ? 'La habitación quedará libre. Si ya se cobró, realiza la devolución por el mismo medio de pago.'
        : 'El cliente podrá volver a registrar su pago.',
      textoConfirmar: ETIQUETAS[accion],
    });
    if (!motivo) {
      return null;
    }
    return api(`${url}/${cancelar ? 'cancelar' : 'rechazar-pago'}`, { metodo: 'POST', cuerpo: { motivo } });
  }
  const rutas = { CONFIRMAR: 'confirmar', INGRESO: 'ingreso', SALIDA: 'salida' };
  return api(`${url}/${rutas[accion]}`, { metodo: 'POST' });
}

export function abrirDetalleReserva(codigo, { alCambiar } = {}) {
  const base = '/api/operaciones/reservas';
  const { cuerpo, modal, cerrar } = abrirModal({
    titulo: `Reserva ${codigo}`,
    ancho: true,
    contenido: '<div class="esqueleto esqueleto--titulo"></div><div class="esqueleto esqueleto--bloque"></div><div class="esqueleto esqueleto--bloque"></div>',
    pie: '<span></span>',
  });
  const pie = modal.querySelector('.modal__pie');

  async function cargar() {
    try {
      const reserva = (await api(`${base}/${encodeURIComponent(codigo)}`)).datos;
      cuerpo.innerHTML = contenidoDetalle(reserva);
      pie.innerHTML = reserva.accionesPermitidas
        .filter((accion) => ETIQUETAS[accion])
        .map((accion) => `<button class="boton ${['CANCELAR', 'RECHAZAR_PAGO'].includes(accion) ? 'boton--peligro' : 'boton--primario'}" type="button" data-accion="${accion}">${ETIQUETAS[accion]}</button>`)
        .join('') || '<span class="texto-sm texto-suave">Esta reserva no tiene acciones pendientes.</span>';
      pie.querySelectorAll('[data-accion]').forEach((boton) => boton.addEventListener('click', () => conCarga(boton, async () => {
        try {
          const respuesta = await ejecutarAccion(boton.dataset.accion, reserva, base);
          if (respuesta) {
            toast(respuesta.mensaje);
            await cargar();
            alCambiar?.();
          }
        } catch (error) {
          mostrarError(error);
        }
      })));
    } catch (error) {
      mostrarError(error);
      cerrar();
    }
  }

  cargar();
}

async function pedirDatosCambio(codigo, estado) {
  if (estado === 'CANCELADO') {
    const motivo = await pedirMotivo({
      titulo: 'Cancelar reserva',
      mensaje: 'La habitación quedará libre. Si ya se cobró, realiza la devolución por el mismo medio de pago.',
      textoConfirmar: 'Cancelar reserva',
    });
    return motivo ? { estado, motivo } : null;
  }
  if (estado === 'PAGADO') {
    const reserva = (await api(`/api/operaciones/reservas/${encodeURIComponent(codigo)}`)).datos;
    const declarado = reserva.pagos.find((pago) => pago.estado === 'DECLARADO');
    if (declarado) {
      const aceptado = await confirmar({
        titulo: 'Marcar como pagado',
        mensaje: `Confirma que recibiste ${moneda(reserva.total)} por ${declarado.metodoEtiqueta}.`,
        textoConfirmar: 'Confirmar pago',
      });
      return aceptado ? { estado } : null;
    }
    const pago = await pedirPagoPresencial();
    return pago ? { estado, ...pago } : null;
  }
  const aceptado = await confirmar({
    titulo: 'Actualizar estado',
    mensaje: `La reserva ${codigo} pasará a "${ETIQUETAS_ESTADO[estado] || estado}". El cliente verá el cambio en su cuenta.`,
    textoConfirmar: 'Actualizar estado',
  });
  return aceptado ? { estado } : null;
}

export async function cambiarEstadoReserva(codigo, estado) {
  const cuerpo = await pedirDatosCambio(codigo, estado);
  if (!cuerpo) {
    return null;
  }
  return api(`/api/operaciones/reservas/${encodeURIComponent(codigo)}/estado`, { metodo: 'PATCH', cuerpo });
}

const selectorEstado = (reserva) => (reserva.estadosSiguientes?.length
  ? `<label class="oculto-visual" for="estado-${esc(reserva.codigo)}">Actualizar estado de ${esc(reserva.codigo)}</label>
    <select class="entrada entrada--compacta" id="estado-${esc(reserva.codigo)}" data-cambio-estado="${esc(reserva.codigo)}">
      <option value="">Cambiar a…</option>
      ${reserva.estadosSiguientes.map((estado) => `<option value="${estado}">${esc(ETIQUETAS_ESTADO[estado] || estado)}</option>`).join('')}
    </select>`
  : '<span class="texto-sm texto-suave">Sin cambios</span>');

export const filaReserva = (reserva, { cambioEstado = false } = {}) => `
  <tr class="fila-accion" data-codigo="${esc(reserva.codigo)}" tabindex="0">
    <td><strong>${esc(reserva.codigo)}</strong></td>
    <td>${esc(reserva.cliente)}<br><span class="texto-suave">DNI ${esc(reserva.dni)}</span></td>
    <td>${esc(reserva.habitaciones)}</td>
    <td>${fecha(reserva.fechaEntrada)}<br><span class="texto-suave">${reserva.noches} ${reserva.noches === 1 ? 'noche' : 'noches'}</span></td>
    <td>${insigniaEstado(reserva.estado, reserva.estadoEtiqueta)}</td>
    <td>${esc(reserva.metodoPagoEtiqueta || 'Sin definir')}</td>
    <td class="derecha">${moneda(reserva.total)}</td>
    ${cambioEstado ? `<td>${selectorEstado(reserva)}</td>` : ''}
  </tr>`;

export const cabeceraReservas = '<thead><tr><th>Reserva</th><th>Cliente</th><th>Habitaciones</th><th>Entrada</th><th>Estado</th><th>Pago</th><th class="derecha">Total</th></tr></thead>';
export const cabeceraReservasConCambio = '<thead><tr><th>Reserva</th><th>Cliente</th><th>Habitaciones</th><th>Entrada</th><th>Estado</th><th>Pago</th><th class="derecha">Total</th><th>Actualizar estado</th></tr></thead>';

export function activarFilas(contenedor, alCambiar) {
  const abrir = (fila) => abrirDetalleReserva(fila.dataset.codigo, { alCambiar });
  contenedor.addEventListener('change', async (evento) => {
    const selector = evento.target.closest('[data-cambio-estado]');
    if (!selector || !selector.value) {
      return;
    }
    const estado = selector.value;
    selector.disabled = true;
    try {
      const respuesta = await cambiarEstadoReserva(selector.dataset.cambioEstado, estado);
      if (respuesta) {
        toast(respuesta.mensaje);
        alCambiar?.();
      }
    } catch (error) {
      mostrarError(error);
    } finally {
      selector.value = '';
      selector.disabled = false;
    }
  });
  contenedor.addEventListener('click', (evento) => {
    if (evento.target.closest('[data-cambio-estado], label')) {
      return;
    }
    const fila = evento.target.closest('[data-codigo]');
    if (fila) {
      abrir(fila);
    }
  });
  contenedor.addEventListener('keydown', (evento) => {
    if (evento.target.closest('[data-cambio-estado]')) {
      return;
    }
    const fila = evento.target.closest('[data-codigo]');
    if (fila && (evento.key === 'Enter' || evento.key === ' ')) {
      evento.preventDefault();
      abrir(fila);
    }
  });
}
