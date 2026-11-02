import { abrirModal, api, confirmar, conCarga, esc, fecha, fechaHora, insigniaEstado, moneda, mostrarError, pedirMotivo, ruta, toast } from './nucleo.js';

const ETIQUETAS = {
  CONFIRMAR_PAGO: 'Confirmar pago',
  RECHAZAR_PAGO: 'Rechazar pago',
  CONFIRMAR: 'Confirmar reserva',
  INGRESO: 'Registrar ingreso',
  SALIDA: 'Registrar salida',
  CANCELAR: 'Cancelar reserva',
};

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

export const filaReserva = (reserva) => `
  <tr class="fila-accion" data-codigo="${esc(reserva.codigo)}" tabindex="0">
    <td><strong>${esc(reserva.codigo)}</strong></td>
    <td>${esc(reserva.cliente)}<br><span class="texto-suave">DNI ${esc(reserva.dni)}</span></td>
    <td>${esc(reserva.habitaciones)}</td>
    <td>${fecha(reserva.fechaEntrada)}<br><span class="texto-suave">${reserva.noches} ${reserva.noches === 1 ? 'noche' : 'noches'}</span></td>
    <td>${insigniaEstado(reserva.estado, reserva.estadoEtiqueta)}</td>
    <td>${esc(reserva.metodoPagoEtiqueta || 'Sin definir')}</td>
    <td class="derecha">${moneda(reserva.total)}</td>
  </tr>`;

export const cabeceraReservas = '<thead><tr><th>Reserva</th><th>Cliente</th><th>Habitaciones</th><th>Entrada</th><th>Estado</th><th>Pago</th><th class="derecha">Total</th></tr></thead>';

export function activarFilas(contenedor, alCambiar) {
  const abrir = (fila) => abrirDetalleReserva(fila.dataset.codigo, { alCambiar });
  contenedor.addEventListener('click', (evento) => {
    const fila = evento.target.closest('[data-codigo]');
    if (fila) {
      abrir(fila);
    }
  });
  contenedor.addEventListener('keydown', (evento) => {
    const fila = evento.target.closest('[data-codigo]');
    if (fila && (evento.key === 'Enter' || evento.key === ' ')) {
      evento.preventDefault();
      abrir(fila);
    }
  });
}
