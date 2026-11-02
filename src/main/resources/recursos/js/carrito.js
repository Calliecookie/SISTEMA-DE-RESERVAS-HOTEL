import {
  actualizarContadorCarrito, api, confirmar, conCarga, datosFormulario, esc, fecha, fechaHora, fechaLocal,
  icono, insigniaEstado, moneda, mostrarError, rol, ruta, toast, urlImagen, vacio,
} from './nucleo.js';

const NOMBRES = ['Selección de habitación', 'Datos de reserva', 'Confirmación', 'Pago', 'Resultado'];
const etapa = document.querySelector('[data-etapa]');
const pasos = document.querySelector('[data-pasos]');
const parametros = new URLSearchParams(location.search);
const estado = { paso: 1, carrito: null, perfil: null, observaciones: '', codigo: parametros.get('reserva'), reserva: null };

function pintarPasos() {
  pasos.innerHTML = NOMBRES.map((nombre, indice) => {
    const numero = indice + 1;
    const clase = numero === estado.paso ? 'pasos__item--actual' : numero < estado.paso ? 'pasos__item--hecho' : '';
    return `<li class="pasos__item ${clase}" ${numero === estado.paso ? 'aria-current="step"' : ''}>
      <span class="pasos__numero">Paso ${numero}</span>${nombre}</li>`;
  }).join('');
}

function irA(paso) {
  estado.paso = paso;
  pintarPasos();
  ({ 1: pasoSeleccion, 2: pasoDatos, 3: pasoConfirmacion, 4: pasoPago, 5: pasoResultado })[paso]();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

const esqueleto = () => `<div class="superficie superficie--relleno">
  <div class="esqueleto esqueleto--titulo"></div><div class="esqueleto esqueleto--bloque"></div><div class="esqueleto esqueleto--linea esqueleto--media"></div></div>`;

function resumenImportes(carrito) {
  return `
    <dl class="lista-datos">
      <div class="lista-datos__fila"><dt>Noches</dt><dd>${carrito.noches}</dd></div>
      <div class="lista-datos__fila"><dt>Subtotal</dt><dd class="numero">${moneda(carrito.subtotal)}</dd></div>
      ${Number(carrito.descuento) > 0 ? `<div class="lista-datos__fila"><dt>Descuento por estadía larga (${Number(carrito.porcentajeDescuento)} %)</dt><dd class="numero">- ${moneda(carrito.descuento)}</dd></div>` : ''}
      <div class="lista-datos__fila"><dt>IGV (${Number(carrito.porcentajeImpuesto)} %)</dt><dd class="numero">${moneda(carrito.impuesto)}</dd></div>
      <div class="lista-datos__fila lista-datos__fila--total"><dt>Total</dt><dd class="numero">${moneda(carrito.total)}</dd></div>
    </dl>`;
}

function aplicarCarrito(respuesta) {
  estado.carrito = respuesta.datos;
  actualizarContadorCarrito(respuesta.datos.cantidadItems);
}

async function recargarCarrito() {
  aplicarCarrito(await api('/api/carrito'));
}

async function cambiar(accion) {
  try {
    aplicarCarrito(await accion());
    pintarSeleccion();
  } catch (error) {
    mostrarError(error);
    await recargarCarrito();
    pintarSeleccion();
  }
}

function itemCarrito(item) {
  return `
    <li class="item-carrito">
      <img class="item-carrito__foto" data-foto src="${esc(urlImagen(item.imagen))}" alt="" width="240" height="180" loading="lazy">
      <div>
        <div class="item-carrito__fila">
          <a class="item-carrito__nombre" href="${ruta(`/habitaciones/${item.habitacionId}`)}">${esc(item.nombre)}</a>
          <span class="numero">${moneda(item.precioNoche)} por noche</span>
        </div>
        <p class="texto-suave texto-sm">${esc(item.categoria)}, habitación ${esc(item.numero)}, hasta ${item.capacidad} ${item.capacidad === 1 ? 'huésped' : 'huéspedes'}</p>
        ${item.aviso ? `<p class="insignia insignia--error">${esc(item.aviso)}</p>` : ''}
        <div class="item-carrito__fila">
          <span class="selector-huespedes" role="group" aria-label="Huéspedes de ${esc(item.nombre)}">
            <button class="boton boton--secundario boton--icono" type="button" data-huespedes="${item.habitacionId}" data-valor="${item.huespedes - 1}" aria-label="Quitar un huésped" ${item.huespedes <= 1 ? 'disabled' : ''}>${icono('menos')}</button>
            <span class="selector-huespedes__valor">${item.huespedes}</span>
            <button class="boton boton--secundario boton--icono" type="button" data-huespedes="${item.habitacionId}" data-valor="${item.huespedes + 1}" aria-label="Agregar un huésped" ${item.huespedes >= item.capacidad ? 'disabled' : ''}>${icono('mas')}</button>
          </span>
          <button class="boton boton--fantasma boton--compacto" type="button" data-quitar="${item.habitacionId}">${icono('basura')}Quitar</button>
        </div>
      </div>
    </li>`;
}

function pintarSeleccion() {
  const carrito = estado.carrito;
  if (!carrito.items.length) {
    etapa.innerHTML = `<div class="superficie">${vacio({
      nombreIcono: 'cama',
      titulo: 'Aún no elegiste una habitación',
      texto: 'Explora el catálogo y añade la habitación que te guste.',
      accion: `<a class="boton boton--acento" href="${ruta('/habitaciones')}">Ver habitaciones</a>`,
    })}</div>`;
    return;
  }
  etapa.innerHTML = `
    <div class="reserva-rejilla">
      <div class="superficie superficie--relleno">
        <form class="formulario__fila formulario__fila--2" data-estancia>
          <label class="campo"><span class="campo__etiqueta">Entrada</span><input class="entrada" type="date" name="fechaEntrada" min="${fechaLocal()}" value="${carrito.fechaEntrada || ''}" required></label>
          <label class="campo"><span class="campo__etiqueta">Salida</span><input class="entrada" type="date" name="fechaSalida" min="${fechaLocal(1)}" value="${carrito.fechaSalida || ''}" required></label>
        </form>
        ${carrito.avisos.map((aviso) => `<p class="aviso aviso--advertencia">${esc(aviso)}</p>`).join('')}
        <ul>${carrito.items.map(itemCarrito).join('')}</ul>
        <button class="boton boton--fantasma boton--compacto" type="button" data-vaciar>${icono('basura')}Vaciar reserva</button>
      </div>
      <aside class="superficie resumen resumen--fijo">
        <h2 class="resumen__titulo">Resumen</h2>
        ${resumenImportes(carrito)}
        ${carrito.fechaEntrada ? '' : '<p class="texto-sm texto-suave">Selecciona tus fechas para ver el total.</p>'}
        <button class="boton boton--acento boton--ancho" type="button" data-continuar ${carrito.listo ? '' : 'disabled'}>Continuar</button>
        <a class="boton boton--secundario boton--ancho" href="${ruta('/habitaciones')}">Añadir otra habitación</a>
      </aside>
    </div>`;
  const estancia = etapa.querySelector('[data-estancia]');
  estancia.addEventListener('change', () => {
    const datos = datosFormulario(estancia);
    if (datos.fechaEntrada && datos.fechaSalida) {
      cambiar(() => api('/api/carrito/estancia', { metodo: 'PUT', cuerpo: datos }));
    }
  });
}

etapa.addEventListener('click', async (evento) => {
  const huespedes = evento.target.closest('[data-huespedes]');
  if (huespedes) {
    cambiar(() => api(`/api/carrito/items/${huespedes.dataset.huespedes}`, { metodo: 'PATCH', cuerpo: { huespedes: Number(huespedes.dataset.valor) } }));
  }
  const quitar = evento.target.closest('[data-quitar]');
  if (quitar) {
    cambiar(() => api(`/api/carrito/items/${quitar.dataset.quitar}`, { metodo: 'DELETE' }));
  }
  if (evento.target.closest('[data-vaciar]')) {
    const aceptado = await confirmar({ titulo: 'Vaciar reserva', mensaje: 'Se quitarán todas las habitaciones elegidas.', textoConfirmar: 'Vaciar reserva', peligro: true });
    if (aceptado) {
      cambiar(() => api('/api/carrito', { metodo: 'DELETE' }));
    }
  }
  if (evento.target.closest('[data-continuar]')) {
    irA(2);
  }
  if (evento.target.closest('[data-volver]')) {
    irA(estado.paso - 1);
  }
});

async function pasoSeleccion() {
  etapa.innerHTML = esqueleto();
  try {
    await recargarCarrito();
    pintarSeleccion();
  } catch (error) {
    mostrarError(error);
  }
}

async function pasoDatos() {
  if (rol !== 'CLIENTE') {
    etapa.innerHTML = `<div class="superficie">${vacio({
      nombreIcono: 'usuario',
      titulo: rol ? 'Las reservas en línea son para clientes' : 'Inicia sesión para continuar',
      texto: rol ? 'Ingresa con una cuenta de cliente para completar la reserva.' : 'Tu reserva se conserva mientras inicias sesión o creas tu cuenta.',
      accion: rol ? '' : `<a class="boton boton--acento" href="${ruta('/ingresar?destino=%2Fcarrito%3Fpaso%3D2')}">Iniciar sesión</a>
                          <a class="boton boton--secundario" href="${ruta('/registro')}">Crear cuenta</a>`,
    })}</div>`;
    return;
  }
  etapa.innerHTML = esqueleto();
  try {
    if (!estado.carrito) {
      await recargarCarrito();
    }
    if (!estado.carrito.listo) {
      irA(1);
      return;
    }
    estado.perfil = (await api('/api/cliente/perfil')).datos;
  } catch (error) {
    mostrarError(error);
    return;
  }
  const perfil = estado.perfil;
  etapa.innerHTML = `
    <div class="reserva-rejilla">
      <form class="superficie superficie--relleno formulario" data-datos>
        <h2 class="resumen__titulo">Titular de la reserva</h2>
        <dl class="lista-datos">
          <div class="lista-datos__fila"><dt>Nombre</dt><dd>${esc(perfil.nombres)} ${esc(perfil.apellidos)}</dd></div>
          <div class="lista-datos__fila"><dt>DNI</dt><dd>${esc(perfil.dni)}</dd></div>
          <div class="lista-datos__fila"><dt>Correo</dt><dd>${esc(perfil.correo)}</dd></div>
        </dl>
        <label class="campo"><span class="campo__etiqueta">Observaciones (opcional)</span>
          <textarea class="entrada" name="observaciones" maxlength="500" placeholder="Hora estimada de llegada, pedidos especiales">${esc(estado.observaciones)}</textarea></label>
      </form>
      <aside class="superficie resumen resumen--fijo">
        <h2 class="resumen__titulo">Resumen</h2>
        ${resumenImportes(estado.carrito)}
        <button class="boton boton--acento boton--ancho" type="button" data-revisar>Revisar reserva</button>
        <button class="boton boton--fantasma boton--ancho" type="button" data-volver>Volver</button>
      </aside>
    </div>`;
  etapa.querySelector('[data-revisar]').addEventListener('click', () => {
    estado.observaciones = etapa.querySelector('[name="observaciones"]').value;
    irA(3);
  });
}

function pasoConfirmacion() {
  const carrito = estado.carrito;
  etapa.innerHTML = `
    <div class="reserva-rejilla">
      <div class="superficie superficie--relleno">
        <h2 class="resumen__titulo">Revisa tu reserva</h2>
        <dl class="lista-datos">
          <div class="lista-datos__fila"><dt>Entrada</dt><dd>${fecha(carrito.fechaEntrada)}</dd></div>
          <div class="lista-datos__fila"><dt>Salida</dt><dd>${fecha(carrito.fechaSalida)}</dd></div>
          <div class="lista-datos__fila"><dt>Huéspedes</dt><dd>${carrito.totalHuespedes}</dd></div>
        </dl>
        <ul>${carrito.items.map((item) => `<li class="item-carrito">
          <img class="item-carrito__foto" data-foto src="${esc(urlImagen(item.imagen))}" alt="" width="240" height="180" loading="lazy">
          <div><p class="item-carrito__nombre">${esc(item.nombre)}</p>
          <p class="texto-suave texto-sm">Habitación ${esc(item.numero)}, ${item.huespedes} ${item.huespedes === 1 ? 'huésped' : 'huéspedes'}, ${moneda(item.subtotal)}</p></div></li>`).join('')}</ul>
        ${estado.observaciones ? `<p class="texto-sm"><strong>Observaciones:</strong> ${esc(estado.observaciones)}</p>` : ''}
      </div>
      <aside class="superficie resumen resumen--fijo">
        <h2 class="resumen__titulo">Total a pagar</h2>
        ${resumenImportes(carrito)}
        <p class="texto-sm texto-suave">Al confirmar reservamos tu habitación mientras completas el pago.</p>
        <button class="boton boton--acento boton--ancho" type="button" data-confirmar>Confirmar reserva</button>
        <button class="boton boton--fantasma boton--ancho" type="button" data-volver>Volver</button>
      </aside>
    </div>`;
  const boton = etapa.querySelector('[data-confirmar]');
  boton.addEventListener('click', () => conCarga(boton, async () => {
    try {
      const respuesta = await api('/api/cliente/reservas', { metodo: 'POST', cuerpo: { observaciones: estado.observaciones } });
      estado.codigo = respuesta.datos.codigo;
      actualizarContadorCarrito(0);
      toast(respuesta.mensaje);
      irA(4);
    } catch (error) {
      mostrarError(error);
      if (error.estado === 409) {
        irA(1);
      }
    }
  }));
}

async function cargarReserva() {
  estado.reserva = (await api(`/api/cliente/reservas/${encodeURIComponent(estado.codigo)}`)).datos;
}

async function pasoPago() {
  etapa.innerHTML = esqueleto();
  let metodos;
  try {
    await cargarReserva();
    if (estado.reserva.estado !== 'PENDIENTE') {
      irA(5);
      return;
    }
    metodos = (await api(`/api/cliente/reservas/${encodeURIComponent(estado.codigo)}/metodos-pago`)).datos;
  } catch (error) {
    mostrarError(error);
    return;
  }
  const opciones = metodos.map((metodo, indice) => `
    <label class="metodo">
      <input type="radio" name="metodo" value="${metodo.metodo}" ${indice === 0 ? 'checked' : ''}>
      <span><span class="metodo__titulo">${esc(metodo.etiqueta)}</span>
      <span class="metodo__texto"><br>${esc(metodo.descripcion)}${metodo.montoExterno ? ` Total a cobrar: ${esc(metodo.monedaExterna)} ${esc(metodo.montoExterno)}.` : ''}</span></span>
    </label>`).join('');
  etapa.innerHTML = `
    <div class="reserva-rejilla">
      <form class="superficie superficie--relleno formulario" data-pago novalidate>
        <h2 class="resumen__titulo">Elige cómo pagar</h2>
        ${parametros.get('pago') === 'cancelado' ? '<p class="aviso aviso--advertencia">Cancelaste el pago en PayPal. Puedes elegir otro método.</p>' : ''}
        <div class="metodos">${opciones}</div>
        <div data-detalle-metodo></div>
      </form>
      <aside class="superficie resumen resumen--fijo">
        <h2 class="resumen__titulo">Reserva ${esc(estado.codigo)}</h2>
        <dl class="lista-datos">
          <div class="lista-datos__fila lista-datos__fila--total"><dt>Total</dt><dd class="numero">${moneda(estado.reserva.total)}</dd></div>
        </dl>
        <p class="texto-sm texto-suave">Paga antes del ${fechaHora(estado.reserva.expiraEn)}. Después la reserva expira.</p>
        <button class="boton boton--acento boton--ancho" type="button" data-pagar>Registrar pago</button>
      </aside>
    </div>`;
  const formulario = etapa.querySelector('[data-pago]');
  const detalle = etapa.querySelector('[data-detalle-metodo]');
  const pagar = etapa.querySelector('[data-pagar]');
  const seleccionado = () => metodos.find((metodo) => metodo.metodo === formulario.elements.metodo.value);

  function pintarDetalle() {
    const metodo = seleccionado();
    pagar.textContent = metodo.redirige ? 'Pagar con PayPal' : 'Registrar pago';
    if (!metodo.requiereNumeroOperacion) {
      detalle.innerHTML = '';
      return;
    }
    detalle.innerHTML = `
      <div class="yape">
        ${metodo.yapeQr ? `<img class="yape__qr" src="${esc(urlImagen(metodo.yapeQr))}" alt="Código QR de Yape" width="176" height="176">` : ''}
        <p>Yapea <strong>${moneda(estado.reserva.total)}</strong> al número <strong>${esc(metodo.yapeNumero)}</strong>, a nombre de ${esc(metodo.yapeTitular)}.</p>
        <div class="campo" data-campo="numeroOperacion">
          <label class="campo__etiqueta" for="numero-operacion">Número de operación</label>
          <input class="entrada" id="numero-operacion" name="numeroOperacion" maxlength="40" autocomplete="off" required>
          <span class="campo__error"></span>
        </div>
      </div>`;
  }

  formulario.addEventListener('change', (evento) => {
    if (evento.target.name === 'metodo') {
      pintarDetalle();
    }
  });
  pintarDetalle();
  pagar.addEventListener('click', () => conCarga(pagar, async () => {
    const metodo = seleccionado();
    try {
      if (metodo.redirige) {
        const respuesta = await api(`/api/cliente/reservas/${encodeURIComponent(estado.codigo)}/paypal`, { metodo: 'POST' });
        location.assign(respuesta.datos.urlAprobacion);
        return;
      }
      const numeroOperacion = formulario.elements.numeroOperacion?.value.trim() || null;
      const respuesta = await api(`/api/cliente/reservas/${encodeURIComponent(estado.codigo)}/pago`, {
        metodo: 'POST',
        cuerpo: { metodo: metodo.metodo, numeroOperacion },
      });
      toast(respuesta.mensaje);
      irA(5);
    } catch (error) {
      mostrarError(error);
    }
  }));
}

async function pasoResultado() {
  etapa.innerHTML = esqueleto();
  try {
    await cargarReserva();
  } catch (error) {
    mostrarError(error);
    return;
  }
  const reserva = estado.reserva;
  const pagoFallido = parametros.get('pago') === 'fallido';
  const pagado = !['PENDIENTE', 'CANCELADO', 'EXPIRADO'].includes(reserva.estado);
  const declarado = reserva.pagos.some((pago) => pago.estado === 'DECLARADO');
  const comprobante = reserva.comprobantes[0];
  const titulo = pagoFallido ? 'No se completó el pago' : pagado ? 'Tu pago está confirmado' : declarado ? 'Estamos verificando tu pago' : 'Tu reserva está registrada';
  const texto = pagoFallido
    ? 'PayPal no confirmó el cobro. Tu reserva sigue pendiente y puedes intentarlo otra vez.'
    : pagado ? 'Te enviamos el comprobante actualizado a tu correo.'
      : declarado ? 'Te avisaremos por correo cuando el hotel confirme tu pago.'
        : `Completa el pago antes del ${fechaHora(reserva.expiraEn)}.`;
  etapa.innerHTML = `
    <div class="superficie resultado">
      <div class="resultado__icono">${icono(pagoFallido ? 'alerta' : 'check-circulo')}</div>
      <h2 class="resultado__titulo">${esc(titulo)}</h2>
      <p class="resultado__codigo">${esc(reserva.codigo)}</p>
      <p>${insigniaEstado(reserva.estado, reserva.estadoEtiqueta)}</p>
      <p class="texto-suave">${esc(texto)}</p>
      <div class="resultado__acciones">
        ${pagoFallido ? `<a class="boton boton--acento" href="${ruta(`/carrito?reserva=${encodeURIComponent(reserva.codigo)}&paso=4`)}">Intentar de nuevo</a>` : ''}
        ${comprobante ? `<a class="boton boton--primario" href="${ruta(comprobante.url)}" target="_blank" rel="noopener">${icono('documento')}Ver comprobante</a>` : ''}
        <a class="boton boton--secundario" href="${ruta(`/mi-cuenta/reservas/${encodeURIComponent(reserva.codigo)}`)}">Ver mi reserva</a>
      </div>
    </div>`;
}

function iniciar() {
  const paso = Number(parametros.get('paso')) || 1;
  if (estado.codigo && paso >= 4) {
    irA(paso);
    return;
  }
  irA(paso === 2 ? 2 : 1);
}

iniciar();
